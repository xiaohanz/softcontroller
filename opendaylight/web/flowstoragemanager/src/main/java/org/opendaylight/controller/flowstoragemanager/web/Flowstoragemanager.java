/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.flowstoragemanager.web;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.opendaylight.controller.flowstoragemanager.IFlowStorage;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.web.IDaylightWeb;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class Flowstoragemanager implements IDaylightWeb{
    private Connection conn = null;
    private IFlowStorage dbstorage = null;
    private final String WEB_NAME = "Troubleshoot";
    private final String WEB_ID = "troubleshoot";
    private final short WEB_ORDER = 4;
    private static final UserLevel AUTH_LEVEL = UserLevel.CONTAINERUSER;
    private static final List<String> nodesColumnNames = Arrays.asList("Node", "Node ID", "Statistics");
    private static final List<String> flowStatsColumnNames = Arrays.asList("Flow ID", "Node Type",
            "Entry Priority", "Actions", "Reason", "Setup Time", "Duration Seconds", "Duration Nanoseconds", "Recv Pkts", "Recv Bytes",
            "IdleTimeout", "HardTimeout", "Cookie", "IN PORT","OUT PORT", "DL SRC", "DL DST", "DL TYPE", "DL VLAN", "DL VLAN PR","NW SRC",
            "NW DST","NW PROTO","NW TOS","TP SRC","TP DST");
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
    private String getNodeDesc(Node node, ISwitchManager switchManager) {
        if (switchManager == null) {
            return null;
        }
        Description desc = (Description) switchManager.getNodeProp(node, Description.propertyName);
        return (desc == null) ? "" : desc.getValue();
    }
    @RequestMapping(value = "activenodes")
    @ResponseBody
    public FlowstoragemanagerJsonBean getActiveNode(HttpServletRequest request,  @RequestParam(required = false) String container) {
        List<Map<String, String>> lines = new ArrayList<Map<String, String>>();
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        Set<Node> nodeSet = (switchManager != null) ? switchManager.getNodes() : null;
        if (nodeSet != null) {
            for (Node node : nodeSet) {
                Map<String, String> device = new HashMap<String, String>();
                device.put("nodeName", getNodeDesc(node, switchManager));
                device.put("nodeId", node.toString());
                lines.add(device);
            }
        }
        FlowstoragemanagerJsonBean fjb = new FlowstoragemanagerJsonBean();
        fjb.setNodeData(lines);
        fjb.setColumnNames(nodesColumnNames);
        return fjb;
    }

    @RequestMapping(value = "inactivenodes")
    @ResponseBody
    public FlowstoragemanagerJsonBean getInactiveNode(HttpServletRequest request, @RequestParam(required = false) String container) {
        Map<String, String> nodemap = new HashMap<String, String>();
        List<Map<String,String>> nodeData = new ArrayList<Map<String,String>>();
        List<Map<String, String>> lines = new ArrayList<Map<String, String>>();
        String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, this);
        Set<Node> nodeSet = (switchManager != null) ? switchManager.getNodes() : null;
        if (nodeSet != null) {
            for (Node node : nodeSet) {
                Map<String, String> device = new HashMap<String, String>();
                device.put("nodeName", getNodeDesc(node, switchManager));
                device.put("nodeId", node.toString());
                lines.add(device);
            }
        }
        if(conn != null){
            conn = dbstorage.connectDb();
        }else{
            dbstorage = (IFlowStorage) ServiceHelper.getGlobalInstance(
                    IFlowStorage.class, this);
            conn = dbstorage.connectDb();
        }
        ResultSet rs = null;
        String Sql = "select distinct `node_id` from `flow`";
        try{
            Statement st = conn.createStatement();
            rs = st.executeQuery(Sql);
            //System.out.println("The value is ");
            while(rs.next()) {
                nodemap.put("nodeName","None");
                nodemap.put("nodeId","OF|"+rs.getString(1));
                if(!lines.contains(nodemap)){
                    Map<String, String> nodemaps = new HashMap<String, String>();
                    nodemaps.put("nodeName","NONE");
                    nodemaps.put("nodeId",rs.getString(1));
                    nodeData.add(nodemaps);
                }
            }
        }catch(SQLException e) {
            System.out.println("SQLException");
        }finally {
            if (conn != null){
                dbstorage.disConnectDb();
                conn=null;
            }
        }
        FlowstoragemanagerJsonBean fjb = new FlowstoragemanagerJsonBean();
        fjb.setNodeData(nodeData);
        fjb.setColumnNames(nodesColumnNames);
        return fjb;
    }

    @RequestMapping(value = "/activeflowMatch", method = RequestMethod.GET)
    @ResponseBody
    public FlowstoragemanagerJsonBean getActiveFlowMatch(
            @RequestParam("nodeId") String nodeId,
            HttpServletRequest request, @RequestParam(required = false) String container) {
        List<Map<String,String>> nodeData = new ArrayList<Map<String,String>>();
        if(conn != null){
            conn = dbstorage.connectDb();
        }else{
            dbstorage = (IFlowStorage) ServiceHelper.getGlobalInstance(
                    IFlowStorage.class, this);
            conn = dbstorage.connectDb();
        }
        ResultSet rs = null;
        String NodeID = nodeId.substring(3, nodeId.length());
        String Sql = "select f.flow_id,n.node_type,f.entry_priority,a.actions,s.reason, s.setup_time, s.durationSeconds, s.durationNanoseconds, "
                + "s.recv_pkts, s.recv_bytes,f.idleTimeout,f.hardTimeout,f.cookie,m.IN_PORT,m.OUT_PORT,m.DL_SRC,m.DL_DST,m.DL_TYPE,m.DL_VLAN,"
                + "m.DL_VLAN_PR,m.NW_SRC,m.NW_DST,m.NW_PROTO,m.NW_TOS,m.TP_SRC,m.TP_DST "
                + "from `flow` f,`statistic` s,`match` m,`nodetype` n,`action` a "
                + "where f.statistic_id=s.statistic_id and s.durationSeconds is not null and "
                + "f.match_id=m.match_id and f.node_type=n.type_id and f.actions=a.action_id "
                + "and f.node_id='"+NodeID+"'";
        try{
            Statement st = conn.createStatement();
            rs = st.executeQuery(Sql);
            while(rs.next()) {
               Map<String, String> nodemap = new HashMap<String, String>();
               nodemap.put("flowId", rs.getString(1));
               nodemap.put("nodeType", rs.getString(2));
               nodemap.put("entryPriority", rs.getString(3));
               nodemap.put("actions", rs.getString(4));
               nodemap.put("reason", rs.getString(5));
               nodemap.put("setupTime", rs.getString(6));
               nodemap.put("durationSeconds", rs.getString(7));
               nodemap.put("durationNanoseconds", rs.getString(8));
               nodemap.put("recvPkts", rs.getString(9));
               nodemap.put("recvBytes", rs.getString(10));
               nodemap.put("idleTimeout", rs.getString(11));
               nodemap.put("hardTimeout", rs.getString(12));
               nodemap.put("cookie", rs.getString(13));
               nodemap.put("inPort", rs.getString(14));
               nodemap.put("outPort", rs.getString(15));
               nodemap.put("dlSrc", rs.getString(16));
               nodemap.put("dlDst", rs.getString(17));
               nodemap.put("dlType", rs.getString(18));
               nodemap.put("dlVlan", rs.getString(19));
               nodemap.put("dlVlanPR", rs.getString(20));
               nodemap.put("nwSrc", rs.getString(21));
               nodemap.put("nwDst", rs.getString(22));
               nodemap.put("nwProto", rs.getString(23));
               nodemap.put("nwTos", rs.getString(24));
               nodemap.put("tpSrc", rs.getString(25));
               nodemap.put("tpDst", rs.getString(26));
               nodeData.add(nodemap);
            }
        }catch(SQLException e) {
            System.out.println("SQLException");
        }finally {
            if (conn != null){
                dbstorage.disConnectDb();
                conn=null;
            }
        }
        FlowstoragemanagerJsonBean fjb = new FlowstoragemanagerJsonBean();
        fjb.setNodeData(nodeData);
        fjb.setColumnNames(flowStatsColumnNames);
        return fjb;
    }
    @RequestMapping(value = "/inactiveflowMatch", method = RequestMethod.GET)
    @ResponseBody
    public FlowstoragemanagerJsonBean getinactiveflowMatch(
            @RequestParam("nodeId") String nodeId,
            HttpServletRequest request, @RequestParam(required = false) String container) {
        List<Map<String,String>> nodeData = new ArrayList<Map<String,String>>();
        if(conn != null){
            conn = dbstorage.connectDb();
        }else{
            dbstorage = (IFlowStorage) ServiceHelper.getGlobalInstance(
                    IFlowStorage.class, this);
            conn = dbstorage.connectDb();
        }
        ResultSet rs = null;
        String Sql = "select f.flow_id,n.node_type,f.entry_priority,a.actions,s.reason, s.setup_time, s.durationSeconds, s.durationNanoseconds, s.recv_pkts, s.recv_bytes,f.idleTimeout,f.hardTimeout,f.cookie,m.IN_PORT,m.OUT_PORT,m.DL_SRC,m.DL_DST,m.DL_TYPE,m.DL_VLAN,m.DL_VLAN_PR,m.NW_SRC,m.NW_DST,m.NW_PROTO,m.NW_TOS,m.TP_SRC,m.TP_DST from `flow` f,`match` m,`nodetype` n,`action` a,`statistic` s where f.match_id=m.match_id and f.node_type=n.type_id and f.actions=a.action_id and f.node_id='"+nodeId+"'"+" and f.statistic_id=s.statistic_id and s.durationSeconds is not null";
        try{
            Statement st = conn.createStatement();
            rs = st.executeQuery(Sql);
            while(rs.next()) {
                Map<String, String> nodemap = new HashMap<String, String>();
                nodemap.put("flowId", rs.getString(1));
                nodemap.put("nodeType", rs.getString(2));
                nodemap.put("entryPriority", rs.getString(3));
                nodemap.put("actions", rs.getString(4));
                nodemap.put("reason", rs.getString(5));
                nodemap.put("setupTime", rs.getString(6));
                nodemap.put("durationSeconds", rs.getString(7));
                nodemap.put("durationNanoseconds", rs.getString(8));
                nodemap.put("recvPkts", rs.getString(9));
                nodemap.put("recvBytes", rs.getString(10));
                nodemap.put("idleTimeout", rs.getString(11));
                nodemap.put("hardTimeout", rs.getString(12));
                nodemap.put("cookie", rs.getString(13));
                nodemap.put("inPort", rs.getString(14));
                nodemap.put("outPort", rs.getString(15));
                nodemap.put("dlSrc", rs.getString(16));
                nodemap.put("dlDst", rs.getString(17));
                nodemap.put("dlType", rs.getString(18));
                nodemap.put("dlVlan", rs.getString(19));
                nodemap.put("dlVlanPR", rs.getString(20));
                nodemap.put("nwSrc", rs.getString(21));
                nodemap.put("nwDst", rs.getString(22));
                nodemap.put("nwProto", rs.getString(23));
                nodemap.put("nwTos", rs.getString(24));
                nodemap.put("tpSrc", rs.getString(25));
                nodemap.put("tpDst", rs.getString(26));
                nodeData.add(nodemap);
            }
        }catch(SQLException e) {
            System.out.println("SQLException");
        }finally {
            if (conn != null){
                dbstorage.disConnectDb();
                conn=null;
            }
        }
        FlowstoragemanagerJsonBean fjb = new FlowstoragemanagerJsonBean();
        fjb.setNodeData(nodeData);
        fjb.setColumnNames(flowStatsColumnNames);
        return fjb;
    }
}
