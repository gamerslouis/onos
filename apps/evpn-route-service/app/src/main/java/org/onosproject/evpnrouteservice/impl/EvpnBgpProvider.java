package org.onosproject.evpnrouteservice.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Arrays;
import java.util.ArrayList;

import javax.crypto.Mac;

import com.esotericsoftware.asm.Type;

import com.google.common.collect.ImmutableList;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.evpnrouteservice.EvpnRoute;
import org.onosproject.evpnrouteservice.EvpnRouteAdminService;
import org.onosproject.evpnrouteservice.EvpnRouteStore;
import org.onosproject.evpnrouteservice.VpnRouteTarget;
import org.onosproject.routing.bgp.mbgp.MBgpHandler;
import org.onosproject.routing.bgp.mbgp.MBgpHandlerFactory;
import org.onosproject.routing.bgp.mbgp.MBgpHandlerFactoryRegistry;
import org.onosproject.routing.bgp.mbgp.MBgpProtocolType;
import org.onosproject.routing.bgp.BgpMessage;
import org.onosproject.routing.bgp.BgpSession;
import org.onosproject.component.ComponentService;
import org.onosproject.core.CoreService;
import org.onosproject.core.ApplicationId;

import org.onosproject.evpnrouteservice.Label;
import org.onosproject.evpnrouteservice.RouteDistinguisher;

@Component
public class EvpnBgpProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EvpnRouteAdminService evpnRouteAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EvpnRouteStore evpnRouteStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MBgpHandlerFactoryRegistry mBgpHandlerFactoryRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentService componentService;

    private MBgpProtocolType protocolType = MBgpProtocolType.valueOf(25, 70);
    private EvpnMBgpHandlerFactory factory = new EvpnMBgpHandlerFactory();

    private final List<String> components = ImmutableList.of("org.onosproject.routing.bgp.BgpSessionManager");

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.evpnrouteservice.bgpProvider");
        components.forEach(name -> componentService.activate(appId, name));
        mBgpHandlerFactoryRegistry.registerMBgpHandlerFactory(protocolType, factory);
        log.info("Evpn Bgp provider activate!");
    }

    @Deactivate
    protected void deactivate() {
        components.forEach(name -> componentService.deactivate(appId, name));
        mBgpHandlerFactoryRegistry.unregisterMBgpHandlerFactory(protocolType);
    }

    private class EvpnMBgpHandlerFactory extends MBgpHandlerFactory<EvpnMBgpHandler> {
        @Override
        public EvpnMBgpHandler createHandler() {
            return new EvpnMBgpHandler();
        }
    }

    synchronized private void commit(Collection<EvpnRoute> routes, Collection<EvpnRoute> delRoutes) {
        log.debug("Start commit");
        routes.forEach(route -> log.debug("Add New route [{}]:[{}]:[{}]:[{}] to evpn route store",
        route.prefixMac(), route.prefixIp(), route.routeDistinguisher(), route.label()));
        delRoutes.forEach(route -> log.debug("Delete route [{}]:[{}]:[{}]:[{}] from evpn route store",
            route.prefixMac(), route.prefixIp(), route.routeDistinguisher(), route.label()));
        Collection<EvpnRoute> realDelRoutes = new ArrayList<>();
        delRoutes.forEach(route->realDelRoutes.addAll(
            evpnRouteStore.getRoutesForEvpnPrefix(route.evpnPrefix())
        ));

        evpnRouteAdminService.withdraw(realDelRoutes);
        evpnRouteAdminService.update(routes);
        log.debug("End commit");
    }

    private class EvpnMBgpHandler implements MBgpHandler {
        Collection<EvpnRoute> routes = new ArrayList<>();
        Collection<EvpnRoute> delRoutes = new ArrayList<>();

        public void parseAttributeTypeMpReachNlri(MBgpProtocolType type, IpAddress nextHop, int totalLength,
                ChannelBuffer message) throws BgpMessage.BgpParseException {
            parseNlri(type, nextHop, totalLength, message, true);
        }

        public void parseAttributeTypeMpUnreachNlri(MBgpProtocolType type, int totalLength, ChannelBuffer message)
                throws BgpMessage.BgpParseException {
            parseNlri(type, null, totalLength, message, false);
        }

        private void parseNlri(MBgpProtocolType type, IpAddress nextHop, int totalLength, ChannelBuffer message, boolean reach)
                throws BgpMessage.BgpParseException {
            int nlriEnd = message.readerIndex() + totalLength;

            while (message.readerIndex() < nlriEnd) {
                int routeType = message.readUnsignedByte();
                int routeLength = message.readUnsignedByte();
                int routeEnd = message.readerIndex() + routeLength;
                if (routeLength > nlriEnd - message.readerIndex()) {
                    // ERROR: Malformed Attribute List
                    String errorMsg = "Malformed nlri route length";
                    throw new BgpMessage.BgpParseException(errorMsg);
                }
                switch (routeType) {
                case EvpnConstants.RouteTypes.ETHERNET_AD:
                    break;
                case EvpnConstants.RouteTypes.MAC_IP_ADVERTISMENT:
                    {
                        Optional<EvpnRoute> route = parseType2Route(routeLength, message, nextHop);
                        if (route.isPresent()) {
                            if(reach) routes.add(route.get());
                            else delRoutes.add(route.get());    
                        }
                        break;
                    }
                case EvpnConstants.RouteTypes.INCLUSIVE_MULTICAT_ETHERNET_TAG:
                    break;
                case EvpnConstants.RouteTypes.ETHERNET_SEGMENT:
                    break;
                case EvpnConstants.RouteTypes.EXTENDED_COMMUNITY:
                    int subtype = message.readUnsignedByte();

                    switch (subtype) {
                    case EvpnConstants.ExtendedRouteTypes.ESI_LEBEL:
                        break;
                    case EvpnConstants.ExtendedRouteTypes.ES_IMPORT:
                        break;
                    case EvpnConstants.ExtendedRouteTypes.MAC_MOBILITY:
                        break;
                    default:
                        break;
                    }
                    break;
                default:
                    String errorMsg = "Undefined evpn route type";
                    throw new BgpMessage.BgpParseException(errorMsg);
                }
                if (routeEnd - message.readerIndex() > 0) {
                    message.skipBytes(routeEnd - message.readerIndex());
                }
            }

            if (nlriEnd - message.readerIndex() > 0) {
                message.skipBytes(nlriEnd - message.readerIndex());
            }
        }

        public void commit(BgpSession bgpSession, ChannelHandlerContext ctx) {
            // TODO Other types rather than type 2
            EvpnBgpProvider.this.commit(routes, delRoutes);
        }
    }

    public Optional<EvpnRoute> parseType2Route(int totalLength, ChannelBuffer message, IpAddress nextHop) {
        int routeEnd = message.readerIndex() + totalLength;
        RouteDistinguisher rd = null;
        MacAddress mac = null;
        IpAddress ip = null;
        ArrayList<Label> labels = new ArrayList<>();

        rd = EvpnBgpParser.parseRouteDistinguisher(message);

        message.skipBytes(10); // Pass esi
        message.skipBytes(4); // Pass ethtag

        int macBitLength = message.readUnsignedByte();
        if (macBitLength != 0) {
            mac = EvpnBgpParser.parseMac(macBitLength, message);
        }

        int ipBitLength = message.readUnsignedByte();
        if (ipBitLength != 0) {
            ip = EvpnBgpParser.parseIpAddress(ipBitLength, message);
        }

        while (routeEnd > message.readerIndex()) {
            labels.add(EvpnBgpParser.parseLabel(message));
        }

        if(ip == null || mac == null) {
            return Optional.empty();
        }

        EvpnRoute route = new EvpnRoute(
            EvpnRoute.Source.REMOTE,
            mac,
            ip.toIpPrefix(),
            nextHop,
            rd,
            (List<VpnRouteTarget>)null,
            (List<VpnRouteTarget>)null,
            Label.label(3700)
            // labels.get(0)
        );

        return Optional.of(route);
    }

    public static final class EvpnConstants {
        private EvpnConstants() {
        }

        public static final class RouteTypes {
            private RouteTypes() {
            }

            public static final int ETHERNET_AD = 1; // Ethernet Auto-discovery Route
            public static final int MAC_IP_ADVERTISMENT = 2; // MAC/IP Advertisement route
            public static final int INCLUSIVE_MULTICAT_ETHERNET_TAG = 3; // Inclusive Multicast Ethernet Tag route
            public static final int ETHERNET_SEGMENT = 4; // Ethernet Segment route
            public static final int EXTENDED_COMMUNITY = 6; // Extended Community
        }

        public static final class ExtendedRouteTypes {
            private static final int ESI_LEBEL = 1;
            private static final int ES_IMPORT = 2;
            private static final int MAC_MOBILITY = 0;
        }
    }
}