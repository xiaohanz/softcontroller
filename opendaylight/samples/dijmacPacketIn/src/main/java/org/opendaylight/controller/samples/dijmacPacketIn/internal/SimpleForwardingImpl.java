
package org.opendaylight.controller.samples.dijmacPacketIn.internal;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.core.Config;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.routing.IListenRoutingUpdates;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.samples.dijmacPacketIn.HostHostPair;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements basic L2 forwarding within the managed devices.
 * Forwarding is only done within configured subnets.</br>
 * <br/>
 * The basic flow is that the module listens for recieved packet from the
 * {@link import org.opendaylight.controller.sal.packet.IDataPacketService}
 * service and on recieving packet it first calls
 * <tt>preparePerPacketRules()</tt> to create a set of new rules that must be
 * installed in the network. This is done by repeatedly calling
 * <tt>updatePerPacketRuleInSW()</tt> for each switch in the network. Then it
 * installs those rules using <tt>installPerPacketRules()</tt>.
 */
public class SimpleForwardingImpl implements IfNewHostNotify,
        IListenRoutingUpdates, IListenDataPacket, IInventoryListener {
    private static Logger log = LoggerFactory
            .getLogger(SimpleForwardingImpl.class);
    private static short DEFAULT_MACSWITCH_PRIORITY = 1;
    private static String FORWARDING_RULES_CACHE_NAME = "forwarding.macpacketin.rules";
    private IfIptoHost hostTracker;
    private IForwardingRulesManager frm;
    private ITopologyManager topologyManager;
    private IRouting routing;
    private IDataPacketService dataPacketService;


    /**
     * The set of all forwarding rules: (packet) -> (switch -> flowmod).
     */
    private ConcurrentMap<HostHostPair, HashMap<Node, FlowEntry>> rulesEntryDB;
    private Map<Node, List<FlowEntry>> tobePrunedPos = new HashMap<Node, List<FlowEntry>>();
    private IClusterContainerServices clusterContainerService = null;
    private ISwitchManager switchManager;

    /**
     * Return codes from the programming of the perHost rules in HW
     */
    public enum RulesProgrammingReturnCode {
        SUCCESS, FAILED_FEW_SWITCHES, FAILED_ALL_SWITCHES, FAILED_WRONG_PARAMS
    }

    public void setRouting(IRouting routing) {
        this.routing = routing;
    }

    public void unsetRouting(IRouting routing) {
        if (this.routing == routing) {
            this.routing = null;
        }
    }

    public ITopologyManager getTopologyManager() {
        return topologyManager;
    }

    public void setTopologyManager(ITopologyManager topologyManager) {
        log.debug("Setting topologyManager");
        this.topologyManager = topologyManager;
    }

    public void unsetTopologyManager(ITopologyManager topologyManager) {
        if (this.topologyManager == topologyManager) {
            this.topologyManager = null;
        }
    }

    public void setHostTracker(IfIptoHost hostTracker) {
        log.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void setForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        log.debug("Setting ForwardingRulesManager");
        this.frm = forwardingRulesManager;
    }

    public void unsetHostTracker(IfIptoHost hostTracker) {
        if (this.hostTracker == hostTracker) {
            this.hostTracker = null;
        }
    }

    public void unsetForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        if (this.frm == forwardingRulesManager) {
            this.frm = null;
        }
    }

    /**
     * Function called when the bundle gets activated
     *
     */
    public void startUp() {
        allocateCaches();
        retrieveCaches();
    }

    /**
     * Function called when the bundle gets stopped
     *
     */
    public void shutDown() {
        log.debug("Destroy all the host Rules given we are shutting down");
        uninstallPerHostRules();
        destroyCaches();
    }

    private void allocateCaches() {
        if (this.clusterContainerService == null) {
            log.info("un-initialized clusterContainerService, can't create cache");
            return;
        }

        try {
            clusterContainerService.createCache(FORWARDING_RULES_CACHE_NAME,
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheExistException cee) {
            log.error("\nCache already exists - destroy and recreate if needed");
        } catch (CacheConfigException cce) {
            log.error("\nCache configuration invalid - check cache mode");
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void retrieveCaches() {
        if (this.clusterContainerService == null) {
            log.info("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }

        rulesEntryDB = (ConcurrentMap<HostHostPair, HashMap<Node, FlowEntry>>) clusterContainerService
                .getCache(FORWARDING_RULES_CACHE_NAME);
        if (rulesEntryDB == null) {
            log.error("\nFailed to get rulesEntryDB handle");
        }
    }

    private void destroyCaches() {
        if (this.clusterContainerService == null) {
            log.info("un-initialized clusterContainerService, can't destroy cache");
            return;
        }

        clusterContainerService.destroyCache(FORWARDING_RULES_CACHE_NAME);
    }
    /**
     * Calculate the per-Packet rules to be installed in the rulesDB,
     * and that will later on be installed in HW, this routine will
     * implicitly calculate the shortest path tree among the srcHost
     * to the destHost is attached and will automatically create all the rules.
     *
     * @param srcHost dstHost which we are going to prepare the rules in the rulesDB
     *
     * @return A list of switches touched by the calculation
     */
    private List<Node> preparePerPacketRules(HostNodeConnector srcHost,HostNodeConnector dstHost) {
        if (srcHost == null||dstHost==null) {
            return null;
        }

        //TODO: race condition! unset* functions can make these null.
        if (this.routing == null) {
            return null;
        }
        if (this.switchManager == null) {
            return null;
        }
        if (this.rulesEntryDB == null) {
            return null;
        }

        Node srcNode = srcHost.getnodeconnectorNode();
        Node dstNode = dstHost.getnodeconnectorNode();
        Node currNode = dstHost.getnodeconnectorNode();
        List<Node> switchesToProgram = new ArrayList<Node>();
        HostHostPair key;
        HashMap<Node, FlowEntry> pos;
        FlowEntry po;

        if (srcNode.equals(dstNode)){
            log.debug("srcHost and dstHost are in the same switch");
        }else{
            List<Edge> links;
            Path res = this.routing.getRoute(dstNode, srcNode);
            if ((res==null)||((links =res.getEdges())==null)){
                // No route from dstHost to srcHost can be found, back out any
                // existing forwarding rules if they exist.
                log.debug("NO Route/Path between SW[{}] --> SW[{}] cleaning " +
                        "potentially existing entries", dstNode, srcNode);
                key = new HostHostPair(srcHost, dstHost);
                pos = this.rulesEntryDB.get(key);
                if (pos != null) {
                    for (Map.Entry<Node, FlowEntry> e : pos.entrySet()) {
                        po = e.getValue();
                    }
                    this.rulesEntryDB.remove(key);
                }else{
                    log.debug("the rule between srcHost and dstHost does not exist in rulesDB ");
                }
            }else {
                log.debug("there is route from dstHost to srcHost");
                log.debug("Route between SW[{}] --> SW[{}]", dstNode, srcNode);
                key = new HostHostPair(srcHost, dstHost);
                // for each link in the route from here to there
                for (Edge link : links) {
                    if (link == null) {
                        log.error("Could not retrieve the Link");
                        // TODO: should we keep going?
                        continue;
                    }
                    log.debug(link.toString());
                    // Index all the switches to be programmed
                    updatePerPacketRuleInSW(srcHost, dstHost, currNode, link, key);
                    if ((this.rulesEntryDB.get(key)) != null) {
                        switchesToProgram.add(currNode);
                    }
                    currNode = link.getHeadNodeConnector().getNode();
                    key = new HostHostPair(srcHost, dstHost);
                }
            }
        }
        switchesToProgram.add(srcNode);
        updatePerPacketRuleInSW(srcHost, dstHost, currNode, null,
                              new HostHostPair(srcHost, dstHost));

        //      log.debug("Getting out at the end!");
        return switchesToProgram;
    }
    /**
     * Populates <tt>rulesDB</tt> with rules specifying how to reach
     * <tt>srcHost</tt> from <tt>dstHode</tt> assuming that:
     *
     * @param srcHost
     *            The host to be reached.
     * @param dstHost
     *            The host to be started.
     * @param currNode
     *            The current node being processed.
     * @param link
     *            The link to follow from curNode to get to rootNode
     * @param key
     *            The key to store computed rules at in the rulesDB. For now,
     *            this is a {@link HostHostPair} of srcHost and dstHost.
     */
    private void updatePerPacketRuleInSW(HostNodeConnector srcHost, HostNodeConnector dstHost,
            Node currNode, Edge link, HostHostPair key) {
        // only the link parameter is optional
        if (srcHost == null || key == null || dstHost == null || currNode == null) {
            return;
        }
        Node rootNode=srcHost.getnodeconnectorNode();
        HashMap<Node, FlowEntry> pos = this.rulesEntryDB.get(key);
        if (pos == null) {
            pos = new HashMap<Node, FlowEntry>();
        }
        if (!srcHost.equals(dstHost)){
            //FlowEntry removed_po = pos.remove(inPort);
            Match match = new Match();
            List<Action> actions = new ArrayList<Action>();

            byte[] srcMac=srcHost.getDataLayerAddressBytes();
            byte[] dstMac=dstHost.getDataLayerAddressBytes();

            if (srcMac==null||dstMac==null){
                return;
            }
            match.setField(MatchType.DL_DST, srcMac);
            match.setField(MatchType.DL_SRC, dstMac);
            NodeConnector outPort = null;
            if (currNode.equals(rootNode)) {
                outPort = srcHost.getnodeConnector();
            } else {
                // currNode is NOT the rootNode, find the next hop and create a rule
                if (link != null) {
                    outPort = link.getTailNodeConnector();
                }else{
                    return;
                }
            }
            if (outPort != null) {
                actions.add(new Output(outPort));
            }
            Flow flow = new Flow(match, actions);
            flow.setIdleTimeout((short) 0);
          //  flow.setHardTimeout((short) 0);
            flow.setHardTimeout((short) 300);
            flow.setPriority(DEFAULT_MACSWITCH_PRIORITY);
            String policyName = currNode.getNodeIDString();
            String flowName = "["
                    + srcHost.toString()+"and"+dstHost.toString()
                       + "on"  + currNode + "]";
            FlowEntry po = new FlowEntry(policyName, flowName, flow, currNode);
            /* Now save the rule in the DB rule, so on updates from topology we
             * can selectively */
            pos.put(currNode, po);
                this.rulesEntryDB.put(key, pos);
        }
    }

    /**
     * Cleanup all the host rules for a given host
     *
     * @param host Host for which the host rules need to be cleaned
     * up, the host could be null in that case it match all the hosts
     *
     * @return a return code that convey the programming status of the HW
     */
    private RulesProgrammingReturnCode uninstallPerHostRules(
            HostNodeConnector host) {
        RulesProgrammingReturnCode retCode = RulesProgrammingReturnCode.SUCCESS;
        Map<Node, FlowEntry> pos;
        FlowEntry po;
        // Now program every single switch
        for (HostHostPair key : this.rulesEntryDB.keySet()) {
            if (host==null||key.getsrcHost().getDataLayerAddressBytes().equals(host.getDataLayerAddressBytes())||key.getdstHost().getDataLayerAddressBytes().equals(host.getDataLayerAddressBytes())) {
                pos = this.rulesEntryDB.get(key);
                for (Map.Entry<Node, FlowEntry> e : pos.entrySet()) {
                    po = e.getValue();
                    if (po != null) {
                        // Uninstall the policy
                        this.frm.uninstallFlowEntry(po);
                    }
                }
                this.rulesEntryDB.remove(key);
            }
        }
        return retCode;
    }

    /**
     * Cleanup all the host rules for a given node, triggered when the
     * switch disconnects, so there is no reason for Hw cleanup
     * because it's disconnected anyhow
     * TBD - Revisit above stmt in light of CSCus88743
     * @param targetNode Node for which we want to do cleanup
     *
     */
    private void uninstallPerNodeRules(Node targetNode) {
        Map<Node, FlowEntry> pos;
        FlowEntry po;
        // Now program every single switch
        for (HostHostPair key : this.rulesEntryDB.keySet()) {
            pos = this.rulesEntryDB.get(key);
            if (targetNode == null||pos.containsKey(targetNode)){
                log.debug("Work on host {} host {}", key.getsrcHost(), key.getdstHost());
                for (Map.Entry<Node, FlowEntry> e : pos.entrySet()) {
                    po = e.getValue();
                    if (po !=null) {
                        this.frm.uninstallFlowEntry(po);
                    }
                }
                log.debug("Remove {}", key);
                this.rulesEntryDB.remove(key);
            }
        }
    }

    /**
     * Cleanup all the host rules currently present in the rulesDB
     *
     * @return a return code that convey the programming status of the HW
     */
    private RulesProgrammingReturnCode uninstallPerHostRules() {
        return uninstallPerHostRules(null);
    }

    @Override
    public void recalculateDone() {
        if (this.hostTracker == null) {
            return;
        }
    }

    void addTobePrunedPolicy(Node swId, FlowEntry po, FlowEntry new_po) {
        List<FlowEntry> pl = tobePrunedPos.get(swId);
        if (pl == null) {
            pl = new LinkedList<FlowEntry>();
            tobePrunedPos.put(swId, pl);
        }
        pl.add(po);
        log.debug("Adding Pruned Policy for SwId: {}", swId);
        log.debug("Old Policy: {}", po);
        log.debug("New Policy: {}", new_po);
    }

    /**
     * Routine that fetch the per-Packet rules from the rulesDB and
     * install in HW, the one having the same match rules will be
     * overwritten silently.
     *
     * @param srcHost dstHost which we want to install in HW the per-Packet rules
     * @param switches  list of switches to be programmed in HW
     *
     * @return a return code that convey the programming status of the HW
     */
    private RulesProgrammingReturnCode installPerPacketRules(
            HostNodeConnector srcHost, HostNodeConnector dstHost, List<Node> switches){
        RulesProgrammingReturnCode retCode = RulesProgrammingReturnCode.SUCCESS;
        if (srcHost == null || dstHost == null) {
            return RulesProgrammingReturnCode.FAILED_WRONG_PARAMS;
        }
        Map<Node, FlowEntry> pos;
        FlowEntry po;


        log.debug("Inside installPerPacketRules");
        HostHostPair key = new HostHostPair(srcHost,dstHost);
        pos = this.rulesEntryDB.get(key);
        if (pos == null) {
            return null;
        }else{
            // Now program every single switch
            for (Node node : switches ){
                po = pos.get(node);
                if (po!=null){
                    Status poStatus = this.frm.installFlowEntry(po);
                    if (!poStatus.isSuccess()) {
                        log.error("Failed to install policy: "
                                + po.getGroupName() + " ("
                                + poStatus.getDescription() + ")");
                        retCode = RulesProgrammingReturnCode.FAILED_FEW_SWITCHES;
                        // Remove the entry from the DB, it was not installed!
                        this.rulesEntryDB.get(key).remove(node);
                    } else {
                        log.debug("Successfully installed policy "
                                + po.toString() + " on switch " + dstHost.getnodeConnector());
                    }
                }else {
                    log.error("Cannot find a policy for SW:({}) Host: ({})",
                              dstHost.getnodeconnectorNode(), srcHost);
                }
            }
        }
        log.debug("Leaving installPerPacketRules");
        return retCode;
    }
    // *****************
    // IListenDataPacket
    // *****************

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        if (inPkt == null) {
            return PacketResult.IGNORED;
        }

        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        Ethernet eth;
        if (formattedPak instanceof Ethernet) {
            eth = (Ethernet) formattedPak;
        } else {
            return PacketResult.IGNORED;
        }
        Object pak=formattedPak.getPayload();
        if (pak instanceof IPv4&&!(eth.isBroadcast())) {
            IPv4 nextPak = (IPv4)formattedPak.getPayload();
            InetAddress srcIp = NetUtils.getInetAddress(nextPak.getSourceAddress());
            InetAddress dstIp = NetUtils.getInetAddress(nextPak.getDestinationAddress());
            HostNodeConnector srcHost = hostTracker.hostFind(srcIp);
            HostNodeConnector dstHost = hostTracker.hostFind(dstIp);
            if (srcHost != null&& dstHost != null){
                List<Node> srcSwitches=preparePerPacketRules(srcHost,dstHost);
                List<Node> dstSwitches=preparePerPacketRules(dstHost,srcHost);
                if (srcSwitches!=null&&dstSwitches!=null){
                    installPerPacketRules(srcHost, dstHost, srcSwitches);
                    installPerPacketRules(dstHost, srcHost, dstSwitches);
                }else{
                    return PacketResult.IGNORED;
                }
                return PacketResult.CONSUME;
            }else{
                return PacketResult.IGNORED;
            }

        } else {
            return PacketResult.IGNORED;
        }
    }
    @Override
    public void notifyHTClient(HostNodeConnector host) {
        if (host == null) {
            return;
        }
    }

    @Override
    public void notifyHTClientHostRemoved(HostNodeConnector host) {
        if (host == null) {
            return;
        }
        uninstallPerHostRules(host);
    }

    @Override
    public void notifyNode(Node node, UpdateType type,
            Map<String, Property> propMap) {
        if (node == null)
            return;

        switch (type) {
        case REMOVED:
            log.debug("Node {} gone, doing a cleanup", node);
            uninstallPerNodeRules(node);
            break;
        default:
            break;
        }
    }

    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Map<String, Property> propMap) {
        if (nodeConnector == null)
            return;

        boolean down = false;
        switch (type) {
        case ADDED:
            break;
        case REMOVED:
            down = true;
            break;
        case CHANGED:
            Config con = (Config) propMap.get(Config.ConfigPropName);
            if ((con != null) && (con.getValue() == Config.ADMIN_DOWN)) {
                down = true;
            }
            break;
        default:
            return;
        }

        if (down) {
            uninstallPerNodeRules(nodeConnector.getNode());
        } else {
            //uninstallPerNodeRules(nodeConnector.getNode());
        }
    }

    void setClusterContainerService(IClusterContainerServices s) {
        log.debug("Cluster Service set");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            log.debug("Cluster Service removed!");
            this.clusterContainerService = null;
        }
    }
    void setDataPacketService(IDataPacketService s) {
        this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        startUp();
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
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
    }

    public void setSwitchManager(ISwitchManager switchManager) {
        this.switchManager = switchManager;
    }

    public void unsetSwitchManager(ISwitchManager switchManager) {
        if (this.switchManager == switchManager) {
            this.switchManager = null;
        }
    }
}
