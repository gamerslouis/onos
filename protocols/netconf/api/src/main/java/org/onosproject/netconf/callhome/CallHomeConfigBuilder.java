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

package org.onosproject.netconf.callhome;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

public class CallHomeConfigBuilder {
    private String ip;
    private Integer port;
    private String path = null;
    private String serverKey;
    private String username;
    private String password = null;
    private String sshKey = null;

    private String driver;

    private Integer connectTimeout = null;
    private Integer replyTimeout = null;
    private Integer idleTimeout = null;

    private CallHomeConfigBuilder(IpAddress fakeIp, Integer fakePort, String serverKey, String username, String driver) {
        checkNotNull(fakeIp, "ip address cannot be null");
        checkNotNull(fakePort, "port cannot be null");
        checkNotNull(serverKey, "server key cannot be null");
        checkNotNull(username, "username cannot be null");
        checkNotNull(driver, "driver cannot be null");

        ip = fakeIp.toString();
        port = fakePort;
        this.serverKey = serverKey;
        this.username = username;
        this.driver = driver;
    }

    public static CallHomeConfigBuilder builder(IpAddress fakeIp, Integer fakePort, String serverKey, String username, String driver) {
        return new CallHomeConfigBuilder(fakeIp, fakePort, serverKey, username, driver);
    }

    public String getIp() {
        return ip;
    }

    public CallHomeConfigBuilder setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public CallHomeConfigBuilder setPort(Integer port) {
        this.port = port;
        return this;
    }

    public String getPath() {
        return path;
    }

    public CallHomeConfigBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    public String getServerKey() {
        return serverKey;
    }

    public CallHomeConfigBuilder setServerKey(String serverKey) {
        this.serverKey = serverKey;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public CallHomeConfigBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public CallHomeConfigBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getSshKey() {
        return sshKey;
    }

    public CallHomeConfigBuilder setSshKey(String sshKey) {
        this.sshKey = sshKey;
        return this;
    }

    public String getDriver() {
        return driver;
    }

    public CallHomeConfigBuilder setDriver(String driver) {
        this.driver = driver;
        return this;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public CallHomeConfigBuilder setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public Integer getReplyTimeout() {
        return replyTimeout;
    }

    public CallHomeConfigBuilder setReplyTimeout(Integer replyTimeout) {
        this.replyTimeout = replyTimeout;
        return this;
    }

    public Integer getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Integer idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public Triple<DeviceId, JsonNode, JsonNode> build() {
        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        ObjectNode conf = jsonNodeFactory.objectNode();
        conf.put(NetconfCallHomeDeviceConfig.SERVER_KEY, serverKey);
        conf.put(NetconfCallHomeDeviceConfig.USERNAME, username);

        if (connectTimeout != null) {
            conf.put(NetconfCallHomeDeviceConfig.CONNECT_TIMEOUT, connectTimeout);
        }
        if (replyTimeout != null) {
            conf.put(NetconfCallHomeDeviceConfig.REPLY_TIMEOUT, replyTimeout);
        }
        if (idleTimeout != null) {
            conf.put(NetconfCallHomeDeviceConfig.IDLE_TIMEOUT, idleTimeout);
        }
        if (!Strings.isNullOrEmpty(password)) {
            conf.put(NetconfCallHomeDeviceConfig.PASSWORD, password);
        }
        if (!Strings.isNullOrEmpty(sshKey)) {
            conf.put(NetconfCallHomeDeviceConfig.SSHKEY, sshKey);
        }
        if (!Strings.isNullOrEmpty(path)) {
            conf.put(NetconfCallHomeDeviceConfig.PATH, path);
        }
        String id = "netconf:" + ip;
        if (port != null) {
            id += ":" + port;
        }
        if (!Strings.isNullOrEmpty(path)) {
            id += "/" + path;
        }

        ObjectNode basic = jsonNodeFactory.objectNode();
        basic.put("driver", driver);

        return Triple.of(
                DeviceId.deviceId(id),
                conf,
                basic
        );
    }
}
