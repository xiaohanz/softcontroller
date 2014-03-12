package org.opendaylight.controller.firewall;

import java.io.Serializable;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.TCP;
import org.opendaylight.controller.sal.packet.UDP;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class FirewallRule implements Serializable {
    private static final long serialVersionUID = 1L;
    // private IfIptoHost hostTracker;
    ConcurrentMap<String, FirewallRule> firewallRules;
    public static String maxRuleNo = "65535";
    private static final String NAMEREGEX = "^[a-zA-Z0-9]+$";
    private HostNodeConnector transHost;
    private static final Logger log = LoggerFactory
            .getLogger(FirewallRule.class);
    private String ruleId;
    private String InstallHw;
    @XmlElement
    private String InstallSoon;
    @XmlElement
    private String description;
    @XmlElement
    private String id;
    @XmlElement
    private String name;
    @XmlElement
    private String priority;
    @XmlElement
    // string nodeid
    private Node node;
    @XmlElement
    private String ingressPort;
    @XmlElement
    private String etherType;
    @XmlElement
    private String dlSrc;
    @XmlElement
    private String dlDst;
    @XmlElement
    private String nwSrc;
    @XmlElement
    private String nwDst;
    @XmlElement
    private String protocol;
    @XmlElement
    private String tpSrc;
    @XmlElement
    private String tpDst;
    @XmlElement
    private String action;
    @XmlElement
    private String status;

    // status define firewall rule disabled or enabled
    // string decription
    public FirewallRule() {
    }

    public FirewallRule(String InstallHw, String InstallSoon, String id, String name,
            String description, String priority, Node node, String ingressPort,
            String etherType, String dlSrc, String dlDst, String nwSrc,
            String nwDst, String protocol, String tpSrc, String tpDst,
            String action, HostNodeConnector transHost, String status) {
        super();
        this.InstallHw = InstallHw;
        this.InstallSoon = InstallSoon;
        this.id = id;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.node = node;
        this.ingressPort = ingressPort;
        this.etherType = etherType;
        this.dlSrc = dlSrc;
        this.dlDst = dlDst;
        this.nwSrc = nwSrc;
        this.nwDst = nwDst;
        this.protocol = protocol;
        this.tpSrc = tpSrc;
        this.tpDst = tpDst;
        this.action = action;
        this.transHost = transHost;
        this.status = status;
    }

    public FirewallRule(FirewallRule rules) {
        this.id = rules.id;
        this.name = rules.name;
        this.description = rules.description;
        this.node = rules.node;
        this.priority = rules.priority;
        this.ingressPort = rules.ingressPort;
        this.etherType = rules.etherType;
        this.dlSrc = rules.dlSrc;
        this.dlDst = rules.dlDst;
        this.protocol = rules.protocol;
        this.nwSrc = rules.nwSrc;
        this.nwDst = rules.nwDst;
        this.tpSrc = rules.tpSrc;
        this.tpDst = rules.tpDst;
        this.action = rules.action;
        this.status = rules.status;
    }

    public HostNodeConnector getTransHost() {
        return transHost;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getInstallHw() {
        return InstallHw;
    }

    public String getInstallSoon() {
        return InstallSoon;
    }

    public String getName() {
        return name;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getPriority() {
        return priority;
    }

    public Node getNode() {
        return node;
    }

    public String getIngressPort() {
        return ingressPort;
    }

    public String getEtherType() {
        return etherType;
    }

    public String getEtherTypeName() {
        if (this.etherType != null) {
            String str = this.etherType.substring(2);
            short s = (short) Integer.parseInt(str, 16);
            String etherType = EtherTypes.getEtherTypeName(s);
            return etherType;
        } else {
            return this.etherType;
        }
    }

    public String getDlSrc() {
        return dlSrc;
    }

    public String getDlDst() {
        return dlDst;
    }

    public String getNwSrc() {
        return nwSrc;
    }

    public String getNwDst() {
        return nwDst;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getProtocolName() {
        if (this.protocol != null) {
            int s = Integer.parseInt(this.protocol);
            String protocol = IPProtocols.getProtocolName(s);
            return protocol;
        } else {
            return this.protocol;
        }
    }

    public String getTpSrc() {
        return tpSrc;
    }

    public String getTpDst() {
        return tpDst;
    }

    public String getAction() {
        return action;
    }

    public void setTransHost(HostNodeConnector transHost) {
        this.transHost = transHost;
    }

    public void setInstallHw(String InstallHw) {
        this.InstallHw = InstallHw;
    }

    public void setInstallSoon(String InstallSoon) {
        this.InstallSoon = InstallSoon;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void setIngressPort(String ingressPort) {
        this.ingressPort = ingressPort;
    }

    public void setEtherType(String etherType) {
        this.etherType = etherType;
    }

    public void setDlSrc(String dlSrc) {
        this.dlSrc = dlSrc;
    }

    public void setDlDst(String dlDst) {
        this.dlDst = dlDst;
    }

    public void setNwSrc(String nwSrc) {
        this.nwSrc = nwSrc;
    }

    public void setNwDst(String nwDst) {
        this.nwDst = nwDst;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setTpSrc(String tpSrc) {
        this.tpSrc = tpSrc;
    }

    public void setTpDst(String tpDst) {
        this.tpDst = tpDst;
    }

    public void setActions(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "FirewallRule [ruleId=" + ruleId + ",id=" + id + ",transHost="
                + transHost + ",InstallHw=" + InstallHw + ", InstallSoon=" + InstallSoon + ",description="
                + description + ", name=" + name + ", priority=" + priority
                + ", node=" + node + ", ingressPort=" + ingressPort
                + ", etherType=" + etherType + ", dlSrc=" + dlSrc + ", dlDst="
                + dlDst + ", nwSrc=" + nwSrc + ", nwDst=" + nwDst
                + ", protocol=" + protocol + ", tpSrc=" + tpSrc + ", tpDst="
                + tpDst + ", action=" + action + ", status=" + status + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((ruleId == null) ? 0 : ruleId.hashCode());
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        result = prime * result + ((dlDst == null) ? 0 : dlDst.hashCode());
        result = prime * result + ((dlSrc == null) ? 0 : dlSrc.hashCode());
        result = prime * result
                + ((etherType == null) ? 0 : etherType.hashCode());
        result = prime * result
                + ((ingressPort == null) ? 0 : ingressPort.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        result = prime * result + ((nwDst == null) ? 0 : nwDst.hashCode());
        result = prime * result + ((nwSrc == null) ? 0 : nwSrc.hashCode());
        result = prime * result
                + ((priority == null) ? 0 : priority.hashCode());
        result = prime * result
                + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result
                + ((InstallHw == null) ? 0 : InstallHw.hashCode());
        result = prime * result
                + ((InstallSoon == null) ? 0 : InstallSoon.hashCode());
        result = prime * result
                + ((transHost == null) ? 0 : transHost.hashCode());
        result = prime * result + ((tpDst == null) ? 0 : tpDst.hashCode());
        result = prime * result + ((tpSrc == null) ? 0 : tpSrc.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FirewallRule other = (FirewallRule) obj;
        if (dlDst == null) {
            if (other.dlDst != null)
                return false;
        } else if (!dlDst.equals(other.dlDst))
            return false;
        if (dlSrc == null) {
            if (other.dlSrc != null)
                return false;
        } else if (!dlSrc.equals(other.dlSrc))
            return false;
        if (etherType == null) {
            if (other.etherType != null)
                return false;
        } else if (!etherType.equals(other.etherType))
            return false;
        if (ingressPort == null) {
            if (other.ingressPort != null)
                return false;
        } else if (!ingressPort.equals(other.ingressPort))
            return false;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        if (nwDst == null) {
            if (other.nwDst != null)
                return false;
        } else if (!nwDst.equals(other.nwDst))
            return false;
        if (nwSrc == null) {
            if (other.nwSrc != null)
                return false;
        } else if (!nwSrc.equals(other.nwSrc))
            return false;
        if (priority == null) {
            if (other.priority != null)
                return false;
        } else if (!priority.equals(other.priority))
            return false;
        if (protocol == null) {
            if (other.protocol != null)
                return false;
        } else if (!protocol.equals(other.protocol))
            return false;
        if (tpDst == null) {
            if (other.tpDst != null)
                return false;
        } else if (!tpDst.equals(other.tpDst))
            return false;
        if (tpSrc == null) {
            if (other.tpSrc != null)
                return false;
        } else if (!tpSrc.equals(other.tpSrc))
            return false;
        if (action == null) {
            if (other.action != null)
                return false;
        } else if (!action.equalsIgnoreCase(other.action))
            return false;
        return true;
    }

    private enum EtherIPType {
        ANY, V4, V6;
    };

    public enum ActionType {
        ALLOW, DENY, DISPATCH;
    };

    public boolean isPortValid(Short port) {
        if (port < 1) {
            log.debug("port {} is not valid", port);
            return false;
        }
        return true;
    }

    public boolean isL2AddressValid(String mac) {
        if (mac == null) {
            return false;
        }

        Pattern macPattern = Pattern
                .compile("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}");
        Matcher mm = macPattern.matcher(mac);
        if (!mm.matches()) {
            log.debug(
                    "Ethernet address {} is not valid. Example: 00:05:b9:7c:81:5f",
                    mac);
            return false;
        }
        return true;
    }

    public boolean isProtocolValid(String protocol) {
        IPProtocols proto = IPProtocols.fromString(protocol);
        return (proto != null);
    }

    public boolean isTpPortValid(String tpPort) {
        int port = Integer.decode(tpPort);
        return ((port >= 0) && (port <= 0xffff));
    }

    public Status validate() {
        EtherIPType etype = EtherIPType.ANY;
        EtherIPType ipsrctype = EtherIPType.ANY;
        EtherIPType ipdsttype = EtherIPType.ANY;
        try {
            if (ingressPort != null) {
                Short port = Short.decode(ingressPort);
                if (isPortValid(port) == false) {
                    String msg = String.format(
                            "Ingress port %d is not valid for Switch", port);
                    return new Status(StatusCode.BADREQUEST, msg);
                }
            }
            if (name == null || name.trim().isEmpty()
                    || !name.matches(FirewallRule.NAMEREGEX)) {
                return new Status(StatusCode.BADREQUEST, "Invalid name");
            }
            if (priority == null) {
                return new Status(StatusCode.BADREQUEST,
                        String.format("priority is null"));
            }
            if (Integer.decode(priority) < 0
                    || (Integer.decode(priority) > 65534)) {
                return new Status(StatusCode.BADREQUEST, String.format(
                        "priority %s is not in the range 0 - 65534", priority));
            }
            if ((dlSrc != null) && !isL2AddressValid(dlSrc)) {
                return new Status(
                        StatusCode.BADREQUEST,
                        String.format(
                                "Ethernet source address %s is not valid. Example: 00:05:b9:7c:81:5f",
                                dlSrc));
            }

            if ((dlDst != null) && !isL2AddressValid(dlDst)) {
                return new Status(
                        StatusCode.BADREQUEST,
                        String.format(
                                "Ethernet destination address %s is not valid. Example: 00:05:b9:7c:81:5f",
                                dlDst));
            }
            if (nwSrc != null) {
                if (NetUtils.isIPv4AddressValid(nwSrc)) {
                    ipsrctype = EtherIPType.V4;
                } else if (NetUtils.isIPv6AddressValid(nwSrc)) {
                    ipsrctype = EtherIPType.V6;
                } else {
                    return new Status(StatusCode.BADREQUEST, String.format(
                            "IP source address %s is not valid", nwSrc));
                }
            }
            if (nwDst != null) {
                if (NetUtils.isIPv4AddressValid(nwDst)) {
                    ipdsttype = EtherIPType.V4;
                } else if (NetUtils.isIPv6AddressValid(nwDst)) {
                    ipdsttype = EtherIPType.V6;
                } else {
                    return new Status(StatusCode.BADREQUEST, String.format(
                            "IP destination address %s is not valid", nwDst));
                }
            }
            if (etherType == null) {
                if (nwSrc != null || nwDst != null) {
                    return new Status(
                            StatusCode.BADREQUEST,
                            String.format("Ethernet type is null or empty with src/dst ip exist"));
                }
            }
            if (etherType != null) {
                int type = Integer.decode(etherType);
                if ((type < 0) || (type > 0xffff)) {
                    return new Status(StatusCode.BADREQUEST, String.format(
                            "Ethernet type %s is not valid", etherType));
                } else {
                    if (type == 0x800) {
                        etype = EtherIPType.V4;
                    } else if (type == 0x86dd) {
                        etype = EtherIPType.V6;
                    }
                }
            }
            if (etype != EtherIPType.ANY) {
                if ((ipsrctype != EtherIPType.ANY) && (ipsrctype != etype)) {
                    return new Status(
                            StatusCode.BADREQUEST,
                            String.format("Type mismatch between Ethernet & Src IP"));
                }
                if ((ipdsttype != EtherIPType.ANY) && (ipdsttype != etype)) {
                    return new Status(
                            StatusCode.BADREQUEST,
                            String.format("Type mismatch between Ethernet & Dst IP"));
                }
            }
            if (ipsrctype != ipdsttype) {
                if (!((ipsrctype == EtherIPType.ANY) || (ipdsttype == EtherIPType.ANY))) {
                    return new Status(StatusCode.BADREQUEST,
                            String.format("IP Src Dest Type mismatch"));
                }
            }
            if ((tpSrc != null) && !isTpPortValid(tpSrc)) {
                return new Status(StatusCode.BADREQUEST, String.format(
                        "Transport source port %s is not valid", tpSrc));
            }
            if ((tpDst != null) && !isTpPortValid(tpDst)) {
                return new Status(StatusCode.BADREQUEST, String.format(
                        "Transport destination port %s is not valid", tpDst));
            }
            if (protocol == null) {
                if (tpSrc != null || tpDst != null) {
                    return new Status(
                            StatusCode.BADREQUEST,
                            String.format("Protocol is null or empty with src/dst tcp/udp port exist"));
                }
            }
            if ((protocol != null) && !isProtocolValid(protocol)) {
                return new Status(StatusCode.BADREQUEST, String.format(
                        "Protocol %s is not valid", protocol));
            }
            if (action == null) {
                return new Status(StatusCode.BADREQUEST,
                        "Action value is null or empty");
            } else {
                if (action.startsWith("DISPATCH")) {
                    String dst = action.substring(action.indexOf(":") + 1);
                    if (dst == null) {
                        return new Status(
                                StatusCode.BADREQUEST,
                                String.format("dispatch destnation is null or empty"));
                    } else {
                        if (!NetUtils.isIPv4AddressValid(dst)
                                && !NetUtils.isIPv6AddressValid(dst)) {
                            return new Status(
                                    StatusCode.BADREQUEST,
                                    String.format(
                                            "IP destination address %s is not valid",
                                            dst));
                        }
                    }
                } else if (!action.equals("ALLOW") && !action.equals("DENY")) {
                    return new Status(StatusCode.BADREQUEST, String.format(
                            "Action value %s is not valid", action));
                }
            }
        } catch (NumberFormatException e) {
            return new Status(StatusCode.BADREQUEST, String.format(
                    "Invalid number format %s", e.getMessage()));
        }
        return new Status(StatusCode.SUCCESS);
    }

    public Flow getFlow(Packet formattedPak) {
        Match match = new Match();
        if (this.ingressPort != null) {
            match.setField(
                    MatchType.IN_PORT,
                    NodeConnectorCreator.createOFNodeConnector(
                            Short.parseShort(ingressPort), getNode()));
        }
        byte[] dstMAC = ((Ethernet) formattedPak).getDestinationMACAddress();
        byte[] srcMAC = ((Ethernet) formattedPak).getSourceMACAddress();
        match.setField(MatchType.DL_DST, dstMAC.clone());
        match.setField(MatchType.DL_SRC, srcMAC.clone());
        if (this.etherType != null) {
            match.setField(MatchType.DL_TYPE, Integer.decode(etherType)
                    .shortValue());
        }
        if (this.nwSrc != null) {
            String parts[] = this.nwSrc.split("/");
            InetAddress ip = NetUtils.parseInetAddress(parts[0]);
            InetAddress mask = null;
            int maskLen = 0;
            if (parts.length > 1) {
                maskLen = Integer.parseInt(parts[1]);
            } else {
                maskLen = (ip instanceof Inet6Address) ? 128 : 32;
            }
            mask = NetUtils.getInetNetworkMask(maskLen,
                    ip instanceof Inet6Address);
            match.setField(MatchType.NW_SRC, ip, mask);
        }
        if (this.nwDst != null) {
            String parts[] = this.nwDst.split("/");
            InetAddress ip = NetUtils.parseInetAddress(parts[0]);
            InetAddress mask = null;
            int maskLen = 0;
            if (parts.length > 1) {
                maskLen = Integer.parseInt(parts[1]);
            } else {
                maskLen = (ip instanceof Inet6Address) ? 128 : 32;
            }
            mask = NetUtils.getInetNetworkMask(maskLen,
                    ip instanceof Inet6Address);
            match.setField(MatchType.NW_DST, ip, mask);
        }
        if (IPProtocols.fromString(this.protocol) != IPProtocols.ANY) {
            match.setField(MatchType.NW_PROTO,
                    IPProtocols.getProtocolNumberByte(this.protocol));
        }
        if (this.tpSrc != null) {
            match.setField(MatchType.TP_SRC, Integer.valueOf(this.tpSrc)
                    .shortValue());
        }
        if (this.tpDst != null) {
            match.setField(MatchType.TP_DST, Integer.valueOf(this.tpDst)
                    .shortValue());
        }
        List<Action> actionList = new ArrayList<Action>();
        Flow flow = new Flow(match, actionList);
        flow.setMatch(match);
        flow.setActions(actionList);
        // short hardTime=120;
        // flow.setHardTimeout(hardTime);
        if (this.priority != null) {
            flow.setPriority(Integer.decode(priority).shortValue());
        }
        return flow;
    }

    public FlowEntry changeToFlow(Packet formattedPak,
            NodeConnector incoming_connector) {
        String group = "FirewallRule";
        Random rd = new Random();
        String str = String.valueOf(rd.nextInt(512));
        String name = this.ruleId + "_" + str;
        return new FlowEntry(group, name, this.getFlow(formattedPak),
                incoming_connector.getNode());
    }

    public boolean equalToRule(Packet pak, NodeConnector nodeConnector) {
        // get src/dst mac,mac type
        Ethernet eHeader = (Ethernet) pak;
        byte[] dMac = eHeader.getDestinationMACAddress();
        byte[] sMac = eHeader.getSourceMACAddress();
        String srcMac = macByteToString(sMac);
        String dstMac = macByteToString(dMac);
        String etherType = EtherTypes.getEtherTypeName(eHeader.getEtherType());
        if (this.dlSrc != null) {
            if (!this.dlSrc.equalsIgnoreCase(srcMac)) {
                return false;
            }
        }
        if (this.dlDst != null) {
            if (!this.dlDst.equalsIgnoreCase(dstMac)) {
                return false;
            }
        }
        /*
         * String rdlSrc=""; String rdlDst=""; if (this.dlSrc!=null){ String
         * dSrc=this.getDlSrc().trim(); if(dSrc!=null&&!rdlSrc.equals(dSrc)){
         * for(int i=0;i<dSrc.length();i++){
         * if(dSrc.charAt(i)>48&&dSrc.charAt(i)<=57){ rdlSrc+=dSrc.charAt(i); }
         * } } if (!rdlSrc.equals(srcMac)){ return false; } } if
         * (this.dlDst!=null){ String dDst=this.getDlDst().trim();
         * if(dDst!=null&&!rdlDst.equals(dDst)){ for(int
         * i=0;i<dDst.length();i++){ if(dDst.charAt(i)>48&&dDst.charAt(i)<=57){
         * rdlDst+=dDst.charAt(i); } } } if (!rdlDst.equals(dstMac)){ return
         * false; } }
         */
        // get src/dst ip,nw protocol
        IPv4 pkt = (IPv4) pak.getPayload();
        String protocol = IPProtocols.getProtocolName(pkt.getProtocol());
        InetAddress srcIp = NetUtils.getInetAddress(pkt.getSourceAddress());
        InetAddress dstIp = NetUtils
                .getInetAddress(pkt.getDestinationAddress());
        // get connector info
        Node node = nodeConnector.getNode();
        String nodePort = nodeConnector.getNodeConnectorIDString();
        // get src/dst tcp/udp port
        Packet frame = pkt.getPayload();
        String srcPort = null;
        String dstPort = null;
        if (protocol.equals(IPProtocols.TCP.toString())) {
            TCP tcpFrame = (TCP) frame;
            srcPort = String.valueOf(tcpFrame.getSourcePort());
            dstPort = String.valueOf(tcpFrame.getDestinationPort());
        } else if (protocol.equals(IPProtocols.UDP.toString())) {
            UDP udpFrame = (UDP) frame;
            srcPort = String.valueOf(udpFrame.getSourcePort());
            dstPort = String.valueOf(udpFrame.getDestinationPort());
        } else if (protocol.equals(IPProtocols.ICMP.toString())) {
        }
        // get dispatch host ip mac
        /**
         * boolean flag=false; HostNodeConnector tHost=this.getTransHost();
         * byte[] tMac=null; InetAddress tIp=null; if (tHost!=null){
         * tMac=tHost.getDataLayerAddressBytes(); tIp=tHost.getNetworkAddress();
         * //compare with dipatch host if exist //compare tHost with if
         * (tMac.equals(sMac)&&tIp.equals(srcIp.getHostAddress())){ flag=true;
         * if (this.dlSrc!=null&&!(rdlSrc.equals(dstMac))){ flag=false; }else{
         * if (this.nwSrc!=null&&!(this.nwSrc.equals(dstIp.getHostAddress()))){
         * flag=false; } } }else{ flag=false; } //compare with dipatch host if
         * exist if (tMac.equals(sMac)&&tIp.equals(srcIp.getHostAddress())){
         * flag=true; if (this.dlSrc!=null&&!(rdlSrc.equals(dstMac))){
         * flag=false; }else{ if
         * (this.nwSrc!=null&&!(this.nwSrc.equals(dstIp.getHostAddress()))){
         * flag=false; } } }else{ flag=false; } }
         **/
        // change pkt to pktRule
        FirewallRule pktRule = new FirewallRule(null, null, null, null, null, null,
                node, nodePort, etherType, srcMac, dstMac,
                srcIp.getHostAddress(), dstIp.getHostAddress(), protocol,
                srcPort, dstPort, null, null, null);
        // change rule
        /**
         * if (flag==false){
         *
         * String rnwSrc=this.getNwSrc(); String rnwDst=this.getNwDst(); if
         * (this.nwSrc!=null&&srcIp.getHostAddress().equals(rnwSrc)){ return
         * false; } if
         * (this.nwDst!=null&&dstIp.getHostAddress().equals(rnwDst)){ return
         * false; } }
         */
        // compare rule with pktRule
        if (this.etherType == null
                || pktRule.getEtherType().equals(this.getEtherTypeName()))
            if (this.nwSrc == null
                    || pktRule.getNwSrc().equals(this.getNwSrc()))
                if (this.nwDst == null
                        || pktRule.getNwDst().equals(this.getNwDst()))
                    if (this.protocol == null
                            || pktRule.getProtocol().equals(
                                    this.getProtocolName()))
                        if (this.tpSrc == null
                                || pktRule.getTpSrc().equals(this.getTpSrc()))
                            if (this.tpDst == null
                                    || pktRule.getTpDst().equals(
                                            this.getTpDst())) {
                                return true;
                            }
        return false;
    }

    public boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    public static String macByteToString(byte[] mac) {
        String s = null;
        String macString = "";
        for (int i = 0; i < mac.length; i++) {
            int emac = mac[i] & 0xff;
            s = Integer.toString(emac, 16);
            if (emac < 16 && emac > -1) {
                s = "0" + s;
            }
            macString = macString + ":" + s;
        }
        return macString.substring(1);
    }
}