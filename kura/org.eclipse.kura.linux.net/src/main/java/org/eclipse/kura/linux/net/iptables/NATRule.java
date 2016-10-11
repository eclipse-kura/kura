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

import java.util.List;

import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an iptables command for a NAT Rule.
 *
 */
public class NATRule {

    private static final Logger s_logger = LoggerFactory.getLogger(NATRule.class);

    private String m_sourceInterface;						// i.e. eth0
    private String m_destinationInterface;				// i.e. ppp0
    private String m_protocol;	// protocol (i.e. all, tcp, udp)
    private String m_source; // source network/host (i.e. 192.168.1.0/24 or 192.168.1.1/32)
    private String m_destination; // destination network/host (i.e. 192.168.1.0/24 or 192.168.1.1/32)
    private boolean m_masquerade;

    /**
     * Constructor of <code>NATRule</code> object.
     *
     * @param sourceInterface
     *            interface name of source network (such as eth0)
     * @param destinationInterface
     *            interface name of destination network to be reached via NAT (such as ppp0)
     * @param masquerade
     *            add masquerade entry
     */
    public NATRule(String sourceInterface, String destinationInterface, boolean masquerade) {
        this.m_sourceInterface = sourceInterface;
        this.m_destinationInterface = destinationInterface;
        this.m_masquerade = masquerade;
    }

    public NATRule(String sourceInterface, String destinationInterface, String protocol, String source,
            String destination, boolean masquerade) {
        this(sourceInterface, destinationInterface, masquerade);
        this.m_source = source;
        this.m_destination = destination;
        this.m_protocol = protocol;
    }

    /**
     * Constructor of <code>NATRule</code> object.
     */
    public NATRule() {
        this.m_sourceInterface = null;
        this.m_destinationInterface = null;
    }

    /**
     * Returns true if the <code>NATRule</code> parameters have all been set. Returns false otherwise.
     *
     * @return A boolean representing whether all parameters have been set.
     */
    public boolean isComplete() {
        if (this.m_sourceInterface != null && this.m_destinationInterface != null) {
            return true;
        }
        return false;
    }

    /**
     * Converts the <code>NATRule</code> to a <code>String</code>.
     * Returns single iptables string based on the <code>NATRule</code>, which establishes the MASQUERADE and FORWARD
     * rules:
     * <code>
     * <p>  -A POSTROUTING -o {destinationInterface} -j MASQUERADE;
     * <p>  -A FORWARD -i {sourceInterface} -o {destinationInterface} -j ACCEPT;
     * <p>  -A FORWARD -i {destinationInterface} -o {sourceInterface} -j ACCEPT
     * </code>
     *
     * @return A String representation of the <code>NATRule</code>.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<String> forwardRules = getFilterForwardChainRule().toStrings();
        for (String forwardRule : forwardRules) {
            sb.append(forwardRule).append("; ");
        }
        sb.append(getNatPostroutingChainRule().toString());
        return sb.toString();
    }

    /**
     * Setter for the sourceInterface.
     *
     * @param sourceInterface
     *            A String representing the sourceInterface.
     */
    public void setSourceInterface(String sourceInterface) {
        this.m_sourceInterface = sourceInterface;
    }

    /**
     * Setter for the destinationInterface.
     *
     * @param destinationInterface
     *            A String representing the destinationInterface.
     */
    public void setDestinationInterface(String destinationInterface) {
        this.m_destinationInterface = destinationInterface;
    }

    /**
     * Setter for the masquerade.
     *
     * @param masquerade
     *            A boolean representing the masquerade.
     */
    public void setMasquerade(boolean masquerade) {
        this.m_masquerade = masquerade;
    }

    public String getSource() {
        return this.m_source;
    }

    public String getDestination() {
        return this.m_destination;
    }

    public String getProtocol() {
        return this.m_protocol;
    }

    /**
     * Getter for the sourceInterface.
     *
     * @return sourceInterface A String representing the sourceInterface.
     */
    public String getSourceInterface() {
        return this.m_sourceInterface;
    }

    /**
     * Getter for the destinationInterface.
     *
     * @return destinationInterface A String representing the destinationInterface.
     */
    public String getDestinationInterface() {
        return this.m_destinationInterface;
    }

    /**
     * Getter for the masquerade.
     *
     * @return masquerade A boolean representing the masquerade.
     */
    public boolean isMasquerade() {
        return this.m_masquerade;
    }

    public NatPostroutingChainRule getNatPostroutingChainRule() {
        NatPostroutingChainRule ret = null;
        if (this.m_protocol == null) {
            ret = new NatPostroutingChainRule(this.m_destinationInterface, this.m_masquerade);
        } else {
            try {
                ret = new NatPostroutingChainRule(this.m_destinationInterface, this.m_protocol, this.m_destination,
                        this.m_source, this.m_masquerade);
            } catch (KuraException e) {
                s_logger.error("failed to obtain NatPostroutingChainRule {}", e);
            }
        }
        return ret;
    }

    public FilterForwardChainRule getFilterForwardChainRule() {
        String srcNetwork = null;
        String dstNetwork = null;
        short srcMask = 0;
        short dstMask = 0;
        if (this.m_source != null) {
            srcNetwork = this.m_source.split("/")[0];
            srcMask = Short.parseShort(this.m_source.split("/")[1]);
        }
        if (this.m_destination != null) {
            dstNetwork = this.m_destination.split("/")[0];
            dstMask = Short.parseShort(this.m_destination.split("/")[1]);
        }
        return new FilterForwardChainRule(this.m_sourceInterface, this.m_destinationInterface, srcNetwork, srcMask,
                dstNetwork, dstMask, this.m_protocol, null, 0, 0);
    }

    @Override
    public int hashCode() {
        final int prime = 71;
        int result = 1;

        result = prime * result + (this.m_sourceInterface == null ? 0 : this.m_sourceInterface.hashCode());

        result = prime * result + (this.m_destinationInterface == null ? 0 : this.m_destinationInterface.hashCode());

        result = prime * result + (this.m_source == null ? 0 : this.m_source.hashCode());

        result = prime * result + (this.m_destination == null ? 0 : this.m_destination.hashCode());

        result = prime * result + (this.m_protocol == null ? 0 : this.m_protocol.hashCode());

        result = prime * result + (this.m_masquerade ? 1277 : 1279);

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NATRule)) {
            return false;
        }
        NATRule other = (NATRule) o;
        if (!compareObjects(this.m_sourceInterface, other.m_sourceInterface)) {
            return false;
        } else if (!compareObjects(this.m_destinationInterface, other.m_destinationInterface)) {
            return false;
        } else if (this.m_masquerade != other.isMasquerade()) {
            return false;
        } else if (!compareObjects(this.m_protocol, other.m_protocol)) {
            return false;
        } else if (!compareObjects(this.m_source, other.m_source)) {
            return false;
        } else if (!compareObjects(this.m_destination, other.m_destination)) {
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
}
