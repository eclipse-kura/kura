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
	
	public NatPostroutingChainRule(String dstNetwork, short dstMask,
			String srcNetwork, short srcMask, String dstInterface,
			String protocol, boolean masquerade) {
		m_dstNetwork = dstNetwork;
		m_dstMask = dstMask;
		m_srcNetwork = srcNetwork;
		m_srcMask = srcMask;
		m_dstInterface = dstInterface;
		m_protocol = protocol;
		m_masquerade = masquerade;
	}
	
	public NatPostroutingChainRule(String dstInterface, boolean masquerade) {
		m_dstInterface = dstInterface;
		m_masquerade = masquerade;
		StringBuilder sbRule = new StringBuilder("iptables -t nat -o ");
		sbRule.append(m_dstInterface);
		if (m_masquerade) {
			sbRule.append(" -j MASQUERADE");
		}
		m_rule = sbRule.toString();
	}
	
	public NatPostroutingChainRule(String dstInterface, String protocol,
			String dstNetwork, String srcNetwork, boolean masquerade) throws KuraException {
		try {
			m_dstInterface = dstInterface;
			m_protocol = protocol;
			m_masquerade = masquerade;
			if (dstNetwork != null) {
				m_dstNetwork = dstNetwork.split("/")[0];
				m_dstMask = Short.parseShort(dstNetwork.split("/")[1]);
			}
			if (srcNetwork != null) {
				m_srcNetwork = srcNetwork.split("/")[0];
				m_srcMask = Short.parseShort(srcNetwork.split("/")[1]);
			}
			StringBuilder sbRule = new StringBuilder("iptables -t nat ");
			if (m_dstNetwork != null) {
				sbRule.append("-d ");
				sbRule.append(m_dstNetwork);
				sbRule.append('/');
				sbRule.append(m_dstMask);
			}
			if(m_srcNetwork != null) {
				sbRule.append("-s ");
				sbRule.append(m_srcNetwork);
				sbRule.append('/');
				sbRule.append(m_srcMask);
			}
			sbRule.append("-o ");
			sbRule.append(m_dstInterface);
			sbRule.append("-p ");
			sbRule.append(m_protocol);
			if (m_masquerade) {
				sbRule.append(" -j MASQUERADE");
			}
			m_rule = sbRule.toString();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public NatPostroutingChainRule(String rule) throws KuraException {
		try {
			String [] aRuleTokens = rule.split(" ");
			for (int i = 0; i < aRuleTokens.length; i++) {
				if ("-o".equals(aRuleTokens[i])) {
					m_dstInterface = aRuleTokens[++i];
				} else if ("-p".equals(aRuleTokens[i])) {
					m_protocol = aRuleTokens[++i];
				} else if ("-s".equals(aRuleTokens[i])) {
					m_srcNetwork = aRuleTokens[i+1].split("/")[0];
					m_srcMask = Short.parseShort(aRuleTokens[++i].split("/")[1]);
				} else if ("-d".equals(aRuleTokens[i])) {
					m_dstNetwork = aRuleTokens[i+1].split("/")[0];
					m_dstMask = Short.parseShort(aRuleTokens[++i].split("/")[1]);
				} else if ("-j".equals(aRuleTokens[i])) {
					if ("MASQUERADE".equals(aRuleTokens[++i])) {
						m_masquerade = true;
					}
				}
			}
			m_rule = new StringBuilder("iptables -t nat ").append(rule).toString();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (m_masquerade) {
			sb.append("-A POSTROUTING");
			if ((m_srcNetwork != null) && !m_srcNetwork.equals("0.0.0.0")) {
				sb.append(" -s ").append(m_srcNetwork).append('/').append(m_srcMask);
			}
			if (m_dstNetwork != null) {
				sb.append(" -d ").append(m_dstNetwork).append('/').append(m_dstMask);
			}
			sb.append(" -o ").append(m_dstInterface);
			
			if (m_protocol != null) {
				sb.append(" -p ").append(m_protocol);
			}
			sb.append(" -j MASQUERADE");
		}
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 71;
		int result = 1;
		result = prime * result + ((m_rule == null) ? 0 : m_rule.hashCode());
		result = prime * result + ((m_dstNetwork == null) ? 0 : m_dstNetwork.hashCode());
		result = prime * result + m_dstMask;
		result = prime * result + ((m_srcNetwork == null) ? 0 : m_srcNetwork.hashCode());
		result = prime * result + m_srcMask;
		result = prime * result + ((m_dstInterface == null) ? 0 : m_dstInterface.hashCode());
		result = prime * result + ((m_protocol == null) ? 0 : m_protocol.hashCode());
		result = prime * result + (m_masquerade ? 1277 : 1279);
		return result;
	}
	
	@Override
	public boolean equals (Object o) {
		NatPostroutingChainRule other = null;
		if (o instanceof NatPostroutingChainRule) {
			other = (NatPostroutingChainRule)o;
		} else if (o instanceof String) {
			try {
				other = new NatPostroutingChainRule((String)o);
			} catch (KuraException e) {
				s_logger.error("equals() :: failed to parse NatPostroutingChainRule - {}", e);
				return false;
			}
		} else {
			return false;
		}
		
		if (!compareObjects(m_rule, other.m_rule)) {
			return false;
		} else if (!compareObjects(m_dstNetwork, other.m_dstNetwork)) {
			return false;
		} else if (m_dstMask != other.m_dstMask) {
			return false;
		} else if (!compareObjects(m_srcNetwork, other.m_srcNetwork)) {
			return false;
		} else if (m_srcMask != other.m_srcMask) {
			return false;
		} else if (!compareObjects(m_dstInterface, other.m_dstInterface)) {
			return false;
		} else if (!compareObjects(m_protocol, other.m_protocol)) {
			return false;
		} else if (m_masquerade != other.m_masquerade) {
			return false;
		}
		return true;
	}
	
	private boolean compareObjects(Object obj1, Object obj2) {
        if(obj1 != null) {
            return obj1.equals(obj2);
        } else if(obj2 != null) {
            return false;
        }
        return true;
    }
		
	public boolean isMatchingForwardChainRule(FilterForwardChainRule forwardChainRule) {
		if (forwardChainRule.getState() != null) {
			// ignore 'inbound' forward rule
			return false;
		}
		if (!m_dstInterface.equals(forwardChainRule.getOutputInterface())) {
			return false;
		}
		if (m_protocol != null) {
			if (!m_protocol.equals(forwardChainRule.getProtocol())) {
				return false;
			}
		} else {
			if (forwardChainRule.getProtocol() != null) {
				return false;
			}
		}
		if (m_srcNetwork != null) {
			if (!m_srcNetwork.equals(forwardChainRule.getSrcNetwork())) {
				return false;
			}
		} else {
			if (forwardChainRule.getSrcNetwork() != null) {
				return false;
			}
		}
		if (m_dstNetwork != null) {
			if (!m_dstNetwork.equals(forwardChainRule.getDstNetwork())) {
				return false;
			}
		} else {
			if (forwardChainRule.getDstNetwork() != null) {
				return false;
			}
		}
		if (m_srcMask != forwardChainRule.getSrcMask()) {
			return false;
		}
		if(m_dstMask != forwardChainRule.getDstMask()) {
			return false;
		}
		return true;
	}
	
	public String getDstNetwork() {
		return m_dstNetwork;
	}

	public short getDstMask() {
		return m_dstMask;
	}
	
	public String getSrcNetwork() {
		return m_srcNetwork;
	}

	public short getSrcMask() {
		return m_srcMask;
	}

	public String getDstInterface() {
		return m_dstInterface;
	}

	public String getProtocol() {
		return m_protocol;
	}

	public boolean isMasquerade() {
		return m_masquerade;
	}
}
