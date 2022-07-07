package org.onosproject.netconf.ctl.impl.callhome;

import org.apache.sshd.client.session.ClientSession;
import org.onosproject.netconf.callhome.CallHomeSSHSession;

import java.net.SocketAddress;
import java.security.PublicKey;

public interface CallHomeSSHSessionFactory {
    CallHomeSSHSession createIfNotExists(
            final ClientSession sshSession, final CallHomeAuthorization authorization,
            final SocketAddress remoteAddress, final PublicKey serverKey);

    void onSessionAuthComplete(CallHomeSSHSession context);

    void remove(final CallHomeSSHSession session);
}

