package org.onosproject.routing.bgp.mbgp;

import org.onlab.packet.IpAddress;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.onosproject.routing.bgp.BgpMessage;
import org.onosproject.routing.bgp.BgpSession;

public interface MBgpHandler {
        public void parseAttributeTypeMpReachNlri(MBgpProtocolType type, IpAddress nextHop, int totalLength,
                        ChannelBuffer message) throws BgpMessage.BgpParseException;

        public void parseAttributeTypeMpUnreachNlri(MBgpProtocolType type, int totalLength, ChannelBuffer message)
                        throws BgpMessage.BgpParseException;

        public void commit(BgpSession bgpSession, ChannelHandlerContext ctx);
}
