package org.onosproject.routing.cli;

import java.util.ArrayList;
import java.util.Collection;
import org.onosproject.routing.bgp.*;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Command to show the routes learned through BGP.
 */
@Service
@Command(scope = "onos", name = "bgp-test", description = "Test")
public class BgpTest extends AbstractShellCommand {
    @Override
    protected void doExecute() {
        ChannelBuffer message = ChannelBuffers.buffer(BgpConstants.BGP_MESSAGE_MAX_LENGTH);

        byte[] evpn = { (byte) 0x90, 0x0e, 0x00, 0x30, 0x00, 0x19, 0x46, 0x04, 0x0a, 0x00, 0x03, 0x65, 0x00, 0x02, 0x25,
                0x00, 0x01, 0x0a, 0x00, 0x0a, 0x01, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x30, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc,
                0x40, 0x20, (byte) 0xc0, (byte) 0xa8, 0x01, 0x28, 0x00, 0x0e, 0x74 };
        byte[] origin = { 0x40, 0x01, 0x01, 0x00 };
        byte[] path = { 0x50, 0x02, 0x00, 0x06, 0x02, 0x01, 0x00, 0x00, (byte) 0xfd, (byte) 0xe8 };
        byte[] localpref = {0x40, 0x05, 0x04, 0x00, 0x00, 0x00, 0x64};
        byte[] ext = { (byte) 0xc0, 0x10, 0x10, 0x03, 0x0c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00, 0x02, (byte) 0xfd,
                (byte) 0xe8, 0x00, 0x00, (byte) 0x0e, 0x74 };

        message.writeShort(0); // withdraw routes length
        message.writeShort(92); 
        message.writeBytes(evpn);
        message.writeBytes(origin);
        message.writeBytes(path);
        message.writeBytes(localpref);
        message.writeBytes(ext);

        BgpInfoService service = AbstractShellCommand.get(BgpInfoService.class);
        ((BgpSession)service.getBgpSessions().toArray()[0]).channel
                .write(BgpMessage.prepareBgpMessage(BgpConstants.BGP_TYPE_UPDATE, message));
    }
}
