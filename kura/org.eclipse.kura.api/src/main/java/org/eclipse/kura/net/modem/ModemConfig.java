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
package org.eclipse.kura.net.modem;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;

/**
 * Modem configuration representation
 *
 */
public class ModemConfig implements NetConfig {

	/**
	 * Configuration for a cellular modem.
	 */
	public static enum PdpType { IP, PPP, IPv6, UNKNOWN };
	public static enum AuthType { NONE, AUTO, PAP, CHAP };
	
	private boolean m_enabled = false;
	private String m_dialString = "";
	private int m_pppNumber = 0;
	private int m_profileID = 0;
	private PdpType m_pdpType = PdpType.IP;
	private AuthType m_authType = AuthType.NONE;
	private String m_apn = "";
	private SimCardSlot m_activeSimCardSlot = SimCardSlot.A;
	private String m_username = "";
	private String m_password = "";
	private boolean m_persist = false;
	private int m_maxFail = 0;
	private int m_idle = 0;
	private String m_activeFilter = "";
	private int m_lcpEchoInterval = 0;
	private int m_lcpEchoFailure = 0;
	private IPAddress m_ipAddress = null;
	private int m_dataCompression = 0;			// FIXME: change to enum?
	private int m_headerCompression = 0;		// FIXME: change to enum?
	private boolean m_gpsEnabled = false;
	private int m_resetTimeout = 0;
	
	/**
	 * Empty constructor
	 */
	public ModemConfig() {}
	
	/**
	 * PDP config constructor
	 * 
	 * @param apn - access point name as {@link String}
	 * @param ipAddress - IP address as {@link String}
	 * @param profileID - PDP profile ID as {@link int}
	 * @param pdpType - PDP type as {@link PDP_Type}
	 * @param dataCompression - PDP data compression as {@link int}
	 * @param headerCompresion - PDP header compression as {@link int}
	 */
	public ModemConfig(int profileID, PdpType pdpType, String apn, IPAddress ipAddress,
			int dataCompression, int headerCompresion) {
		
		this.m_profileID = profileID;
		this.m_pdpType = pdpType;
		this.m_apn = apn;
		this.m_ipAddress = ipAddress;
		this.m_dataCompression = dataCompression;
		this.m_headerCompression = headerCompresion;
	}
	
	
	
    /**
     * Reports whether it is enabled.
     * 
     * @return is enabled as {@link boolean}
     */
    public boolean isEnabled() {
        return this.m_enabled;
    }
    
    /**
     * Sets the enabled setting.
     * 
     * @param enabled - enabled status as {@link boolean}
     */
    public void setEnabled(boolean enabled) {
        this.m_enabled = enabled;
    }

	/**
	 * Gets the dial string.
	 * 
	 * @return dial string as {@link String}
	 */
	public String getDialString() {
		return this.m_dialString;
	}
	
	/**
	 * Sets the dial string.
	 * 
	 * @param dialString - dial string as {@link String}
	 */
	public void setDialString(String dialString) {
		this.m_dialString = dialString;
	}
	
	/**
	 * Reports authentication type.
	 * 
	 * @return authentication type as {@link AuthType}
	 */
	public AuthType getAuthType() {
		return this.m_authType;
	}
	
	/**
	 * Sets authentication type.
	 * 
	 * @param authType - authentication type as {@link AuthType}
	 */
	public void setAuthType(AuthType authType) {
		this.m_authType = authType;
	}
	
	/**
	 * Reports user name.
	 * 
	 * @return user name as {@link String}
	 */
	public String getUsername() {
		return this.m_username;
	}
	
	/**
	 * Sets user name.
	 * 
	 * @param username - user name as {@link String}
	 */
	public void setUsername(String username) {
		this.m_username = username;
	}
	
	/**
	 * Reports password.
	 * 
	 * @return password as {@link String}
	 */
	public String getPassword() {
		return this.m_password;
	}
	
	/**
	 * Sets password.
	 * 
	 * @param password - password as {@link String}
	 */
	public void setPassword(String password) {
		this.m_password = password;
	}
	
	/**
	 * Reports if pppd is instructed to exit after a connection is terminated.
	 * 
	 * @return 'persist' flag {@link boolean}
	 */
	public boolean isPersist() {
		return m_persist;
	}
	
	/**
	 * Sets 'persist' flag to instruct pppd if it needs to exit after a connection is terminated.  
	 * @param persist as {@link boolean}
	 */
	public void setPersist(boolean persist) {
		m_persist = persist;
	}
	
	/**
	 * Reports maximum number of failed connection attempts. 
	 * 
	 * @return maximum number of failed connection attempts as {@link int}
	 */
	public int getMaxFail() {
		return m_maxFail;
	}
	
	/**
	 * Sets maximum number of failed connection attempts
	 * 
	 * @param maxFail - maximum number of failed connection attempts as {@link int}
	 */
	public void setMaxFail(int maxFail) {
		m_maxFail = maxFail;
	}
	
	/**
	 * Reports value of the 'idle' option.
	 * The 'idle' option specifies that pppd should disconnect if the link is idle for n seconds.
	 * 
	 * @return value of the 'idle' option as {@link int}
	 */
	public int getIdle() {
		return m_idle;
	}
	
	/**
	 * Sets value of the 'idle' option. 
	 * The 'idle' option specifies that pppd should disconnect if the link is idle for n seconds.
	 * 
	 * @param idle 
	 */
	public void setIdle(int idle) {
		m_idle = idle;
	}
	
	/**
	 * Reports the value of the 'active-filter' option that specifies a packet filter to be 
	 * applied to data packets to determine which packets are to be regarded as link activity.
	 * 
	 * @return value of the 'active-filter' option as {@link String}
	 */
	public String getActiveFilter() {
		return m_activeFilter;
	}
	
	/**
	 * Sets the value of the 'active-filter' option that specifies a packet filter to be 
	 * applied to data packets to determine which packets are to be regarded as link activity.
	 * 
	 * @param activeFilter - active filter as {@link String}
	 */
	public void setActiveFilter(String activeFilter) {
		m_activeFilter = activeFilter;
	}
	
	/**
	 * Reports LCP echo interval
	 * 
	 * @return LCP echo interval (in sec) as {@link int}
	 */
	public int getLcpEchoInterval() {
		return this.m_lcpEchoInterval;
	}
	
	/**
	 * Sets LCP echo interval
	 * 
	 * @param lcpEchoInterval - LCP Echo interval as {@link int}
	 */
	public void setLcpEchoInterval(int lcpEchoInterval) {
		this.m_lcpEchoInterval = lcpEchoInterval;
	}
	
	/**
	 * Reports number of failed LCP echo requests 
	 * @return number of failed LCP echo requests as {@link int}
	 */
	public int getLcpEchoFailure() {
		return this.m_lcpEchoFailure;
	}
	
	/**
	 * Sets number of failed LCP echo requests 
	 * (unacknowledged LCP echo requests to be sent for pppd to presume the peer to be dead)
	 *  
	 * @param lcpEchoFailure
	 */
	public void setLcpEchoFailure(int lcpEchoFailure) {
		this.m_lcpEchoFailure = lcpEchoFailure;
	}
    
    /**
     * Reports PPP number (i.e. '0' for ppp0).
     * 
     * @return PPP number as {@link int}
     */
    public int getPppNumber() {
        return this.m_pppNumber;
    }
    
    /**
     * Sets PPP number (i.e. '0' for ppp0).
     * 
     * @param pppNumber - PPP number as {@link int}
     */
    public void setPppNumber(int pppNumber) {
        this.m_pppNumber = pppNumber;
    }
    
    /**
     * Reports PDP profile ID.
     * 
     * @return PDP profile ID as {@link int}
     */
    public int getProfileID() {
        return this.m_profileID;
    }
	
	/**
	 * Sets PDP profile ID.
	 * 
	 * @param id - PDP profile ID as {@link int}
	 */
	public void setProfileID(int id) {
		this.m_profileID = id;
	}
	
	/**
	 * Reports PDP type.
	 * 
	 * @return PDP type as {@link PDP_Type}
	 */
	public PdpType getPdpType() {
		return this.m_pdpType;
	}
	
	/**
	 * Sets PDP type.
	 * 
	 * @param pdpType - PDP type as {@link PDP_Type}
	 */
	public void setPdpType(PdpType pdpType) {
		this.m_pdpType = pdpType;
	}
	
	/**
	 * Reports access point name.
	 * 
	 * @return access point name as {@link String}
	 */
	public String getApn() {
		return this.m_apn;
	}
	
	/**
	 * Sets access point name.
	 * 
	 * @param apn - access point name as {@link String}
	 */
	public void setApn(String apn) {
		this.m_apn = apn;
	}
	
	
	/**
	 * Reports active SIM card slot
	 * 
	 * @return active SIM card slot as {@link SimCardSlot}
	 */
	public SimCardSlot getActiveSimCardSlot() {
		return m_activeSimCardSlot;
	}

	/**
	 * Sets active SIM card slot
	 * 
	 * @param activeSimCardSlot - SIM card slot as {@link SimCardSlot}
	 */
	public void setActiveSimCardSlot(SimCardSlot activeSimCardSlot) {
		m_activeSimCardSlot = activeSimCardSlot;
	}

	/**
	 * Reports PDP IP address.
	 * 
	 * @return IP address as {@link IPAddress}
	 */
	public IPAddress getIpAddress() {
		return this.m_ipAddress;
	}
	
	/**
	 * Sets PDP IP address.
	 * 
	 * @param ip - IP address as {@link IPAddress}
	 */
	public void setIpAddress(IPAddress address) {
		this.m_ipAddress = address;
	}
	
	/**
	 * Reports a value of numeric parameter that supports PDP data compression.
	 * 
	 * @return PDP data compression as {@link int}
	 */
	public int getDataCompression() {
		return this.m_dataCompression;
	}
	
	/**
	 * Sets a value of numeric parameter that supports PDP data compression.
	 * 
	 * @param dataCompression - PDP data compression as {@link int}
	 */
	public void setDataCompression(int dataCompression) {
		this.m_dataCompression = dataCompression;
	}
	
	/**
	 * Reports a value of numeric parameter that supports PDP header compression.
	 * 
	 * @return PDP header compression as {@link int}
	 */
	public int getHeaderCompression() {
		return this.m_headerCompression;
	}
	
	/**
	 * Sets a value of numeric parameter that supports PDP header compression.
	 * 
	 * @param - headerCompression PDP header compression as {@link int}
	 */
	public void setHeaderCompression(int headerCompression) {
		this.m_headerCompression = headerCompression;
	}
	
	/**
	 * Reports if PDP data compression is enabled.
	 * 
	 * @return {@link boolean}
	 */
	public boolean isDataCompression() {
		return (this.m_dataCompression == 0)? false : true;
	}
	
	/**
	 * Reports if PDP header compression is enabled.
	 * 
	 * @return {@link boolean}
	 */
	public boolean isHeaderCompression() {
		return (this.m_headerCompression == 0)? false : true;
	}
	
	public boolean isGpsEnabled() {
		return m_gpsEnabled;
	}
	
	public int getResetTimeout() {
		return m_resetTimeout;
	}
	
	public void setResetTimeout(int tout) {
		m_resetTimeout = tout;
	}
	
	public void setGpsEnabled(boolean gpsEnabled) {
		m_gpsEnabled = gpsEnabled;
	}

	@Override
	public int hashCode() {
		final int prime = 59;
		int result = super.hashCode();
		
		result = prime * result
				+ m_profileID;
		result = prime * result
				+ ((m_pdpType == null) ? 0 : m_pdpType.hashCode());
		result = prime * result
				+ ((m_authType == null) ? 0 : m_authType.hashCode());
        result = prime * result
                + ((m_apn == null) ? 0 : m_apn.hashCode());
        result = prime * result
                + ((m_activeSimCardSlot == null) ? 0 : m_activeSimCardSlot.hashCode());
		result = prime * result
				+ ((m_username == null) ? 0 : m_username.hashCode());
		result = prime * result
				+ ((m_password == null) ? 0 : m_password.hashCode());
		result = prime * result
				+ ((m_ipAddress == null) ? 0 : m_ipAddress.hashCode());
        result = prime * result
                + m_pppNumber;
        result = prime * result
                + m_maxFail;
        result = prime * result
                + m_resetTimeout;
        result = prime * result
                + m_idle;
        result = prime * result
				+ ((m_activeFilter == null) ? 0 : m_activeFilter.hashCode());
        result = prime * result
                + m_lcpEchoFailure;
        result = prime * result
                + m_lcpEchoInterval;
		result = prime * result
				+ m_dataCompression;
		result = prime * result
				+ m_headerCompression;
		result = prime * result + (m_persist? 1 : 0);
		result = prime * result + (m_gpsEnabled? 1 : 0);
		
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof ModemConfig)) {
			return false;
		}
		
		ModemConfig otherConfig = (ModemConfig) obj;
		
		if (this.m_enabled != otherConfig.isEnabled()) {
		    return false;
		}
        
        if (this.m_pppNumber != otherConfig.getPppNumber()) {
            return false;
        }
        
        if (this.m_persist != otherConfig.isPersist()) {
        	return false;
        }
        
        if (this.m_maxFail != otherConfig.getMaxFail()) {
        	return false;
        }
        
        if (this.m_resetTimeout != otherConfig.getResetTimeout()) {
        	return false;
        }
        
        if (this.m_idle != otherConfig.getIdle()) {
        	return false;
        }
        
        if (this.m_lcpEchoInterval != otherConfig.getLcpEchoInterval()) {
        	return false;
        }
        
        if (this.m_lcpEchoFailure != otherConfig.getLcpEchoFailure()) {
        	return false;
        }
		
		if (this.m_profileID != otherConfig.getProfileID()) {
		    return false;
		}
		
		if (this.m_pdpType != otherConfig.getPdpType()) {
			return false;
		}
		
		if (this.m_authType != otherConfig.getAuthType()) {
			return false;
		}
		
		if (this.m_dataCompression != otherConfig.getDataCompression()) {
			return false;
		}
		
		if (this.m_headerCompression != otherConfig.getHeaderCompression()) {
			return false;
		}
		
		if (this.m_gpsEnabled != otherConfig.isGpsEnabled()) {
			return false;
		}
		
		if (this.m_dialString != null) {
			if (!this.m_dialString.equals(otherConfig.getDialString())) {
				return false;
			}
		} else {
			if (otherConfig.getDialString() != null) {
				return false;
			}
		}
        if (this.m_apn != null) {
            if (!this.m_apn.equals(otherConfig.getApn())) {
                return false;
            }
        } else {
            if (otherConfig.getApn() != null) {
                return false;
            }
        }
        
        if(this.m_activeSimCardSlot != null) {
        	if (this.m_activeSimCardSlot != otherConfig.getActiveSimCardSlot()) {
        		return false;
        	}
        } else {
        	if (otherConfig.getActiveSimCardSlot() != null) {
        		return false;
        	}
        }

		if ((this.m_username != null) && (this.m_username.length() > 0)) {
			if (!this.m_username.equals(otherConfig.getUsername())) {
				return false;
			}
		} else {
			if ((otherConfig.getUsername() != null) && (otherConfig.getUsername().length() > 0)) {
				return false;
			}
		}
		
		if ((this.m_password != null) && (this.m_password.length() > 0)) {
			if (!this.m_password.equals(otherConfig.getPassword())) {
				return false;
			}
		} else {
			if ((otherConfig.getPassword() != null) && (otherConfig.getPassword().length() > 0)) {
				return false;
			}
		}
		
		if ((this.m_activeFilter != null) && (this.m_activeFilter.length() > 0)) {
			if (!this.m_activeFilter.equals(otherConfig.getActiveFilter())) {
				return false;
			}
		} else {
			if ((otherConfig.getActiveFilter() != null) && (otherConfig.getActiveFilter().length() > 0)) {
				return false;
			}
		}
		
		if (this.m_ipAddress != null) {
			if (!this.m_ipAddress.equals(otherConfig)) {
				return false;
			}
		} else {
			if (otherConfig.getIpAddress() != null) {
				return false;
			}
		}
		
		return true;
	}
	
   
	@Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        
        sb.append("ModemConfig - ");
        
        sb.append("Enabled: ").append(m_enabled);
        sb.append(" - PPP Number: ").append(m_pppNumber);
        sb.append(" - Dial String: ").append(m_dialString);
       // sb.append(" - Provider: ").append(m_provider);
        sb.append(" - Profile ID: ").append(m_profileID);
        sb.append(" - PDP Type: ").append(m_pdpType);
        sb.append(" - Auth Type: ").append(m_authType);
        sb.append(" - APN: ").append(m_apn);
        sb.append(" - Active SIM slot: ").append(m_activeSimCardSlot);
        sb.append(" - Username: ").append(m_username);
        sb.append(" - Password: ").append(m_password);
        sb.append(" - IP Address: ").append((m_ipAddress == null) ? "null" : m_ipAddress.getHostAddress());
        sb.append(" - Data Compression: ").append(m_dataCompression);
        sb.append(" - Header Compression: ").append(m_headerCompression);
        
        return sb.toString();
	}
	
	@Override
	public boolean isValid() {
	    if(m_pppNumber < 0) {
	        return false;
	    }
	    
		return true;
	}
}
