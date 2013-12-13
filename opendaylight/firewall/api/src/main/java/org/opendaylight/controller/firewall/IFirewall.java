package org.opendaylight.controller.firewall;

import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.utils.Status;

/**
 * Interface that describes methods for .
 */
public interface IFirewall {

    public boolean isEnabled();
    /** */
    public void setstatus(boolean enabled);
    /**
     * Adds a new Firewall rule
     * @return
     */
    public Status addRule(FirewallRule rule);
    /**
     * get all firewallRule ConfigList
     * @return
     */
    ConcurrentMap<String, FirewallRule> getRuleConfigList();
    /**
     *
     * @param containerName
     * @param name
     * @return
     */
    public FirewallRule getFirewallRule(String name);
    /**
     *
     * @param containerName
     * @param name
     * @return
     */
    public Status removeFirewallRule(String name,FirewallRule ruleconfig);
    /**
     * @return
     */
    public Status saveFirewallRule();
    /**
     * modify a rule with given name
     * @param rule
     * @return
     */
    public Status updateRule(FirewallRule rule);
}