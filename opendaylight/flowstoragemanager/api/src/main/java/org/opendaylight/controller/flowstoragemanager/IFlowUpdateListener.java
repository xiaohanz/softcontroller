
package org.opendaylight.controller.flowstoragemanager;

import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
//import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
//import org.opendaylight.controller.sal.action.Action;
//import org.opendaylight.controller.sal.action.Drop;
//import org.opendaylight.controller.sal.core.ConstructionException;
//import org.opendaylight.controller.sal.core.Node;
//import org.opendaylight.controller.sal.flowprogrammer.Flow;
//import org.opendaylight.controller.sal.match.Match;
//import org.opendaylight.controller.sal.match.MatchType;
//import org.opendaylight.controller.sal.utils.Status;
//import org.opendaylight.controller.sal.utils.StatusCode;
//import org.ops4j.pax.exam.Option;
//import org.ops4j.pax.exam.Configuration;
//import org.ops4j.pax.exam.junit.PaxExam;
//import org.ops4j.pax.exam.util.PathUtils;
//import org.osgi.framework.Bundle;
//import org.osgi.framework.BundleContext;
//import org.osgi.framework.ServiceReference;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 * Interface that listen to flow add or remove from the forwardingrulesmanager module.
 *
 */
public interface IFlowUpdateListener{

    /**
     * monitor the addEntry operation in FRM and will update flow database accordingly.
     *
     * @param flow
     *            the flow entry to install
     * @return the {@code Status} object indicating the result of this action.
     */
    public void writeflowEntry(FlowEntry flowEntry);

    /**
     * monitor the removeEntry operation in FRM and will update flow database accordingly.
     *
     * @param flow
     *            the flow entry to uninstall
     * @return the {@code Status} object indicating the result of this action
     */
    public int updateFlowEntry(ConcurrentMap<Integer, Object[]> statistic);
}
