/*
 * Copyright (c) 2013 BNC Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.firewall.web;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.Name;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.switchmanager.SwitchConfig;
import org.opendaylight.controller.web.DaylightWebUtil;
import org.opendaylight.controller.web.IDaylightWeb;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.opendaylight.controller.firewall.IFirewall;
import org.opendaylight.controller.firewall.FirewallRule;

import com.google.gson.Gson;

//import org.opendaylight.controller.sal.utils.EtherTypes;
//import org.opendaylight.controller.sal.utils.IPProtocols;

@Controller
@RequestMapping("/")
public class Firewall implements IDaylightWeb {
    private static final UserLevel AUTH_LEVEL = UserLevel.CONTAINERUSER;
    private static final String WEB_NAME = "Firewall";

    private static final String WEB_ID = "firewall";
    private static final short WEB_ORDER = 5;
    private short status = 1;

    private final Gson gson;

    public Firewall() {
        ServiceHelper.registerGlobalService(IDaylightWeb.class, this, null);
        gson = new Gson();
    }

    @Override
    public String getWebName() {
        return WEB_NAME;
    }

    @Override
    public String getWebId() {
        return WEB_ID;
    }

    @Override
    public short getWebOrder() {
        return WEB_ORDER;
    }

    @Override
    public boolean isAuthorized(UserLevel userLevel) {
        return userLevel.ordinal() <= AUTH_LEVEL.ordinal();
    }

    @RequestMapping(value = "/firewall_lists")
    @ResponseBody
    public Map<String, Object> getFirewalls(HttpServletRequest request,
            @RequestParam(required = false) String container) {
        Map<String, Object> output = new HashMap<String, Object>();

        // Template etherTypes and protocols must be provided by interfaces
        // dlTypes
        Map<String, String> map_etherTypes = new HashMap<String, String>(3);
        // for(EtherTypes s : EtherTypes.values()){
        // map_etherTypes.put(Integer.toString(s.intValue()), s.name());
        // }
        map_etherTypes.put("0x800", "IPv4");
        output.put("etherTypes", map_etherTypes);

        // nwProtos
        Map<String, String> map_protocols = new HashMap<String, String>();
        // for(IPProtocols s : IPProtocols.values()){
        // map_protocols.put(Integer.toString(s.intValue()), s.name());
        // }
        map_protocols.put("1", "ICMP");
        map_protocols.put("6", "TCP");
        map_protocols.put("17", "UDP");
        output.put("protocols", map_protocols);

        // actions
        Map<String, String> map_actions = new HashMap<String, String>(3);
        map_actions.put("DENY", "DENY");
        map_actions.put("ALLOW", "ALLOW");
        map_actions.put("DISPATCH:", "DISPATCH");
        output.put("actions", map_actions);

        // statuses
        Map<String, String> map_statuses = new HashMap<String, String>(2);
        map_statuses.put("0", "Disabled");
        map_statuses.put("1", "Enabled");
        output.put("statuses", map_statuses);
        return output;
    }

    @RequestMapping(value = "/setStatus")
    @ResponseBody
    public String setStatus(HttpServletRequest request,
            @RequestParam(required = true) String status,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;
        IFirewall fw = (IFirewall) ServiceHelper.getInstance(IFirewall.class,
                containerName, this);
        if (fw == null) {
            return null;
        }
        if (status.equals("2")) {
            this.status = fw.isEnabled() ? (short) 1 : (short) 0;
        } else {
            this.status = Short.parseShort(status);
            fw.setstatus(this.status == 1);
        }

        return String.valueOf(this.status);
    }

    @RequestMapping(value = "/main")
    @ResponseBody
    public Map<String, Object> getFirewall(HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        Privilege privilege = DaylightWebUtil.getContainerPrivilege(userName,
                containerName, this);
        if (privilege == Privilege.NONE) {
            return null;
        }
        // fetch sm
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            return null;
        }
        IFirewall fw = (IFirewall) ServiceHelper.getInstance(IFirewall.class,
                containerName, this);
        if (fw == null) {
            return null;
        }

        // get static firewall list
        ConcurrentMap<String, FirewallRule> firewallMap = fw
                .getRuleConfigList();
        Set<Map<String, Object>> firewallSet = new HashSet<Map<String, Object>>();
        Collection<FirewallRule> firewallC = firewallMap.values();
        for (FirewallRule firewallConfig : firewallC) {
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put("firewall", firewallConfig);
            entry.put("name", firewallConfig.getName());
            entry.put("action", firewallConfig.getAction());
            entry.put("id", firewallConfig.getId());
            Node node = firewallConfig.getNode();
            if (node != null) {
                entry.put("node", getNodeDesc(node, switchManager));
                entry.put("nodeId", node.toString());
            } else {
                entry.put("node", null);
                entry.put("nodeId", null);
            }
            firewallSet.add(entry);
        }

        Map<String, Object> output = new HashMap<String, Object>(3);
        output.put("firewalls", firewallSet);
        output.put("privilege", privilege);
        output.put("moduleStatus", fw.isEnabled() ? "1" : "0");
        return output;
    }

    @RequestMapping(value = "/firewall", method = RequestMethod.POST)
    @ResponseBody
    public String actionNewFirewall(@RequestParam(required = true) String body,
            @RequestParam(required = false) String nodeId,
            HttpServletRequest request,
            @RequestParam(required = false) String container) {
        Status result = null;
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil
                .getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return "Operation not authorized";
        }

        IFirewall fw = (IFirewall) ServiceHelper.getInstance(IFirewall.class,
                containerName, this);
        if (fw == null) {
            return null;
        }

        FirewallRule firewall = gson.fromJson(body, FirewallRule.class);
        Node node = Node.fromString(nodeId);
        firewall.setNode(node);
        firewall.setInstallSoon("0");

        if (firewall.getId().equals("0")) {
            result = fw.addRule(firewall);
        } else {
            result = fw.updateRule(firewall.getId(), firewall);
        }

        return (result.isSuccess()) ? StatusCode.SUCCESS.toString() : result
                .getDescription();
    }
    @RequestMapping(value = "/firewall/install", method = RequestMethod.POST)
    @ResponseBody
    public String actionNewFirewallInstall(@RequestParam(required = true) String body,
            @RequestParam(required = false) String nodeId,
            HttpServletRequest request,
            @RequestParam(required = false) String container) {
        Status result = null;
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil
                .getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return "Operation not authorized";
        }

        IFirewall fw = (IFirewall) ServiceHelper.getInstance(IFirewall.class,
                containerName, this);
        if (fw == null) {
            return null;
        }

        FirewallRule firewall = gson.fromJson(body, FirewallRule.class);
        String action=firewall.getAction();
        if (action==null||!action.equals("DENY")){
            return "Action must be deny";
        }
        Node node = Node.fromString(nodeId);
        firewall.setNode(node);
        firewall.setInstallSoon("1");

        if (firewall.getId().equals("0")) {
            result = fw.installRule(firewall);
        } else {
            return "cannot updateRule and install flowentry";
            //result = fw.updateRule(firewall.getId(), firewall);
        }

        return (result.isSuccess()) ? StatusCode.SUCCESS.toString() : result
                .getDescription();
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/firewall/deleteFirewalls", method = RequestMethod.POST)
    @ResponseBody
    public String removeSelectedFirewalls(
            @RequestParam(required = false) String firewallListIds,
            HttpServletRequest request,
            @RequestParam(required = false) String container) {
        Status result = null;
        boolean status = true;
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;
        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil
                .getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return "Operation not authorized";
        }

        IFirewall fw = (IFirewall) ServiceHelper.getInstance(IFirewall.class,
                containerName, this);
        if (fw == null) {
            return null;
        }

        String[] frList = firewallListIds.split(";");
        for (String id : frList) {
            result = fw.removeFirewallRule(id);
            if (!result.isSuccess()) {
                status = false;
                break;
            }
        }
        return status ? "Success" : "Failure";
    }

    @RequestMapping(value = "/firewall/toggleFirewalls", method = RequestMethod.POST)
    @ResponseBody
    public String toggleFirewalls(
            @RequestParam(required = true) String firewallListIds,
            @RequestParam(required = false) String firewallStatus,
            HttpServletRequest request,
            @RequestParam(required = false) String container) {
        // Status result = null;
        // boolean status = true;
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Authorization check
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil
                .getContainerPrivilege(userName, containerName, this) != Privilege.WRITE) {
            return "Operation not authorized";
        }
        IFirewall fw = (IFirewall) ServiceHelper.getInstance(IFirewall.class,
                containerName, this);
        if (fw == null) {
            return null;
        }

        String[] frList = firewallListIds.split(";");
        for (String id : frList) {
            FirewallRule fr = fw.getFirewallRule(id);
            fr.setStatus(firewallStatus);
        }
        return "Success";// (result.isSuccess()) ? StatusCode.SUCCESS.toString()
                         // : result.getDescription();
    }

    // //////////////////////////////////

    @RequestMapping(value = "/node-ports")
    @ResponseBody
    public Map<String, Object> getNodePorts(HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil
                .getContainerPrivilege(userName, containerName, this) == Privilege.NONE) {
            return null;
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            return null;
        }

        Map<String, Object> nodes = new HashMap<String, Object>();
        Map<Short, String> port;

        for (Switch node : switchManager.getNetworkDevices()) {
            port = new HashMap<Short, String>(); // new port
            Set<NodeConnector> nodeConnectorSet = node.getNodeConnectors();

            if (nodeConnectorSet != null) {
                for (NodeConnector nodeConnector : nodeConnectorSet) {
                    String nodeConnectorName = ((Name) switchManager
                            .getNodeConnectorProp(nodeConnector,
                                    Name.NamePropName)).getValue();
                    port.put((Short) nodeConnector.getID(), nodeConnectorName
                            + "(" + nodeConnector.getNodeConnectorIDString()
                            + ")");
                }
            }

            // add ports
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put("ports", port);

            // add name
            entry.put("name", getNodeDesc(node.getNode(), switchManager));

            // add to the node
            nodes.put(node.getNode().toString(), entry);
        }

        return nodes;
    }

    @RequestMapping(value = "/node-firewalls")
    @ResponseBody
    public Map<String, Object> getNodeFirewalls(HttpServletRequest request,
            @RequestParam(required = false) String container) {
        String containerName = (container == null) ? GlobalConstants.DEFAULT
                .toString() : container;

        // Derive the privilege this user has on the current container
        String userName = request.getUserPrincipal().getName();
        if (DaylightWebUtil
                .getContainerPrivilege(userName, containerName, this) == Privilege.NONE) {
            return null;
        }

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        if (switchManager == null) {
            return null;
        }
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper
                .getInstance(IForwardingRulesManager.class, containerName, this);
        if (frm == null) {
            return null;
        }

        Map<String, Object> nodes = new HashMap<String, Object>();

        for (Switch sw : switchManager.getNetworkDevices()) {
            Node node = sw.getNode();

            List<FlowConfig> firewalls = frm.getStaticFlows(node);

            String nodeDesc = node.toString();
            SwitchConfig config = switchManager
                    .getSwitchConfig(node.toString());
            if ((config != null)
                    && (config.getProperty(Description.propertyName) != null)) {
                nodeDesc = ((Description) config
                        .getProperty(Description.propertyName)).getValue();
            }

            nodes.put(nodeDesc, firewalls.size());
        }

        return nodes;
    }

    private String getNodeDesc(Node node, ISwitchManager switchManager) {
        Description desc = (Description) switchManager.getNodeProp(node,
                Description.propertyName);
        String description = (desc == null) ? "" : desc.getValue();
        return (description.isEmpty() || description.equalsIgnoreCase("none")) ? node
                .toString() : description;
    }

}
