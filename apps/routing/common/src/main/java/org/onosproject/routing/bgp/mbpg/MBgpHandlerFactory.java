package org.onosproject.routing.bgp.mbgp;

import org.onosproject.net.config.ConfigFactory;

public abstract class MBgpHandlerFactory<C extends MBgpHandler> {
    public abstract C createHandler();
}
