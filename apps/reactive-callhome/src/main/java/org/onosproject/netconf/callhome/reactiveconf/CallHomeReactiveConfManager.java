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

import com.google.common.base.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.Ip4Address;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.netconf.callhome.CallHomeConfigBuilder;
import org.onosproject.netconf.callhome.NetconfCallHomeController;
import org.onosproject.netconf.callhome.NetconfCallHomeDeviceConfig;
import org.onosproject.netconf.callhome.NetconfCallHomeSSHListener;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.Dictionary;

import static org.onosproject.netconf.callhome.reactiveconf.OsgiPropertyConstants.*;


@Component(immediate = true,
        property = {
                SSH_USERNAME + ":String=" + SSH_USERNAME_DEFAULT,
                SSH_PASSWORD + ":String=" + SSH_PASSWORD_DEFAULT,
                SSH_CLIENT_KEY + ":String=" + SSH_CLIENT_KEY_DEFAULT,
                CONNECT_TIMEOUT + ":Integer=" + CONNECT_TIMEOUT_DEFAULT,
                REPLY_TIMEOUT + ":Integer=" + REPLY_TIMEOUT_DEFAULT,
                IDLE_TIMEOUT + ":Integer=" + IDLE_TIMEOUT_DEFAULT,
                DEVICE_DRIVER + ":String=" + DEVICE_DRIVER_DEFAULT,
        }
)
public class CallHomeReactiveConfManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    NetconfCallHomeController netconfCallHomeController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private ApplicationId appId;

    private AtomicCounter counter;

    private String sshUsername = SSH_USERNAME_DEFAULT;
    private String sshPassword = SSH_PASSWORD_DEFAULT;
    private String sshClientKey = SSH_CLIENT_KEY_DEFAULT;
    private int connectTimeout = CONNECT_TIMEOUT_DEFAULT;
    private int replyTimeout = REPLY_TIMEOUT_DEFAULT;
    private int idleTimeout = IDLE_TIMEOUT_DEFAULT;
    private String deviceDriver = DEVICE_DRIVER_DEFAULT;

    @Activate
    protected void activate(ComponentContext context) {
        appId = coreService.registerApplication("org.onosproject.netconf.callhome.reactiveconf");
        cfgService.registerProperties(getClass());
        netconfCallHomeController.addListener(callHomeSSHListener);
        counter = storageService.getAtomicCounter("netconf-callhome-reactiveconf-counter");
        log.info("Started", appId.id());
        modified(context);
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        netconfCallHomeController.removeListener(callHomeSSHListener);
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        sshUsername = Tools.get(properties, SSH_USERNAME);
        sshPassword = Tools.get(properties, SSH_PASSWORD);
        sshClientKey = Tools.get(properties, SSH_CLIENT_KEY);
        connectTimeout = Tools.getIntegerProperty(properties, CONNECT_TIMEOUT, CONNECT_TIMEOUT_DEFAULT);
        replyTimeout = Tools.getIntegerProperty(properties, REPLY_TIMEOUT, REPLY_TIMEOUT_DEFAULT);
        idleTimeout = Tools.getIntegerProperty(properties, IDLE_TIMEOUT, IDLE_TIMEOUT_DEFAULT);
        deviceDriver = Tools.get(properties, DEVICE_DRIVER);
    }

    NetconfCallHomeSSHListener callHomeSSHListener = new NetconfCallHomeSSHListener() {
        @Override
        public CallHomeConfigBuilder sessionAuthFailed(PublicKey publicKey, InetSocketAddress address) {
            if (Strings.isNullOrEmpty(sshUsername) || (Strings.isNullOrEmpty(sshPassword) && Strings.isNullOrEmpty(sshClientKey))) {
                log.warn("No SSH credentials configured, reactive call home disabled");
                return null;
            }

            String driver = Strings.isNullOrEmpty(deviceDriver) ? DEVICE_DRIVER_DEFAULT : deviceDriver;

            Pair<Ip4Address, Integer> pair = generateNetconfCallHomeDeviceId();
            CallHomeConfigBuilder builder = CallHomeConfigBuilder.builder(pair.getLeft(),
                                                                          pair.getRight(),
                                                                          netconfCallHomeController.encodePublicKey(publicKey),
                                                                          sshUsername,
                                                                          driver
            );
            if (!Strings.isNullOrEmpty(sshPassword))
                builder.setPassword(sshPassword);
            if (!Strings.isNullOrEmpty(sshClientKey))
                builder.setSshKey(sshClientKey);
            if (connectTimeout > 0)
                builder.setConnectTimeout(connectTimeout);
            if (replyTimeout > 0)
                builder.setReplyTimeout(replyTimeout);
            if (idleTimeout > 0)
                builder.setIdleTimeout(idleTimeout);

            return builder;
        }
    };

    // generate new netconf call home device id
    private Pair<Ip4Address, Integer> generateNetconfCallHomeDeviceId() {
        while (true) {
            Pair<Ip4Address, Integer> deviceIdPair = nextDeviceIdPair();
            DeviceId deviceId = DeviceId.deviceId(
                    "netconf" +
                            deviceIdPair.getLeft().toString() +
                            ":" + deviceIdPair.getRight());

            if (networkConfigService.getConfig(deviceId, NetconfCallHomeDeviceConfig.class) == null) {
                return deviceIdPair;
            }
        }
    }

    private Pair<Ip4Address, Integer> nextDeviceIdPair() {
        long id = counter.incrementAndGet();
        long deviceId_port = id % 255;
        long deviceId_ip = id / 255;

        return Pair.of(Ip4Address.valueOf((int) deviceId_ip), (int) deviceId_port);
    }


}
