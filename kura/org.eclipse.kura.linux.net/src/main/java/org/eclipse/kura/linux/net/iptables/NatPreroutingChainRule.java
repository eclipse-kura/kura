/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.linux.net.iptables;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.firewall.RuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatPreroutingChainRule {

    private static final Logger logger = LoggerFactory.getLogger(NatPreroutingChainRule.class);

    private String rule;
    private String inputInterface;
    private String protocol;
    private int externalPort;
    private int internalPort;
    private int srcPortFirst;
    private int srcPortLast;
    private String dstIpAddress;
    private String permittedNetwork;
    private int permittedNetworkMask;
    private String permittedMacAddress;
    private RuleType type;

    public NatPreroutingChainRule() {
        this.type = RuleType.GENERIC;
    }

    public NatPreroutingChainRule inputInterface(String inputInterface) {
        this.inputInterface = inputInterface;
        return this;
    }

    public NatPreroutingChainRule protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public NatPreroutingChainRule externalPort(int externalPort) {
        this.externalPort = externalPort;
        return this;
    }

    public NatPreroutingChainRule internalPort(int internalPort) {
        this.internalPort = internalPort;
        return this;
    }

    public NatPreroutingChainRule srcPortFirst(int srcPortFirst) {
        this.srcPortFirst = srcPortFirst;
        return this;
    }

    public NatPreroutingChainRule srcPortLast(int srcPortLast) {
        this.srcPortLast = srcPortLast;
        return this;
    }

    public NatPreroutingChainRule dstIpAddress(String dstIpAddress) {
        this.dstIpAddress = dstIpAddress;
        return this;
    }

    public NatPreroutingChainRule permittedNetwork(String permittedNetwork) {
        this.permittedNetwork = permittedNetwork;
        return this;
    }

    public NatPreroutingChainRule permittedNetworkMask(int permittedNetworkMask) {
        this.permittedNetworkMask = permittedNetworkMask;
        return this;
    }

    public NatPreroutingChainRule permittedMacAddress(String permittedMacAddress) {
        this.permittedMacAddress = permittedMacAddress;
        return this;
    }

    public NatPreroutingChainRule type(RuleType type) {
        this.type = type;
        return this;
    }

    public NatPreroutingChainRule(String rule) throws KuraException {
        this.type = RuleType.GENERIC;
        try {
            for (Iterator<String> ruleIterator = Arrays.asList(rule.split(" ")).iterator(); ruleIterator.hasNext();) {
                String aRuleToken = ruleIterator.next();
                if ("-i".equals(aRuleToken)) {
                    this.inputInterface = ruleIterator.next();
                } else if ("-p".equals(aRuleToken)) {
                    this.protocol = ruleIterator.next();
                } else if ("--dport".equals(aRuleToken)) {
                    this.externalPort = Integer.parseInt(ruleIterator.next());
                } else if ("--sport".equals(aRuleToken)) {
                    parsePorts(ruleIterator);
                } else if ("--to-destination".equals(aRuleToken)) {
                    String[] port = ruleIterator.next().split(":");
                    this.dstIpAddress = port[0];
                    this.internalPort = Integer.parseInt(port[1]);
                } else if ("-s".equals(aRuleToken)) {
                    String[] network = ruleIterator.next().split("/");
                    this.permittedNetwork = network[0];
                    this.permittedNetworkMask = Integer.parseInt(network[1]);
                } else if ("--mac-source".equals(aRuleToken)) {
                    this.permittedMacAddress = ruleIterator.next();
                } else if ("prerouting-kura-pf".equals(aRuleToken)) {
                    this.type = RuleType.PORT_FORWARDING;
                } else if ("prerouting-kura-ipf".equals(aRuleToken)) {
                    this.type = RuleType.IP_FORWARDING;
                }
            }
            this.rule = new StringBuilder("iptables -t nat ").append(rule).toString();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }
    }

    private void parsePorts(Iterator<String> ruleIterator) {
        String port = ruleIterator.next();
        if (port.contains(":")) {
            String[] ports = port.split(":");
            this.srcPortFirst = Integer.parseInt(ports[0]);
            this.srcPortLast = Integer.parseInt(ports[1]);
        } else {
            this.srcPortFirst = Integer.parseInt(port);
            this.srcPortLast = this.srcPortFirst;
        }
    }

    @Override
    public String toString() {
        String chain;
        if (this.type == RuleType.IP_FORWARDING) {
            chain = "prerouting-kura-ipf";
        } else if (this.type == RuleType.PORT_FORWARDING) {
            chain = "prerouting-kura-pf";
        } else {
            chain = "prerouting-kura";
        }
        StringBuilder sb = new StringBuilder("-A " + chain);
        if (this.permittedNetwork != null && !this.permittedNetwork.equals("0.0.0.0")) {
            sb.append(" -s ").append(this.permittedNetwork).append('/').append(this.permittedNetworkMask);
        }
        sb.append(" -i ").append(this.inputInterface).append(" -p ").append(this.protocol);
        if (this.permittedMacAddress != null) {
            sb.append(" -m mac --mac-source ").append(this.permittedMacAddress);
        }
        sb.append(" -m ").append(this.protocol);
        if (this.srcPortFirst > 0 && this.srcPortLast >= this.srcPortFirst) {
            sb.append(" --sport ").append(this.srcPortFirst).append(':').append(this.srcPortLast);
        }
        sb.append(" --dport ").append(this.externalPort);
        sb.append(" -j DNAT --to-destination ").append(this.dstIpAddress).append(':').append(this.internalPort);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 71;
        int result = 1;
        result = prime * result + (this.rule == null ? 0 : this.rule.hashCode());
        result = prime * result + (this.inputInterface == null ? 0 : this.inputInterface.hashCode());
        result = prime * result + (this.protocol == null ? 0 : this.protocol.hashCode());
        result = prime * result + this.externalPort;
        result = prime * result + this.internalPort;
        result = prime * result + this.srcPortFirst;
        result = prime * result + this.srcPortLast;
        result = prime * result + (this.dstIpAddress == null ? 0 : this.dstIpAddress.hashCode());
        result = prime * result + (this.permittedNetwork == null ? 0 : this.permittedNetwork.hashCode());
        result = prime * result + this.permittedNetworkMask;
        result = prime * result + (this.permittedMacAddress == null ? 0 : this.permittedMacAddress.hashCode());
        result = prime * result + (this.type == null ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        NatPreroutingChainRule other;
        if (o instanceof NatPreroutingChainRule) {
            other = (NatPreroutingChainRule) o;
        } else if (o instanceof String) {
            try {
                other = new NatPreroutingChainRule((String) o);
            } catch (KuraException e) {
                logger.error("equals() :: failed to parse NatPreroutingChainRule ", e);
                return false;
            }
        } else {
            return false;
        }

        return compareObjects(this.rule, other.rule) && compareObjects(this.inputInterface, other.inputInterface)
                && compareObjects(this.protocol, other.protocol) && this.externalPort == other.externalPort
                && this.internalPort == other.internalPort && this.srcPortFirst == other.srcPortFirst
                && this.srcPortLast == other.srcPortLast && compareObjects(this.dstIpAddress, other.dstIpAddress)
                && compareObjects(this.permittedNetwork, other.permittedNetwork)
                && this.permittedNetworkMask == other.permittedNetworkMask
                && compareObjects(this.permittedMacAddress, other.permittedMacAddress)
                && compareObjects(this.type, other.type);
    }

    private boolean compareObjects(Object obj1, Object obj2) {
        if (obj1 != null) {
            return obj1.equals(obj2);
        } else if (obj2 != null) {
            return false;
        }
        return true;
    }

    public String getInputInterface() {
        return this.inputInterface;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public int getExternalPort() {
        return this.externalPort;
    }

    public int getInternalPort() {
        return this.internalPort;
    }

    public int getSrcPortFirst() {
        return this.srcPortFirst;
    }

    public int getSrcPortLast() {
        return this.srcPortLast;
    }

    public String getDstIpAddress() {
        return this.dstIpAddress;
    }

    public String getPermittedMacAddress() {
        return this.permittedMacAddress;
    }

    public String getPermittedNetwork() {
        return this.permittedNetwork;
    }

    public int getPermittedNetworkMask() {
        return this.permittedNetworkMask;
    }

    public RuleType getType() {
        return this.type;
    }
}
