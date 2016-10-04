/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.linux.net.iptables;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatPostroutingChainRule {

    private static final Logger s_logger = LoggerFactory.getLogger(NatPostroutingChainRule.class);

    private String m_rule;
    private String m_dstNetwork;
    private short m_dstMask;
    private String m_srcNetwork;
    private short m_srcMask;
    private String m_dstInterface;
    private String m_protocol;
    private boolean m_masquerade;

    public NatPostroutingChainRule(String dstNetwork, short dstMask, String srcNetwork, short srcMask,
            String dstInterface, String protocol, boolean masquerade) {
        this.m_dstNetwork = dstNetwork;
        this.m_dstMask = dstMask;
        this.m_srcNetwork = srcNetwork;
        this.m_srcMask = srcMask;
        this.m_dstInterface = dstInterface;
        this.m_protocol = protocol;
        this.m_masquerade = masquerade;
    }

    public NatPostroutingChainRule(String dstInterface, boolean masquerade) {
        this.m_dstInterface = dstInterface;
        this.m_masquerade = masquerade;
        StringBuilder sbRule = new StringBuilder("iptables -t nat -o ");
        sbRule.append(this.m_dstInterface);
        if (this.m_masquerade) {
            sbRule.append(" -j MASQUERADE");
        }
        this.m_rule = sbRule.toString();
    }

    public NatPostroutingChainRule(String dstInterface, String protocol, String dstNetwork, String srcNetwork,
            boolean masquerade) throws KuraException {
        try {
            this.m_dstInterface = dstInterface;
            this.m_protocol = protocol;
            this.m_masquerade = masquerade;
            if (dstNetwork != null) {
                this.m_dstNetwork = dstNetwork.split("/")[0];
                this.m_dstMask = Short.parseShort(dstNetwork.split("/")[1]);
            }
            if (srcNetwork != null) {
                this.m_srcNetwork = srcNetwork.split("/")[0];
                this.m_srcMask = Short.parseShort(srcNetwork.split("/")[1]);
            }
            StringBuilder sbRule = new StringBuilder("iptables -t nat ");
            if (this.m_dstNetwork != null) {
                sbRule.append("-d ");
                sbRule.append(this.m_dstNetwork);
                sbRule.append('/');
                sbRule.append(this.m_dstMask);
            }
            if (this.m_srcNetwork != null) {
                sbRule.append("-s ");
                sbRule.append(this.m_srcNetwork);
                sbRule.append('/');
                sbRule.append(this.m_srcMask);
            }
            sbRule.append("-o ");
            sbRule.append(this.m_dstInterface);
            sbRule.append("-p ");
            sbRule.append(this.m_protocol);
            if (this.m_masquerade) {
                sbRule.append(" -j MASQUERADE");
            }
            this.m_rule = sbRule.toString();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public NatPostroutingChainRule(String rule) throws KuraException {
        try {
            String[] aRuleTokens = rule.split(" ");
            for (int i = 0; i < aRuleTokens.length; i++) {
                if ("-o".equals(aRuleTokens[i])) {
                    this.m_dstInterface = aRuleTokens[++i];
                } else if ("-p".equals(aRuleTokens[i])) {
                    this.m_protocol = aRuleTokens[++i];
                } else if ("-s".equals(aRuleTokens[i])) {
                    this.m_srcNetwork = aRuleTokens[i + 1].split("/")[0];
                    this.m_srcMask = Short.parseShort(aRuleTokens[++i].split("/")[1]);
                } else if ("-d".equals(aRuleTokens[i])) {
                    this.m_dstNetwork = aRuleTokens[i + 1].split("/")[0];
                    this.m_dstMask = Short.parseShort(aRuleTokens[++i].split("/")[1]);
                } else if ("-j".equals(aRuleTokens[i])) {
                    if ("MASQUERADE".equals(aRuleTokens[++i])) {
                        this.m_masquerade = true;
                    }
                }
            }
            this.m_rule = new StringBuilder("iptables -t nat ").append(rule).toString();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.m_masquerade) {
            sb.append("-A POSTROUTING");
            if (this.m_srcNetwork != null && !this.m_srcNetwork.equals("0.0.0.0")) {
                sb.append(" -s ").append(this.m_srcNetwork).append('/').append(this.m_srcMask);
            }
            if (this.m_dstNetwork != null) {
                sb.append(" -d ").append(this.m_dstNetwork).append('/').append(this.m_dstMask);
            }
            sb.append(" -o ").append(this.m_dstInterface);

            if (this.m_protocol != null) {
                sb.append(" -p ").append(this.m_protocol);
            }
            sb.append(" -j MASQUERADE");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 71;
        int result = 1;
        result = prime * result + (this.m_rule == null ? 0 : this.m_rule.hashCode());
        result = prime * result + (this.m_dstNetwork == null ? 0 : this.m_dstNetwork.hashCode());
        result = prime * result + this.m_dstMask;
        result = prime * result + (this.m_srcNetwork == null ? 0 : this.m_srcNetwork.hashCode());
        result = prime * result + this.m_srcMask;
        result = prime * result + (this.m_dstInterface == null ? 0 : this.m_dstInterface.hashCode());
        result = prime * result + (this.m_protocol == null ? 0 : this.m_protocol.hashCode());
        result = prime * result + (this.m_masquerade ? 1277 : 1279);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        NatPostroutingChainRule other = null;
        if (o instanceof NatPostroutingChainRule) {
            other = (NatPostroutingChainRule) o;
        } else if (o instanceof String) {
            try {
                other = new NatPostroutingChainRule((String) o);
            } catch (KuraException e) {
                s_logger.error("equals() :: failed to parse NatPostroutingChainRule - {}", e);
                return false;
            }
        } else {
            return false;
        }

        if (!compareObjects(this.m_rule, other.m_rule)) {
            return false;
        } else if (!compareObjects(this.m_dstNetwork, other.m_dstNetwork)) {
            return false;
        } else if (this.m_dstMask != other.m_dstMask) {
            return false;
        } else if (!compareObjects(this.m_srcNetwork, other.m_srcNetwork)) {
            return false;
        } else if (this.m_srcMask != other.m_srcMask) {
            return false;
        } else if (!compareObjects(this.m_dstInterface, other.m_dstInterface)) {
            return false;
        } else if (!compareObjects(this.m_protocol, other.m_protocol)) {
            return false;
        } else if (this.m_masquerade != other.m_masquerade) {
            return false;
        }
        return true;
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
        if (!this.m_dstInterface.equals(forwardChainRule.getOutputInterface())) {
            return false;
        }
        if (this.m_protocol != null) {
            if (!this.m_protocol.equals(forwardChainRule.getProtocol())) {
                return false;
            }
        } else {
            if (forwardChainRule.getProtocol() != null) {
                return false;
            }
        }
        if (this.m_srcNetwork != null) {
            if (!this.m_srcNetwork.equals(forwardChainRule.getSrcNetwork())) {
                return false;
            }
        } else {
            if (forwardChainRule.getSrcNetwork() != null) {
                return false;
            }
        }
        if (this.m_dstNetwork != null) {
            if (!this.m_dstNetwork.equals(forwardChainRule.getDstNetwork())) {
                return false;
            }
        } else {
            if (forwardChainRule.getDstNetwork() != null) {
                return false;
            }
        }
        if (this.m_srcMask != forwardChainRule.getSrcMask()) {
            return false;
        }
        if (this.m_dstMask != forwardChainRule.getDstMask()) {
            return false;
        }
        return true;
    }

    public String getDstNetwork() {
        return this.m_dstNetwork;
    }

    public short getDstMask() {
        return this.m_dstMask;
    }

    public String getSrcNetwork() {
        return this.m_srcNetwork;
    }

    public short getSrcMask() {
        return this.m_srcMask;
    }

    public String getDstInterface() {
        return this.m_dstInterface;
    }

    public String getProtocol() {
        return this.m_protocol;
    }

    public boolean isMasquerade() {
        return this.m_masquerade;
    }
}
