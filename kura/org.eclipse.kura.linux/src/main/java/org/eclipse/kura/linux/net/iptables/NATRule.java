/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.linux.net.iptables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/* 
 * Copyright (c) 2013 Eurotech Inc. All rights reserved.
 */

/**
 * Creates an iptables command for a NAT Rule.
 * 
 * CONFIGURATION
 * 
 * Configuration will be accepted in the form of key/value pairs.  The key/value pairs are
 * strictly defined here:
 * 
 *   CONFIG_ENTRY     -> KEY + "=" + VALUE
 *   KEY              -> TYPE + INDEX + "_" + PARAM
 *   TYPE             -> "NATRule"
 *   INDEX            -> "0" | "1" | "2" | ... | "N"
 *   PARAM (required) -> "sourceInterface" | "destinationInterface"
 *   PARAM (optional) -> "masquerade"
 *   VALUE	          -> (value of the specified parameter)
 * 
 *   EXAMPLE:
 *   
 *   NATRule0_sourceInterface=eth0
 *   NATRule0_destinationInterface=wlan0
 *   NATRule0_masquerade=true
 */
public class NATRule {
	
	/*
	masquerading/NAT
 		must define:
			source network or ipaddress (i.e. xxx.xxx.xxx.xxx/mask or xxx.xxx.xxx.xxx)
			source interface
			destination interface

	*/
	
    private static final Logger s_logger = LoggerFactory.getLogger(NATRule.class);
	private String sourceInterface;						//i.e. eth0
	private String destinationInterface;				//i.e. ppp0
	private boolean masquerade;
	
	/**
	 * Constructor of <code>NATRule</code> object.
	 * 
	 * @param sourceInterface	interface name of source network (such as eth0)
	 * @param destinationInterface	interface name of destination network to be reached via NAT (such as ppp0)
	 * @param masquerade add masquerade entry
	 */
	public NATRule(String sourceInterface, String destinationInterface, boolean masquerade) {
		this.sourceInterface = sourceInterface;
		this.destinationInterface = destinationInterface;
		this.masquerade = masquerade;
	}
	
	/**
	 * Constructor of <code>NATRule</code> object.
	 */
	public NATRule() {
		sourceInterface = null;
		destinationInterface = null;
	}
	
	/**
	 * Returns true if the <code>NATRule</code> parameters have all been set.  Returns false otherwise.
	 * 
	 * @return		A boolean representing whether all parameters have been set.
	 */
	public boolean isComplete() {
		if(sourceInterface != null && destinationInterface != null)
			return true;
		return false;
	}
	
	
	/**
	 * Converts the <code>NATRule</code> to a <code>String</code>.  
	 * Returns single iptables string based on the <code>NATRule</code>, which establishes the MASQUERADE and FORWARD rules:
	 * <code>
	 * <p>  iptables -t nat -A POSTROUTING -o {destinationInterface} -j MASQUERADE;
	 * <p>  iptables -A FORWARD -i {sourceInterface} -o {destinationInterface} -j ACCEPT;
	 * <p>  iptables -A FORWARD -i {destinationInterface} -o {sourceInterface} -j ACCEPT
	 * </code>
	 * 
	 * @return		A String representation of the <code>NATRule</code>.
	 */
	public String toString() {
		
		/*EXAMPLE
		iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
		iptables -A FORWARD -i eth0 -o eth1 -m state --state RELATED,ESTABLISHED -j ACCEPT
		iptables -A FORWARD -i eth1 -o eth0 -j ACCEPT
		*/
		
        StringBuilder forward = new StringBuilder();
        forward.append("iptables -A FORWARD -i ").append(destinationInterface);
        forward.append(" -o ").append(sourceInterface);
        forward.append(" -m state --state RELATED,ESTABLISHED -j ACCEPT; ");
        
        forward.append("iptables -A FORWARD -i ");
        forward.append(sourceInterface);
        forward.append(" -o ");
        forward.append(destinationInterface);
        forward.append(" -j ACCEPT");
//      
//      String forward = new String (
//              "iptables -A FORWARD -i " + destinationInterface + " -o " + sourceInterface + " -m state --state RELATED,ESTABLISHED -j ACCEPT; " +
//              "iptables -A FORWARD -i " + sourceInterface + " -o " + destinationInterface + " -j ACCEPT");
        
        if (this.masquerade) {
            StringBuilder masquerade = new StringBuilder();
            masquerade.append("iptables -t nat -A POSTROUTING -o ");
            masquerade.append(destinationInterface);
            masquerade.append(" -j MASQUERADE");
            
            masquerade.append("; ");
            masquerade.append(forward);
    
            return masquerade.toString();
//          String masquerade = new String ("iptables -t nat -A POSTROUTING -o " + destinationInterface + " -j MASQUERADE");
//          return masquerade + "; " + forward;
        } else {
            return forward.toString();
        }
	}

	
	/**
	 * Setter for the sourceInterface.
	 * 
	 * @param sourceInterface	A String representing the sourceInterface.
	 */
	public void setSourceInterface(String sourceInterface) {
		this.sourceInterface = sourceInterface;
	}
	
	/**
	 * Setter for the destinationInterface.
	 * 
	 * @param destinationInterface	A String representing the destinationInterface.
	 */
	public void setDestinationInterface(String destinationInterface) {
		this.destinationInterface = destinationInterface;
	}
	
	/**
	 * Setter for the masquerade.
	 * 
	 * @param masquerade	A boolean representing the masquerade.
	 */
	public void setMasquerade(boolean masquerade) {
		this.masquerade = masquerade;
	}
	
	/**
	 * Getter for the sourceInterface.
	 * 
	 * @return sourceInterface		A String representing the sourceInterface.
	 */
	public String getSourceInterface() {
		return this.sourceInterface;
	}
	
	/**
	 * Getter for the destinationInterface.
	 * 
	 * @return destinationInterface		A String representing the destinationInterface.
	 */
	public String getDestinationInterface() {
		return this.destinationInterface;
	}
	
	/**
	 * Getter for the masquerade.
	 * 
	 * @return masquerade		A boolean representing the masquerade.
	 */
	public boolean isMasquerade() {
		return this.masquerade;
	}
	
	@Override
	public boolean equals(Object o) {
	    if(!(o instanceof NATRule)) {
	        return false;
	    }
	    
	    NATRule other = (NATRule) o;

	    if (!compareObjects(this.sourceInterface, other.sourceInterface)) {
	        return false;
	    } else if (!compareObjects(this.destinationInterface, other.destinationInterface)) {
	        return false;
	    } else if (masquerade != other.isMasquerade()) {
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
    public int hashCode() {
        final int prime = 71;
        int result = 1;
        result = prime * result
                + ((sourceInterface == null) ? 0 : sourceInterface.hashCode());
        result = prime * result
                + ((destinationInterface == null) ? 0 : destinationInterface.hashCode());
        result = prime * result + (masquerade ? 1277 : 1279);
        
        return result;
    }
}

