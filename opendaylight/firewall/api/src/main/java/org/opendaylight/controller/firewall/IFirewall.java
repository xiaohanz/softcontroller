package org.opendaylight.controller.firewall;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.utils.Status;

/**
 * Interface that describes methods for firewall.
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
     * @param id
     * @return
     */
    public FirewallRule getFirewallRule(String id);
    /**
     *
     * @param containerName
     * @param id
     * @return
     */
    public Status removeFirewallRule(String id);
    /**
     * @return
     */
    public Status saveFirewallRule();
    /**
     * modify a rule with given name
     * @param rule
     * @return
     */
    public Status updateRule(String id,FirewallRule rule);
    /**
     *
     * @param ruleConfig
     * @return
     */
    public String addRuleInternal(FirewallRule ruleConfig);
    /**
     *
     * @param ruleName
     * @return
     */
    public String getRuleName_Id(String ruleName);
    /**
     *
     * @return
     */
    public Map<String,String> getRuleName_Ids();
}
