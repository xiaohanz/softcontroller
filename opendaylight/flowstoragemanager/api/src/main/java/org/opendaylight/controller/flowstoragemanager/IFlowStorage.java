
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
