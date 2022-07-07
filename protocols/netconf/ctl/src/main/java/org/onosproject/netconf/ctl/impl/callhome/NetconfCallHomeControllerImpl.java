package org.onosproject.netconf.ctl.impl.callhome;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceService;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.callhome.CallHomeConfigBuilder;
import org.onosproject.netconf.callhome.CallHomeSSHSession;
import org.onosproject.netconf.callhome.NetconfCallHomeController;
import org.onosproject.netconf.callhome.NetconfCallHomeDeviceConfig;
import org.onosproject.netconf.callhome.NetconfCallHomeSSHListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.getIntegerProperty;
import static org.onosproject.netconf.NetconfDeviceInfo.extractIpPortPath;
import static org.onosproject.netconf.ctl.impl.callhome.OsgiPropertyConstants.CALL_HOME_SSH_SERVER_PORT;
import static org.onosproject.netconf.ctl.impl.callhome.OsgiPropertyConstants.DEFAULT_CALL_HOME_SSH_SERVER_PORT;

@Component(immediate = true, service = NetconfCallHomeController.class,
        property = {
                CALL_HOME_SSH_SERVER_PORT + ":Integer=" + DEFAULT_CALL_HOME_SSH_SERVER_PORT
        })
public class NetconfCallHomeControllerImpl implements NetconfCallHomeController {
    private static final Logger log = LoggerFactory.getLogger(NetconfCallHomeControllerImpl.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentCfgService;

    protected final ConfigFactory<DeviceId, NetconfCallHomeDeviceConfig> configFactory =
            // TODO consider moving Config registration to NETCONF ctl bundle
            new ConfigFactory<>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    NetconfCallHomeDeviceConfig.class, NetconfCallHomeDeviceConfig.CONFIG_KEY) {
                @Override
                public NetconfCallHomeDeviceConfig createConfig() {
                    return new NetconfCallHomeDeviceConfig();
                }
            };

    protected static int callHomeSshServerPort = DEFAULT_CALL_HOME_SSH_SERVER_PORT;

    private final ConcurrentMap<DeviceId, CallHomeSSHSession> sessions = new ConcurrentHashMap<>();
    protected Set<NetconfCallHomeSSHListener> listeners = new CopyOnWriteArraySet<>();

    private boolean isStarted = false;
    private ReverseSSHServer sshServer;

    @Activate
    protected void activate(ComponentContext context) {
        componentCfgService.registerProperties(getClass());
        cfgService.registerConfigFactory(configFactory);
        cfgService.addListener(cfgListener);

        modified(context);

        startSSHServer();
        log.info("NetconfCallHomeController started");
    }

    @Deactivate
    protected void deactivate() {
        stopSSHServer();

        listeners.clear();
        cfgService.addListener(cfgListener);
        cfgService.unregisterConfigFactory(configFactory);
        componentCfgService.unregisterProperties(getClass(), false);
        listeners.clear();
        sessions.clear();
        log.info("NetconfCallHomeController stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            callHomeSshServerPort = DEFAULT_CALL_HOME_SSH_SERVER_PORT;
            return;
        }
        Dictionary<?, ?> properties = context.getProperties();
        int newSshServerPort = getIntegerProperty(
                properties, CALL_HOME_SSH_SERVER_PORT, callHomeSshServerPort);
        if (newSshServerPort <= 0 || newSshServerPort >= 65536) {
            log.warn("Invalid call home ssh server port {}", newSshServerPort);
            return;
        }
        callHomeSshServerPort = newSshServerPort;
        if (isStarted) {
            stopSSHServer();
            startSSHServer();
        }
    }

    public void startSSHServer() {
        synchronized (this) {
            if (isStarted) {
                return;
            }
            isStarted = true;
            try {
                sshServer = new ReverseSSHServer(
                        authenticationProvider, sessionFactory,
                        new InetSocketAddress(callHomeSshServerPort));
                sshServer.bind();
                log.info("netconf call home ssh server started");
            } catch (IOException e) {
                log.error("netconf call home ssh server binding fail");
                isStarted = false;
            }
        }
    }

    public void stopSSHServer() {
        synchronized (this) {
            if (!isStarted) {
                return;
            }
            isStarted = false;
            sshServer.close();
            sshServer = null;
        }
    }

    @Override
    public NetconfSession createNetconfSession(NetconfDeviceInfo netconfDeviceInfo) throws NetconfException {
        log.debug("Try to create netconf session for {}", netconfDeviceInfo.getDeviceId().toString());
        CallHomeSSHSession session = sessions.get(netconfDeviceInfo.getDeviceId());
        if (session == null) {
            log.warn("Create netconf session for {} fail", netconfDeviceInfo.getDeviceId().toString());
            throw new NetconfException("No call home ssh session for device " + netconfDeviceInfo.getDeviceId());
        }
        return session.getNetconfSession(netconfDeviceInfo);
    }

    @Override
    public NetconfDeviceInfo createDeviceInfo(DeviceId deviceId, boolean isMaster) throws NetconfException {
        if (isMaster) {
            CallHomeSSHSession session = sessions.get(deviceId);
            if (session == null) {
                log.warn("Create device info for {} fail", deviceId.toString());
                throw new NetconfException("No call home ssh session for device " + deviceId);
            }
            InetSocketAddress remoteAddress = session.getRemoteAddress();
            NetconfCallHomeDeviceConfig cfg = session.getConfig();

            NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo(deviceId,
                                                                 session.getSSHUsername(),
                                                                 cfg.password(),
                                                                 IpAddress.valueOf(remoteAddress.getAddress()),
                                                                 remoteAddress.getPort(),
                                                                 cfg.path().orElse(null),
                                                                 cfg.sshKey());

            deviceInfo.setIdleTimeoutSec(cfg.idleTimeout());
            deviceInfo.setConnectTimeoutSec(cfg.connectTimeout());
            deviceInfo.setReplyTimeoutSec(cfg.replyTimeout());
            return deviceInfo;

        } else {
            NetconfCallHomeDeviceConfig cfg = cfgService.getConfig(deviceId, NetconfCallHomeDeviceConfig.class);
            Device device = deviceService.getDevice(deviceId);
            if (device == null || cfg == null) {
                throw new NetconfException("Create device info on slave fail. No device or config for device " + deviceId);
            }
            NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo(deviceId,
                                         cfg.username(),
                                         cfg.password(),
                                         IpAddress.valueOf(device.annotations().value("ipaddress")),
                                         Integer.parseInt(device.annotations().value("port")),
                                         cfg.path().orElse(null),
                                         cfg.sshKey());
            deviceInfo.setIdleTimeoutSec(cfg.idleTimeout());
            deviceInfo.setConnectTimeoutSec(cfg.connectTimeout());
            deviceInfo.setReplyTimeoutSec(cfg.replyTimeout());
            return deviceInfo;
        }
    }

    @Override
    public void removeSession(DeviceId deviceId) {
        if (sessions.containsKey(deviceId)) {
            sessions.get(deviceId).terminate();
        }
    }

    @Override
    public Map<DeviceId, CallHomeSSHSession> getSessionMap() {
        return sessions;
    }

    @Override
    public boolean isCallHomeDeviceId(DeviceId deviceId) {
        if (deviceId.toString().startsWith("netconf")) {
            Triple<String, Integer, Optional<String>> info = extractIpPortPath(deviceId);
            return IpAddress.valueOf(info.getLeft()).toOctets()[0] == 0;
        }
        return false;
    }

    @Override
    public PublicKey decodePublicKeyString(String key) {
        try {
            AuthorizedKeyEntry entry = AuthorizedKeyEntry.parseAuthorizedKeyEntry(key);
            return entry.resolvePublicKey(PublicKeyEntryResolver.IGNORING);
        } catch (IOException | GeneralSecurityException e) {
            log.warn("Decode ssh server key fail", e);
            return null;
        }
    }

    @Override
    public void addListener(NetconfCallHomeSSHListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(NetconfCallHomeSSHListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void registerDevice(CallHomeConfigBuilder builder) {
        Pair<DeviceId, JsonNode> pair = builder.build();
        cfgService.applyConfig("devices", pair.getLeft(),
                               "netconf-ch", pair.getRight()
        );
    }

    @Override
    public void unregisterDevice(DeviceId deviceId) {
        cfgService.removeConfig(deviceId, NetconfCallHomeDeviceConfig.class);
    }

    CallHomeSSHSessionFactory sessionFactory = new CallHomeSSHSessionFactory() {
        @Override
        public void remove(final CallHomeSSHSession session) {
            String sessionId = session.getSessionId();
            log.debug("Remove session {}", sessionId);
            cfgService.removeConfig("devices", DeviceId.deviceId(sessionId), "netconf");
            sessions.remove(DeviceId.deviceId(session.getSessionId()), session);
            for (NetconfCallHomeSSHListener l : listeners) {
                l.sessionRemoved(session);
            }
        }

        @Override
        public CallHomeSSHSession createIfNotExists(final ClientSession sshSession,
                                                    final CallHomeAuthorization authorization, final SocketAddress remoteAddress,
                                                    final PublicKey serverKey) {
            log.debug("Try to create session for {}", authorization.getSessionName());
            CallHomeSSHSession session = new CallHomeSSHSessionImpl(sshSession, authorization, serverKey);
            CallHomeSSHSession preexisting = sessions.putIfAbsent(DeviceId.deviceId(session.getSessionId()), session);
            if (preexisting != null) {
                log.debug("Session for {} already existed", authorization.getSessionName());
            }
            // If preexisting is null - session does not exist, so we can safely create new
            // one, otherwise we return
            // null and incoming connection will be rejected.
            return preexisting == null ? session : null;
        }

        @Override
        public void onSessionAuthComplete(CallHomeSSHSession context) {
            String sessionId = context.getSessionId();
            log.debug("Auth for {} completed, start to trigger device discovery.", sessionId);

            NetconfCallHomeDeviceConfig config = cfgService.getConfig(DeviceId.deviceId(sessionId), NetconfCallHomeDeviceConfig.class);
            if (config == null) {
                log.error("Network cfg for {} not found, cancel device discovery", sessionId);
                return;
            }

            JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
            ObjectNode conf = jsonNodeFactory.objectNode();
            conf.put("username", context.getSSHUsername());
            conf.put("ip", context.getRemoteAddress().getAddress().getHostAddress());
            conf.put("port", context.getRemoteAddress().getPort());

            if (config.connectTimeout().isPresent()) {
                conf.put(NetconfCallHomeDeviceConfig.CONNECT_TIMEOUT, config.connectTimeout().getAsInt());
            }
            if (config.replyTimeout().isPresent()) {
                conf.put(NetconfCallHomeDeviceConfig.REPLY_TIMEOUT, config.replyTimeout().getAsInt());
            }
            if (config.idleTimeout().isPresent()) {
                conf.put(NetconfCallHomeDeviceConfig.IDLE_TIMEOUT, config.idleTimeout().getAsInt());
            }
            if (!Objects.equals(config.password(), "")) {
                conf.put(NetconfCallHomeDeviceConfig.PASSWORD, config.password());
            }
            if (!Objects.equals(config.sshKey(), "")) {
                conf.put(NetconfCallHomeDeviceConfig.SSHKEY, config.sshKey());
            }
            if (config.path().isPresent()) {
                conf.put(NetconfCallHomeDeviceConfig.PATH, config.path().get());
            }
            cfgService.applyConfig("devices", DeviceId.deviceId(sessionId), "netconf", conf);

            for (NetconfCallHomeSSHListener l : listeners) {
                l.sessionCreated(context);
            }
        }
    };

    private final CallHomeAuthorizationProvider authenticationProvider = new CallHomeAuthorizationProvider() {
        @Override
        public CallHomeAuthorization provideAuth(SocketAddress remoteAddress, PublicKey serverKey) {
            List<DeviceId> subjects = new ArrayList<>(cfgService.getSubjects(DeviceId.class));
            List<NetconfCallHomeDeviceConfig> cfgs = subjects.stream()
                    .map(d -> cfgService.getConfig(d, NetconfCallHomeDeviceConfig.class))
                    .filter(cfg -> cfg != null && serverKey.equals(decodePublicKeyString(cfg.serverKey())))
                    .collect(Collectors.toList());
            if (cfgs.size() == 0) {
                for (NetconfCallHomeSSHListener l : listeners) {
                    CallHomeConfigBuilder builder = l.sessionAuthFailed(serverKey, (InetSocketAddress) remoteAddress);
                    if (builder != null) {
                        Pair<DeviceId, JsonNode> newConfig = builder.build();
                        NetconfCallHomeDeviceConfig cfg = cfgService.applyConfig("devices", newConfig.getLeft(),
                                                                                               "netconf-ch", newConfig.getRight()
                        );
                        return CallHomeAuthorization.serverAccepted(cfg.subject().toString(), cfg);
                    }
                }
                return CallHomeAuthorization.rejected();
            } else {
                NetconfCallHomeDeviceConfig cfg = cfgs.get(0);
                log.debug("Authorization match for {}", cfg.subject().toString());
                return CallHomeAuthorization.serverAccepted(cfg.subject().toString(), cfg);
            }
        }
    };

    protected final NetworkConfigListener cfgListener = new NetworkConfigListener() {
        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
                case CONFIG_UPDATED:
                case CONFIG_REMOVED:
                    // Let session reconnect to reload config
                    DeviceId deviceId = (DeviceId) event.subject();
                    removeSession(deviceId);
                    break;

                default:
                    break;
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(NetconfCallHomeDeviceConfig.class);
        }
    };
}

