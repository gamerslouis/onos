package org.onosproject.routing.bgp.mbgp;

import java.util.Objects;

public class MBgpProtocolType {
    private int afi; // Address Family Identifier (2 octets)
    private int safi; // Subsequent Address Family Identifier (1 octet)

    private MBgpProtocolType(int afi, int safi) {
        this.afi = afi;
        this.safi = safi;
    }

    public int getAfi() {
        return afi;
    }

    public int getSafi() {
        return safi;
    }

    public static MBgpProtocolType valueOf(int afi, int safi) {
        if (afi < 0 || afi >= 65536) {
            throw new IllegalArgumentException("afi is can only be 2 octets.");
        }
        if (safi < 0 || safi >= 256) {
            throw new IllegalArgumentException("safi is can only be 1 octets.");
        }
        return new MBgpProtocolType(afi, safi);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof MBgpProtocolType)) {
            return false;
        }

        MBgpProtocolType that = (MBgpProtocolType) other;

        return afi == that.afi && safi == that.safi;
    }

    @Override
    public int hashCode() {
        return Objects.hash(afi, safi);
    }
}