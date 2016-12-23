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

public class NatPreroutingChainRule {

    private static final Logger s_logger = LoggerFactory.getLogger(NatPreroutingChainRule.class);

    private String m_rule;
    private String m_inputInterface;
    private String m_protocol;
    private int m_externalPort;
    private int m_internalPort;
    private int m_srcPortFirst;
    private int m_srcPortLast;
    private String m_dstIpAddress;
    private String m_permittedNetwork;
    private int m_permittedNetworkMask;
    private String m_permittedMacAddress;

    public NatPreroutingChainRule(String inputInterface, String protocol, int externalPort, int internalPort,
            int srcPortFirst, int srcPortLast, String dstIpAddress, String permittedNetwork, int permittedNetworkMask,
            String permittedMacAddress) {
        this.m_inputInterface = inputInterface;
        this.m_protocol = protocol;
        this.m_externalPort = externalPort;
        this.m_internalPort = internalPort;
        this.m_srcPortFirst = srcPortFirst;
        this.m_srcPortLast = srcPortLast;
        this.m_dstIpAddress = dstIpAddress;
        this.m_permittedNetwork = permittedNetwork;
        this.m_permittedNetworkMask = permittedNetworkMask;
        this.m_permittedMacAddress = permittedMacAddress;
    }

    public NatPreroutingChainRule(String rule) throws KuraException {
        try {
            String[] aRuleTokens = rule.split(" ");
            for (int i = 0; i < aRuleTokens.length; i++) {
                if ("-i".equals(aRuleTokens[i])) {
                    this.m_inputInterface = aRuleTokens[++i];
                } else if ("-p".equals(aRuleTokens[i])) {
                    this.m_protocol = aRuleTokens[++i];
                } else if ("--dport".equals(aRuleTokens[i])) {
                    this.m_externalPort = Integer.parseInt(aRuleTokens[++i]);
                } else if ("--sport".equals(aRuleTokens[i])) {
                    if (aRuleTokens[i + 1].indexOf(':') > 0) {
                        this.m_srcPortFirst = Integer.parseInt(aRuleTokens[i + 1].split(":")[0]);
                        this.m_srcPortLast = Integer.parseInt(aRuleTokens[++i].split(":")[1]);
                    } else {
                        this.m_srcPortFirst = Integer.parseInt(aRuleTokens[++i]);
                        this.m_srcPortLast = this.m_srcPortFirst;
                    }
                } else if ("--to-destination".equals(aRuleTokens[i])) {
                    this.m_dstIpAddress = aRuleTokens[i + 1].split(":")[0];
                    this.m_internalPort = Integer.parseInt(aRuleTokens[++i].split(":")[1]);
                } else if ("-s".equals(aRuleTokens[i])) {
                    this.m_permittedNetwork = aRuleTokens[i + 1].split("/")[0];
                    this.m_permittedNetworkMask = Integer.parseInt(aRuleTokens[++i].split("/")[1]);
                } else if ("--mac-source".equals(aRuleTokens[i])) {
                    this.m_permittedMacAddress = aRuleTokens[++i];
                }
            }
            this.m_rule = new StringBuilder("iptables -t nat ").append(rule).toString();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("-A PREROUTING");
        if (this.m_permittedNetwork != null && !this.m_permittedNetwork.equals("0.0.0.0")) {
            sb.append(" -s ").append(this.m_permittedNetwork).append('/').append(this.m_permittedNetworkMask);
        }
        sb.append(" -i ").append(this.m_inputInterface).append(" -p ").append(this.m_protocol);
        if (this.m_permittedMacAddress != null) {
            sb.append(" -m mac --mac-source ").append(this.m_permittedMacAddress);
        }
        sb.append(" -m ").append(this.m_protocol);
        if (this.m_srcPortFirst > 0 && this.m_srcPortLast >= this.m_srcPortFirst) {
            sb.append(" --sport ").append(this.m_srcPortFirst).append(':').append(this.m_srcPortLast);
        }
        sb.append(" --dport ").append(this.m_externalPort);
        sb.append(" -j DNAT --to-destination ").append(this.m_dstIpAddress).append(':').append(this.m_internalPort);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 71;
        int result = 1;
        result = prime * result + (this.m_rule == null ? 0 : this.m_rule.hashCode());
        result = prime * result + (this.m_inputInterface == null ? 0 : this.m_inputInterface.hashCode());
        result = prime * result + (this.m_protocol == null ? 0 : this.m_protocol.hashCode());
        result = prime * result + this.m_externalPort;
        result = prime * result + this.m_internalPort;
        result = prime * result + this.m_srcPortFirst;
        result = prime * result + this.m_srcPortLast;
        result = prime * result + (this.m_dstIpAddress == null ? 0 : this.m_dstIpAddress.hashCode());
        result = prime * result + (this.m_permittedNetwork == null ? 0 : this.m_permittedNetwork.hashCode());
        result = prime * result + this.m_permittedNetworkMask;
        result = prime * result + (this.m_permittedMacAddress == null ? 0 : this.m_permittedMacAddress.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        NatPreroutingChainRule other = null;
        if (o instanceof NatPreroutingChainRule) {
            other = (NatPreroutingChainRule) o;
        } else if (o instanceof String) {
            try {
                other = new NatPreroutingChainRule((String) o);
            } catch (KuraException e) {
                s_logger.error("equals() :: failed to parse NatPreroutingChainRule - {}", e);
                return false;
            }
        } else {
            return false;
        }

        if (!compareObjects(this.m_rule, other.m_rule)) {
            return false;
        } else if (!compareObjects(this.m_inputInterface, other.m_inputInterface)) {
            return false;
        } else if (!compareObjects(this.m_protocol, other.m_protocol)) {
            return false;
        } else if (this.m_externalPort != other.m_externalPort) {
            return false;
        } else if (this.m_internalPort != other.m_internalPort) {
            return false;
        } else if (this.m_srcPortFirst != other.m_srcPortFirst) {
            return false;
        } else if (this.m_srcPortLast != other.m_srcPortLast) {
            return false;
        } else if (!compareObjects(this.m_dstIpAddress, other.m_dstIpAddress)) {
            return false;
        } else if (!compareObjects(this.m_permittedNetwork, other.m_permittedNetwork)) {
            return false;
        } else if (this.m_permittedNetworkMask != other.m_permittedNetworkMask) {
            return false;
        } else if (!compareObjects(this.m_permittedMacAddress, other.m_permittedMacAddress)) {
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

    public String getInputInterface() {
        return this.m_inputInterface;
    }

    public String getProtocol() {
        return this.m_protocol;
    }

    public int getExternalPort() {
        return this.m_externalPort;
    }

    public int getInternalPort() {
        return this.m_internalPort;
    }

    public int getSrcPortFirst() {
        return this.m_srcPortFirst;
    }

    public int getSrcPortLast() {
        return this.m_srcPortLast;
    }

    public String getDstIpAddress() {
        return this.m_dstIpAddress;
    }

    public String getPermittedMacAddress() {
        return this.m_permittedMacAddress;
    }

    public String getPermittedNetwork() {
        return this.m_permittedNetwork;
    }

    public int getPermittedNetworkMask() {
        return this.m_permittedNetworkMask;
    }
}
