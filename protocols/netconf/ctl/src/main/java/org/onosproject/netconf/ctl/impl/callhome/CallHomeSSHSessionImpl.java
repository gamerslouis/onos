/*
 * Copyright (c) 2016 Brocade Communication Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.onosproject.netconf.ctl.impl.callhome;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.Objects;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.client.future.AuthFuture;

import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.callhome.CallHomeSSHSession;

import org.onosproject.netconf.callhome.NetconfCallHomeDeviceConfig;
import org.onosproject.netconf.ctl.impl.NetconfControllerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.security.KeyPair;
import java.io.CharArrayReader;
import java.util.concurrent.TimeUnit;

class CallHomeSSHSessionImpl implements CallHomeSSHSession {

    private static final Logger LOG = LoggerFactory.getLogger(CallHomeSSHSessionImpl.class);
    static final Session.AttributeKey<CallHomeSSHSessionImpl> SESSION_KEY = new Session.AttributeKey<>();

    private final ClientSession sshSession;
    private final CallHomeAuthorization authorization;

    private final InetSocketAddress remoteAddress;
    private final PublicKey serverKey;

    private Boolean netconfCreated = false;
    private NetconfSession netconfSession;

    CallHomeSSHSessionImpl(final ClientSession sshSession, final CallHomeAuthorization authorization,
                           final PublicKey serverKey) {
        this.authorization = requireNonNull(authorization, "authorization");
        checkArgument(this.authorization.isServerAllowed(), "Server was not allowed.");
        this.sshSession = requireNonNull(sshSession, "sshSession");
        this.sshSession.setAttribute(SESSION_KEY, this);
        this.remoteAddress = (InetSocketAddress) this.sshSession.getIoSession().getRemoteAddress();
        this.serverKey = serverKey;
    }

    static CallHomeSSHSessionImpl getFrom(final ClientSession sshSession) {
        return sshSession.getAttribute(SESSION_KEY);
    }

    AuthFuture authorize() throws IOException {
        sshSession.setUsername(authorization.getConfig().username());
        String password = authorization.getConfig().password();
        if (!Objects.equals(password, "")) {
            sshSession.addPasswordIdentity(password);
        }
        String clientKey = authorization.getConfig().sshKey();
        if (!Objects.equals(clientKey, "")) {
            sshSession.addPublicKeyIdentity(decodeKeyPair(clientKey));
        }
        AuthFuture authFuture = sshSession.auth();
        int connectTimeout = getConfig().connectTimeout().orElse(
                NetconfControllerImpl.getNetconfConnectTimeout()
        );
        authFuture.verify(connectTimeout, TimeUnit.SECONDS);

        return authFuture;
    }

    private KeyPair decodeKeyPair(String key) {
        try (PEMParser pemParser = new PEMParser(new CharArrayReader(key.toCharArray()))) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            return converter.getKeyPair((PEMKeyPair) pemParser.readObject());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public NetconfSession getNetconfSession(NetconfDeviceInfo deviceInfo) {
        synchronized (this) {
            try {
                if (!netconfCreated) {
                    netconfCreated = true;
                    netconfSession = new NetconfSessionMinaCallhomeImpl(deviceInfo, sshSession);
                }
                return netconfSession;
            } catch (NetconfException e) {
                LOG.error("Create call home netconf session fail", e);
                return null;
            }
        }
    }

    @Override
    public void terminate() {
        sshSession.close(false);
    }

    @Override
    public void terminate(boolean immediately) {
        sshSession.close(false);
    }

    @Override
    public PublicKey getRemoteServerKey() {
        return serverKey;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public String getSessionId() {
        return authorization.getSessionName();
    }

    @Override
    public String getSSHUsername() {
        return sshSession.getUsername();
    }

    @Override
    public NetconfCallHomeDeviceConfig getConfig() {
        return authorization.getConfig();
    }
}

