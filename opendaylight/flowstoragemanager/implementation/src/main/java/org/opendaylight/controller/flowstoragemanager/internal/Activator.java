/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.flowstoragemanager.internal;

//import java.util.Dictionary;
//import java.util.HashSet;
//import java.util.Hashtable;
//import java.util.Set;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.flowstoragemanager.IFlowUpdateListener;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
//import org.opendaylight.controller.flowstoragemanager.IFlowStorage;
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
        Object[] res = { FlowStorageManager.class };
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
        if (imp.equals(FlowStorageManager.class)) {
            String interfaces[] = null;
            // export the service
            interfaces = new String[] { IFlowUpdateListener.class.getName()
            };
            c.setInterface(interfaces, null);
        }
    }

    @Override
    protected Object[] getGlobalImplementations() {
        final Object[] res = { FlowStorageManager.class };
        return res;
    }
}
