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
package org.eclipse.kura.net.modem;

import java.util.Arrays;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Modem configuration representation
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class ModemConfig implements NetConfig {

    /**
     * Configuration for a cellular modem.
     */
    public enum PdpType {
        IP,
        PPP,
        IPv6,
        UNKNOWN
    }

    public enum AuthType {
        NONE,
        AUTO,
        PAP,
        CHAP
    }

    private boolean enabled = false;
    private String dialString = "";
    private int pppNumber = 0;
    private int profileID = 0;
    private PdpType pdpType = PdpType.IP;
    private AuthType authType = AuthType.NONE;
    private String apn = "";
    private String username = "";
    private Password password = new Password("");
    private boolean persist = false;
    private int maxFail = 0;
    private int idle = 0;
    private String activeFilter = "";
    private int lcpEchoInterval = 0;
    private int lcpEchoFailure = 0;
    private IPAddress ipAddress = null;
    private int dataCompression = 0;			// FIXME: change to enum?
    private int headerCompression = 0;		// FIXME: change to enum?
    private boolean gpsEnabled = false;
    private int resetTimeout = 0;

    /**
     * Empty constructor
     */
    public ModemConfig() {
    }

    /**
     * PDP config constructor
     *
     * @param apn
     *            - access point name as {@link String}
     * @param ipAddress
     *            - IP address as {@link String}
     * @param profileID
     *            - PDP profile ID as {@link int}
     * @param pdpType
     *            - PDP type as {@link PdpType}
     * @param dataCompression
     *            - PDP data compression as {@link int}
     * @param headerCompresion
     *            - PDP header compression as {@link int}
     */
    public ModemConfig(int profileID, PdpType pdpType, String apn, IPAddress ipAddress, int dataCompression,
            int headerCompresion) {

        this.profileID = profileID;
        this.pdpType = pdpType;
        this.apn = apn;
        this.ipAddress = ipAddress;
        this.dataCompression = dataCompression;
        this.headerCompression = headerCompresion;
    }

    /**
     * Reports whether it is enabled.
     *
     * @return is enabled as {@link boolean}
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Sets the enabled setting.
     *
     * @param enabled
     *            - enabled status as {@link boolean}
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the dial string.
     *
     * @return dial string as {@link String}
     */
    public String getDialString() {
        return this.dialString;
    }

    /**
     * Sets the dial string.
     *
     * @param dialString
     *            - dial string as {@link String}
     */
    public void setDialString(String dialString) {
        this.dialString = dialString;
    }

    /**
     * Reports authentication type.
     *
     * @return authentication type as {@link AuthType}
     */
    public AuthType getAuthType() {
        return this.authType;
    }

    /**
     * Sets authentication type.
     *
     * @param authType
     *            - authentication type as {@link AuthType}
     */
    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    /**
     * Reports user name.
     *
     * @return user name as {@link String}
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Sets user name.
     *
     * @param username
     *            - user name as {@link String}
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Reports password.
     *
     * @return password as {@link String}
     * @deprecated
     */
    public String getPassword() {
        return this.password.toString();
    }

    /**
     * Reports password.
     *
     * @return password as {@link Password}
     * @since 1.3
     */
    public Password getPasswordAsPassword() {
        return this.password;
    }

    /**
     * Sets password.
     *
     * @param password
     *            - password as {@link String}
     */
    public void setPassword(String password) {
        this.password = new Password(password);
    }

    /**
     * Sets password.
     *
     * @param password
     *            - password as {@link Password}
     * @since 1.3
     */
    public void setPassword(Password password) {
        this.password = password;
    }

    /**
     * Reports if pppd is instructed to exit after a connection is terminated.
     *
     * @return 'persist' flag {@link boolean}
     */
    public boolean isPersist() {
        return this.persist;
    }

    /**
     * Sets 'persist' flag to instruct pppd if it needs to exit after a connection is terminated.
     *
     * @param persist
     *            as {@link boolean}
     */
    public void setPersist(boolean persist) {
        this.persist = persist;
    }

    /**
     * Reports maximum number of failed connection attempts.
     *
     * @return maximum number of failed connection attempts as {@link int}
     */
    public int getMaxFail() {
        return this.maxFail;
    }

    /**
     * Sets maximum number of failed connection attempts
     *
     * @param maxFail
     *            - maximum number of failed connection attempts as {@link int}
     */
    public void setMaxFail(int maxFail) {
        this.maxFail = maxFail;
    }

    /**
     * Reports value of the 'idle' option.
     * The 'idle' option specifies that pppd should disconnect if the link is idle for n seconds.
     *
     * @return value of the 'idle' option as {@link int}
     */
    public int getIdle() {
        return this.idle;
    }

    /**
     * Sets value of the 'idle' option.
     * The 'idle' option specifies that pppd should disconnect if the link is idle for n seconds.
     *
     * @param idle
     */
    public void setIdle(int idle) {
        this.idle = idle;
    }

    /**
     * Reports the value of the 'active-filter' option that specifies a packet filter to be
     * applied to data packets to determine which packets are to be regarded as link activity.
     *
     * @return value of the 'active-filter' option as {@link String}
     */
    public String getActiveFilter() {
        return this.activeFilter;
    }

    /**
     * Sets the value of the 'active-filter' option that specifies a packet filter to be
     * applied to data packets to determine which packets are to be regarded as link activity.
     *
     * @param activeFilter
     *            - active filter as {@link String}
     */
    public void setActiveFilter(String activeFilter) {
        this.activeFilter = activeFilter;
    }

    /**
     * Reports LCP echo interval
     *
     * @return LCP echo interval (in sec) as {@link int}
     */
    public int getLcpEchoInterval() {
        return this.lcpEchoInterval;
    }

    /**
     * Sets LCP echo interval
     *
     * @param lcpEchoInterval
     *            - LCP Echo interval as {@link int}
     */
    public void setLcpEchoInterval(int lcpEchoInterval) {
        this.lcpEchoInterval = lcpEchoInterval;
    }

    /**
     * Reports number of failed LCP echo requests
     *
     * @return number of failed LCP echo requests as {@link int}
     */
    public int getLcpEchoFailure() {
        return this.lcpEchoFailure;
    }

    /**
     * Sets number of failed LCP echo requests
     * (unacknowledged LCP echo requests to be sent for pppd to presume the peer to be dead)
     *
     * @param lcpEchoFailure
     */
    public void setLcpEchoFailure(int lcpEchoFailure) {
        this.lcpEchoFailure = lcpEchoFailure;
    }

    /**
     * Reports PPP number (i.e. '0' for ppp0).
     *
     * @return PPP number as {@link int}
     */
    public int getPppNumber() {
        return this.pppNumber;
    }

    /**
     * Sets PPP number (i.e. '0' for ppp0).
     *
     * @param pppNumber
     *            - PPP number as {@link int}
     */
    public void setPppNumber(int pppNumber) {
        this.pppNumber = pppNumber;
    }

    /**
     * Reports PDP profile ID.
     *
     * @return PDP profile ID as {@link int}
     */
    public int getProfileID() {
        return this.profileID;
    }

    /**
     * Sets PDP profile ID.
     *
     * @param id
     *            - PDP profile ID as {@link int}
     */
    public void setProfileID(int id) {
        this.profileID = id;
    }

    /**
     * Reports PDP type.
     *
     * @return PDP type as {@link PdpType}
     */
    public PdpType getPdpType() {
        return this.pdpType;
    }

    /**
     * Sets PDP type.
     *
     * @param pdpType
     *            - PDP type as {@link PdpType}
     */
    public void setPdpType(PdpType pdpType) {
        this.pdpType = pdpType;
    }

    /**
     * Reports access point name.
     *
     * @return access point name as {@link String}
     */
    public String getApn() {
        return this.apn;
    }

    /**
     * Sets access point name.
     *
     * @param apn
     *            - access point name as {@link String}
     */
    public void setApn(String apn) {
        this.apn = apn;
    }

    /**
     * Reports PDP IP address.
     *
     * @return IP address as {@link IPAddress}
     */
    public IPAddress getIpAddress() {
        return this.ipAddress;
    }

    /**
     * Sets PDP IP address.
     *
     * @param address
     *            - IP address as {@link IPAddress}
     */
    public void setIpAddress(IPAddress address) {
        this.ipAddress = address;
    }

    /**
     * Reports a value of numeric parameter that supports PDP data compression.
     *
     * @return PDP data compression as {@link int}
     */
    public int getDataCompression() {
        return this.dataCompression;
    }

    /**
     * Sets a value of numeric parameter that supports PDP data compression.
     *
     * @param dataCompression
     *            - PDP data compression as {@link int}
     */
    public void setDataCompression(int dataCompression) {
        this.dataCompression = dataCompression;
    }

    /**
     * Reports a value of numeric parameter that supports PDP header compression.
     *
     * @return PDP header compression as {@link int}
     */
    public int getHeaderCompression() {
        return this.headerCompression;
    }

    /**
     * Sets a value of numeric parameter that supports PDP header compression.
     *
     * @param headerCompression
     *            headerCompression PDP header compression as {@link int}
     */
    public void setHeaderCompression(int headerCompression) {
        this.headerCompression = headerCompression;
    }

    /**
     * Reports if PDP data compression is enabled.
     *
     * @return {@link boolean}
     */
    public boolean isDataCompression() {
        return this.dataCompression == 0 ? false : true;
    }

    /**
     * Reports if PDP header compression is enabled.
     *
     * @return {@link boolean}
     */
    public boolean isHeaderCompression() {
        return this.headerCompression == 0 ? false : true;
    }

    public boolean isGpsEnabled() {
        return this.gpsEnabled;
    }

    public int getResetTimeout() {
        return this.resetTimeout;
    }

    public void setResetTimeout(int tout) {
        this.resetTimeout = tout;
    }

    public void setGpsEnabled(boolean gpsEnabled) {
        this.gpsEnabled = gpsEnabled;
    }

    @Override
    public int hashCode() {
        final int prime = 59;
        int result = super.hashCode();

        result = prime * result + this.profileID;
        result = prime * result + (this.pdpType == null ? 0 : this.pdpType.hashCode());
        result = prime * result + (this.authType == null ? 0 : this.authType.hashCode());
        result = prime * result + (this.apn == null ? 0 : this.apn.hashCode());
        result = prime * result + (this.username == null ? 0 : this.username.hashCode());
        result = prime * result + (this.password == null ? 0 : this.password.hashCode());
        result = prime * result + (this.ipAddress == null ? 0 : this.ipAddress.hashCode());
        result = prime * result + this.pppNumber;
        result = prime * result + this.maxFail;
        result = prime * result + this.resetTimeout;
        result = prime * result + this.idle;
        result = prime * result + (this.activeFilter == null ? 0 : this.activeFilter.hashCode());
        result = prime * result + this.lcpEchoFailure;
        result = prime * result + this.lcpEchoInterval;
        result = prime * result + this.dataCompression;
        result = prime * result + this.headerCompression;
        result = prime * result + (this.persist ? 1 : 0);
        result = prime * result + (this.gpsEnabled ? 1 : 0);

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ModemConfig)) {
            return false;
        }

        ModemConfig otherConfig = (ModemConfig) obj;

        if (this.enabled != otherConfig.isEnabled()) {
            return false;
        }

        if (this.pppNumber != otherConfig.getPppNumber()) {
            return false;
        }

        if (this.persist != otherConfig.isPersist()) {
            return false;
        }

        if (this.maxFail != otherConfig.getMaxFail()) {
            return false;
        }

        if (this.resetTimeout != otherConfig.getResetTimeout()) {
            return false;
        }

        if (this.idle != otherConfig.getIdle()) {
            return false;
        }

        if (this.lcpEchoInterval != otherConfig.getLcpEchoInterval()) {
            return false;
        }

        if (this.lcpEchoFailure != otherConfig.getLcpEchoFailure()) {
            return false;
        }

        if (this.profileID != otherConfig.getProfileID()) {
            return false;
        }

        if (this.pdpType != otherConfig.getPdpType()) {
            return false;
        }

        if (this.authType != otherConfig.getAuthType()) {
            return false;
        }

        if (this.dataCompression != otherConfig.getDataCompression()) {
            return false;
        }

        if (this.headerCompression != otherConfig.getHeaderCompression()) {
            return false;
        }

        if (this.gpsEnabled != otherConfig.isGpsEnabled()) {
            return false;
        }

        if (this.dialString != null) {
            if (!this.dialString.equals(otherConfig.getDialString())) {
                return false;
            }
        } else {
            if (otherConfig.getDialString() != null) {
                return false;
            }
        }
        if (this.apn != null) {
            if (!this.apn.equals(otherConfig.getApn())) {
                return false;
            }
        } else {
            if (otherConfig.getApn() != null) {
                return false;
            }
        }

        if (this.username != null && this.username.length() > 0) {
            if (!this.username.equals(otherConfig.getUsername())) {
                return false;
            }
        } else {
            if (otherConfig.getUsername() != null && otherConfig.getUsername().length() > 0) {
                return false;
            }
        }

        if (this.password != null && this.password.getPassword().length > 0) {
            if (!Arrays.equals(this.password.getPassword(), otherConfig.getPasswordAsPassword().getPassword())) {
                return false;
            }
        } else {
            Password otherConfigPassword = otherConfig.getPasswordAsPassword();
            if (otherConfigPassword != null && otherConfigPassword.getPassword().length > 0) {
                return false;
            }
        }

        if (this.activeFilter != null && this.activeFilter.length() > 0) {
            if (!this.activeFilter.equals(otherConfig.getActiveFilter())) {
                return false;
            }
        } else {
            if (otherConfig.getActiveFilter() != null && otherConfig.getActiveFilter().length() > 0) {
                return false;
            }
        }

        if (this.ipAddress != null) {
            if (!this.ipAddress.equals(otherConfig.ipAddress)) {
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
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());

        sb.append("ModemConfig - ");

        sb.append("Enabled: ").append(this.enabled);
        sb.append(" - PPP Number: ").append(this.pppNumber);
        sb.append(" - Dial String: ").append(this.dialString);
        sb.append(" - Profile ID: ").append(this.profileID);
        sb.append(" - PDP Type: ").append(this.pdpType);
        sb.append(" - Auth Type: ").append(this.authType);
        sb.append(" - APN: ").append(this.apn);
        sb.append(" - Username: ").append(this.username);
        sb.append(" - Password: ").append(this.password);
        sb.append(" - IP Address: ").append(this.ipAddress == null ? "null" : this.ipAddress.getHostAddress());
        sb.append(" - Data Compression: ").append(this.dataCompression);
        sb.append(" - Header Compression: ").append(this.headerCompression);

        return sb.toString();
    }

    @Override
    public boolean isValid() {
        if (this.pppNumber < 0) {
            return false;
        }

        return true;
    }
}
