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
package org.eclipse.kura.net.admin.modem;

import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.eclipse.kura.net.admin.visitor.linux.util.ChapLinux;
import org.eclipse.kura.net.admin.visitor.linux.util.PapLinux;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;

/**
 * Defines settings per service provider
 *
 * @author ilya.binshtok
 *
 */
public class PppPeer {

    private static final String CHAT_PROGRAM = "chat";

    private boolean m_enabled = false;

    // private int persistence = 0;

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
        return this.m_pppUnitNumber;
    }

    /**
     * Sets PPP unit number
     *
     * @param pppUnitNumber
     *            as <code>int</code>
     */
    public void setPppUnitNumber(int pppUnitNumber) {
        this.m_pppUnitNumber = pppUnitNumber;
    }

    /**
     * Reports if PPP is enabled in configuration
     *
     * @return boolean <br>
     *         true - PPP is enabled
     *         false - PPP is disabled
     */
    public boolean isEnabled() {
        return this.m_enabled;
    }

    /**
     * Enables/disables PPP
     *
     * @param enabled
     *            <br>
     *            true - PPP is enabled
     *            false - PPP is disabled
     *
     */
    public void setEnabled(boolean enabled) {
        this.m_enabled = enabled;
    }

    /**
     * Reports service provider
     *
     * @return - service provider
     */
    public String getProvider() {
        return this.m_provider;
    }

    /**
     * Sets service provider
     *
     * @param provider
     *            as String
     */
    public void setProvider(String provider) {
        this.m_provider = provider;
    }

    /**
     * Reports APN (Access Point Name) of cellular data connection.
     *
     * @return APN
     */
    public String getApn() {
        return this.m_apn;
    }

    /**
     * Sets APN (Access Point Name) for cellular data connection.
     *
     * @param apn
     *            as String
     */
    public void setApn(String apn) {
        this.m_apn = apn;
    }

    /**
     * Reports dial string
     *
     * @return dial string
     */
    public String getDialString() {
        return this.m_dialString;
    }

    /**
     * Sets dial string
     *
     * @param dialString
     *            such as "atdt555-1212"
     */
    public void setDialString(String dialString) {
        this.m_dialString = dialString;
    }

    /**
     * Reports authentication type. (e.g. NONE, PAP, CHAP)
     *
     * @return authentication type
     */
    public AuthType getAuthType() {
        return this.m_authType;
    }

    /**
     * Sets authentication type
     *
     * @param authType
     *            int (NONE, PAP, CHAP) as defined in {@link org.eclipe.kura.net.ppp.service.PppAuthentication}
     */
    public void setAuthType(AuthType authType) {
        this.m_authType = authType;
    }

    /**
     * Reports user name for authentication
     *
     * @return user name
     */
    public String getUsername() {
        return this.m_username;
    }

    /**
     * Sets user name used for authentication
     *
     * @param username
     *            as String
     */
    public void setUsername(String username) {
        this.m_username = username;
    }

    /**
     * Reports password for authentication
     *
     * @return password
     */
    public String getPassword() {
        return this.m_password;
    }

    /**
     * Sets password used for authentication
     *
     * @param password
     *            as String
     */
    public void setPassword(String password) {
        this.m_password = password;
    }

    /**
     * Reports baud rate used for PPP
     *
     * @return baud rate
     */
    public int getBaudRate() {
        return this.m_baudRate;
    }

    /**
     * Sets baud rate used for PPP
     *
     * @param baudRate
     *            on serial port, as int
     */
    public void setBaudRate(int baudRate) {
        this.m_baudRate = baudRate;
    }

    /**
     * Reports name of the log file
     *
     * @return name of the log file
     */
    public String getLogfile() {
        return this.m_logfile;
    }

    /**
     * Sets the name of the log file
     *
     * @param logfile
     *            as String
     */
    public void setLogfile(String logfile) {
        this.m_logfile = logfile;
    }

    /**
     * Reports connect script
     *
     * @return connect script filename as String
     */
    public String getConnectScript() {
        return this.m_connectScriptFilename;
    }

    /**
     * Sets connect script filename
     *
     * @param connectScript
     *            as <code>String</code>
     */
    public void setConnectScript(String connectScript) {
        this.m_connectScriptFilename = connectScript;
    }

    /**
     * Reports disconnect script
     *
     * @return disconnect script filename as String
     */
    public String getDisconnectScript() {
        return this.m_disconnectScriptFilename;
    }

    /**
     * Sets disconnect script filename
     *
     * @param disconnectScript
     *            as <code>String</code>
     */
    public void setDisconnectScript(String disconnectScript) {
        this.m_disconnectScriptFilename = disconnectScript;
    }

    /**
     * Reports 'defaultroute' option
     *
     * @return boolean <br>
     *         true - add default route to the system routing table <br>
     *         false - do not add default route
     */
    public boolean isAddDefaultRoute() {
        return this.m_addDefaultRoute;
    }

    /**
     * Sets 'defaultroute' option
     *
     * @param addDefaultRoute
     *            <br>
     *            true - add default route to the system routing table <br>
     *            false - do not add default route
     */
    public void setAddDefaultRoute(boolean addDefaultRoute) {
        this.m_addDefaultRoute = addDefaultRoute;
    }

    /**
     * Reports if peer is expected to supply local IP address using IPCP negotiation.
     *
     * @return boolean <br>
     *         true - peer is expected to supply local IP address
     *         false - peer is not supplying local IP address
     */
    public boolean isPeerToSupplyLocalIP() {
        return this.m_peerToSupplyLocalIP;
    }

    /**
     * Sets whether peer is to supply local IP address or not
     *
     * @param peerToSupplyLocalIP
     *            <br>
     *            true - peer is expected to supply local IP address
     *            false - peer is not supplying local IP address
     */
    public void setPeerToSupplyLocalIP(boolean peerToSupplyLocalIP) {
        this.m_peerToSupplyLocalIP = peerToSupplyLocalIP;
    }

    /**
     * Reports if peer supplied DNS should be used
     *
     * @return boolean <br>
     *         true - use peer DNS <br>
     *         false - do not use peer DNS
     */
    public boolean isUsePeerDns() {
        return this.m_usePeerDns;
    }

    /**
     * Sets whether to use peer supplied DNS.
     *
     * @param usePeerDns
     *            <br>
     *            true - use peer DNS <br>
     *            false - do not use peer DNS
     */
    public void setUsePeerDns(boolean usePeerDns) {
        this.m_usePeerDns = usePeerDns;
    }

    /**
     * Reports if 'proxyarp' option is enabled
     *
     * @return boolean <br>
     *         true - 'proxyarp' is enabled <br>
     *         false - 'proxyarp' is disabled
     */
    public boolean isAllowProxyArps() {
        return this.m_allowProxyArps;
    }

    /**
     * Enable/disable 'proxyarp' option
     *
     * @param allowProxyArps
     *            <br>
     *            true - 'proxyarp' is enabled <br>
     *            false - 'proxyarp' is disabled
     */
    public void setAllowProxyArps(boolean allowProxyArps) {
        this.m_allowProxyArps = allowProxyArps;
    }

    /**
     * Reports if Van Jacobson TCP/IP header compression is allowed
     *
     * @return boolean <br>
     *         true - Van Jacobson TCP/IP header compression is allowed <br>
     *         false - Van Jacobson TCP/IP header compression is not allowed
     */
    public boolean isAllowVanJacobsonTcpIpHdrCompression() {
        return this.m_allowVanJacobsonTcpIpHdrCompression;
    }

    /**
     * Enable/disable Van Jacobson TCP/IP header compression
     *
     * @param allowVanJacobsonTcpIpHdrCompression
     *            <br>
     *            true - Van Jacobson TCP/IP header compression is allowed <br>
     *            false - Van Jacobson TCP/IP header compression is not allowed
     *
     */
    public void setAllowVanJacobsonTcpIpHdrCompression(boolean allowVanJacobsonTcpIpHdrCompression) {
        this.m_allowVanJacobsonTcpIpHdrCompression = allowVanJacobsonTcpIpHdrCompression;
    }

    /**
     * Reports if Van Jacobson Connection ID compression is allowed
     *
     * @return boolean <br>
     *         true - Van Jacobson Connection ID compression is allowed <br>
     *         false - Van Jacobson Connection ID compression is not allowed
     */
    public boolean isAllowVanJacobsonConnectionIDCompression() {
        return this.m_allowVanJacobsonConnectionIDCompression;
    }

    /**
     * Enable/disable Van Jacobson Connection ID compression
     *
     * @param allowVanJacobsonConnectionIDCompression
     *            <br>
     *            true - Van Jacobson Connection ID compression is allowed <br>
     *            false - Van Jacobson Connection ID compression is not allowed
     */
    public void setAllowVanJacobsonConnectionIDCompression(boolean allowVanJacobsonConnectionIDCompression) {
        this.m_allowVanJacobsonConnectionIDCompression = allowVanJacobsonConnectionIDCompression;
    }

    /**
     * Reports if BSD compression is allowed
     *
     * @return boolean <br>
     *         true - BSD compression is allowed <br>
     *         false - BSD compression is not allowed
     */
    public boolean isAllowBsdCompression() {
        return this.m_allowBsdCompression;
    }

    /**
     * Enable/disable BSD compression
     *
     * @param allowBsdCompression
     *            <br>
     *            true - BSD compression is allowed <br>
     *            false - BSD compression is not allowed
     */
    public void setAllowBsdCompression(boolean allowBsdCompression) {
        this.m_allowBsdCompression = allowBsdCompression;
    }

    /**
     * Reports if 'Deflate' compression is allowed
     *
     * @return boolean <br>
     *         true - 'Deflate' compression is allowed <br>
     *         false - 'Deflate' compression is not allowed
     */
    public boolean isAllowDeflateCompression() {
        return this.m_allowDeflateCompression;
    }

    /**
     * Enable/disable 'Deflate' compression
     *
     * @param allowDeflateCompression
     *            <br>
     *            true - 'Deflate' compression is allowed <br>
     *            false - 'Deflate' compression is not allowed
     */
    public void setAllowDeflateCompression(boolean allowDeflateCompression) {
        this.m_allowDeflateCompression = allowDeflateCompression;
    }

    /**
     * Reports is magic number negotiation is enabled
     *
     * @return boolean <br>
     *         true - magic number negotiation is enabled <br>
     *         false - magic number negotiation is disabled
     */
    public boolean isAllowMagic() {
        return this.m_allowMagic;
    }

    /**
     * Enable/disable magic number negotiation
     *
     * @param allowMagic
     *            <br>
     *            true - magic number negotiation is enabled <br>
     *            false - magic number negotiation is disabled
     */
    public void setAllowMagic(boolean allowMagic) {
        this.m_allowMagic = allowMagic;
    }

    public int getIdleTime() {
        return this.m_idle;
    }

    public void setIdleTime(int idle) {
        this.m_idle = idle;
    }

    public String getActiveFilter() {
        return this.m_activeFilter;
    }

    public void setActiveFilter(String activeFilter) {
        this.m_activeFilter = activeFilter;
    }

    public boolean isPersist() {
        return this.m_persist;
    }

    public void setPersist(boolean persist) {
        this.m_persist = persist;
    }

    public int getMaxFail() {
        return this.m_maxFail;
    }

    public void setMaxFail(int maxfail) {
        this.m_maxFail = maxfail;
    }

    /**
     * Reports number 'lcp-echo-failure' parameter that is number of
     * unanswered LCP echo requests to presume the peer to be
     * dead.
     *
     * @return LCP echo failure
     */
    public int getLcp_echo_failure() {
        return this.m_lcp_echo_failure;
    }

    /**
     * Sets 'lcp-echo-faillure' parameter
     *
     * @param lcp_echo_failure
     *            number of unanswered LCP echo requests before peer is assumed dead
     */
    public void setLcp_echo_failure(int lcp_echo_failure) {
        this.m_lcp_echo_failure = lcp_echo_failure;
    }

    /**
     * Reports LCP echo interval in seconds
     *
     * @return LCP echo interval
     */
    public int getLcp_echo_interval() {
        return this.m_lcp_echo_interval;
    }

    /**
     * Sets LCP echo interval
     *
     * @param lcp_echo_interval
     *            in seconds
     */
    public void setLcp_echo_interval(int lcp_echo_interval) {
        this.m_lcp_echo_interval = lcp_echo_interval;
    }

    /**
     * Reports if RTS/CTS hardware flow control is to be used
     *
     * @return boolean <br>
     *         true - use RTS/CTS flow control <br>
     *         false - do not use RTS/CTS flow control
     */
    public boolean isUseRtsCtsFlowControl() {
        return this.m_useRtsCtsFlowControl;
    }

    /**
     * Enable/disable RTS/CTS flow control
     *
     * @param useRtsCtsFlowControl
     *            <br>
     *            true - use RTS/CTS flow control <br>
     *            false - do not use RTS/CTS flow control
     */
    public void setUseRtsCtsFlowControl(boolean useRtsCtsFlowControl) {
        this.m_useRtsCtsFlowControl = useRtsCtsFlowControl;
    }

    /**
     * Reports if PPP daemon is setup to create a lock file
     *
     * @return boolean <br>
     *         true - create lock file <br>
     *         false - do not create lock file
     */
    public boolean isLockSerialDevice() {
        return this.m_lockSerialDevice;
    }

    /**
     * Enable/disable lock file
     *
     * @param lockSerialDevice
     *            <br>
     *            true - create lock file <br>
     *            false - do not create lock file
     */
    public void setLockSerialDevice(boolean lockSerialDevice) {
        this.m_lockSerialDevice = lockSerialDevice;
    }

    /**
     * Reports if modem control lines are to be used.
     *
     * @return boolean <br>
     *         true - use modem control lines
     *         false - do not use modem control lines
     */
    public boolean isUseModemControlLines() {
        return this.m_useModemControlLines;
    }

    /**
     * Sets 'use modem control lines' flag
     *
     * @param useModemControlLines
     *            <br>
     *            true - use modem control lines
     *            false - do not use modem control lines
     */
    public void setUseModemControlLines(boolean useModemControlLines) {
        this.m_useModemControlLines = useModemControlLines;
    }

    /**
     * Reports if peer is expected to authenticate itself
     *
     * @return boolean <br>
     *         true - peer must authenticate itself
     *         false - peer is not required to authenticate itself
     */
    public boolean isPeerMustAuthenticateItself() {
        return this.m_peerMustAuthenticateItself;
    }

    /**
     * Sets 'peer must authenticate itself' flag
     *
     * @param peerMustAuthenticateItself
     *            <br>
     *            true - peer must authenticate itself
     *            false - peer is not required to authenticate itself
     *
     */
    public void setPeerMustAuthenticateItself(boolean peerMustAuthenticateItself) {
        this.m_peerMustAuthenticateItself = peerMustAuthenticateItself;
    }

    /**
     * Reports connect delay in milliseconds
     *
     * @return - connect delay
     */
    public int getConnect_delay() {
        return this.m_connect_delay;
    }

    /**
     * Sets connect delay
     *
     * @param connect_delay
     *            in milliseconds
     */
    public void setConnect_delay(int connect_delay) {
        this.m_connect_delay = connect_delay;
    }

    /**
     * Reports if connection debugging is enabled
     *
     * @return boolean
     *         true - connection debugging is enabled <br>
     *         false - connection debugging is not enabled
     */
    public boolean isEnableDebug() {
        return this.m_enableDebug;
    }

    /**
     * Sets 'enable connection debugging' flag
     *
     * @param enableDebug
     *            - enable connection debugging
     */
    public void setEnableDebug(boolean enableDebug) {
        this.m_enableDebug = enableDebug;
    }

    /**
     * Write the current parameters to the specified peer file
     */
    public void write(String filename) throws Exception {

        // open output stream
        FileOutputStream fos = new FileOutputStream(filename);
        PrintWriter writer = new PrintWriter(fos);

        // set baud rate
        if (this.m_baudRate != -1) {
            writer.println(this.m_baudRate);
        }

        if (this.m_pppUnitNumber != -1) {
            writer.print("unit ");
            writer.println(this.m_pppUnitNumber);
        }

        // set logfile
        if (this.m_logfile != null) {
            writer.print("logfile ");
            writer.println(this.m_logfile);
        }

        if (this.m_enableDebug) {
            writer.println("debug");
        }

        if (this.m_connectScriptFilename != null) {
            StringBuffer connectLine = new StringBuffer("connect '");
            connectLine.append(CHAT_PROGRAM);
            connectLine.append(" -v -f ");
            connectLine.append(this.m_connectScriptFilename);
            connectLine.append("'");
            writer.println(connectLine.toString());
        }

        if (this.m_disconnectScriptFilename != null) {
            StringBuffer disconnectLine = new StringBuffer("disconnect '");
            disconnectLine.append(CHAT_PROGRAM);
            disconnectLine.append(" -v -f ");
            disconnectLine.append(this.m_disconnectScriptFilename);
            disconnectLine.append("'");
            writer.println(disconnectLine.toString());
        }

        if (this.m_useModemControlLines) {
            writer.println("modem");
        }

        if (this.m_useRtsCtsFlowControl) {
            writer.println("crtscts");
        }

        if (this.m_lockSerialDevice) {
            writer.println("lock");
        }

        if (!this.m_peerMustAuthenticateItself) {
            writer.println("noauth");
        }

        ChapLinux chapLinux = ChapLinux.getInstance();
        PapLinux papLinux = PapLinux.getInstance();

        if (getAuthType() != AuthType.NONE) {
            writer.print("user \"");
            writer.print(this.m_username);
            writer.println("\"");

            writer.println("hide-password");

            switch (getAuthType()) {
            case AUTO:
                if (!papLinux.checkForEntry(this.m_provider, this.m_username, "*", this.m_password, "*")) {
                    papLinux.addEntry(this.m_provider, this.m_username, "*", this.m_password, "*");
                }
                if (!chapLinux.checkForEntry(this.m_provider, this.m_username, "*", this.m_password, "*")) {
                    chapLinux.addEntry(this.m_provider, this.m_username, "*", this.m_password, "*");
                }
                break;
            case PAP:
                // remove CHAP entry if exists
                chapLinux.removeEntry("provider", this.m_provider);
                if (!papLinux.checkForEntry(this.m_provider, this.m_username, "*", this.m_password, "*")) {
                    papLinux.addEntry(this.m_provider, this.m_username, "*", this.m_password, "*");
                }
                break;
            case CHAP:
                // remove PAP entry if exists
                papLinux.removeEntry("provider", this.m_provider);
                if (!chapLinux.checkForEntry(this.m_provider, this.m_username, "*", this.m_password, "*")) {
                    chapLinux.addEntry(this.m_provider, this.m_username, "*", this.m_password, "*");
                }
                break;
            case NONE:
                break;
            }
        } else {
            // remove CHAP/PAP entries if exist
            chapLinux.removeEntry("provider", this.m_provider);
            papLinux.removeEntry("provider", this.m_provider);
        }

        if (this.m_peerToSupplyLocalIP) {
            writer.println("noipdefault");
        }

        if (this.m_addDefaultRoute) {
            writer.println("defaultroute");
        } else {
            writer.println("nodefaultroute");
        }

        if (this.m_usePeerDns) {
            writer.println("usepeerdns");
        }

        if (!this.m_allowProxyArps) {
            writer.println("noproxyarp");
        }

        if (!this.m_allowVanJacobsonTcpIpHdrCompression) {
            writer.println("novj");
        }

        if (!this.m_allowVanJacobsonConnectionIDCompression) {
            writer.println("novjccomp");
        }

        if (!this.m_allowBsdCompression) {
            writer.println("nobsdcomp");
        }

        if (!this.m_allowDeflateCompression) {
            writer.println("nodeflate");
        }

        if (!this.m_allowMagic) {
            writer.println("nomagic");
        }

        if (this.m_idle > 0) {
            writer.println("idle " + this.m_idle);
        }

        if (this.m_idle > 0 && this.m_activeFilter != null && this.m_activeFilter.length() > 0) {
            writer.println("active-filter '" + this.m_activeFilter + "'");
        }

        if (this.m_persist) {
            writer.println("persist");
            writer.println("holdoff 1");
        } else {
            writer.println("nopersist");
        }

        writer.println("maxfail " + this.m_maxFail);

        if (this.m_connect_delay != -1) {
            writer.println("connect-delay " + this.m_connect_delay);
        }

        if (this.m_lcp_echo_failure > 0) {
            writer.println("lcp-echo-failure " + this.m_lcp_echo_failure);
        }

        if (this.m_lcp_echo_interval > 0) {
            writer.println("lcp-echo-interval " + this.m_lcp_echo_interval);
        }

        writer.flush();
        fos.getFD().sync();
        writer.close();
        fos.close();
    }
}
