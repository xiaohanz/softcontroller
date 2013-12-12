/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.flowmanager.internal;

import java.util.Map;
import java.util.Hashtable;

//import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.flowmanager.IL2flowmanager;
//import org.osgi.framework.BundleContext;
//import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2FlowManager implements IL2flowmanager {
    private static final Logger logger = LoggerFactory.getLogger(L2FlowManager.class);

    //Configuration the idleTimeout and the hardTimeout of the flow
    //total set the idleTimeout and the hardTimeout;
    private short idleTimeout = 10;
    private short hardTimeout = 20;
    //Store each node's idleTimeout and the hardTimeout;
    private Map<String,L2FlowManagerUnit> L2FlowManagerDB = new Hashtable<String,L2FlowManagerUnit>();

/*    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }*/
    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        logger.info("L2flowmanager Initialized");
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        logger.info("L2flowmanager Started");
        // OSGI console
//        registerWithOSGIConsole();
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        logger.info("Stopped");
    }

    @Override
    public void setIdleTimeout(short idleTimeout) {
        this.idleTimeout = idleTimeout;
        return;
    }

    @Override
    public short getIdleTimeout() {
        return this.idleTimeout;
    }

    @Override
    public void setHardTimeout(short hardTimeout) {
        this.hardTimeout = hardTimeout;
        return;
    }

    @Override
    public short getHardTimeout() {
         return this.hardTimeout;
    }

    @Override
    public void setIdleTimeoutByNodeId(String nodeId, short idleTimeout) {
        L2FlowManagerUnit l2Timeout = this.L2FlowManagerDB.get(nodeId);
        if(l2Timeout == null) {
            this.L2FlowManagerDB.put(nodeId,new L2FlowManagerUnit(idleTimeout,this.hardTimeout));
        } else {
            l2Timeout.setIdleTimeout(idleTimeout);
        }
        return;
    }

    @Override
    public void setHardTimeoutByNodeId(String nodeId, short hardTimeout) {
        L2FlowManagerUnit l2Timeout = this.L2FlowManagerDB.get(nodeId);
        if(l2Timeout == null) {
            this.L2FlowManagerDB.put(nodeId,new L2FlowManagerUnit(this.idleTimeout,hardTimeout));
        } else {
            l2Timeout.setHardTimeout(hardTimeout);
        }
        return;
    }

    @Override
    public short getIdleTimeoutByNodeId(String nodeId) {
        L2FlowManagerUnit l2Timeout = this.L2FlowManagerDB.get(nodeId);
        if(l2Timeout == null) {
            l2Timeout = new L2FlowManagerUnit(this.idleTimeout,this.hardTimeout);
            this.L2FlowManagerDB.put(nodeId, l2Timeout);
        }
        return l2Timeout.getIdleTimeout();
    }

    @Override
    public short getHardTimeoutByNodeId(String nodeId) {
        L2FlowManagerUnit l2Timeout = this.L2FlowManagerDB.get(nodeId);
        if(l2Timeout == null) {
            l2Timeout = new L2FlowManagerUnit(this.idleTimeout,this.hardTimeout);
            this.L2FlowManagerDB.put(nodeId, l2Timeout);
        }
        return l2Timeout.getHardTimeout();
    }
}

