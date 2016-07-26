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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterForwardChainRule {
	
	private static final Logger s_logger = LoggerFactory.getLogger(FilterForwardChainRule.class);
	
	private String m_rule;
	private String m_inputInterface;
	private String m_outputInterface;
	private String m_state;
	private String m_srcNetwork;
	private short m_srcMask;
	private String m_dstNetwork;
	private short m_dstMask;
	private String m_protocol;
	private String m_permittedMacAddress;
	private int m_srcPortFirst;
	private int m_srcPortLast;
	private int m_dstPort;
	
	public FilterForwardChainRule(String inputInterface, String outputInterface,
			String srcNetwork, short srcMask, String dstNetwork, short dstMask,
			String protocol, String permittedMacAddress, int srcPortFirst, int srcPortLast) {
		m_inputInterface = inputInterface;
		m_outputInterface = outputInterface;
		m_srcNetwork = srcNetwork;
		m_srcMask = srcMask;
		m_dstNetwork = dstNetwork;
		m_dstMask = dstMask;
		m_protocol = protocol;
		m_permittedMacAddress = permittedMacAddress;
		m_srcPortFirst = srcPortFirst;
		m_srcPortLast = srcPortLast;
	}

	public FilterForwardChainRule(String rule) throws KuraException {
		try {
			String [] aRuleTokens = rule.split(" ");
			for (int i = 0; i < aRuleTokens.length; i++) {
				if ("-i".equals(aRuleTokens[i])) {
					m_inputInterface = aRuleTokens[++i];
				} else if ("-o".equals(aRuleTokens[i])) {
					m_outputInterface = aRuleTokens[++i];
				} else if ("--state".equals(aRuleTokens[i])) {
					m_state = aRuleTokens[++i];
				} else if ("-p".equals(aRuleTokens[i])) {
					m_protocol = aRuleTokens[++i];
				} else if ("-s".equals(aRuleTokens[i])) {
					m_srcNetwork = aRuleTokens[i+1].split("/")[0];
					m_srcMask = Short.parseShort(aRuleTokens[++i].split("/")[1]);
				} else if ("-d".equals(aRuleTokens[i])) {
					m_dstNetwork = aRuleTokens[i+1].split("/")[0];
					m_dstMask = Short.parseShort(aRuleTokens[++i].split("/")[1]);
				}
			}
			m_rule = new StringBuilder("iptables ").append(rule).toString();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public List<String> toStrings() {
		List<String> ret = new ArrayList<String>();
		StringBuilder sb = new StringBuilder("-A FORWARD");
		if (m_srcNetwork != null) {
			sb.append(" -s ").append(m_srcNetwork).append('/').append(m_srcMask);
		}
		if (m_dstNetwork != null) {
			sb.append(" -d ").append(m_dstNetwork).append('/').append(m_dstMask);
		}
		sb.append(" -i ").append(m_inputInterface);
		sb.append(" -o ").append(m_outputInterface);
		if (m_protocol != null) {
			sb.append(" -p ").append(m_protocol);
			sb.append(" -m ").append(m_protocol);
		}
		if (m_permittedMacAddress != null) {
			sb.append(" -m mac --mac-source ").append(m_permittedMacAddress);
		}
		if ((m_srcPortFirst > 0) && (m_srcPortLast >= m_srcPortFirst)) {
			sb.append(" --sport ").append(m_srcPortFirst).append(':').append(m_srcPortLast);
		}
		if (m_dstPort > 0) {
			sb.append(" --dport ").append(m_dstPort);
		}
		sb.append(" -j ACCEPT");
		ret.add(sb.toString());
		sb = new StringBuilder("-A FORWARD");
		if (m_dstNetwork != null) {
			sb.append(" -s ").append(m_dstNetwork).append('/').append(m_dstMask);
		}
		sb.append(" -i ").append(m_outputInterface);
		sb.append(" -o ").append(m_inputInterface);
		if (m_protocol != null) {
			sb.append(" -p ").append(m_protocol);
		}
		sb.append(" -m state --state RELATED,ESTABLISHED -j ACCEPT");
		ret.add(sb.toString());
		return ret;
	}
	
	@Override
	public int hashCode() {
		final int prime = 71;
		int result = 1;
		result = prime * result + ((m_rule == null) ? 0 : m_rule.hashCode());
		result = prime * result + ((m_inputInterface == null) ? 0 : m_inputInterface.hashCode());
		result = prime * result + ((m_outputInterface == null) ? 0 : m_outputInterface.hashCode());
		result = prime * result + ((m_state == null) ? 0 : m_state.hashCode());
		result = prime * result + ((m_srcNetwork == null) ? 0 : m_srcNetwork.hashCode());
		result = prime * result + m_srcMask;
		result = prime * result + ((m_dstNetwork == null) ? 0 : m_dstNetwork.hashCode());
		result = prime * result + m_dstMask;
		result = prime * result + ((m_protocol == null) ? 0 : m_protocol.hashCode());
		return result;
	}
	
	@Override
	public boolean equals (Object o) {
		FilterForwardChainRule other = null;
		if (o instanceof FilterForwardChainRule) {
			other = (FilterForwardChainRule)o;
		} else if (o instanceof String) {
			try {
				other = new FilterForwardChainRule((String)o);
			} catch (KuraException e) {
				s_logger.error("equals() :: failed to parse FilterForwardChainRule - {}", e);
				return false;
			}
		} else {
			return false;
		}
		
		if (!compareObjects(m_rule, other.m_rule)) {
			return false;
		} else if (!compareObjects(m_inputInterface, other.m_inputInterface)) {
			return false;
		} else if (!compareObjects(m_outputInterface, other.m_outputInterface)) {
			return false;
		} else if (!compareObjects(m_state, other.m_state)) {
			return false;
		} else if (!compareObjects(m_srcNetwork, other.m_srcNetwork)) {
			return false;
		} else if (m_srcMask != other.m_srcMask) {
			return false;
		} else if (!compareObjects(m_dstNetwork, other.m_dstNetwork)) {
			return false;
		} else if (m_dstMask != other.m_dstMask) {
			return false;
		} else if (!compareObjects(m_protocol, other.m_protocol)) {
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

	@Override
	public String toString() {
		return m_rule;
	}
	
	public String getInputInterface() {
		return m_inputInterface;
	}

	public String getOutputInterface() {
		return m_outputInterface;
	}

	public String getState() {
		return m_state;
	}

	public String getSrcNetwork() {
		return m_srcNetwork;
	}

	public short getSrcMask() {
		return m_srcMask;
	}

	public String getDstNetwork() {
		return m_dstNetwork;
	}

	public short getDstMask() {
		return m_dstMask;
	}

	public String getProtocol() {
		return m_protocol;
	}
}
