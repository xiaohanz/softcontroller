package org.opendaylight.controller.firewall.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.dm.Component;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.firewall.FirewallRule;
import org.opendaylight.controller.firewall.IFirewall;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.packet.ARP;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IEEE8021Q;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.LLDP;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Firewall implements IFirewall, IObjectReader, IConfigurationContainerAware,IListenDataPacket{
    private IForwardingRulesManager frm;
    private IfIptoHost hostTracker;
    private ExecutorService executor;
    private ISwitchManager switchManager = null;
    private static Logger log = LoggerFactory.getLogger(FirewallRule.class);
    ConcurrentMap<String,FirewallRule> ruleConfigList;
    private IClusterContainerServices clusterContainerService = null;
    private IDataPacketService dataPacketService;
    private Map<Node, Map<Long, NodeConnector>> mac_to_port_per_switch = new HashMap<Node, Map<Long, NodeConnector>>();
    private static String ROOT = GlobalConstants.STARTUPHOME.toString();
    private String firewallFileName = null;
    protected boolean enabled;
    static final String FIREWALL_EVENT_CACHE_NAME = "firewall.requestReplyEvent";
    @Override
    public boolean isEnabled(){
        return enabled;
    }
    /**实现功能:防火墙状态设置*/
    @Override
    public void setstatus(boolean enabled) {
    // TODO Auto-generated method stub
        this.enabled=enabled;
    }
    @Override
    public ConcurrentMap<String, FirewallRule> getRuleConfigList() {
        return ruleConfigList;
    }
    /**
     *实现功能:添加规则
     */
    @Override
    public Status addRule(FirewallRule ruleConfig) {
        // rule validate
        //Status status = ruleConfig.validate(container);
        Status status = ruleConfig.validate();
        if (!status.isSuccess()) {
            log.warn("Invalid Configuration for flow {}. The failure is {}", ruleConfig, status.getDescription());
            String error = "Invalid Configuration (" + status.getDescription() + ")";
            ruleConfig.setStatus(error);
            return new Status(StatusCode.BADREQUEST, error);
        }
        return addRuleInternal(ruleConfig, false);
    }
    /**
     *实现功能:update规则
     */
    @Override
    public Status updateRule(FirewallRule ruleConfig) {
        // rule validate
        //Status status = ruleConfig.validate(container);
        Status status = ruleConfig.validate();
        if (!status.isSuccess()) {
            log.warn("Invalid Configuration for flow {}. The failure is {}", ruleConfig, status.getDescription());
            String error = "Invalid Configuration (" + status.getDescription() + ")";
            ruleConfig.setStatus(error);
            return new Status(StatusCode.BADREQUEST, error);
        }
        ruleConfig.setStatus(StatusCode.SUCCESS.toString());
        if (ruleConfigList.get(ruleConfig.getName()) == null) {
            this.addRule(ruleConfig);
            return new Status(StatusCode.CONFLICT,
                    "Firewall Rule with the specified name does not exist,create new one");
        }
        return modifyRule(ruleConfig);
    }

    @Override
    public FirewallRule getFirewallRule(String name) {
    // TODO Auto-generated method stub
        for (Entry<String, FirewallRule> firewallRule : ruleConfigList.entrySet()) {
            FirewallRule rule = firewallRule.getValue();
            if (rule.getName().equals(name)) {
                return rule;
            }
        }
        return null;
    }
    @Override
    public Status removeFirewallRule(String name,FirewallRule mapRule) {
        // TODO Auto-generated method stub
        Status status=new Status(StatusCode.SUCCESS);
        if(mapRule!=null){
            ruleConfigList.remove(mapRule.getName());
            if(mapRule.getInstallHw().equalsIgnoreCase("true")){
                List<FlowEntry> flowEntry=frm.getFlowEntriesForGroup("FirewallRule");
                if(flowEntry!=null){
                    for (FlowEntry entry : flowEntry){
                        String fName=entry.getFlowName();
                        if(fName.startsWith(name+"_")){
                            status =frm.uninstallFlowEntry(entry);
                        }
                    }
                }else{
                    log.info("flowentry list is null");
                    status= new Status(StatusCode.NOTFOUND);
                }
            }
        }
        if(!status.isSuccess()){
            ruleConfigList.put(mapRule.getName(), mapRule);
        }
        //List<FlowEntry> flowEntry1=frm.getFlowEntriesForGroup("FirewallRule");
        return status;
    }
    @Override
    public Status saveFirewallRule(){
        Status retS = null;
        ObjectWriter objWriter = new ObjectWriter();
        retS = objWriter.write(new ConcurrentHashMap<String, FirewallRule>(
                ruleConfigList), firewallFileName);
        return retS;
    }
    @Override
    public Status saveConfiguration() {
        return saveFirewallRule();
    }
    private Status addRuleInternal(FirewallRule ruleConfig, boolean isAdding) {
        ruleConfig.setStatus(StatusCode.SUCCESS.toString());
        if (ruleConfigList.get(ruleConfig.getName()) != null) {
            return new Status(StatusCode.CONFLICT,
                    "Firewall Rule with the specified name already exists.");
        }
        for (Map.Entry<String, FirewallRule> entry : ruleConfigList.entrySet()) {
            if (entry.getValue().equals(ruleConfig) == true) {
                return new Status(StatusCode.CONFLICT,
                        "This conflicts with an existing firewall rule " +
                                "Configuration. Please check the configuration " +
                                        "and try again");
            }
        }
        ruleConfig.setInstallHw("false");
        ruleConfigList.put(ruleConfig.getName(), ruleConfig);
        return new Status(StatusCode.SUCCESS);
    }
    private Status modifyRule(FirewallRule rule) {
    // TODO Auto-generated method stub
       // FirewallRule mRule=ruleConfigList.get(rule.getName());
        this.removeFirewallRule(rule.getName(),rule);
        this.addRule(rule);
        return new Status(StatusCode.SUCCESS);
    }
    void setDataPacketService(IDataPacketService s) {
        this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }
    @SuppressWarnings({ "unchecked" })
    private void allocateCaches() {
        if (this.clusterContainerService == null) {
            log.info("un-initialized clusterContainerService, can't create cache");
            return;
        }
        try {
            clusterContainerService.createCache(
                    "firewall.rule.configs", EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
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
        ruleConfigList = (ConcurrentMap<String, FirewallRule>) clusterContainerService.getCache("firewall.rule.configs");
        if (ruleConfigList == null) {
            log.error("\nFailed to get rulesDB handle");
        }
    }
    @SuppressWarnings("unchecked")
    private void loadConfiguration() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<String, FirewallRule> confList = (ConcurrentMap<String, FirewallRule>) objReader.read(this, firewallFileName);
        if (confList == null) {
            return;
        }
        for (FirewallRule conf : confList.values()) {
            addRule(conf);
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
    public void setForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        log.debug("Setting ForwardingRulesManager");
        this.frm = forwardingRulesManager;
    }
    public void unsetForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        if (this.frm == forwardingRulesManager) {
            this.frm = null;
        }
    }
    void setSwitchManager(ISwitchManager s) {
        log.debug("SwitchManager set");
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            log.debug("SwitchManager removed!");
            this.switchManager = null;
        }
    }
    public void setHostTracker(IfIptoHost hostTracker) {
        log.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void unsetHostTracker(IfIptoHost hostTracker) {
        if (this.hostTracker == hostTracker) {
            this.hostTracker = null;
        }
    }
    void init(Component c) {
        String containerName = null;
        Dictionary props = c.getServiceProperties();
        if (props != null) {
            containerName = (String) props.get("containerName");
        } else {
            // In the Global instance case the containerName is empty
            containerName = "";
        }
        //isDefaultContainer = containerName.equals(GlobalConstants.DEFAULT
        //        .toString());
        firewallFileName = ROOT + "firewall_" + containerName + ".conf";
        log.debug("firewall starting on container {}",containerName);
        // Instantiate cluster synced variables
        allocateCaches();
        retrieveCaches();
        Executors.newFixedThreadPool(1);
        if (ruleConfigList.isEmpty()) {
            loadConfiguration();
        }
    }
    @Override
    public Object readObject(ObjectInputStream ois) throws FileNotFoundException, IOException, ClassNotFoundException {
    // Perform the class deserialization locally, from inside the package
    // where the class is defined
        return ois.readObject();
    }
    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        // OSGI console
        registerWithOSGIConsole();
    }
    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }
    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
    }
    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
        executor.shutdown();
    }
    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        if (inPkt==null){
            return PacketResult.IGNORED;
        }
        log.trace("Received a frame of size: {}", inPkt.getPacketData().length);
        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        NodeConnector incoming_connector = inPkt.getIncomingNodeConnector();
        Node incoming_node = incoming_connector.getNode();
        if (formattedPak instanceof Ethernet) {
            Object nextPak = formattedPak.getPayload();
            if (nextPak instanceof IPv4&&!((Ethernet)formattedPak).isBroadcast()) {
                byte[] srcMAC = ((Ethernet)formattedPak).getSourceMACAddress();
                long srcMAC_val = BitBufferHelper.toNumber(srcMAC);
                // Set up the mapping: switch -> src MAC address -> incoming port
                if (this.mac_to_port_per_switch.get(incoming_node) == null) {
                    this.mac_to_port_per_switch.put(incoming_node, new HashMap<Long, NodeConnector>());
                }
                if(srcMAC_val==3){
                    System.out.println(srcMAC_val);
                }
                this.mac_to_port_per_switch.get(incoming_node).put(srcMAC_val, incoming_connector);
                if (this.enabled!=true){
                    log.trace("Firewall disabled");
                    return PacketResult.IGNORED;
                    }
                log.trace("Handle IP packet: {}", formattedPak);
                FirewallRule matched_rule=this.matchWithRule(formattedPak, incoming_connector);
                if(matched_rule!= null){
                    Status installStatus=this.installFlow(matched_rule, inPkt, incoming_connector);
                    if(installStatus.isSuccess()){
                        log.info("Firewall rule install flow in sw successfully!");
                    }
                }else{
                    return PacketResult.CONSUME;
                }
            } else if (nextPak instanceof ARP) {
                log.trace("Handle ARP packet: {}", formattedPak);
            } else if (nextPak instanceof IEEE8021Q){
                log.trace("Handle IEEE8021Q packet: {}", formattedPak);
            } else if (nextPak instanceof LLDP){
                log.trace("Handle LLDP packet: {}", formattedPak);
            }
        }
        return PacketResult.IGNORED;
    }
    private FirewallRule matchWithRule(Packet formattedPak, NodeConnector incoming_connector) {
        // TODO Auto-generated method stub
         //match with rule
        String priority=FirewallRule.maxRuleNo;
        FirewallRule matched_rule=null;
        for (Entry<String, FirewallRule> firewallRule : ruleConfigList.entrySet()) {
            boolean rm=true;
            FirewallRule conf = firewallRule.getValue();
            FirewallRule confcopy=new FirewallRule(conf);
            Node ruleNode =confcopy.getNode();
            IPv4 pkt = (IPv4)formattedPak.getPayload();
            InetAddress srcIp=NetUtils.getInetAddress(pkt.getSourceAddress());
            HostNodeConnector sHost=this.hostTracker.hostFind(srcIp);
            String k=incoming_connector.getNode().getNodeIDString();
            if (ruleNode!=null){
                if(k.equals(ruleNode.getNodeIDString())){
                    rm=true;
                }else{
                    if (sHost!=null){
                        NodeConnector sNodeCon=sHost.getnodeConnector();
                        if (!sNodeCon.getNode().equals(ruleNode)){
                            rm=false;
                        }else{
                            rm=true;
                        }
                    }else{
                        rm=false;
                    }
                }

            }
            if (conf.getIngressPort()!=null){
                if(incoming_connector.getNodeConnectorIDString().equals(conf.getIngressPort())){
                    rm=true;
                }else{
                    if (sHost!=null){
                        NodeConnector sNodeCon=sHost.getnodeConnector();
                        if (!sNodeCon.getNodeConnectorIDString().equals(conf.getIngressPort())){
                            rm=false;
                        }else{
                            rm=true;
                        }
                    }else{
                        rm=false;
                    }
                }

            }
            if ((rm==true)&&confcopy.equalToRule(formattedPak,incoming_connector)){
                String prio=conf.getPriority();
                if(Integer.valueOf(prio).intValue()<Integer.valueOf(priority).intValue()){
                    priority=prio;
                    matched_rule=conf;
                }else{
                    matched_rule=conf;
                }
            }
        }
        return matched_rule;
    }
    private NodeConnector getDstMac(RawPacket inPkt, NodeConnector incoming_connector){
        // TODO Auto-generated method stub
        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        Node incoming_node = incoming_connector.getNode();
        byte[] srcMAC = ((Ethernet)formattedPak).getSourceMACAddress();
        byte[] dstMAC = ((Ethernet)formattedPak).getDestinationMACAddress();
        long srcMAC_val = BitBufferHelper.toNumber(srcMAC);
        long dstMAC_val = BitBufferHelper.toNumber(dstMAC);
        Match match = new Match();
        match.setField( new MatchField(MatchType.IN_PORT, incoming_connector) );
        match.setField( new MatchField(MatchType.DL_DST, dstMAC.clone()) );
        // Set up the mapping: switch -> src MAC address -> incoming port
        if (this.mac_to_port_per_switch.get(incoming_node) == null) {
            this.mac_to_port_per_switch.put(incoming_node, new HashMap<Long, NodeConnector>());
        }
        this.mac_to_port_per_switch.get(incoming_node).put(srcMAC_val, incoming_connector);

        NodeConnector dst_connector = this.mac_to_port_per_switch.get(incoming_node).get(dstMAC_val);
        // Do I know the destination MAC?
        if (dst_connector != null) {
            if(dstMAC_val>100){
                System.out.println("dstMAC_val");
            }
            this.mac_to_port_per_switch.get(dst_connector.getNode()).put(dstMAC_val, dst_connector);
        }else{
            Set<NodeConnector> nodeConnectors =
                    this.switchManager.getUpNodeConnectors(incoming_node);

            for (NodeConnector p : nodeConnectors) {
                if (!p.equals(incoming_connector)) {
                    try {
                        RawPacket destPkt = new RawPacket(inPkt);
                        destPkt.setOutgoingNodeConnector(p);
                        this.dataPacketService.transmitDataPacket(destPkt);
                    } catch (ConstructionException e2) {
                        continue;
                    }
                }
            }
         }
        return dst_connector;
    }
    private Status installFlow(FirewallRule rule,RawPacket inPkt,NodeConnector incoming_connector) {
        // TODO Auto-generated method stub
        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        FlowEntry flowentry=rule.changeToFlow(formattedPak,incoming_connector);
        if (rule.getActions().equalsIgnoreCase("allow")){
            NodeConnector dstNode=this.getDstMac(inPkt, incoming_connector);
            if(dstNode==null){
                return new Status(StatusCode.NOTFOUND);
            }
            List<Action> actions = new ArrayList<Action>();
            actions.add(new Output(dstNode));
            flowentry.getFlow().setActions(actions);
        }
        Status status = this.frm.installFlowEntry(flowentry);
        if (!status.isSuccess()) {
            log.error("Failed to install policy: "
                    + flowentry.getGroupName() + " ("
                    + status.getDescription() + ")");
            return new Status(StatusCode.NOTIMPLEMENTED, "Install flowentry Refused");
        } else {
            log.debug("Successfully installed policy "
                    + flowentry.toString() + " on switch " + flowentry.getNode().getNodeIDString());
            ruleConfigList.get(rule.getName()).setInstallHw("true");
        }
        return new Status(StatusCode.SUCCESS);
    }
}
