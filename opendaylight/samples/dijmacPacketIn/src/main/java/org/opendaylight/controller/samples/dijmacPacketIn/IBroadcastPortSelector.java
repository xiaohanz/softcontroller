
package org.opendaylight.controller.samples.dijmacPacketIn;

import java.util.Set;

import org.opendaylight.controller.sal.core.NodeConnector;

public interface IBroadcastPortSelector {
    Set<NodeConnector> getBroadcastPorts();
}
