/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.flowstoragemanager;
import java.sql.Connection;
/**
 * Interface that describes methods for add or remove forwarding rules
 * to the flows database.
 *
 */
public interface IFlowStorage{

    /**
     * connect to database server.
     *
     */
    public Connection connectDb();

    /**
     * disconnect from the database server
     *
     */
    public void disConnectDb();
}
