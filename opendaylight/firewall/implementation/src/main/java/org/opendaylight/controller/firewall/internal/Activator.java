

/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.firewall.internal;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.firewall.IFirewall;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory.getLogger(Activator.class);


    /**
     * Function that is used to communicate to dependency manager the list of
     * known implementations for services inside a container
     *
     *
     * @return An array containing all the CLASS objects that will be
     *         instantiated in order to get an fully working implementation
     *         Object
     */
    @Override
    public Object[] getImplementations() {
        Object[] res = { Firewall.class };
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies is
     * required.
     *
     * @param c
     *            dependency manager Component object, used for configuring the
     *            dependencies exported and imported
     * @param imp
     *            Implementation class that is being configured, needed as long
     *            as the same routine can configure multiple implementations
     * @param containerName
     *            The containerName being configured, this allow also optional
     *            per-container different behavior if needed, usually should not
     *            be the case though.
     */
    @Override
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(Firewall.class)) {
            // export the service
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("salListenerName", "firewall");
            Set<String> propSet = new HashSet<String>();
            propSet.add(Firewall.FIREWALL_EVENT_CACHE_NAME);
            props.put("cachename", propSet);
            c.setInterface(new String[] {IFirewall.class.getName(), IConfigurationContainerAware.class.getName(),IListenDataPacket.class.getName()},props);
        }
        c.add(createContainerServiceDependency(containerName).setService(
                IClusterContainerServices.class).setCallbacks(
                "setClusterContainerService",
                "unsetClusterContainerService").setRequired(true));
        c.add(createContainerServiceDependency(containerName).setService(
                IDataPacketService.class).setCallbacks(
                "setDataPacketService", "unsetDataPacketService")
                .setRequired(true));
        c.add(createContainerServiceDependency(containerName).setService(
                ISwitchManager.class).setCallbacks("setSwitchManager",
                "unsetSwitchManager").setRequired(true));
        c.add(createContainerServiceDependency(containerName).setService(
                IfIptoHost.class).setCallbacks("setHostTracker",
                "unsetHostTracker").setRequired(true));
        c.add(createContainerServiceDependency(containerName).setService(
                IForwardingRulesManager.class).setCallbacks(
                "setForwardingRulesManager", "unsetForwardingRulesManager")
                .setRequired(true));
    }
}
