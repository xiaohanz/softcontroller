package org.opendaylight.controller.firewall.web;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Firewallbak implements Serializable{

    private static final long serialVersionUID = 1L;
    @XmlElement
    private String id;
    @XmlElement
    private String name;
    @XmlElement
    private String nodeId;
    @XmlElement
    private String srcInport;
    @XmlElement
    private String srcMac;
    @XmlElement
    private String dstMac;
    @XmlElement
    private String dlType;
    @XmlElement
    private String srcIp;
    @XmlElement
    private String dstIp;
    @XmlElement
    private String nwProto;
    @XmlElement
    private String tpSrc;
    @XmlElement
    private String tpDst;
    @XmlElement
    private String priority;
    @XmlElement
    private String action;
    @XmlElement
    private String description;
    @XmlElement
    private String status;
    public Firewallbak(){
    };
    public Firewallbak(String id,String name,String nodeId,String srcInport,String srcMac,String dstMac,
            String dlType,String srcIp,String dstIp,String nwProto,String tpSrc,String tpDst,
            String priority,String action,String description,String status){
        this.id = id;
        this.name = name;
        this.nodeId = nodeId;
        this.srcInport = srcInport;
        this.srcMac = srcMac;
        this.dstMac = dstMac;
        this.dlType = dlType;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.nwProto = nwProto;
        this.tpSrc = tpSrc;
        this.tpDst = tpDst;
        this.priority = priority;
        this.action = action;
        this.description = description;
        this.status = status;
    };

    public Firewallbak(Firewallbak form){
        this.id = form.id;
        this.name = form.name;
        this.nodeId = form.nodeId;
        this.srcInport = form.srcInport;
        this.srcMac = form.srcMac;
        this.dstMac = form.dstMac;
        this.dlType = form.dlType;
        this.srcIp = form.srcIp;
        this.dstIp = form.dstIp;
        this.nwProto = form.nwProto;
        this.tpSrc = form.tpSrc;
        this.tpDst = form.tpDst;
        this.priority = form.priority;
        this.action = form.action;
        this.description = form.description;
        this.status = form.status;
    };

    /**
     *
     * Set\Get mathods
     */
    public void setId(String id){
        this.id = id;
    };
    public String getId(){
        return this.id;
    };

    public void setName(String name){
        this.name = name;
    };
    public String getName(){
        return this.name;
    };

    public void setNodeId(String nodeId){
        this.nodeId = nodeId;
    };
    public String getNodeId(){
        return this.nodeId;
    };

    public void setSrcInport(String srcInport){
        this.srcInport = srcInport;
    };
    public String getSrcInport(){
        return this.srcInport;
    };

    public void setSrcMac(String srcMac){
        this.srcMac = srcMac;
    };
    public String getSrcMac(){
        return this.srcMac;
    };

    public void setDstMac(String dstMac){
        this.dstMac = dstMac;
    };
    public String getDstMac(){
        return this.dstMac;
    };

    public void setDlType(String dlType){
        this.dlType = dlType;
    };
    public String getDlType(){
        return this.dlType;
    };

    public void setSrcIp(String srcIp){
        this.srcIp = srcIp;
    };
    public String getSrcIp(){
        return this.srcIp;
    };

    public void setDstIp(String dstIp){
        this.dstIp = dstIp;
    };
    public String getDstIp(){
        return this.dstIp;
    };

    public void setNwProto(String nwProto){
        this.nwProto = nwProto;
    };
    public String getNwProto(){
        return this.nwProto;
    };

    public void setTpSrc(String tpSrc){
        this.tpSrc = tpSrc;
    };
    public String getTpSrc(){
        return this.tpSrc;
    }

    public void setTpDst(String tpDst){
        this.tpDst = tpDst;
    };
    public String getTpDst(){
        return this.tpDst;
    };

    public void setPriority(String priority){
        this.priority = priority;
    };
    public String getPriority(){
        return this.priority;
    };

    public void setAction(String action){
        this.action = action;
    };
    public String getAction(){
        return this.action;
    };

    public void setDescription(String description){
        this.description = description;
    };
    public String getDescription(){
        return this.description;
    };

    public void setStatus(String status){
        this.status = status;
    };
    public String getStatus(){
        return this.status;
    };
}
