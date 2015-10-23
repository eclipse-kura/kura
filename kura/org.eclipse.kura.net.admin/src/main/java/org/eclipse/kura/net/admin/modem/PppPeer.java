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
package org.eclipse.kura.net.admin.modem;

import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.eclipse.kura.net.admin.visitor.linux.util.ChapLinux;
import org.eclipse.kura.net.admin.visitor.linux.util.PapLinux;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;

/* 
 * Copyright 2013 Eurotech Inc. All rights reserved.
 */

/**
 * Defines settings per service provider
 * 
 * @author ilya.binshtok
 *
 */
public class PppPeer {
	
    private static final String CHAT_PROGRAM = "chat";
    
	private boolean m_enabled = false;
	
	//private int persistence = 0; 
	
	/* for a ppp0 or ppp1 etc interface name */
	private int m_pppUnitNumber = 0;
	
	/* name of the provider */
	private String m_provider = null;
	
	/* access point name */
	private String m_apn = null;
	
	/* dial string */
	private String m_dialString = null;
	
	/* Enables connection debugging facilities */
	private boolean m_enableDebug = false;
	
	/* authentication type (e.g. PAP, CHAP) */
	private AuthType m_authType = AuthType.NONE;
	
	/* username */
	private String m_username = null;
	
	/* password */
	private String m_password = null;
	
	/* baud rate */
	private int m_baudRate = -1;
	
	/* PPP log file */
	private String m_logfile = null;
	
	/* Use RTS/CTS hardware flow control */
	private boolean m_useRtsCtsFlowControl = false;
	
	/* use modem control lines? */
	private boolean m_useModemControlLines = false;
	
	/* lock serial port? */
	private boolean m_lockSerialDevice = false;
	
	/* require the peer to authenticate itself */
	private boolean m_peerMustAuthenticateItself = false;
	
	/*
	 * Add a default route to the system routing tables, 
	 * using the peer as the gateway.
	 */
	private boolean m_addDefaultRoute = false;
	
	/*
	 * Disables the default behavior when no local IP address is specified, 
	 * which is to determine (if possible) the local IP address from the hostname. 
	 * With this option, the peer will have to supply the local IP address during 
	 * IPCP negotiation (unless it specified explicitly on the command line 
	 * or in an options file). 
	 */
	private boolean m_peerToSupplyLocalIP = false;
	
	/* Ask the peer for DNS server addresses */
	private boolean m_usePeerDns = false;
	
	/* Allow Proxy-ARPs */
	private boolean m_allowProxyArps = false;
	

	/*
	 * Allow Van Jacobson style TCP/IP header compression in both the 
	 * transmit and the receive direction. 
	 */
	private boolean m_allowVanJacobsonTcpIpHdrCompression = false;
	
	/*
	 * Allow the connection-ID compression option in Van Jacobson style TCP/IP header 
	 * compression. With this option, pppd will not omit the connection-ID byte from 
	 * Van Jacobson compressed TCP/IP headers, nor ask the peer to do so. 
	 */
	private boolean m_allowVanJacobsonConnectionIDCompression = false;
	
	/*
	 * Allow BSD-Compress compression
	 */
	private boolean m_allowBsdCompression = false;
	
	/*
	 * Allow Deflate compression
	 */
	private boolean m_allowDeflateCompression = false;
	
	/*
	 * Allow magic number negotiation
	 */
	private boolean m_allowMagic = false;
	
	private boolean m_persist = false;
	
	private int m_maxFail = 0;
	
	private int m_idle = 0;
	
	private String m_activeFilter = null;
		
	/*
	 * If this option is given, ppp daemon will presume the peer to be dead 
	 * if n LCP echo-requests are sent without receiving a valid LCP echo-reply. 
	 * If this happens, ppp daemon will terminate the connection. 
	 * Use of this option requires a non-zero value for the 
	 * lcp-echo-interval parameter. 
	 * This option can be used to enable ppp daemon to terminate after the 
	 * physical connection has been broken (e.g., the modem has hung up) 
	 * in situations where no hardware modem control lines are available. 
	 */
	private int m_lcp_echo_failure = -1;
	
	/*
	 * If this option is given, ppp daemon will send an LCP echo-request frame 
	 * to the peer every n seconds. Normally the peer should respond to 
	 * the echo-request by sending an echo-reply. 
	 * This option can be used with the lcp-echo-failure option to detect 
	 * that the peer is no longer connected. 
	 */
	private int m_lcp_echo_interval = -1;
	
	/*
	 * Wait for up to n milliseconds after the connect script finishes for a 
	 * valid PPP packet from the peer.
	 */
	private int m_connect_delay = -1;
		   
    /* Modem Exchange script */
    private String m_connectScriptFilename = null;
    private String m_disconnectScriptFilename = null;

    
	/**
	 * Reports PPP unit number
	 * 
	 * @return PPP unit number as <code>int</code>
	 */
	public int getPppUnitNumber() {
		return m_pppUnitNumber;
	}

	/**
	 * Sets PPP unit number
	 * 
	 * @param pppUnitNumber as <code>int</code>
	 */
	public void setPppUnitNumber(int pppUnitNumber) {
		m_pppUnitNumber = pppUnitNumber;
	}



	/**
	 * Reports if PPP is enabled in configuration
	 * 
	 * @return boolean <br>
	 * 		true - PPP is enabled
	 * 		false - PPP is disabled
	 */
	public boolean isEnabled() {
		return m_enabled;
	}

	/**
	 * Enables/disables PPP
	 * 
	 * @param enabled <br>
	 * 		true - PPP is enabled
	 * 		false - PPP is disabled

	 */
	public void setEnabled(boolean enabled) {
		m_enabled = enabled;
	}

	/**
	 * Reports service provider
	 * 
	 * @return - service provider
	 */
	public String getProvider() {
		return m_provider;
	}

	/**
	 * Sets service provider
	 * 
	 * @param provider as String
	 */
	public void setProvider(String provider) {
		m_provider = provider;
	}
	
	/**
	 * Reports APN (Access Point Name) of cellular data connection.
	 * 
	 * @return APN
	 */
	public String getApn() {
		return m_apn;
	}

	/**
	 * Sets APN (Access Point Name) for cellular data connection.
	 * 
	 * @param apn as String
	 */
	public void setApn(String apn) {
		m_apn = apn;
	}

	/**
	 * Reports dial string
	 * 
	 * @return dial string
	 */
	public String getDialString() {
		return m_dialString;
	}

	/**
	 * Sets dial string
	 * 
	 * @param dialString such as "atdt555-1212"
	 */
	public void setDialString(String dialString) {
		m_dialString = dialString;
	}

	/**
	 * Reports authentication type. (e.g. NONE, PAP, CHAP)
	 * 
	 * @return authentication type
	 */
	public AuthType getAuthType() {
		return m_authType;
	}

	/**
	 * Sets authentication type
	 * 
	 * @param authType int (NONE, PAP, CHAP) as defined in {@link org.eclipe.kura.net.ppp.service.PppAuthentication}
	 */
	public void setAuthType(AuthType authType) {
		m_authType = authType;
	}

	/**
	 * Reports user name for authentication
	 * 
	 * @return user name
	 */
	public String getUsername() {
		return m_username;
	}

	/**
	 * Sets user name used for authentication
	 * 
	 * @param username as String
	 */
	public void setUsername(String username) {
		m_username = username;
	}

	/**
	 * Reports password for authentication
	 * 
	 * @return password
	 */ 
	public String getPassword() {
		return m_password;
	}

	/**
	 * Sets password used for authentication
	 * 
	 * @param password as String
	 */
	public void setPassword(String password) {
		m_password = password;
	}

	/**
	 * Reports baud rate used for PPP
	 * 
	 * @return baud rate
	 */
	public int getBaudRate() {
		return m_baudRate;
	}

	/**
	 * Sets baud rate used for PPP
	 * 
	 * @param baudRate on serial port, as int
	 */
	public void setBaudRate(int baudRate) {
		m_baudRate = baudRate;
	}

    /**
     * Reports name of the log file
     * 
     * @return name of the log file
     */
    public String getLogfile() {
        return m_logfile;
    }

    /**
     * Sets the name of the log file
     * 
     * @param logfile as String
     */
    public void setLogfile(String logfile) {
        m_logfile = logfile;
    }

    /**
     * Reports connect script
     * 
     * @return connect script filename as String
     */
    public String getConnectScript() {
        return m_connectScriptFilename;
    }

    /**
     * Sets connect script filename
     * 
     * @param connectScript as <code>String</code>
     */
    public void setConnectScript(String connectScript) {
        m_connectScriptFilename = connectScript;
    }

    /**
     * Reports disconnect script
     * 
     * @return disconnect script filename as String
     */
    public String getDisconnectScript() {
        return m_disconnectScriptFilename;
    }

    /**
     * Sets disconnect script filename
     * 
     * @param disconnectScript as <code>String</code>
     */
    public void setDisconnectScript(String disconnectScript) {
        m_disconnectScriptFilename = disconnectScript;
    }

	/**
	 * Reports 'defaultroute' option
	 * 
	 * @return boolean <br>
	 * 		true - add default route to the system routing table <br>
	 * 		false - do not add default route
	 */
	public boolean isAddDefaultRoute() {
		return m_addDefaultRoute;
	}

	/**
	 * Sets 'defaultroute' option
	 * 
	 * @param addDefaultRoute <br>
	 * 		true - add default route to the system routing table <br>
	 * 		false - do not add default route
	 */
	public void setAddDefaultRoute(boolean addDefaultRoute) {
		m_addDefaultRoute = addDefaultRoute;
	}

	/**
	 * Reports if peer is expected to supply local IP address using IPCP negotiation.
	 * 
	 * @return boolean <br>
	 * 		true - peer is expected to supply local IP address 
	 * 		false - peer is not supplying local IP address
	 */
	public boolean isPeerToSupplyLocalIP() {
		return m_peerToSupplyLocalIP;
	}

	/**
	 * Sets whether peer is to supply local IP address or not
	 *  
	 * @param peerToSupplyLocalIP <br>
	 * 		true - peer is expected to supply local IP address 
	 * 		false - peer is not supplying local IP address
	 */
	public void setPeerToSupplyLocalIP(boolean peerToSupplyLocalIP) {
		m_peerToSupplyLocalIP = peerToSupplyLocalIP;
	}

	/**
	 * Reports if peer supplied DNS should be used
	 * 
	 * @return boolean <br>
	 * 		true - use peer DNS <br>
	 * 		false - do not use peer DNS
	 */
	public boolean isUsePeerDns() {
		return m_usePeerDns;
	}

	/**
	 * Sets whether to use peer supplied DNS.
	 * 
	 * @param usePeerDns <br>
	 * 		true - use peer DNS <br>
	 * 		false - do not use peer DNS
	 */
	public void setUsePeerDns(boolean usePeerDns) {
		m_usePeerDns = usePeerDns;
	}

	/**
	 * Reports if 'proxyarp' option is enabled
	 * 
	 * @return boolean <br>
	 * 		true - 'proxyarp' is enabled <br>
	 * 		false - 'proxyarp' is disabled
	 */
	public boolean isAllowProxyArps() {
		return m_allowProxyArps;
	}

	/**
	 * Enable/disable 'proxyarp' option
	 * 
	 * @param allowProxyArps <br>
	 * 		true - 'proxyarp' is enabled <br>
	 * 		false - 'proxyarp' is disabled
	 */
	public void setAllowProxyArps(boolean allowProxyArps) {
		m_allowProxyArps = allowProxyArps;
	}

	/**
	 * Reports if Van Jacobson TCP/IP header compression is allowed
	 * 
	 * @return boolean <br>
	 * 		true - Van Jacobson TCP/IP header compression is allowed <br>
	 * 		false - Van Jacobson TCP/IP header compression is not allowed
	 */
	public boolean isAllowVanJacobsonTcpIpHdrCompression() {
		return m_allowVanJacobsonTcpIpHdrCompression;
	}

	/**
	 * Enable/disable Van Jacobson TCP/IP header compression
	 * 
	 * @param allowVanJacobsonTcpIpHdrCompression <br>
	 * 		true - Van Jacobson TCP/IP header compression is allowed <br>
	 * 		false - Van Jacobson TCP/IP header compression is not allowed

	 */
	public void setAllowVanJacobsonTcpIpHdrCompression(
			boolean allowVanJacobsonTcpIpHdrCompression) {
		m_allowVanJacobsonTcpIpHdrCompression = allowVanJacobsonTcpIpHdrCompression;
	}

	
	/**
	 * Reports if Van Jacobson Connection ID compression is allowed
	 * 
	 * @return boolean <br>
	 * 		true - Van Jacobson Connection ID compression is allowed <br>
	 * 		false - Van Jacobson Connection ID compression is not allowed
	 */
	public boolean isAllowVanJacobsonConnectionIDCompression() {
		return m_allowVanJacobsonConnectionIDCompression;
	}

	/**
	 * Enable/disable Van Jacobson Connection ID compression
	 * 
	 * @param allowVanJacobsonConnectionIDCompression <br>
	 * 		true - Van Jacobson Connection ID compression is allowed <br>
	 * 		false - Van Jacobson Connection ID compression is not allowed
	 */
	public void setAllowVanJacobsonConnectionIDCompression(
			boolean allowVanJacobsonConnectionIDCompression) {
		m_allowVanJacobsonConnectionIDCompression = allowVanJacobsonConnectionIDCompression;
	}

	/**
	 * Reports if BSD compression is allowed
	 * 
	 * @return boolean <br> 
	 * 		true - BSD compression is allowed <br>
	 * 		false - BSD compression is not allowed
	 */
	public boolean isAllowBsdCompression() {
		return m_allowBsdCompression;
	}

	/**
	 * Enable/disable BSD compression
	 * 
	 * @param allowBsdCompression <br> 
	 * 		true - BSD compression is allowed <br>
	 * 		false - BSD compression is not allowed
	 */
	public void setAllowBsdCompression(boolean allowBsdCompression) {
		m_allowBsdCompression = allowBsdCompression;
	}

	/**
	 * Reports if 'Deflate' compression is allowed
	 * 
	 * @return boolean <br> 
	 * 		true - 'Deflate' compression is allowed <br>
	 * 		false - 'Deflate' compression is not allowed
	 */
	public boolean isAllowDeflateCompression() {
		return m_allowDeflateCompression;
	}

	/**
	 * Enable/disable 'Deflate' compression
	 * 
	 * @param allowDeflateCompression <br> 
	 * 		true - 'Deflate' compression is allowed <br>
	 * 		false - 'Deflate' compression is not allowed
	 */
	public void setAllowDeflateCompression(boolean allowDeflateCompression) {
		m_allowDeflateCompression = allowDeflateCompression;
	}

	/**
	 * Reports is magic number negotiation is enabled
	 * 
	 * @return boolean <br>
	 * 		true - magic number negotiation is enabled <br>
	 * 		false - magic number negotiation is disabled
	 */
	public boolean isAllowMagic() {
		return m_allowMagic;
	}

	/**
	 * Enable/disable magic number negotiation
	 * 
	 * @param allowMagic <br>
	 * 		true - magic number negotiation is enabled <br>
	 * 		false - magic number negotiation is disabled
	 */
	public void setAllowMagic(boolean allowMagic) {
		m_allowMagic = allowMagic;
	}
	
	public int getIdleTime() {
		return m_idle;
	}
	
	public void setIdleTime (int idle) {
		m_idle = idle;
	}
	
	public String getActiveFilter() {
		return m_activeFilter;
	}
	
	public void setActiveFilter(String activeFilter) {
		m_activeFilter = activeFilter;
	}
	
	public boolean isPersist() {
		return m_persist;
	}
	
	public void setPersist(boolean persist) {
		m_persist = persist;
	}
	
	public int getMaxFail() {
		return m_maxFail;
	}
	
	public void setMaxFail(int maxfail) {
		m_maxFail = maxfail;
	}
	
	/**
	 * Reports number 'lcp-echo-failure' parameter that is number of 
	 * unanswered LCP echo requests to presume the peer to be
	 * dead.  
	 * 
	 * @return LCP echo failure
	 */
	public int getLcp_echo_failure() {
		return m_lcp_echo_failure;
	}

	/**
	 * Sets 'lcp-echo-faillure' parameter
	 * 
	 * @param lcp_echo_failure number of unanswered LCP echo requests before peer is assumed dead
	 */
	public void setLcp_echo_failure(int lcp_echo_failure) {
		m_lcp_echo_failure = lcp_echo_failure;
	}

	/**
	 * Reports LCP echo interval in seconds
	 * 
	 * @return LCP echo interval
	 */
	public int getLcp_echo_interval() {
		return m_lcp_echo_interval;
	}

	/**
	 * Sets LCP echo interval
	 *  
	 * @param lcp_echo_interval in seconds
	 */
	public void setLcp_echo_interval(int lcp_echo_interval) {
		m_lcp_echo_interval = lcp_echo_interval;
	}

	/**
	 * Reports if RTS/CTS hardware flow control is to be used
	 * 
	 * @return boolean <br>
	 * 		true - use RTS/CTS flow control <br>
	 * 		false - do not use RTS/CTS flow control
	 */
	public boolean isUseRtsCtsFlowControl() {
		return m_useRtsCtsFlowControl;
	}

	/**
	 * Enable/disable RTS/CTS flow control
	 * 
	 * @param useRtsCtsFlowControl <br>
	 * 		true - use RTS/CTS flow control <br>
	 * 		false - do not use RTS/CTS flow control
	 */
	public void setUseRtsCtsFlowControl(boolean useRtsCtsFlowControl) {
		m_useRtsCtsFlowControl = useRtsCtsFlowControl;
	}

	/**
	 * Reports if PPP daemon is setup to create a lock file
	 *  
	 * @return boolean <br>
	 * 		true - create lock file <br>
	 * 		false - do not create lock file
	 */
	public boolean isLockSerialDevice() {
		return m_lockSerialDevice;
	}

	/**
	 * Enable/disable lock file
	 * 
	 * @param lockSerialDevice <br>
	 * 		true - create lock file <br>
	 * 		false - do not create lock file
	 */
	public void setLockSerialDevice(boolean lockSerialDevice) {
		m_lockSerialDevice = lockSerialDevice;
	}

	/**
	 * Reports if modem control lines are to be used. 
	 * 
	 * @return boolean <br>
	 * 		true - use modem control lines
	 * 		false - do not use modem control lines
	 */
	public boolean isUseModemControlLines() {
		return m_useModemControlLines;
	}

	/**
	 * Sets 'use modem control lines' flag
	 * 
	 * @param useModemControlLines <br>
	 * 		true - use modem control lines
	 * 		false - do not use modem control lines
	 */
	public void setUseModemControlLines(boolean useModemControlLines) {
		m_useModemControlLines = useModemControlLines;
	}

	/**
	 * Reports if peer is expected to authenticate itself
	 * 
	 * @return boolean <br>
	 * 		true - peer must authenticate itself
	 * 		false - peer is not required to authenticate itself
	 */
	public boolean isPeerMustAuthenticateItself() {
		return m_peerMustAuthenticateItself;
	}

	/**
	 * Sets 'peer must authenticate itself' flag
	 * 
	 * @param peerMustAuthenticateItself <br>
	 * 		true - peer must authenticate itself
	 * 		false - peer is not required to authenticate itself

	 */
	public void setPeerMustAuthenticateItself(boolean peerMustAuthenticateItself) {
		m_peerMustAuthenticateItself = peerMustAuthenticateItself;
	}

	/**
	 * Reports connect delay in milliseconds 
	 * 
	 * @return - connect delay
	 */
	public int getConnect_delay() {
		return m_connect_delay;
	}

	/**
	 * Sets connect delay 
	 * 
	 * @param connect_delay in milliseconds
	 */
	public void setConnect_delay(int connect_delay) {
		m_connect_delay = connect_delay;
	}

	/**
	 * Reports if connection debugging is enabled
	 * 
	 * @return boolean
	 * 		true - connection debugging is enabled <br>
	 * 		false - connection debugging is not enabled 
	 */
	public boolean isEnableDebug() {
		return m_enableDebug;
	}

	/**
	 * Sets 'enable connection debugging' flag
	 * 
	 * @param enableDebug - enable connection debugging
	 */
	public void setEnableDebug(boolean enableDebug) {
		m_enableDebug = enableDebug;
	}	
	
	/**
	 * Write the current parameters to the specified peer file
	 */
	public void write(String filename) throws Exception {

	    // open output stream
		FileOutputStream fos = new FileOutputStream(filename);
	    PrintWriter writer = new PrintWriter(fos);

        // set baud rate
        if (m_baudRate != -1) {
            writer.println(m_baudRate);
        }

        if (m_pppUnitNumber != -1) {
            writer.print("unit ");
            writer.println(m_pppUnitNumber);
        }

        // set logfile
        if (m_logfile != null) {
            writer.print("logfile ");
            writer.println(m_logfile);
        }

        if (m_enableDebug) {
            writer.println("debug");
        }

        if (m_connectScriptFilename != null) {
            StringBuffer connectLine = new StringBuffer("connect '");
            connectLine.append(CHAT_PROGRAM);
            connectLine.append(" -v -f ");
            connectLine.append(m_connectScriptFilename);
            connectLine.append("'");
            writer.println(connectLine.toString());
        }

        if (m_disconnectScriptFilename != null) {
            StringBuffer disconnectLine = new StringBuffer("disconnect '");
            disconnectLine.append(CHAT_PROGRAM);
            disconnectLine.append(" -v -f ");
            disconnectLine.append(m_disconnectScriptFilename);
            disconnectLine.append("'");
            writer.println(disconnectLine.toString());
        }

        if (m_useModemControlLines) {
            writer.println("modem");
        }

        if (m_useRtsCtsFlowControl) {
            writer.println("crtscts");
        }

        if (m_lockSerialDevice) {
            writer.println("lock");
        }

        if (!m_peerMustAuthenticateItself) {
            writer.println("noauth");
        }

        ChapLinux chapLinux = ChapLinux.getInstance();
        PapLinux papLinux = PapLinux.getInstance();
        
        if (this.getAuthType() != AuthType.NONE) {
            writer.print("user \"");
            writer.print(m_username);
            writer.println("\"");
            
            writer.println("hide-password");
            
            switch (this.getAuthType()) {
            case AUTO:
				if (!papLinux.checkForEntry(m_provider, m_username, "*", m_password, "*")) {
					papLinux.addEntry(m_provider, m_username, "*", m_password, "*");
				}
				if (!chapLinux.checkForEntry(m_provider, m_username, "*", m_password, "*")) {
					chapLinux.addEntry(m_provider, m_username, "*", m_password, "*");
				}
            	break;
            case PAP:
				// remove CHAP entry if exists
				chapLinux.removeEntry("provider", m_provider);
                if (!papLinux.checkForEntry(m_provider, m_username, "*", m_password, "*")) {
                    papLinux.addEntry(m_provider, m_username, "*", m_password, "*");
                }
                break;
            case CHAP:
            	// remove PAP entry if exists
				papLinux.removeEntry("provider", m_provider);
                if (!chapLinux.checkForEntry(m_provider, m_username, "*", m_password, "*")) {
                    chapLinux.addEntry(m_provider, m_username, "*", m_password, "*");
                }
                break;
            case NONE:
				break;
            }
        } else {
        	// remove CHAP/PAP entries if exist
			chapLinux.removeEntry("provider", m_provider);
			papLinux.removeEntry("provider", m_provider);
        }

        if (m_peerToSupplyLocalIP) {
            writer.println("noipdefault");
        }

        if (m_addDefaultRoute) {
            writer.println("defaultroute");
        } else {
            writer.println("nodefaultroute");
        }

        if (m_usePeerDns) {
            writer.println("usepeerdns");
        }

        if (!m_allowProxyArps) {
            writer.println("noproxyarp");
        }

        if (!m_allowVanJacobsonTcpIpHdrCompression) {
            writer.println("novj");
        }

        if (!m_allowVanJacobsonConnectionIDCompression) {
            writer.println("novjccomp");
        }

        if (!m_allowBsdCompression) {
            writer.println("nobsdcomp");
        }

        if (!m_allowDeflateCompression) {
            writer.println("nodeflate");
        }

        if (!m_allowMagic) {
            writer.println("nomagic");
        }
        
        if (m_idle > 0) {
        	writer.println("idle " + m_idle);
        }
        
        if ((m_idle > 0) && (m_activeFilter != null) && (m_activeFilter.length() > 0)) {
        	writer.println("active-filter '" + m_activeFilter + "'");
        }
        
        if (m_persist) {
        	 writer.println("persist");
        	 writer.println("holdoff 1");
        } else {
        	 writer.println("nopersist");
        }
        
        writer.println("maxfail " + m_maxFail);

        if (m_connect_delay != -1) {
            writer.println("connect-delay " + m_connect_delay);
        }

        if (m_lcp_echo_failure > 0) {
            writer.println("lcp-echo-failure " + m_lcp_echo_failure);
        }

        if (m_lcp_echo_interval > 0) {
            writer.println("lcp-echo-interval " + m_lcp_echo_interval);
        }
	    
		writer.flush();
		fos.getFD().sync();
		writer.close();
		fos.close();
	}
}
