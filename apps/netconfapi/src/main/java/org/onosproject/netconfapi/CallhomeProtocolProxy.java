package org.onosproject.netconfapi;


import org.onlab.rest.AbstractWebApplication;

import java.util.Set;

/**
 * NetconfAPI Server front-end application.
 */
public class CallhomeProtocolProxy extends AbstractWebApplication {

    @Override
    public Set<Class<?>> getClasses() {
        return getClasses(
                CallhomeWebResources.class
        );
    }
}