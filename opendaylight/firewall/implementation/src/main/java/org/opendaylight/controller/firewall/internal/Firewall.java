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
//import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.dm.Component;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.firewall.FirewallRule;
import org.opendaylight.controller.firewall.IFirewall;
import org.opendaylight.controller.firewall.FirewallRule.ActionType;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.core.Config;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.packet.ARP;
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
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Firewall implements IFirewall, IObjectReader, IConfigurationContainerAware,IListenDataPacket,IInventoryListener,IfNewHostNotify{
    private IForwardingRulesManager frm;
    private IfIptoHost hostTracker;
    private ExecutorService executor;
    private ISwitchManager switchManager;
    private static Logger log = LoggerFactory.getLogger(FirewallRule.class);
    ConcurrentMap<String,FirewallRule> ruleConfigList;
    private Map<String,String> id_ruleName=new HashMap<String,String>();
    private IClusterContainerServices clusterContainerService = null;
    private IDataPacketService dataPacketService;
    private Map<Node, Map<String, NodeConnector>> mac_to_port_per_switch = new HashMap<Node, Map<String, NodeConnector>>();
    private Map<String,String> value = new HashMap<String,String>();
    private static String ROOT = GlobalConstants.STARTUPHOME.toString();
    private String firewallFileName = null;
    protected boolean enabled;
    static final String FIREWALL_EVENT_CACHE_NAME = "firewall.requestReplyEvent";
    @Override
    public boolean isEnabled(){
        return enabled;
    }
    @Override
    public void setstatus(boolean enabled) {
        this.enabled=enabled;
    }
    @Override
    public ConcurrentMap<String, FirewallRule> getRuleConfigList() {
        return ruleConfigList;
    }
    @Override
    public Status addRule(FirewallRule ruleConfig) {
        // rule validate
        Status status = ruleConfig.validate();
        if (!status.isSuccess()) {
            log.warn("Invalid Configuration for flow {}. The failure is {}", ruleConfig, status.getDescription());
            String error = "Invalid Configuration (" + status.getDescription() + ")";
            return new Status(StatusCode.BADREQUEST, error);
        }
        int idInt=0;
/*        if (ruleConfigList.get(ruleConfig.getName()) != null) {
            return new Status(StatusCode.CONFLICT,
                    "Firewall Rule with the specified name already exists.");
        }*/
        for (Map.Entry<String, FirewallRule> entry : ruleConfigList.entrySet()) {
            FirewallRule rule=entry.getValue();
            if (rule.equals(ruleConfig) == true) {
                return new Status(StatusCode.CONFLICT,
                        "This conflicts with an existing firewall rule " +
                                "Configuration. Please check the configuration " +
                                        "and try again");
            }
//            id_ruleName.put(rule.getId(), rule.getName());
            String entryId=entry.getValue().getId();
            int ruleIdInteger=Integer.parseInt(entryId);
            if (ruleIdInteger>idInt){
                idInt=ruleIdInteger;
            }
        }
        String id=String.valueOf(idInt+1);
        String ruleId=id+"_"+ruleConfig.getName();
        ruleConfig.setId(id);
        ruleConfig.setRuleId(ruleId);
        ruleConfig.setInstallHw("false");
        ruleConfigList.put(ruleId, ruleConfig);
        id_ruleName.put(id, ruleConfig.getName());
        return new Status(StatusCode.SUCCESS);
    }
    @Override
    public String addRuleInternal(FirewallRule ruleConfig) {
        // rule validate
        Status status = ruleConfig.validate();
        if (!status.isSuccess()) {
            log.warn("Invalid Configuration for flow {}. The failure is {}", ruleConfig, status.getDescription());
            return "0";
        }
        int idInt=0;
/*        if (ruleConfigList.get(ruleConfig.getName()) != null) {
            return new Status(StatusCode.CONFLICT,
                    "Firewall Rule with the specified name already exists.");
        }*/
        for (Map.Entry<String, FirewallRule> entry : ruleConfigList.entrySet()) {
            FirewallRule rule=entry.getValue();
            if (rule.equals(ruleConfig) == true) {
                return "0";
            }
//            id_ruleName.put(rule.getId(), rule.getName());
            int ruleIdInteger=Integer.parseInt(entry.getKey());
            if (ruleIdInteger>idInt){
                idInt=ruleIdInteger;
            }
        }
        String id=String.valueOf(idInt+1);
        String ruleId=id+"_"+ruleConfig.getName();
        ruleConfig.setId(id);
        ruleConfig.setRuleId(ruleId);
        ruleConfig.setInstallHw("false");
        ruleConfigList.put(ruleId, ruleConfig);
        id_ruleName.put(id, ruleConfig.getName());
        return id;
    }
    /*
    @Override
    public Status updateRule(FirewallRule ruleConfig) {
        // rule validate
        //Status status = ruleConfig.validate(container);
        Status status = ruleConfig.validate();
        if (!status.isSuccess()) {
            log.warn("Invalid Configuration for flow {}. The failure is {}", ruleConfig, status.getDescription());
            String error = "Invalid Configuration (" + status.getDescription() + ")";
            return new Status(StatusCode.BADREQUEST, error);
        }
        if (ruleConfigList.get(ruleConfig.getName()) == null) {
            status=this.addRule(ruleConfig);
            if (status.isSuccess()){
                return new Status(StatusCode.SUCCESS,
                    "Success! New one firewallRule created,because firewall Rule with the specified name does not exist\n");
            }else{
                return new Status(StatusCode.INTERNALERROR,
                    "Firewall Rule with the specified name does not exist,create new one failed\n");
            }
        }
        return modifyRule(ruleConfig);
    }*/
    @Override
    public Status updateRule(String id,FirewallRule ruleConfig) {
        // rule validate
        //Status status = ruleConfig.validate(container);
        Status status = ruleConfig.validate();
        if (!status.isSuccess()) {
            log.warn("Invalid Configuration for flow {}. The failure is {}", ruleConfig, status.getDescription());
            String error = "Invalid Configuration (" + status.getDescription() + ")";
            return new Status(StatusCode.BADREQUEST, error);
        }
        FirewallRule mapRule=this.getFirewallRule(id);
        if (mapRule == null) {
            return new Status(StatusCode.BADREQUEST,"No such firewall rule\n");
        }
        if (!mapRule.getId().equals(id)){
            return new Status(StatusCode.BADREQUEST,"modified rule id is not equal with id\n");
        }
        String ruleName=id_ruleName.get(id);
        String ruleId=id+"_"+ruleName;
        if(this.removeFirewallRule(id).isSuccess()){
            ruleConfig.setId(id);
            ruleConfig.setInstallHw("false");
            ruleConfigList.put(ruleId, ruleConfig);
            id_ruleName.put(id, ruleConfig.getName());
            return new Status(StatusCode.SUCCESS,"Success! FirewallRule modifid\n");
        }
        return new Status(StatusCode.INTERNALERROR,"modify rule failed\n");
    }
    @Override
    public FirewallRule getFirewallRule(String id) {
        for (Entry<String, FirewallRule> firewallRule : ruleConfigList.entrySet()) {
            FirewallRule rule = firewallRule.getValue();
            if (rule.getId().equals(id)) {
                return rule;
            }
        }
        return null;
    }
    @Override
    public Status removeFirewallRule(String id) {
        Status status=new Status(StatusCode.SUCCESS);
        FirewallRule mapRule = this.getFirewallRule(id);
        if (mapRule == null) {
            return new Status(StatusCode.NOTFOUND,"ID"+id+"firewall rule does not exist");
        }
        String ruleName=id_ruleName.get(id);
        String ruleId=id+"_"+ruleName;
        ruleConfigList.remove(ruleId);
        id_ruleName.remove(id);
        String installHw=mapRule.getInstallHw();
        if(installHw!=null&&installHw.equalsIgnoreCase("true")){
            List<FlowEntry> flowEntry=frm.getFlowEntriesForGroup("FirewallRule");
            if(flowEntry!=null){
                for (FlowEntry entry : flowEntry){
                    String fName=entry.getFlowName();
                    if(fName.startsWith(ruleId+"_")){
                        status =frm.uninstallFlowEntry(entry);
                    }
                }
            }else{
                log.info("flowentry list is null");
            }
        }
        if(!status.isSuccess()){
            ruleConfigList.put(ruleId, mapRule);
            id_ruleName.put(id, mapRule.getName());
        }
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
    public String getRuleName_Id(String ruleName){
        String ids="";
        String idset=",null";
        for(Entry<String,String> entry:id_ruleName.entrySet()){
            if (entry.getValue().equals(ruleName)){
                idset=ids+","+entry.getKey();
                ids=idset;
            }
        }
        idset=idset.substring(1);
        return idset;
    }
    @Override
    public Map<String,String> getRuleName_Ids(){
        Map<String,String> ruleName_ids=new HashMap<String,String>();
        String ruleName=null;
        String idset="";
        for(Entry<String,String> entry:id_ruleName.entrySet()){
            ruleName=entry.getValue();
            if(ruleName_ids.get(ruleName)==null){
                ruleName_ids.put(ruleName, entry.getKey());
            }else{
                idset=ruleName_ids.get(ruleName)+","+entry.getKey();
                ruleName_ids.put(ruleName, idset);
            }
        }
        return ruleName_ids;
    }
    @Override
    public Status saveConfiguration() {
        return saveFirewallRule();
    }
/*    private Status modifyRule(FirewallRule rule) {
       // FirewallRule mRule=ruleConfigList.get(rule.getName());
        if(this.removeFirewallRule(rule.getName(),rule).isSuccess())
            if(this.addRule(rule).isSuccess()){
                return new Status(StatusCode.SUCCESS,"Success! FirewallRule modifid\n");
            }
        return new Status(StatusCode.INTERNALERROR,"modify rule failed\n");
    }*/
    void setDataPacketService(IDataPacketService s) {
        this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }
    @SuppressWarnings({ })
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
      //  Executors.newFixedThreadPool(1);
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
                String srcMAC_string = FirewallRule.macByteToString(srcMAC);
                // Set up the mapping: switch -> src MAC address -> incoming port
                if (this.mac_to_port_per_switch.get(incoming_node) == null) {
                    this.mac_to_port_per_switch.put(incoming_node, new HashMap<String, NodeConnector>());
                }
                this.mac_to_port_per_switch.get(incoming_node).put(srcMAC_string, incoming_connector);
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
         //match with rule
        String priority=FirewallRule.maxRuleNo;
        FirewallRule matched_rule=null;
        for (Entry<String, FirewallRule> firewallRule : ruleConfigList.entrySet()) {
            FirewallRule conf = firewallRule.getValue();
            boolean nodeEqual=true;
            Node ruleNode =conf.getNode();
            IPv4 pkt = (IPv4)formattedPak.getPayload();
            InetAddress srcIp=NetUtils.getInetAddress(pkt.getSourceAddress());
            HostNodeConnector srcHost=this.hostTracker.hostFind(srcIp);
            String incomingNode=incoming_connector.getNode().getNodeIDString();
            if (ruleNode!=null){
                if(incomingNode.equals(ruleNode.getNodeIDString())){
                    nodeEqual=true;
                }else{
                    if (srcHost!=null){
                        NodeConnector srcNodeCon=srcHost.getnodeConnector();
                        if (!srcNodeCon.getNode().equals(ruleNode)){
                            nodeEqual=false;
                        }else{
                            nodeEqual=true;
                        }
                    }else{
                        nodeEqual=false;
                    }
                }

            }
            if (conf.getIngressPort()!=null){
                if(incoming_connector.getNodeConnectorIDString().equals(conf.getIngressPort())){
                    nodeEqual=true;
                }else{
                    if (srcHost!=null){
                        NodeConnector sNodeCon=srcHost.getnodeConnector();
                        if (!sNodeCon.getNodeConnectorIDString().equals(conf.getIngressPort())){
                            nodeEqual=false;
                        }else{
                            nodeEqual=true;
                        }
                    }else{
                        nodeEqual=false;
                    }
                }
            }
            if ((nodeEqual==true)&&conf.equalToRule(formattedPak,incoming_connector)){
                String prio=conf.getPriority();
                if(Integer.valueOf(prio).intValue()<Integer.valueOf(priority).intValue()){
                    priority=prio;
                    matched_rule=conf;
                }
            }
        }
        return matched_rule;
    }
    private NodeConnector getDstMac(RawPacket inPkt, NodeConnector incoming_connector){
        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        Node incoming_node = incoming_connector.getNode();
        byte[] srcMAC = ((Ethernet)formattedPak).getSourceMACAddress();
        byte[] dstMAC = ((Ethernet)formattedPak).getDestinationMACAddress();
        String srcMAC_string = FirewallRule.macByteToString(srcMAC);
        String dstMAC_string = FirewallRule.macByteToString(dstMAC);
        Match match = new Match();
        match.setField( new MatchField(MatchType.IN_PORT, incoming_connector) );
        match.setField( new MatchField(MatchType.DL_DST, dstMAC.clone()) );
        // Set up the mapping: switch -> src MAC address -> incoming port
        if (this.mac_to_port_per_switch.get(incoming_node) == null) {
            this.mac_to_port_per_switch.put(incoming_node, new HashMap<String, NodeConnector>());
        }
        this.mac_to_port_per_switch.get(incoming_node).put(srcMAC_string, incoming_connector);

        NodeConnector dst_connector = this.mac_to_port_per_switch.get(incoming_node).get(dstMAC_string);
        // Do I know the destination MAC?
        if (dst_connector != null) {
            this.mac_to_port_per_switch.get(dst_connector.getNode()).put(dstMAC_string, dst_connector);
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
    @SuppressWarnings("unused")
    private Status installFlow(FirewallRule rule,RawPacket inPkt,NodeConnector incoming_connector) {
        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        FlowEntry flowentry=rule.changeToFlow(formattedPak,incoming_connector);
        List<Action> actionList = new ArrayList<Action>();
        Matcher sstr;
        sstr = Pattern.compile(ActionType.DENY.toString()).matcher(rule.getAction());
        if (sstr.matches()) {
            actionList.add(new Drop());
        }
        else {
            sstr= Pattern.compile(ActionType.DISPATCH + ":(.*)").matcher(rule.getAction());
            if (sstr.matches()) {
                NodeConnector dMacConnector=null;
                InetAddress dIp=NetUtils.parseInetAddress(sstr.group(1));
                HostNodeConnector dHost=this.hostTracker.hostFind(dIp);
                Node dHostN=incoming_connector.getNode();
                if (dHost!=null){
                    byte[] dMac=dHost.getDataLayerAddressBytes();
                    String dMac_string = FirewallRule.macByteToString(dMac);
                    dMacConnector=this.mac_to_port_per_switch.get(dHostN).get(dMac_string);
                    if (dMacConnector!=null){
                        if (!dMacConnector.equals(incoming_connector)){
                            actionList.add(new Output(dMacConnector));
                        }else{
                            log.debug("dMacConnector equals incoming_connector");
                        }
                    }else{
                        return new Status(StatusCode.NOTFOUND);
                    }
                }else{
                    return new Status(StatusCode.NOTFOUND);
                }
                NodeConnector dstNode=this.getDstMac(inPkt, incoming_connector);
                if(dstNode==null){
                    return new Status(StatusCode.NOTFOUND);
                }
                if (!dstNode.equals(dMacConnector)){
                    actionList.add(new Output(dstNode));
                }
            }
        }
        if (Pattern.compile(ActionType.ALLOW.toString()).matcher(rule.getAction()).matches()){
            NodeConnector dstNode=this.getDstMac(inPkt, incoming_connector);
            if(dstNode==null){
                return new Status(StatusCode.NOTFOUND);
            }
            actionList.add(new Output(dstNode));
        }
        if (actionList!=null){
            flowentry.getFlow().setActions(actionList);
        }else{
            return new Status(StatusCode.INTERNALERROR);
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
            ruleConfigList.get(rule.getRuleId()).setInstallHw("true");
        }
        return new Status(StatusCode.SUCCESS);
    }
    @Override
    public void notifyHTClientHostRemoved(HostNodeConnector host) {
        if (host == null) {
            return;
        }
        byte[] hMac=host.getDataLayerAddressBytes();
        String hostMac=FirewallRule.macByteToString(hMac);
        List<FlowEntry> flowEntrys=frm.getFlowEntriesForGroup("FirewallRule");
        for (FlowEntry entry: flowEntrys){
            Match match=entry.getFlow().getMatch();
            byte[] sMac=(byte[])(match.getField(MatchType.DL_SRC).getValue());
            byte[] dMac=(byte[])(match.getField(MatchType.DL_DST).getValue());
            String srcMac=FirewallRule.macByteToString(sMac);
            String dstMac=FirewallRule.macByteToString(dMac);
            if (hostMac.equalsIgnoreCase(srcMac)||hostMac.equalsIgnoreCase(dstMac)){
                frm.uninstallFlowEntry(entry);
            }
        }
        for(Node mac_node : mac_to_port_per_switch.keySet()){
            Map<String,NodeConnector> mac_connector=mac_to_port_per_switch.get(mac_node);
            if(mac_connector.containsKey(hostMac)){
                mac_to_port_per_switch.get(mac_node).remove(hostMac);
            }
        }
    }

    @Override
    public void notifyHTClient(HostNodeConnector host) {
        if (host == null){
            return;
        }
    }
    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Map<String, Property> propMap) {
        if (nodeConnector == null)
            return;
        switch (type) {
        case ADDED:
            break;
        case REMOVED:
            removeFlowEntryOnNodeConnectorDown(nodeConnector);
            break;
        case CHANGED:
            Config config = (propMap == null) ? null : (Config) propMap.get(Config.ConfigPropName);
            if (config != null) {
                switch (config.getValue()) {
                case Config.ADMIN_DOWN:
                    log.trace("Port {} is administratively down: uninstalling interested flows", nodeConnector);
                    removeFlowEntryOnNodeConnectorDown(nodeConnector);
                    break;
                default:
                }
            }
            break;
        default:
            return;
        }
    }
    private void removeFlowEntryOnNodeConnectorDown(NodeConnector nodeConnector) {
        Node node=nodeConnector.getNode();
        List<FlowEntry> flowEntrys=frm.getFirewallEntry();
        if(flowEntrys.isEmpty()){
            flowEntrys=frm.getFlowEntriesForNode(node);
        }
        Map<String,Map<String,String>> entryDlMac = new HashMap<String,Map<String,String>>();
        for (FlowEntry entry: flowEntrys){
            value = new HashMap<String,String>();
            Flow flow=entry.getFlow();
            Match match = flow.getMatch();
            if (match.isPresent(MatchType.IN_PORT)) {
                NodeConnector matchPort = (NodeConnector) match.getField(MatchType.IN_PORT).getValue();
                if (matchPort.equals(nodeConnector)) {
                    byte[] sMac=(byte[])(match.getField(MatchType.DL_SRC).getValue());
                    byte[] dMac=(byte[])(match.getField(MatchType.DL_DST).getValue());
                    String srcMac=FirewallRule.macByteToString(sMac);
                    String dstMac=FirewallRule.macByteToString(dMac);
                    value.put(srcMac, dstMac);
                    String key=entry.getFlowName();
                    entryDlMac.put(key, value);
                }
            }else{
                List<Action> actions=flow.getActions();
                if (actions != null) {
                    for (Action action : actions) {
                        if (action instanceof Output) {
                            NodeConnector actionPort = ((Output) action).getPort();
                            if (actionPort.equals(nodeConnector)) {
                                byte[] sMac=(byte[])(match.getField(MatchType.DL_SRC).getValue());
                                byte[] dMac=(byte[])(match.getField(MatchType.DL_DST).getValue());
                                String srcMac=FirewallRule.macByteToString(sMac);
                                String dstMac=FirewallRule.macByteToString(dMac);
                                value.put(srcMac, dstMac);
                                String key=entry.getFlowName();
                                entryDlMac.put(key, value);
                            }
                        }
                    }
                }
            }
        }
        if (!entryDlMac.isEmpty()){
            Status status=removeEntryBasedOnMaclist(entryDlMac);
        }
        if (mac_to_port_per_switch.containsKey(node)){
            mac_to_port_per_switch.remove(node);
        }
        frm.clearFirewallEntryList();
    }
    private Status removeEntryBasedOnMaclist(
            Map<String, Map<String, String>> entryDlMac) {
        Status status=new Status(StatusCode.SUCCESS);
        List<FlowEntry> flowEntrys=frm.getFlowEntriesForGroup("FirewallRule");
        for (FlowEntry entry: flowEntrys){
            value = new HashMap<String,String>();
            Match match=entry.getFlow().getMatch();
            byte[] sMac=(byte[])(match.getField(MatchType.DL_SRC).getValue());
            byte[] dMac=(byte[])(match.getField(MatchType.DL_DST).getValue());
            String srcMac=FirewallRule.macByteToString(sMac);
            String dstMac=FirewallRule.macByteToString(dMac);
            value.put(srcMac, dstMac);
            if (entryDlMac.containsValue(value)){
                status=frm.uninstallFlowEntry(entry);
            }
        }
        return status;
    }
    @Override
    public void notifyNode(Node node, UpdateType type,
            Map<String, Property> propMap) {
        if (node == null){
            return;
        }
        switch (type) {
        case ADDED:
            break;
        case REMOVED:
            List<FlowEntry> flowEntrys=frm.getFirewallEntry();
            if(flowEntrys.isEmpty()){
                flowEntrys=frm.getFlowEntriesForGroup("FirewallRule");
            }
            Map<String,Map<String,String>> entryDlMac = new HashMap<String,Map<String,String>>();
            for (FlowEntry entry: flowEntrys){
                value = new HashMap<String,String>();
                Node nodeMap=entry.getNode();
                Object nodeId=node.getID();
                if (nodeMap.getID().equals(nodeId)){
                    Match match=entry.getFlow().getMatch();
                    byte[] sMac=(byte[])(match.getField(MatchType.DL_SRC).getValue());
                    byte[] dMac=(byte[])(match.getField(MatchType.DL_DST).getValue());
                    String srcMac=FirewallRule.macByteToString(sMac);
                    String dstMac=FirewallRule.macByteToString(dMac);
                    value.put(srcMac, dstMac);
                    String key=entry.getFlowName();
                    entryDlMac.put(key, value);
//                    this.frm.uninstallFlowEntry(entry);
                }
            }
            if (!entryDlMac.isEmpty()){
                Status status=removeEntryBasedOnMaclist(entryDlMac);
            }
            if (mac_to_port_per_switch.containsKey(node)){
                mac_to_port_per_switch.remove(node);
            }
            frm.clearFirewallEntryList();
            break;
        case CHANGED:
            break;
        default:
            return;
        }

    }
}
