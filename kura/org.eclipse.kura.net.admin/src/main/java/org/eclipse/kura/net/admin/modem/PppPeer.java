/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

import org.eclipse.kura.configuration.Password;
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

    private static final String AUTH_SECRETS_TYPE_PROVIDER = "provider";

    private boolean enabled = false;

    /* for a ppp0 or ppp1 etc interface name */
    private int pppUnitNumber = 0;

    /* name of the provider */
    private String provider = null;

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
    private Password password = null;

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

    private boolean persist = false;

    private int maxFail = 0;

    private int idle = 0;

    private String activeFilter = null;

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
    private int lcpEchoFailure = -1;

    /*
     * If this option is given, ppp daemon will send an LCP echo-request frame
     * to the peer every n seconds. Normally the peer should respond to
     * the echo-request by sending an echo-reply.
     * This option can be used with the lcp-echo-failure option to detect
     * that the peer is no longer connected.
     */
    private int lcpEchoInterval = -1;

    /*
     * Wait for up to n milliseconds after the connect script finishes for a
     * valid PPP packet from the peer.
     */
    private int connectDelay = -1;

    /* Modem Exchange script */
    private String connectScriptFilename = null;
    private String disconnectScriptFilename = null;

    /**
     * Reports PPP unit number
     *
     * @return PPP unit number as <code>int</code>
     */
    public int getPppUnitNumber() {
        return this.pppUnitNumber;
    }

    /**
     * Sets PPP unit number
     *
     * @param pppUnitNumber
     *            as <code>int</code>
     */
    public void setPppUnitNumber(int pppUnitNumber) {
        this.pppUnitNumber = pppUnitNumber;
    }

    /**
     * Reports if PPP is enabled in configuration
     *
     * @return boolean <br>
     *         true - PPP is enabled
     *         false - PPP is disabled
     */
    public boolean isEnabled() {
        return this.enabled;
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
        this.enabled = enabled;
    }

    /**
     * Reports service provider
     *
     * @return - service provider
     */
    public String getProvider() {
        return this.provider;
    }

    /**
     * Sets service provider
     *
     * @param provider
     *            as String
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Reports APN (Access Point Name) of cellular data connection.
     *
     * @return APN
     */
    public String getApn() {
        return this.apn;
    }

    /**
     * Sets APN (Access Point Name) for cellular data connection.
     *
     * @param apn
     *            as String
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
        return this.dialString;
    }

    /**
     * Sets dial string
     *
     * @param dialString
     *            such as "atdt555-1212"
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
        return this.authType;
    }

    /**
     * Sets authentication type
     *
     * @param authType
     *            int (NONE, PAP, CHAP) as defined in {@link org.eclipe.kura.net.ppp.service.PppAuthentication}
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
        return this.username;
    }

    /**
     * Sets user name used for authentication
     *
     * @param username
     *            as String
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Reports password for authentication
     *
     * @return password
     */
    public Password getPassword() {
        return this.password;
    }

    /**
     * Sets password used for authentication
     *
     * @param password
     *            as String
     */
    public void setPassword(Password password) {
        this.password = password;
    }

    /**
     * Reports baud rate used for PPP
     *
     * @return baud rate
     */
    public int getBaudRate() {
        return this.baudRate;
    }

    /**
     * Sets baud rate used for PPP
     *
     * @param baudRate
     *            on serial port, as int
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
        return this.logfile;
    }

    /**
     * Sets the name of the log file
     *
     * @param logfile
     *            as String
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
        return this.connectScriptFilename;
    }

    /**
     * Sets connect script filename
     *
     * @param connectScript
     *            as <code>String</code>
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
        return this.disconnectScriptFilename;
    }

    /**
     * Sets disconnect script filename
     *
     * @param disconnectScript
     *            as <code>String</code>
     */
    public void setDisconnectScript(String disconnectScript) {
        this.disconnectScriptFilename = disconnectScript;
    }

    /**
     * Reports 'defaultroute' option
     *
     * @return boolean <br>
     *         true - add default route to the system routing table <br>
     *         false - do not add default route
     */
    public boolean isAddDefaultRoute() {
        return this.addDefaultRoute;
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
        this.addDefaultRoute = addDefaultRoute;
    }

    /**
     * Reports if peer is expected to supply local IP address using IPCP negotiation.
     *
     * @return boolean <br>
     *         true - peer is expected to supply local IP address
     *         false - peer is not supplying local IP address
     */
    public boolean isPeerToSupplyLocalIP() {
        return this.peerToSupplyLocalIP;
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
        this.peerToSupplyLocalIP = peerToSupplyLocalIP;
    }

    /**
     * Reports if peer supplied DNS should be used
     *
     * @return boolean <br>
     *         true - use peer DNS <br>
     *         false - do not use peer DNS
     */
    public boolean isUsePeerDns() {
        return this.usePeerDns;
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
        this.usePeerDns = usePeerDns;
    }

    /**
     * Reports if 'proxyarp' option is enabled
     *
     * @return boolean <br>
     *         true - 'proxyarp' is enabled <br>
     *         false - 'proxyarp' is disabled
     */
    public boolean isAllowProxyArps() {
        return this.allowProxyArps;
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
        this.allowProxyArps = allowProxyArps;
    }

    /**
     * Reports if Van Jacobson TCP/IP header compression is allowed
     *
     * @return boolean <br>
     *         true - Van Jacobson TCP/IP header compression is allowed <br>
     *         false - Van Jacobson TCP/IP header compression is not allowed
     */
    public boolean isAllowVanJacobsonTcpIpHdrCompression() {
        return this.allowVanJacobsonTcpIpHdrCompression;
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
        this.allowVanJacobsonTcpIpHdrCompression = allowVanJacobsonTcpIpHdrCompression;
    }

    /**
     * Reports if Van Jacobson Connection ID compression is allowed
     *
     * @return boolean <br>
     *         true - Van Jacobson Connection ID compression is allowed <br>
     *         false - Van Jacobson Connection ID compression is not allowed
     */
    public boolean isAllowVanJacobsonConnectionIDCompression() {
        return this.allowVanJacobsonConnectionIDCompression;
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
        this.allowVanJacobsonConnectionIDCompression = allowVanJacobsonConnectionIDCompression;
    }

    /**
     * Reports if BSD compression is allowed
     *
     * @return boolean <br>
     *         true - BSD compression is allowed <br>
     *         false - BSD compression is not allowed
     */
    public boolean isAllowBsdCompression() {
        return this.allowBsdCompression;
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
        this.allowBsdCompression = allowBsdCompression;
    }

    /**
     * Reports if 'Deflate' compression is allowed
     *
     * @return boolean <br>
     *         true - 'Deflate' compression is allowed <br>
     *         false - 'Deflate' compression is not allowed
     */
    public boolean isAllowDeflateCompression() {
        return this.allowDeflateCompression;
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
        this.allowDeflateCompression = allowDeflateCompression;
    }

    /**
     * Reports is magic number negotiation is enabled
     *
     * @return boolean <br>
     *         true - magic number negotiation is enabled <br>
     *         false - magic number negotiation is disabled
     */
    public boolean isAllowMagic() {
        return this.allowMagic;
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
        this.allowMagic = allowMagic;
    }

    public int getIdleTime() {
        return this.idle;
    }

    public void setIdleTime(int idle) {
        this.idle = idle;
    }

    public String getActiveFilter() {
        return this.activeFilter;
    }

    public void setActiveFilter(String activeFilter) {
        this.activeFilter = activeFilter;
    }

    public boolean isPersist() {
        return this.persist;
    }

    public void setPersist(boolean persist) {
        this.persist = persist;
    }

    public int getMaxFail() {
        return this.maxFail;
    }

    public void setMaxFail(int maxfail) {
        this.maxFail = maxfail;
    }

    /**
     * Reports number 'lcp-echo-failure' parameter that is number of
     * unanswered LCP echo requests to presume the peer to be
     * dead.
     *
     * @return LCP echo failure
     */
    public int getLcpEchoFailure() {
        return this.lcpEchoFailure;
    }

    /**
     * Sets 'lcp-echo-faillure' parameter
     *
     * @param lcpEchoFailure
     *            number of unanswered LCP echo requests before peer is assumed dead
     */
    public void setLcpEchoFailure(int lcpEchoFailure) {
        this.lcpEchoFailure = lcpEchoFailure;
    }

    /**
     * Reports LCP echo interval in seconds
     *
     * @return LCP echo interval
     */
    public int getLcpEchoInterval() {
        return this.lcpEchoInterval;
    }

    /**
     * Sets LCP echo interval
     *
     * @param lcpEchoInterval
     *            in seconds
     */
    public void setLcpEchoInterval(int lcpEchoInterval) {
        this.lcpEchoInterval = lcpEchoInterval;
    }

    /**
     * Reports if RTS/CTS hardware flow control is to be used
     *
     * @return boolean <br>
     *         true - use RTS/CTS flow control <br>
     *         false - do not use RTS/CTS flow control
     */
    public boolean isUseRtsCtsFlowControl() {
        return this.useRtsCtsFlowControl;
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
        this.useRtsCtsFlowControl = useRtsCtsFlowControl;
    }

    /**
     * Reports if PPP daemon is setup to create a lock file
     *
     * @return boolean <br>
     *         true - create lock file <br>
     *         false - do not create lock file
     */
    public boolean isLockSerialDevice() {
        return this.lockSerialDevice;
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
        this.lockSerialDevice = lockSerialDevice;
    }

    /**
     * Reports if modem control lines are to be used.
     *
     * @return boolean <br>
     *         true - use modem control lines
     *         false - do not use modem control lines
     */
    public boolean isUseModemControlLines() {
        return this.useModemControlLines;
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
        this.useModemControlLines = useModemControlLines;
    }

    /**
     * Reports if peer is expected to authenticate itself
     *
     * @return boolean <br>
     *         true - peer must authenticate itself
     *         false - peer is not required to authenticate itself
     */
    public boolean isPeerMustAuthenticateItself() {
        return this.peerMustAuthenticateItself;
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
        this.peerMustAuthenticateItself = peerMustAuthenticateItself;
    }

    /**
     * Reports connect delay in milliseconds
     *
     * @return - connect delay
     */
    public int getConnectDelay() {
        return this.connectDelay;
    }

    /**
     * Sets connect delay
     *
     * @param connectDelay
     *            in milliseconds
     */
    public void setConnectDelay(int connectDelay) {
        this.connectDelay = connectDelay;
    }

    /**
     * Reports if connection debugging is enabled
     *
     * @return boolean
     *         true - connection debugging is enabled <br>
     *         false - connection debugging is not enabled
     */
    public boolean isEnableDebug() {
        return this.enableDebug;
    }

    /**
     * Sets 'enable connection debugging' flag
     *
     * @param enableDebug
     *            - enable connection debugging
     */
    public void setEnableDebug(boolean enableDebug) {
        this.enableDebug = enableDebug;
    }

    /**
     * Write the current parameters to the specified peer file
     */
    public void write(String filename) throws Exception {

        // open output stream
        try (FileOutputStream fos = new FileOutputStream(filename); PrintWriter writer = new PrintWriter(fos);) {

            // set baud rate
            if (this.baudRate != -1) {
                writer.println(this.baudRate);
            }

            if (this.pppUnitNumber != -1) {
                writer.print("unit ");
                writer.println(this.pppUnitNumber);
            }

            // set logfile
            if (this.logfile != null) {
                writer.print("logfile ");
                writer.println(this.logfile);
            }

            if (this.enableDebug) {
                writer.println("debug");
            }

            if (this.connectScriptFilename != null) {
                StringBuilder connectLine = new StringBuilder("connect '");
                connectLine.append(CHAT_PROGRAM);
                connectLine.append(" -v -f ");
                connectLine.append(this.connectScriptFilename);
                connectLine.append("'");
                writer.println(connectLine.toString());
            }

            if (this.disconnectScriptFilename != null) {
                StringBuilder disconnectLine = new StringBuilder("disconnect '");
                disconnectLine.append(CHAT_PROGRAM);
                disconnectLine.append(" -v -f ");
                disconnectLine.append(this.disconnectScriptFilename);
                disconnectLine.append("'");
                writer.println(disconnectLine.toString());
            }

            if (this.useModemControlLines) {
                writer.println("modem");
            }

            if (this.useRtsCtsFlowControl) {
                writer.println("crtscts");
            }

            if (this.lockSerialDevice) {
                writer.println("lock");
            }

            if (!this.peerMustAuthenticateItself) {
                writer.println("noauth");
            }

            ChapLinux chapLinux = ChapLinux.getInstance();
            PapLinux papLinux = PapLinux.getInstance();

            if (getAuthType() != AuthType.NONE) {
                writer.print("user \"");
                writer.print(this.username);
                writer.println("\"");

                writer.println("hide-password");

                switch (getAuthType()) {
                case AUTO:
                    if (!papLinux.checkForEntry(this.provider, this.username, "*", this.password.toString(), "*")) {
                        papLinux.addEntry(this.provider, this.username, "*", this.password.toString(), "*");
                    }
                    if (!chapLinux.checkForEntry(this.provider, this.username, "*", this.password.toString(), "*")) {
                        chapLinux.addEntry(this.provider, this.username, "*", this.password.toString(), "*");
                    }
                    break;
                case PAP:
                    // remove CHAP entry if exists
                    chapLinux.removeEntry(AUTH_SECRETS_TYPE_PROVIDER, this.provider);
                    if (!papLinux.checkForEntry(this.provider, this.username, "*", this.password.toString(), "*")) {
                        papLinux.addEntry(this.provider, this.username, "*", this.password.toString(), "*");
                    }
                    break;
                case CHAP:
                    // remove PAP entry if exists
                    papLinux.removeEntry(AUTH_SECRETS_TYPE_PROVIDER, this.provider);
                    if (!chapLinux.checkForEntry(this.provider, this.username, "*", this.password.toString(), "*")) {
                        chapLinux.addEntry(this.provider, this.username, "*", this.password.toString(), "*");
                    }
                    break;
                case NONE:
                    break;
                }
            } else {
                // remove CHAP/PAP entries if exist
                chapLinux.removeEntry(AUTH_SECRETS_TYPE_PROVIDER, this.provider);
                papLinux.removeEntry(AUTH_SECRETS_TYPE_PROVIDER, this.provider);
            }

            if (this.peerToSupplyLocalIP) {
                writer.println("noipdefault");
            }

            if (this.addDefaultRoute) {
                writer.println("defaultroute");
            } else {
                writer.println("nodefaultroute");
            }

            if (this.usePeerDns) {
                writer.println("usepeerdns");
            }

            if (!this.allowProxyArps) {
                writer.println("noproxyarp");
            }

            if (!this.allowVanJacobsonTcpIpHdrCompression) {
                writer.println("novj");
            }

            if (!this.allowVanJacobsonConnectionIDCompression) {
                writer.println("novjccomp");
            }

            if (!this.allowBsdCompression) {
                writer.println("nobsdcomp");
            }

            if (!this.allowDeflateCompression) {
                writer.println("nodeflate");
            }

            if (!this.allowMagic) {
                writer.println("nomagic");
            }

            if (this.idle > 0) {
                writer.println("idle " + this.idle);
            }

            if (this.idle > 0 && this.activeFilter != null && this.activeFilter.length() > 0) {
                writer.println("active-filter '" + this.activeFilter + "'");
            }

            if (this.persist) {
                writer.println("persist");
                writer.println("holdoff 1");
            } else {
                writer.println("nopersist");
            }

            writer.println("maxfail " + this.maxFail);

            if (this.connectDelay != -1) {
                writer.println("connect-delay " + this.connectDelay);
            }

            if (this.lcpEchoFailure > 0) {
                writer.println("lcp-echo-failure " + this.lcpEchoFailure);
            }

            if (this.lcpEchoInterval > 0) {
                writer.println("lcp-echo-interval " + this.lcpEchoInterval);
            }

            writer.flush();
            fos.getFD().sync();
        }
    }
}
