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

import java.util.Properties;

/**
 * Defines modem configuration object that each modem will return in the
 * provideConfiguration() method. This object will be piked up by the
 * CellularConnectionManager.
 */
public class ModemConfiguration {

    /**
     * whether or not the modem is enabled
     */
    public static final String ENABLED = "enabled";

    /**
     * type of cellular service (i.e. CDMA, HSPA, WiMAX)
     */
    public static final String SERVICE_MODE = "service_mode";

    /**
     * Carrier image ID on MC8355
     */
    public static final String CARRIER_IMAGE_ID = "carrier_image_id";

    /**
     * UMTS profile ID
     */
    public static final String UMTS_PROFILE_ID = "umts_profile_id";

    /**
     * number of modems on this system
     */
    public static final String NUMBER_OF_MODEMS = "number_of_modems";

    /**
     * ppp unit number (e.g. ppp0 would be 0, ppp1 would be 1)
     */
    public static final String PPP_UNIT_NUMBER = "ppp_unit_number";

    /**
     * USB port address of this modem
     */
    public static final String USB_PORT_ADDRESS = "usb_port_address";

    /**
     * type of serial modem
     */
    public static final String SERIAL_MODEM_TYPE = "modem_type";

    /**
     * modem vendor id
     */
    public static final String VENDOR_ID = "vendor";

    /**
     * modem product id
     */
    public static final String PRODUCT_ID = "product";

    /**
     * whether or not to use the default ppp peer parameters
     */
    public static final String USE_DEFAULT_PEER_PARAM = "use_default_peer_param";

    /**
     * dialstring to use for this modem
     */
    public static final String DIALSTRING = "dialstring";

    /**
     * whether or not the configuration should be persisted in non-volatile RAM
     */
    public static final String PERSISTENCE = "persistence";

    /**
     * the profile name
     */
    public static final String PROFILE_NAME = "profile_name";

    /**
     * PDP type
     */
    public static final String PDP_TYPE = "pdptype";

    /**
     * Frequency band
     */
    public static final String FREQ_BAND = "freq_band";

    /**
     * Allowed frequency bands
     */
    public static final String ALLOWED_FREQ_BANDS = "allowed_freq_bands";

    /**
     * PDP Profiles
     */
    public static final String PDP_PROFILES = "pdp_profiles";

    /**
     * ID of selected PDP profile
     */
    public static final String PDP_PROFILE_ID = "pdp_profile_id";

    /**
     * the APN to use with this provider/carrier
     */
    public static final String APN = "apn";

    /**
     * the authentication type to use (e.g. pap or chap)
     */
    public static final String AUTH_TYPE = "authtype";

    /**
     * the username to use if authentication is required
     */
    public static final String USERNAME = "username";

    /**
     * the password to use if authentication is required
     */
    public static final String PASSWORD = "password";

    /**
     * the modem model number
     */
    public static final String MODEL = "model";

    /**
     * the modem serial number
     */
    public static final String SERIAL_NUMBER = "serial_number";

    /**
     * the network technology
     */
    public static final String NETWORK_TECHNOLOGY = "network_technology";

    public static final String LOGFILE = "logfile";
    public static final String BAUDRATE = "baudrate";
    public static final String DEBUG_ENABLE = "debug_enable";

    public static final String USE_RTS_CTS_FLOWCTRL = "use_rtscts_flowctrl";
    public static final String LOCK_SERIAL_DEVICE = "lock_serial_device";
    public static final String PEER_MUST_AUTHENTICATE_ITSELF = "peer_must_authenticate_itself";
    public static final String ADD_DEFAULT_ROUTE = "add_default_route";
    public static final String USE_PEER_DNS = "use_peer_dns";
    public static final String ALLOW_PROXY_ARPS = "allow_proxy_arps";
    public static final String ALLOW_VJ_TCPIP_HDR_COMP = "allow_vj_tcpip_hdr_comp";
    public static final String ALLOW_VJ_CONNID_COMP = "allow_vj_connid_comp";
    public static final String ALLOW_BSD_COMP = "allow_bsd_comp";
    public static final String ALLOW_DEFLATE_COMP = "allow_deflate_comp";
    public static final String ALLOW_MAGIC = "allow_magic";
    public static final String CONNECT_DELAY = "connect_delay";
    public static final String LCP_ECHO_FAILURE = "lcp_echo_failure";
    public static final String LCP_ECHO_INTERVAL = "lcp_echo_interval";
    public static final String USE_MODEM_CTRL_LINES = "use_modem_ctrl_lines";
    public static final String PEER_SUPPLIES_LOCAL_IP = "peer_supplies_local_ip";
    public static final String IPCP_ACCEPT_LOCAL = "ipcp_accept_local";
    public static final String IPCP_ACCEPT_REMOTE = "ipcp_accept_remote";

    private boolean enabled = false;
    private int serviceMode = 0;
    private int carrierImageID = 1;
    private int umtsProfileID = 1;

    private int pppUnitNumber = 0;
    private String dialstring = null;

    private String profileName = null;
    private String pdpType = null;
    private String apn = null;
    private String authType = null;
    private String username = null;
    private String password = null;
    private String freqBand = null;
    private int pdpProfileID = 0;

    /*
     * modem configuration properties obtained from the most suitable
     * configuration file
     */
    private Properties configProps = null;

    /**
     * ModemConfiguration constructor
     */
    public ModemConfiguration() {

        this.enabled = false;
        this.serviceMode = 0;
        this.carrierImageID = 1;
        this.umtsProfileID = 1;
        this.pppUnitNumber = 0;
        this.dialstring = "";

        this.profileName = "";
        this.pdpType = "";
        this.freqBand = "";
        this.apn = "";
        this.authType = "";
        this.username = "";
        this.password = "";
        this.pdpProfileID = 1;

        this.configProps = new Properties();
        this.configProps.put(ENABLED, Boolean.toString(this.enabled));
        this.configProps.put(SERVICE_MODE, Integer.toString(this.serviceMode));
        this.configProps.put(CARRIER_IMAGE_ID, Integer.toString(this.carrierImageID));
        this.configProps.put(UMTS_PROFILE_ID, Integer.toString(this.umtsProfileID));
        this.configProps.put(PPP_UNIT_NUMBER, Integer.toString(this.pppUnitNumber));
        this.configProps.put(DIALSTRING, this.dialstring);
        this.configProps.put(PDP_TYPE, this.pdpType);
        this.configProps.put(FREQ_BAND, this.freqBand);
        this.configProps.put(PROFILE_NAME, this.profileName);
        this.configProps.put(APN, this.apn);
        this.configProps.put(AUTH_TYPE, this.authType);
        this.configProps.put(USERNAME, this.username);
        this.configProps.put(PASSWORD, this.password);
        this.configProps.put(PDP_PROFILE_ID, Integer.toString(this.pdpProfileID));
    }

    /**
     * ModemConfiguration constructor
     *
     * @param configProps
     *            - configuration properties as <code>Properties</code>
     */
    public ModemConfiguration(Properties configProps) {

        this.configProps = configProps;
        this.enabled = Boolean.valueOf(this.configProps.getProperty(ENABLED, "false")).booleanValue();
        this.serviceMode = Integer.valueOf(this.configProps.getProperty(SERVICE_MODE, "0")).intValue();
        this.carrierImageID = Integer.valueOf(this.configProps.getProperty(CARRIER_IMAGE_ID, "1")).intValue();
        this.umtsProfileID = Integer.valueOf(this.configProps.getProperty(UMTS_PROFILE_ID, "1")).intValue();
        this.pppUnitNumber = Integer.valueOf(this.configProps.getProperty(PPP_UNIT_NUMBER, "0")).intValue();
        this.dialstring = this.configProps.getProperty(DIALSTRING);
        this.profileName = this.configProps.getProperty(PROFILE_NAME);
        this.pdpType = this.configProps.getProperty(PDP_TYPE);
        this.freqBand = this.configProps.getProperty(FREQ_BAND);
        this.apn = this.configProps.getProperty(APN);
        this.authType = this.configProps.getProperty(AUTH_TYPE);
        this.username = this.configProps.getProperty(USERNAME);
        this.password = this.configProps.getProperty(PASSWORD);
        this.pdpProfileID = Integer.valueOf(this.configProps.getProperty(PDP_PROFILE_ID, "1")).intValue();
    }

    /**
     * Reports configuration properties attached
     *
     * @return configuration properties as <code>Properties</code>
     */
    public Properties getConfigProps() {
        return this.configProps;
    }

    /**
     * Reports if modem is enabled in configuration
     *
     * @return <code>boolean</code><br>
     *         true - modem is enables<br>
     *         false - modem is disabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Reports cellular service mode.
     *
     * @return cellular service mode as <code>int</code>
     */
    public int getServiceMode() {
        return this.serviceMode;
    }

    /**
     * Reports PPP unit number (e.g. number of ppp interface)
     *
     * @return PPP unit number as <code>int</code>
     */
    public int getPppUnitNumber() {
        return this.pppUnitNumber;
    }

    /**
     * Reports dialstring
     *
     * @return dialstring as <code>String</code>
     */
    public String getDialString() {
        return this.dialstring;
    }

    /**
     * Reports APN
     *
     * @return APN as <code>String</code>
     */
    public String getApn() {
        return this.apn;
    }

    /**
     * Reports authentication type
     *
     * @return authentication type as <code>String</code>
     */
    public String getAuthenticationType() {
        return this.authType;
    }

    /**
     * Reports username
     *
     * @return username as <code>String</code>
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Reports password
     *
     * @return password as <code>String</code>
     */
    public String getPassword() {
        return this.password;
    }

    public String getProfileName() {
        return this.profileName;
    }

    public String getPdpType() {
        return this.pdpType;
    }

    public String getFreqBand() {
        return this.freqBand;
    }

    public int getPdpProfileID() {
        return this.pdpProfileID;
    }
}
