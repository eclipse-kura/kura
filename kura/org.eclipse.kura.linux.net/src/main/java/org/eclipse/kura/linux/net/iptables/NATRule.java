/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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

    private static final Logger logger = LoggerFactory.getLogger(NATRule.class);

    private String sourceInterface;						// i.e. eth0
    private String destinationInterface;				// i.e. ppp0
    private String protocol;	// protocol (i.e. all, tcp, udp)
    private String source; // source network/host (i.e. 192.168.1.0/24 or 192.168.1.1/32)
    private String destination; // destination network/host (i.e. 192.168.1.0/24 or 192.168.1.1/32)
    private boolean masquerade;

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
        this.sourceInterface = sourceInterface;
        this.destinationInterface = destinationInterface;
        this.masquerade = masquerade;
    }

    public NATRule(String sourceInterface, String destinationInterface, String protocol, String source,
            String destination, boolean masquerade) {
        this(sourceInterface, destinationInterface, masquerade);
        this.source = source;
        this.destination = destination;
        this.protocol = protocol;
    }

    /**
     * Constructor of <code>NATRule</code> object.
     */
    public NATRule() {
        this.sourceInterface = null;
        this.destinationInterface = null;
    }

    /**
     * Returns true if the <code>NATRule</code> parameters have all been set. Returns false otherwise.
     *
     * @return A boolean representing whether all parameters have been set.
     */
    public boolean isComplete() {
        if (this.sourceInterface != null && this.destinationInterface != null) {
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
        this.sourceInterface = sourceInterface;
    }

    /**
     * Setter for the destinationInterface.
     *
     * @param destinationInterface
     *            A String representing the destinationInterface.
     */
    public void setDestinationInterface(String destinationInterface) {
        this.destinationInterface = destinationInterface;
    }

    /**
     * Setter for the masquerade.
     *
     * @param masquerade
     *            A boolean representing the masquerade.
     */
    public void setMasquerade(boolean masquerade) {
        this.masquerade = masquerade;
    }

    public String getSource() {
        return this.source;
    }

    public String getDestination() {
        return this.destination;
    }

    public String getProtocol() {
        return this.protocol;
    }

    /**
     * Getter for the sourceInterface.
     *
     * @return sourceInterface A String representing the sourceInterface.
     */
    public String getSourceInterface() {
        return this.sourceInterface;
    }

    /**
     * Getter for the destinationInterface.
     *
     * @return destinationInterface A String representing the destinationInterface.
     */
    public String getDestinationInterface() {
        return this.destinationInterface;
    }

    /**
     * Getter for the masquerade.
     *
     * @return masquerade A boolean representing the masquerade.
     */
    public boolean isMasquerade() {
        return this.masquerade;
    }

    public NatPostroutingChainRule getNatPostroutingChainRule() {
        NatPostroutingChainRule ret;
        if (this.protocol == null) {
            ret = new NatPostroutingChainRule(this.destinationInterface, this.masquerade);
        } else {
            try {
                ret = new NatPostroutingChainRule(this.destinationInterface, this.protocol, this.destination,
                        this.source, this.masquerade);
            } catch (KuraException e) {
                ret = null;
                logger.error("failed to obtain NatPostroutingChainRule", e);
            }
        }
        return ret;
    }

    public FilterForwardChainRule getFilterForwardChainRule() {
        String srcNetwork = null;
        String dstNetwork = null;
        short srcMask = 0;
        short dstMask = 0;
        if (this.source != null) {
            srcNetwork = this.source.split("/")[0];
            srcMask = Short.parseShort(this.source.split("/")[1]);
        }
        if (this.destination != null) {
            dstNetwork = this.destination.split("/")[0];
            dstMask = Short.parseShort(this.destination.split("/")[1]);
        }
        return new FilterForwardChainRule(this.sourceInterface, this.destinationInterface, srcNetwork, srcMask,
                dstNetwork, dstMask, this.protocol, null, 0, 0);
    }

    @Override
    public int hashCode() {
        final int prime = 71;
        int result = 1;

        result = prime * result + (this.sourceInterface == null ? 0 : this.sourceInterface.hashCode());

        result = prime * result + (this.destinationInterface == null ? 0 : this.destinationInterface.hashCode());

        result = prime * result + (this.source == null ? 0 : this.source.hashCode());

        result = prime * result + (this.destination == null ? 0 : this.destination.hashCode());

        result = prime * result + (this.protocol == null ? 0 : this.protocol.hashCode());

        result = prime * result + (this.masquerade ? 1277 : 1279);

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NATRule)) {
            return false;
        }
        NATRule other = (NATRule) o;

        return compareObjects(this.sourceInterface, other.sourceInterface)
                && compareObjects(this.destinationInterface, other.destinationInterface)
                && this.masquerade == other.masquerade && compareObjects(this.protocol, other.protocol)
                && compareObjects(this.source, other.source) && compareObjects(this.destination, other.destination);
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
