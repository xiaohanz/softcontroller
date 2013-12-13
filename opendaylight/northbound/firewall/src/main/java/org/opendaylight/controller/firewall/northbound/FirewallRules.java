package org.opendaylight.controller.firewall.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.firewall.FirewallRule;

@XmlRootElement (name = "list")
@XmlAccessorType(XmlAccessType.NONE)

public class FirewallRules {
        @XmlElement
        List<FirewallRule> firewallRule;
        //To satisfy JAXB
        private FirewallRules() {

        }

        public FirewallRules(List<FirewallRule> firewallRule) {
                this.firewallRule = firewallRule;
        }

        public List<FirewallRule> getFirewallRule() {
                return firewallRule;
        }

        public void setFirewallRule(List<FirewallRule> firewallRule) {
                this.firewallRule = firewallRule;
        }
}