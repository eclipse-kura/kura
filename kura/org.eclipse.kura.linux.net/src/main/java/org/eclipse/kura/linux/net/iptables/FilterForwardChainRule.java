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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.firewall.RuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterForwardChainRule {

    private static final Logger logger = LoggerFactory.getLogger(FilterForwardChainRule.class);

    private String rule;
    private String inputInterface;
    private String outputInterface;
    private String state;
    private String srcNetwork;
    private short srcMask;
    private String dstNetwork;
    private short dstMask;
    private String protocol;
    private String permittedMacAddress;
    private int srcPortFirst;
    private int srcPortLast;
    private int dstPort;
    private RuleType type;

    public FilterForwardChainRule() {
        this.type = RuleType.GENERIC;
    }

    public FilterForwardChainRule inputInterface(String inputInterface) {
        this.inputInterface = inputInterface;
        return this;
    }

    public FilterForwardChainRule outputInterface(String outputInterface) {
        this.outputInterface = outputInterface;
        return this;
    }

    public FilterForwardChainRule srcNetwork(String srcNetwork) {
        this.srcNetwork = srcNetwork;
        return this;
    }

    public FilterForwardChainRule srcMask(short srcMask) {
        this.srcMask = srcMask;
        return this;
    }

    public FilterForwardChainRule dstNetwork(String dstNetwork) {
        this.dstNetwork = dstNetwork;
        return this;
    }

    public FilterForwardChainRule dstMask(short dstMask) {
        this.dstMask = dstMask;
        return this;
    }

    public FilterForwardChainRule protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public FilterForwardChainRule permittedMacAddress(String permittedMacAddress) {
        this.permittedMacAddress = permittedMacAddress;
        return this;
    }

    public FilterForwardChainRule srcPortFirst(int srcPortFirst) {
        this.srcPortFirst = srcPortFirst;
        return this;
    }

    public FilterForwardChainRule srcPortLast(int srcPortLast) {
        this.srcPortLast = srcPortLast;
        return this;
    }

    public FilterForwardChainRule type(RuleType type) {
        this.type = type;
        return this;
    }

    public FilterForwardChainRule(String rule) throws KuraException {
        this.type = RuleType.GENERIC;
        try {
            for (Iterator<String> ruleIterator = Arrays.asList(rule.split(" ")).iterator(); ruleIterator.hasNext();) {
                String aRuleToken = ruleIterator.next();
                if ("-i".equals(aRuleToken)) {
                    this.inputInterface = ruleIterator.next();
                } else if ("-o".equals(aRuleToken)) {
                    this.outputInterface = ruleIterator.next();
                } else if ("--state".equals(aRuleToken)) {
                    this.state = ruleIterator.next();
                } else if ("-p".equals(aRuleToken)) {
                    this.protocol = ruleIterator.next();
                } else if ("-s".equals(aRuleToken)) {
                    String[] network = ruleIterator.next().split("/");
                    this.srcNetwork = network[0];
                    this.srcMask = Short.parseShort(network[1]);
                } else if ("-d".equals(aRuleToken)) {
                    String[] network = ruleIterator.next().split("/");
                    this.dstNetwork = network[0];
                    this.dstMask = Short.parseShort(network[1]);
                } else if ("forward-kura-pf".equals(aRuleToken)) {
                    this.type = RuleType.PORT_FORWARDING;
                } else if ("forward-kura-ipf".equals(aRuleToken)) {
                    this.type = RuleType.IP_FORWARDING;
                }
            }
            this.rule = new StringBuilder("iptables ").append(rule).toString();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }
    }

    public List<String> toStrings() {
        String chain;
        if (this.type == RuleType.IP_FORWARDING) {
            chain = "forward-kura-ipf";
        } else if (this.type == RuleType.PORT_FORWARDING) {
            chain = "forward-kura-pf";
        } else {
            chain = "forward-kura";
        }
        List<String> ret = new ArrayList<>();
        StringBuilder sb = new StringBuilder("-A " + chain);
        if (this.srcNetwork != null) {
            sb.append(" -s ") //
                    .append(this.srcNetwork) //
                    .append('/') //
                    .append(this.srcMask);
        }
        if (this.dstNetwork != null) {
            sb.append(" -d ") //
                    .append(this.dstNetwork) //
                    .append('/') //
                    .append(this.dstMask);
        }
        sb.append(" -i ").append(this.inputInterface);
        sb.append(" -o ").append(this.outputInterface);
        if (this.protocol != null) {
            sb.append(" -p ").append(this.protocol);
            sb.append(" -m ").append(this.protocol);
        }
        if (this.permittedMacAddress != null) {
            sb.append(" -m mac --mac-source ").append(this.permittedMacAddress);
        }
        if (this.srcPortFirst > 0 && this.srcPortLast >= this.srcPortFirst) {
            sb.append(" --sport ") //
                    .append(this.srcPortFirst) //
                    .append(':') //
                    .append(this.srcPortLast);
        }
        if (this.dstPort > 0) {
            sb.append(" --dport ").append(this.dstPort);
        }
        sb.append(" -j ACCEPT");
        ret.add(sb.toString());
        sb = new StringBuilder("-A " + chain);
        if (this.dstNetwork != null) {
            sb.append(" -s ") //
                    .append(this.dstNetwork) //
                    .append('/') //
                    .append(this.dstMask);
        }
        sb.append(" -i ").append(this.outputInterface);
        sb.append(" -o ").append(this.inputInterface);
        if (this.protocol != null) {
            sb.append(" -p ").append(this.protocol);
        }
        sb.append(" -m state --state RELATED,ESTABLISHED -j ACCEPT");
        ret.add(sb.toString());
        return ret;
    }

    @Override
    public int hashCode() {
        final int prime = 71;
        int result = 1;
        result = prime * result + (this.rule == null ? 0 : this.rule.hashCode());
        result = prime * result + (this.inputInterface == null ? 0 : this.inputInterface.hashCode());
        result = prime * result + (this.outputInterface == null ? 0 : this.outputInterface.hashCode());
        result = prime * result + (this.state == null ? 0 : this.state.hashCode());
        result = prime * result + (this.srcNetwork == null ? 0 : this.srcNetwork.hashCode());
        result = prime * result + this.srcMask;
        result = prime * result + (this.dstNetwork == null ? 0 : this.dstNetwork.hashCode());
        result = prime * result + this.dstMask;
        result = prime * result + (this.protocol == null ? 0 : this.protocol.hashCode());
        result = prime * result + (this.type == null ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        FilterForwardChainRule other;
        if (o instanceof FilterForwardChainRule) {
            other = (FilterForwardChainRule) o;
        } else if (o instanceof String) {
            try {
                other = new FilterForwardChainRule((String) o);
            } catch (KuraException e) {
                logger.error("equals() :: failed to parse FilterForwardChainRule ", e);
                return false;
            }
        } else {
            return false;
        }

        return compareObjects(this.rule, other.rule) && compareObjects(this.inputInterface, other.inputInterface)
                && compareObjects(this.outputInterface, other.outputInterface)
                && compareObjects(this.state, other.state) && compareObjects(this.srcNetwork, other.srcNetwork)
                && this.srcMask == other.srcMask && compareObjects(this.dstNetwork, other.dstNetwork)
                && this.dstMask == other.dstMask && compareObjects(this.protocol, other.protocol)
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

    @Override
    public String toString() {
        return this.rule;
    }

    public String getInputInterface() {
        return this.inputInterface;
    }

    public String getOutputInterface() {
        return this.outputInterface;
    }

    public String getState() {
        return this.state;
    }

    public String getSrcNetwork() {
        return this.srcNetwork;
    }

    public short getSrcMask() {
        return this.srcMask;
    }

    public String getDstNetwork() {
        return this.dstNetwork;
    }

    public short getDstMask() {
        return this.dstMask;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public RuleType getType() {
        return this.type;
    }
}
