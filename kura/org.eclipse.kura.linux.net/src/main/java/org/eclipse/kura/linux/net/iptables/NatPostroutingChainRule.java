/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.firewall.RuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatPostroutingChainRule {

    private static final Logger logger = LoggerFactory.getLogger(NatPostroutingChainRule.class);

    private String rule;
    private String dstNetwork;
    private short dstMask;
    private String srcNetwork;
    private short srcMask;
    private String dstInterface;
    private String protocol;
    private boolean masquerade;
    private RuleType type;

    public NatPostroutingChainRule() {
        this.type = RuleType.GENERIC;
    }

    public NatPostroutingChainRule dstNetwork(String dstNetwork) {
        this.dstNetwork = dstNetwork;
        return this;
    }

    public NatPostroutingChainRule dstMask(short dstMask) {
        this.dstMask = dstMask;
        return this;
    }

    public NatPostroutingChainRule srcNetwork(String srcNetwork) {
        this.srcNetwork = srcNetwork;
        return this;
    }

    public NatPostroutingChainRule srcMask(short srcMask) {
        this.srcMask = srcMask;
        return this;
    }

    public NatPostroutingChainRule dstInterface(String dstInterface) {
        this.dstInterface = dstInterface;
        return this;
    }

    public NatPostroutingChainRule protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public NatPostroutingChainRule masquerade(boolean masquerade) {
        this.masquerade = masquerade;
        return this;
    }

    public NatPostroutingChainRule type(RuleType type) {
        this.type = type;
        return this;
    }

    public NatPostroutingChainRule(String dstInterface, boolean masquerade, RuleType type) {
        this.dstInterface = dstInterface;
        this.masquerade = masquerade;
        this.type = type;
        StringBuilder sbRule = new StringBuilder("-t nat -A ");
        sbRule.append(getRuleTypeString(type));
        sbRule.append(" -o ");
        sbRule.append(this.dstInterface);
        if (this.masquerade) {
            sbRule.append(" -j MASQUERADE");
        }
        this.rule = sbRule.toString();
    }

    public NatPostroutingChainRule(String dstInterface, String protocol, String dstNetwork, String srcNetwork,
            boolean masquerade, RuleType type) throws KuraException {
        try {
            this.dstInterface = dstInterface;
            this.protocol = protocol;
            this.masquerade = masquerade;
            this.type = type;
            if (dstNetwork != null) {
                this.dstNetwork = dstNetwork.split("/")[0];
                this.dstMask = Short.parseShort(dstNetwork.split("/")[1]);
            }
            if (srcNetwork != null) {
                this.srcNetwork = srcNetwork.split("/")[0];
                this.srcMask = Short.parseShort(srcNetwork.split("/")[1]);
            }
            StringBuilder sbRule = new StringBuilder("-t nat -A ");
            sbRule.append(getRuleTypeString(this.type));
            sbRule.append(" ");
            if (this.dstNetwork != null) {
                sbRule.append(" -d ");
                sbRule.append(this.dstNetwork);
                sbRule.append('/');
                sbRule.append(this.dstMask);
            }
            if (this.srcNetwork != null) {
                sbRule.append(" -s ");
                sbRule.append(this.srcNetwork);
                sbRule.append('/');
                sbRule.append(this.srcMask);
            }
            sbRule.append(" -o ");
            sbRule.append(this.dstInterface);
            sbRule.append(" -p ");
            sbRule.append(this.protocol);
            if (this.masquerade) {
                sbRule.append(" -j MASQUERADE");
            }
            this.rule = sbRule.toString();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }
    }

    public NatPostroutingChainRule(String rule) throws KuraException {
        this.type = RuleType.GENERIC;
        try {
            String[] aRuleTokens = rule.split(" ");
            for (int i = 0; i < aRuleTokens.length; i++) {
                if ("-o".equals(aRuleTokens[i])) {
                    this.dstInterface = aRuleTokens[++i];
                } else if ("-p".equals(aRuleTokens[i])) {
                    this.protocol = aRuleTokens[++i];
                } else if ("-s".equals(aRuleTokens[i])) {
                    this.srcNetwork = aRuleTokens[i + 1].split("/")[0];
                    this.srcMask = Short.parseShort(aRuleTokens[++i].split("/")[1]);
                } else if ("-d".equals(aRuleTokens[i])) {
                    this.dstNetwork = aRuleTokens[i + 1].split("/")[0];
                    this.dstMask = Short.parseShort(aRuleTokens[++i].split("/")[1]);
                } else if ("-j".equals(aRuleTokens[i])) {
                    if ("MASQUERADE".equals(aRuleTokens[++i])) {
                        this.masquerade = true;
                    }
                } else if ("postrouting-kura-pf".equals(aRuleTokens[i])) {
                    this.type = RuleType.PORT_FORWARDING;
                } else if ("postrouting-kura-ipf".equals(aRuleTokens[i])) {
                    this.type = RuleType.IP_FORWARDING;
                }
            }
            this.rule = new StringBuilder("-t nat ").append(rule).toString();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.masquerade) {
            sb.append("-A ").append(getRuleTypeString(this.type));
            if (this.srcNetwork != null && (this.type == RuleType.IP_FORWARDING || (!this.srcNetwork.equals("0.0.0.0")
                    && !this.srcNetwork.equals("::") && !this.srcNetwork.equals("0:0:0:0:0:0:0:0")))) {
                sb.append(" -s ").append(this.srcNetwork).append('/').append(this.srcMask);
            }
            if (this.dstNetwork != null) {
                sb.append(" -d ").append(this.dstNetwork).append('/').append(this.dstMask);
            }
            sb.append(" -o ").append(this.dstInterface);

            if (this.protocol != null) {
                sb.append(" -p ").append(this.protocol);
            }
            sb.append(" -j MASQUERADE");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 71;
        int result = 1;
        result = prime * result + (this.rule == null ? 0 : this.rule.hashCode());
        result = prime * result + (this.dstNetwork == null ? 0 : this.dstNetwork.hashCode());
        result = prime * result + this.dstMask;
        result = prime * result + (this.srcNetwork == null ? 0 : this.srcNetwork.hashCode());
        result = prime * result + this.srcMask;
        result = prime * result + (this.dstInterface == null ? 0 : this.dstInterface.hashCode());
        result = prime * result + (this.protocol == null ? 0 : this.protocol.hashCode());
        result = prime * result + (this.masquerade ? 1277 : 1279);
        result = prime * result + (this.type == null ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        NatPostroutingChainRule other;
        if (o instanceof NatPostroutingChainRule) {
            other = (NatPostroutingChainRule) o;
        } else if (o instanceof String) {
            try {
                other = new NatPostroutingChainRule((String) o);
            } catch (KuraException e) {
                logger.error("equals() :: failed to parse NatPostroutingChainRule ", e);
                return false;
            }
        } else {
            return false;
        }

        return compareObjects(this.rule, other.rule) && compareObjects(this.dstNetwork, other.dstNetwork)
                && this.dstMask == other.dstMask && compareObjects(this.srcNetwork, other.srcNetwork)
                && this.srcMask == other.srcMask && compareObjects(this.dstInterface, other.dstInterface)
                && compareObjects(this.protocol, other.protocol) && this.masquerade == other.masquerade
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

    public boolean isMatchingForwardChainRule(FilterForwardChainRule forwardChainRule) {
        if (forwardChainRule.getState() != null) {
            // ignore 'inbound' forward rule
            return false;
        }
        if (!this.dstInterface.equals(forwardChainRule.getOutputInterface())) {
            return false;
        }
        if (this.protocol != null) {
            if (!this.protocol.equals(forwardChainRule.getProtocol())) {
                return false;
            }
        } else {
            if (forwardChainRule.getProtocol() != null) {
                return false;
            }
        }
        if (this.srcNetwork != null) {
            if (!this.srcNetwork.equals(forwardChainRule.getSrcNetwork())) {
                return false;
            }
        } else {
            if (forwardChainRule.getSrcNetwork() != null) {
                return false;
            }
        }
        if (this.dstNetwork != null) {
            if (!this.dstNetwork.equals(forwardChainRule.getDstNetwork())) {
                return false;
            }
        } else {
            if (forwardChainRule.getDstNetwork() != null) {
                return false;
            }
        }
        if (this.srcMask != forwardChainRule.getSrcMask()) {
            return false;
        }
        if (this.dstMask != forwardChainRule.getDstMask()) {
            return false;
        }
        if (this.type != forwardChainRule.getType()) {
            return false;
        }
        return true;
    }

    public String getDstNetwork() {
        return this.dstNetwork;
    }

    public short getDstMask() {
        return this.dstMask;
    }

    public String getSrcNetwork() {
        return this.srcNetwork;
    }

    public short getSrcMask() {
        return this.srcMask;
    }

    public String getDstInterface() {
        return this.dstInterface;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public boolean isMasquerade() {
        return this.masquerade;
    }

    public RuleType getType() {
        return this.type;
    }

    private String getRuleTypeString(RuleType type) {
        if (type == RuleType.IP_FORWARDING) {
            return "postrouting-kura-ipf";
        } else if (type == RuleType.PORT_FORWARDING) {
            return "postrouting-kura-pf";
        } else {
            return "postrouting-kura";
        }
    }
}
