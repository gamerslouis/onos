package org.onosproject.evpnrouteservice.impl;

import org.onosproject.evpnrouteservice.Label;
import org.onosproject.evpnrouteservice.RouteDistinguisher;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.jboss.netty.buffer.ChannelBuffer;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

public class EvpnBgpParser {
    public static MacAddress parseMac(int macBitLength, ChannelBuffer message) {
        byte[] bytes = readBytes(macBitLength / 8, message);
        return MacAddress.valueOf(bytes);
    }

    public static IpAddress parseIpAddress(int ipBitLength, ChannelBuffer message) {
        byte[] bytes = readBytes(ipBitLength / 8, message);
        if (ipBitLength == IpAddress.INET_BIT_LENGTH) {
            return Ip4Address.valueOf(bytes);
        } else {
            return Ip6Address.valueOf(bytes);
        }
    }

    public static Label parseLabel(ChannelBuffer message) {
        byte[] bytes = readBytes(3, message);
        return Label.label(bytes[0] * 65536 + bytes[1] * 256 + bytes[2]);
    }

    public static RouteDistinguisher parseRouteDistinguisher(ChannelBuffer message) {
        byte[] bytes = readBytes(8, message);
        String rd = null;
        // Only support type 1 rd now
        if (bytes[0] == 0x00 && bytes[1] == 0x01) {
            Ip4Address ipv4 = Ip4Address.valueOf(bytes, 2);
            int number = bytes[6] * 256 + bytes[7];
            rd = String.format("%s:%d", ipv4.toString(), number);
        } else {
            rd = bytesToHex(bytes);
        }
        return RouteDistinguisher.routeDistinguisher(rd);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static byte[] readBytes(int length, ChannelBuffer message) {
        byte[] buffer = new byte[length];
        message.readBytes(buffer, 0, length);
        return buffer;
    }
}
