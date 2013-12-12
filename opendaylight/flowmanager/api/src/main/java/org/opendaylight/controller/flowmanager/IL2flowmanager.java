
/*
 * Copyright (c) 2013 BNC Systems, Inc. and others.  All rights reserved.
 *
 */

package org.opendaylight.controller.flowmanager;


/**
 * Primary purpose of this interface is to provide methods to listen to inventory changes
 */
public interface IL2flowmanager {

    //set the idleTimeout and the hardTimeout
    public void setIdleTimeout(short idleTimeout);
    public short getIdleTimeout();
    public void setHardTimeout(short hardTimeout);
    public short getHardTimeout();

    //set SwitchNode timeout by nodeId
    public void setIdleTimeoutByNodeId(String nodeId, short idleTimeout);
    public void setHardTimeoutByNodeId(String nodeId, short hardTimeout);
    //get idleTimeout by nodeId
    public short getIdleTimeoutByNodeId(String nodeId);
    public short getHardTimeoutByNodeId(String nodeId);
}
