package org.opendaylight.controller.devices.web;

import java.util.List;

public class NodeInformationJsonBean {
    private String idleTimeout;
    private String hardTimeout;
    private List<String> tiers;

    public void setIdleTimeout(String idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public String getIdleTimeout() {
        return this.idleTimeout;
    }

    public void setHardTimeout(String hardTimeout) {
        this.hardTimeout = hardTimeout;
    }

    public String getHardTimeout() {
        return this.hardTimeout;
    }

    public void setTiers(List<String> tiers) {
        this.tiers = tiers;
    }

    public List<String> getTiers() {
        return this.tiers;
    }

}
