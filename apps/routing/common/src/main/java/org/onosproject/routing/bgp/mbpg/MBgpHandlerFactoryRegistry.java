package org.onosproject.routing.bgp.mbgp;

import java.util.Set;

public interface MBgpHandlerFactoryRegistry {
    void registerMBgpHandlerFactory(MBgpProtocolType type, MBgpHandlerFactory factroy);

    void unregisterMBgpHandlerFactory(MBgpProtocolType type);

    MBgpHandlerFactory getMBgpHandlerFactory(MBgpProtocolType type);

    Set<MBgpProtocolType> getMBgpTypes();
}
