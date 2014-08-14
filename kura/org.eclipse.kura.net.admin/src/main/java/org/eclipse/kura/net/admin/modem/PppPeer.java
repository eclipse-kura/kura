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
import org.eclipse.kura.net.modem.ModemTechnologyType;
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
    
	private boolean enabled = false;
	
	//private int persistence = 0; 
	
	/* for a ppp0 or ppp1 etc interface name */
	private int pppUnitNumber = 0;
	
	/* name of the provider */
	private String provider = null;
	
	/* network technology (e.g. EVDO, 3G) */
	private ModemTechnologyType networkTechnology = null;
	
	/* access point name */
	private String apn = null;
	
	/* dial string */
	private String dialString = null;
	
	/* Enables connection debugging facilities */
	private boolean enableDebug = false;
	
	/* authentication type (e.g. PAP, CHAP) */
	private AuthType authType = AuthType.NONE;
	
	/* username */
	private String username = null;
	
	/* password */
	private String password = null;
	
	/* baud rate */
	private int baudRate = -1;
	
	/* PPP log file */
	private String logfile = null;
	
	/* Use RTS/CTS hardware flow control */
	private boolean useRtsCtsFlowControl = false;
	
	/* use modem control lines? */
	private boolean useModemControlLines = false;
	
	/* lock serial port? */
	private boolean lockSerialDevice = false;
	
	/* require the peer to authenticate itself */
	private boolean peerMustAuthenticateItself = false;
	
	/*
	 * Add a default route to the system routing tables, 
	 * using the peer as the gateway.
	 */
	private boolean addDefaultRoute = false;
	
	/*
	 * Disables the default behavior when no local IP address is specified, 
	 * which is to determine (if possible) the local IP address from the hostname. 
	 * With this option, the peer will have to supply the local IP address during 
	 * IPCP negotiation (unless it specified explicitly on the command line 
	 * or in an options file). 
	 */
	private boolean peerToSupplyLocalIP = false;
	
	/* Ask the peer for DNS server addresses */
	private boolean usePeerDns = false;
	
	/* Allow Proxy-ARPs */
	private boolean allowProxyArps = false;
	

	/*
	 * Allow Van Jacobson style TCP/IP header compression in both the 
	 * transmit and the receive direction. 
	 */
	private boolean allowVanJacobsonTcpIpHdrCompression = false;
	
	/*
	 * Allow the connection-ID compression option in Van Jacobson style TCP/IP header 
	 * compression. With this option, pppd will not omit the connection-ID byte from 
	 * Van Jacobson compressed TCP/IP headers, nor ask the peer to do so. 
	 */
	private boolean allowVanJacobsonConnectionIDCompression = false;
	
	/*
	 * Allow BSD-Compress compression
	 */
	private boolean allowBsdCompression = false;
	
	/*
	 * Allow Deflate compression
	 */
	private boolean allowDeflateCompression = false;
	
	/*
	 * Allow magic number negotiation
	 */
	private boolean allowMagic = false;
	
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
	private int lcp_echo_failure = -1;
	
	/*
	 * If this option is given, ppp daemon will send an LCP echo-request frame 
	 * to the peer every n seconds. Normally the peer should respond to 
	 * the echo-request by sending an echo-reply. 
	 * This option can be used with the lcp-echo-failure option to detect 
	 * that the peer is no longer connected. 
	 */
	private int lcp_echo_interval = -1;
	
	/*
	 * Wait for up to n milliseconds after the connect script finishes for a 
	 * valid PPP packet from the peer.
	 */
	private int connect_delay = -1;
		   
    /* Modem Exchange script */
    private String connectScriptFilename = null;
    private String disconnectScriptFilename = null;

    
	/**
	 * Reports PPP unit number
	 * 
	 * @return PPP unit number as <code>int</code>
	 */
	public int getPppUnitNumber() {
		return pppUnitNumber;
	}

	/**
	 * Sets PPP unit number
	 * 
	 * @param pppUnitNumber as <code>int</code>
	 */
	public void setPppUnitNumber(int pppUnitNumber) {
		this.pppUnitNumber = pppUnitNumber;
	}



	/**
	 * Reports if PPP is enabled in configuration
	 * 
	 * @return boolean <br>
	 * 		true - PPP is enabled
	 * 		false - PPP is disabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables/disables PPP
	 * 
	 * @param enabled <br>
	 * 		true - PPP is enabled
	 * 		false - PPP is disabled

	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Reports service provider
	 * 
	 * @return - service provider
	 */
	public String getProvider() {
		return provider;
	}

	/**
	 * Sets service provider
	 * 
	 * @param provider as String
	 */
	public void setProvider(String provider) {
		this.provider = provider;
	}
	
	/**
	 * Reports network technology. (e.g. evdo, 3G)
	 * 
	 * @return - network technology
	 */
	public ModemTechnologyType getNetworkTechnology() {
		return networkTechnology;
	}

	/**
	 * Sets network technology
	 * 
	 * @param networkTechnology as ModemServiceType 
	 */
	public void setNetworkTechnology(ModemTechnologyType networkTechnology) {
		this.networkTechnology = networkTechnology;
	}
	
	/**
	 * Reports APN (Access Point Name) of cellular data connection.
	 * 
	 * @return APN
	 */
	public String getApn() {
		return apn;
	}

	/**
	 * Sets APN (Access Point Name) for cellular data connection.
	 * 
	 * @param apn as String
	 */
	public void setApn(String apn) {
		this.apn = apn;
	}

	/**
	 * Reports dial string
	 * 
	 * @return dial string
	 */
	public String getDialString() {
		return dialString;
	}

	/**
	 * Sets dial string
	 * 
	 * @param dialString such as "atdt555-1212"
	 */
	public void setDialString(String dialString) {
		this.dialString = dialString;
	}

	/**
	 * Reports authentication type. (e.g. NONE, PAP, CHAP)
	 * 
	 * @return authentication type
	 */
	public AuthType getAuthType() {
		return authType;
	}

	/**
	 * Sets authentication type
	 * 
	 * @param authType int (NONE, PAP, CHAP) as defined in {@link org.eclipe.kura.net.ppp.service.PppAuthentication}
	 */
	public void setAuthType(AuthType authType) {
		this.authType = authType;
	}

	/**
	 * Reports user name for authentication
	 * 
	 * @return user name
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets user name used for authentication
	 * 
	 * @param username as String
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Reports password for authentication
	 * 
	 * @return password
	 */ 
	public String getPassword() {
		return password;
	}

	/**
	 * Sets password used for authentication
	 * 
	 * @param password as String
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Reports baud rate used for PPP
	 * 
	 * @return baud rate
	 */
	public int getBaudRate() {
		return baudRate;
	}

	/**
	 * Sets baud rate used for PPP
	 * 
	 * @param baudRate on serial port, as int
	 */
	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

    /**
     * Reports name of the log file
     * 
     * @return name of the log file
     */
    public String getLogfile() {
        return logfile;
    }

    /**
     * Sets the name of the log file
     * 
     * @param logfile as String
     */
    public void setLogfile(String logfile) {
        this.logfile = logfile;
    }

    /**
     * Reports connect script
     * 
     * @return connect script filename as String
     */
    public String getConnectScript() {
        return connectScriptFilename;
    }

    /**
     * Sets connect script filename
     * 
     * @param connectScript as <code>String</code>
     */
    public void setConnectScript(String connectScript) {
        this.connectScriptFilename = connectScript;
    }

    /**
     * Reports disconnect script
     * 
     * @return disconnect script filename as String
     */
    public String getDisconnectScript() {
        return disconnectScriptFilename;
    }

    /**
     * Sets disconnect script filename
     * 
     * @param disconnectScript as <code>String</code>
     */
    public void setDisconnectScript(String disconnectScript) {
        this.disconnectScriptFilename = disconnectScript;
    }

	/**
	 * Reports 'defaultroute' option
	 * 
	 * @return boolean <br>
	 * 		true - add default route to the system routing table <br>
	 * 		false - do not add default route
	 */
	public boolean isAddDefaultRoute() {
		return addDefaultRoute;
	}

	/**
	 * Sets 'defaultroute' option
	 * 
	 * @param addDefaultRoute <br>
	 * 		true - add default route to the system routing table <br>
	 * 		false - do not add default route
	 */
	public void setAddDefaultRoute(boolean addDefaultRoute) {
		this.addDefaultRoute = addDefaultRoute;
	}

	/**
	 * Reports if peer is expected to supply local IP address using IPCP negotiation.
	 * 
	 * @return boolean <br>
	 * 		true - peer is expected to supply local IP address 
	 * 		false - peer is not supplying local IP address
	 */
	public boolean isPeerToSupplyLocalIP() {
		return peerToSupplyLocalIP;
	}

	/**
	 * Sets whether peer is to supply local IP address or not
	 *  
	 * @param peerToSupplyLocalIP <br>
	 * 		true - peer is expected to supply local IP address 
	 * 		false - peer is not supplying local IP address
	 */
	public void setPeerToSupplyLocalIP(boolean peerToSupplyLocalIP) {
		this.peerToSupplyLocalIP = peerToSupplyLocalIP;
	}

	/**
	 * Reports if peer supplied DNS should be used
	 * 
	 * @return boolean <br>
	 * 		true - use peer DNS <br>
	 * 		false - do not use peer DNS
	 */
	public boolean isUsePeerDns() {
		return usePeerDns;
	}

	/**
	 * Sets whether to use peer supplied DNS.
	 * 
	 * @param usePeerDns <br>
	 * 		true - use peer DNS <br>
	 * 		false - do not use peer DNS
	 */
	public void setUsePeerDns(boolean usePeerDns) {
		this.usePeerDns = usePeerDns;
	}

	/**
	 * Reports if 'proxyarp' option is enabled
	 * 
	 * @return boolean <br>
	 * 		true - 'proxyarp' is enabled <br>
	 * 		false - 'proxyarp' is disabled
	 */
	public boolean isAllowProxyArps() {
		return allowProxyArps;
	}

	/**
	 * Enable/disable 'proxyarp' option
	 * 
	 * @param allowProxyArps <br>
	 * 		true - 'proxyarp' is enabled <br>
	 * 		false - 'proxyarp' is disabled
	 */
	public void setAllowProxyArps(boolean allowProxyArps) {
		this.allowProxyArps = allowProxyArps;
	}

	/**
	 * Reports if Van Jacobson TCP/IP header compression is allowed
	 * 
	 * @return boolean <br>
	 * 		true - Van Jacobson TCP/IP header compression is allowed <br>
	 * 		false - Van Jacobson TCP/IP header compression is not allowed
	 */
	public boolean isAllowVanJacobsonTcpIpHdrCompression() {
		return allowVanJacobsonTcpIpHdrCompression;
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
		this.allowVanJacobsonTcpIpHdrCompression = allowVanJacobsonTcpIpHdrCompression;
	}

	
	/**
	 * Reports if Van Jacobson Connection ID compression is allowed
	 * 
	 * @return boolean <br>
	 * 		true - Van Jacobson Connection ID compression is allowed <br>
	 * 		false - Van Jacobson Connection ID compression is not allowed
	 */
	public boolean isAllowVanJacobsonConnectionIDCompression() {
		return allowVanJacobsonConnectionIDCompression;
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
		this.allowVanJacobsonConnectionIDCompression = allowVanJacobsonConnectionIDCompression;
	}

	/**
	 * Reports if BSD compression is allowed
	 * 
	 * @return boolean <br> 
	 * 		true - BSD compression is allowed <br>
	 * 		false - BSD compression is not allowed
	 */
	public boolean isAllowBsdCompression() {
		return allowBsdCompression;
	}

	/**
	 * Enable/disable BSD compression
	 * 
	 * @param allowBsdCompression <br> 
	 * 		true - BSD compression is allowed <br>
	 * 		false - BSD compression is not allowed
	 */
	public void setAllowBsdCompression(boolean allowBsdCompression) {
		this.allowBsdCompression = allowBsdCompression;
	}

	/**
	 * Reports if 'Deflate' compression is allowed
	 * 
	 * @return boolean <br> 
	 * 		true - 'Deflate' compression is allowed <br>
	 * 		false - 'Deflate' compression is not allowed
	 */
	public boolean isAllowDeflateCompression() {
		return allowDeflateCompression;
	}

	/**
	 * Enable/disable 'Deflate' compression
	 * 
	 * @param allowDeflateCompression <br> 
	 * 		true - 'Deflate' compression is allowed <br>
	 * 		false - 'Deflate' compression is not allowed
	 */
	public void setAllowDeflateCompression(boolean allowDeflateCompression) {
		this.allowDeflateCompression = allowDeflateCompression;
	}

	/**
	 * Reports is magic number negotiation is enabled
	 * 
	 * @return boolean <br>
	 * 		true - magic number negotiation is enabled <br>
	 * 		false - magic number negotiation is disabled
	 */
	public boolean isAllowMagic() {
		return allowMagic;
	}

	/**
	 * Enable/disable magic number negotiation
	 * 
	 * @param allowMagic <br>
	 * 		true - magic number negotiation is enabled <br>
	 * 		false - magic number negotiation is disabled
	 */
	public void setAllowMagic(boolean allowMagic) {
		this.allowMagic = allowMagic;
	}

	/**
	 * Reports number 'lcp-echo-failure' parameter that is number of 
	 * unanswered LCP echo requests to presume the peer to be
	 * dead.  
	 * 
	 * @return LCP echo failure
	 */
	public int getLcp_echo_failure() {
		return lcp_echo_failure;
	}

	/**
	 * Sets 'lcp-echo-faillure' parameter
	 * 
	 * @param lcp_echo_failure number of unanswered LCP echo requests before peer is assumed dead
	 */
	public void setLcp_echo_failure(int lcp_echo_failure) {
		this.lcp_echo_failure = lcp_echo_failure;
	}

	/**
	 * Reports LCP echo interval in seconds
	 * 
	 * @return LCP echo interval
	 */
	public int getLcp_echo_interval() {
		return lcp_echo_interval;
	}

	/**
	 * Sets LCP echo interval
	 *  
	 * @param lcp_echo_interval in seconds
	 */
	public void setLcp_echo_interval(int lcp_echo_interval) {
		this.lcp_echo_interval = lcp_echo_interval;
	}

	/**
	 * Reports if RTS/CTS hardware flow control is to be used
	 * 
	 * @return boolean <br>
	 * 		true - use RTS/CTS flow control <br>
	 * 		false - do not use RTS/CTS flow control
	 */
	public boolean isUseRtsCtsFlowControl() {
		return useRtsCtsFlowControl;
	}

	/**
	 * Enable/disable RTS/CTS flow control
	 * 
	 * @param useRtsCtsFlowControl <br>
	 * 		true - use RTS/CTS flow control <br>
	 * 		false - do not use RTS/CTS flow control
	 */
	public void setUseRtsCtsFlowControl(boolean useRtsCtsFlowControl) {
		this.useRtsCtsFlowControl = useRtsCtsFlowControl;
	}

	/**
	 * Reports if PPP daemon is setup to create a lock file
	 *  
	 * @return boolean <br>
	 * 		true - create lock file <br>
	 * 		false - do not create lock file
	 */
	public boolean isLockSerialDevice() {
		return lockSerialDevice;
	}

	/**
	 * Enable/disable lock file
	 * 
	 * @param lockSerialDevice <br>
	 * 		true - create lock file <br>
	 * 		false - do not create lock file
	 */
	public void setLockSerialDevice(boolean lockSerialDevice) {
		this.lockSerialDevice = lockSerialDevice;
	}

	/**
	 * Reports if modem control lines are to be used. 
	 * 
	 * @return boolean <br>
	 * 		true - use modem control lines
	 * 		false - do not use modem control lines
	 */
	public boolean isUseModemControlLines() {
		return useModemControlLines;
	}

	/**
	 * Sets 'use modem control lines' flag
	 * 
	 * @param useModemControlLines <br>
	 * 		true - use modem control lines
	 * 		false - do not use modem control lines
	 */
	public void setUseModemControlLines(boolean useModemControlLines) {
		this.useModemControlLines = useModemControlLines;
	}

	/**
	 * Reports if peer is expected to authenticate itself
	 * 
	 * @return boolean <br>
	 * 		true - peer must authenticate itself
	 * 		false - peer is not required to authenticate itself
	 */
	public boolean isPeerMustAuthenticateItself() {
		return peerMustAuthenticateItself;
	}

	/**
	 * Sets 'peer must authenticate itself' flag
	 * 
	 * @param peerMustAuthenticateItself <br>
	 * 		true - peer must authenticate itself
	 * 		false - peer is not required to authenticate itself

	 */
	public void setPeerMustAuthenticateItself(boolean peerMustAuthenticateItself) {
		this.peerMustAuthenticateItself = peerMustAuthenticateItself;
	}

	/**
	 * Reports connect delay in milliseconds 
	 * 
	 * @return - connect delay
	 */
	public int getConnect_delay() {
		return connect_delay;
	}

	/**
	 * Sets connect delay 
	 * 
	 * @param connect_delay in milliseconds
	 */
	public void setConnect_delay(int connect_delay) {
		this.connect_delay = connect_delay;
	}

	/**
	 * Reports if connection debugging is enabled
	 * 
	 * @return boolean
	 * 		true - connection debugging is enabled <br>
	 * 		false - connection debugging is not enabled 
	 */
	public boolean isEnableDebug() {
		return enableDebug;
	}

	/**
	 * Sets 'enable connection debugging' flag
	 * 
	 * @param enableDebug - enable connection debugging
	 */
	public void setEnableDebug(boolean enableDebug) {
		this.enableDebug = enableDebug;
	}	
	
	/**
	 * Write the current parameters to the specified peer file
	 */
	public void write(String filename) throws Exception {

	    // open output stream
		FileOutputStream fos = new FileOutputStream(filename);
	    PrintWriter writer = new PrintWriter(fos);

        // set baud rate
        if (this.getBaudRate() != -1) {
            writer.println(this.getBaudRate());
        }

        if (this.getPppUnitNumber() != -1) {
            writer.print("unit ");
            writer.println(this.getPppUnitNumber());
        }

        // set logfile
        if (this.getLogfile() != null) {
            writer.print("logfile ");
            writer.println(this.getLogfile());
        }

        if (this.isEnableDebug()) {
            writer.println("debug");
        }

        if (this.getConnectScript() != null) {
            StringBuffer connectLine = new StringBuffer("connect '");
            connectLine.append(CHAT_PROGRAM);
            connectLine.append(" -v -f ");
            connectLine.append(this.getConnectScript());
            connectLine.append("'");
            writer.println(connectLine.toString());
        }

        if (this.getDisconnectScript() != null) {
            StringBuffer disconnectLine = new StringBuffer("disconnect '");
            disconnectLine.append(CHAT_PROGRAM);
            disconnectLine.append(" -v -f ");
            disconnectLine.append(this.getDisconnectScript());
            disconnectLine.append("'");
            writer.println(disconnectLine.toString());
        }

        if (this.isUseModemControlLines()) {
            writer.println("modem");
        }

        if (this.isUseRtsCtsFlowControl()) {
            writer.println("crtscts");
        }

        if (this.isLockSerialDevice()) {
            writer.println("lock");
        }

        if (!this.isPeerMustAuthenticateItself()) {
            writer.println("noauth");
        }

        ChapLinux chapLinux = ChapLinux.getInstance();
        PapLinux papLinux = PapLinux.getInstance();
        
        if (this.getAuthType() != AuthType.NONE) {
            writer.print("user \"");
            writer.print(this.getUsername());
            writer.println("\"");
            
            writer.println("hide-password");
            
            switch (this.getAuthType()) {
            case AUTO:
				if (!papLinux.checkForEntry(this.getProvider(),
						this.getUsername(), "*", this.getPassword(), "*")) {
					papLinux.addEntry(this.getProvider(), this.getUsername(),
							"*", this.getPassword(), "*");
				}
				if (!chapLinux.checkForEntry(this.getProvider(),
						this.getUsername(), "*", this.getPassword(), "*")) {
					chapLinux.addEntry(this.getProvider(), this.getUsername(),
							"*", this.getPassword(), "*");
				}
            	break;
            case PAP:
				// remove CHAP entry if exists
				chapLinux.removeEntry("provider", this.getProvider());
				            	
                if (!papLinux.checkForEntry(this.getProvider(), this.getUsername(),
                        "*", this.getPassword(), "*")) {
                    papLinux.addEntry(this.getProvider(), this.getUsername(), "*",
                            this.getPassword(), "*");
                }
                break;
            case CHAP:
            	// remove PAP entry if exists
				papLinux.removeEntry("provider", this.getProvider());
				
                if (!chapLinux.checkForEntry(this.getProvider(), this.getUsername(),
                        "*", this.getPassword(), "*")) {
                    chapLinux.addEntry(this.getProvider(), this.getUsername(), "*",
                            this.getPassword(), "*");
                }
                break;
            case NONE:
				break;
            }
        } else {
        	// remove CHAP/PAP entries if exist
			chapLinux.removeEntry("provider", this.getProvider());
			papLinux.removeEntry("provider", this.getProvider());
        }

        if (this.isPeerToSupplyLocalIP()) {
            writer.println("noipdefault");
        }

        if (this.isAddDefaultRoute()) {
            writer.println("defaultroute");
        } else {
            writer.println("nodefaultroute");
        }

        if (this.isUsePeerDns()) {
            writer.println("usepeerdns");
        }

        if (!this.isAllowProxyArps()) {
            writer.println("noproxyarp");
        }

        if (!this.isAllowVanJacobsonTcpIpHdrCompression()) {
            writer.println("novj");
        }

        if (!this.isAllowVanJacobsonConnectionIDCompression()) {
            writer.println("novjccomp");
        }

        if (!this.isAllowBsdCompression()) {
            writer.println("nobsdcomp");
        }

        if (!this.isAllowDeflateCompression()) {
            writer.println("nodeflate");
        }

        if (!this.isAllowMagic()) {
            writer.println("nomagic");
        }

        if (this.getConnect_delay() != -1) {
            writer.println("connect-delay " + this.getConnect_delay());
        }

        if (this.getLcp_echo_failure() > 0) {
            writer.println("lcp-echo-failure " + this.getLcp_echo_failure());
        }

        if (this.getLcp_echo_interval() > 0) {
            writer.println("lcp-echo-interval " + this.getLcp_echo_interval());
        }
	    
		writer.flush();
		fos.getFD().sync();
		writer.close();
		fos.close();
	}
}
