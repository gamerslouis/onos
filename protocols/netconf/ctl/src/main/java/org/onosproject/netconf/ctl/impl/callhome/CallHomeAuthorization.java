/*
 * Copyright 2022-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.netconf.ctl.impl.callhome;

import static java.util.Objects.requireNonNull;

import org.onosproject.netconf.callhome.NetconfCallHomeDeviceConfig;

/**
 * Authorization context for incoming call home sessions.
 *
 * @see CallHomeAuthorizationProvider
 */
public abstract class CallHomeAuthorization {
    private static final CallHomeAuthorization REJECTED = new CallHomeAuthorization() {

        @Override
        public boolean isServerAllowed() {
            return false;
        }

        @Override
        protected String getSessionName() {
            return "";
        }

        @Override
        protected NetconfCallHomeDeviceConfig getConfig() {
            throw new IllegalStateException("Server is not allowed.");
        }
    };

    /**
     * Returns CallHomeAuthorization object with intent to
     * reject incoming connection.
     *
     * <p>
     * {@link CallHomeAuthorizationProvider} may use returned object
     * as return value for
     * {@link CallHomeAuthorizationProvider#provideAuth(java.net.SocketAddress, java.security.PublicKey)}
     * if the incoming session should be rejected due to policy implemented
     * by provider.
     *
     * @return CallHomeAuthorization with {@code isServerAllowed() == false}
     */
    public static CallHomeAuthorization rejected() {
        return REJECTED;
    }

    /**
     * Creates a builder for CallHomeAuthorization with intent
     * to accept incoming connection and to provide credentials.
     *
     * <p>
     * Note: If session with same sessionName is already opened and
     * active, incoming session will be rejected.
     *
     * @param sessionName Application specific unique identifier for incoming session
     * @param cfg    NetconfCallHomeDeviceConfig for incoming session
     * @return CallHomeAuthorization with {@code isServerAllowed() == true}
     */
    public static CallHomeAuthorization serverAccepted(final String sessionName, NetconfCallHomeDeviceConfig cfg) {
        return new ServerAllowed(sessionName, cfg);
    }

    /**
     * Returns true if incomming connection is allowed.
     *
     * @return true if incoming connection from SSH Server is allowed.
     */
    public abstract boolean isServerAllowed();

    /**
     * get NetconfCallHomeDeviceConfig for incoming session
     */
    protected abstract NetconfCallHomeDeviceConfig getConfig();

    /**
     * Returns session name or device id for incoming session.
     *
     * @return session name
     */
    protected abstract String getSessionName();

    private static class ServerAllowed extends CallHomeAuthorization {

        private final String sessionId;
        private final NetconfCallHomeDeviceConfig cfg;

        ServerAllowed(String sessionId, NetconfCallHomeDeviceConfig cfg) {
            this.sessionId = requireNonNull(sessionId);
            this.cfg = requireNonNull(cfg);
        }

        @Override
        protected String getSessionName() {
            return sessionId;
        }

        @Override
        public boolean isServerAllowed() {
            return true;
        }

        @Override
        protected NetconfCallHomeDeviceConfig getConfig() {
            return cfg;
        }
    }
}

