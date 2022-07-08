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

package org.onosproject.netconf.callhome.reactiveconf;

public class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    static final String SSH_USERNAME = "sshUsername";
    static final String SSH_USERNAME_DEFAULT = "netconf";

    static final String SSH_PASSWORD = "sshPassword";
    static final String SSH_PASSWORD_DEFAULT = "";

    static final String SSH_CLIENT_KEY = "sshClientKey";
    static final String SSH_CLIENT_KEY_DEFAULT = "";

    static final String CONNECT_TIMEOUT = "connectTimeout";
    static final int CONNECT_TIMEOUT_DEFAULT = -1;

    static final String REPLY_TIMEOUT = "replyTimeout";
    static final int REPLY_TIMEOUT_DEFAULT = -1;

    static final String IDLE_TIMEOUT = "idleTimeout";
    static final int IDLE_TIMEOUT_DEFAULT = -1;

    static final String DEVICE_DRIVER = "deviceDriver";
    static final String DEVICE_DRIVER_DEFAULT = "netconf";
}
