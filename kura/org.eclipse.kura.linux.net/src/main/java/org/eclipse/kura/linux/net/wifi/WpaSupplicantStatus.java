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
package org.eclipse.kura.linux.net.wifi;

import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpaSupplicantStatus {

    private static Logger logger = LoggerFactory.getLogger(WpaSupplicantStatus.class);

    private static final String PROP_MODE = "mode";
    private static final String PROP_BSSID = "bssid";
    private static final String PROP_SSID = "ssid";
    private static final String PROP_PAIRWISE_CIPHER = "pairwise_cipher";
    private static final String PROP_GROUP_CIPHER = "group_cipher";
    private static final String PROP_KEY_MGMT = "key_mgmt";
    private static final String PROP_WPA_STATE = "wpa_state";
    private static final String PROP_IP_ADDRESS = "ip_address";
    private static final String PROP_ADDRESS = "address";

    private Properties props = null;

    public WpaSupplicantStatus(String iface) throws KuraException {

        this.props = new Properties();
        SafeProcess proc = null;
        try {
            proc = ProcessUtil.exec(formSupplicantStatusCommand(iface));
            if (proc.waitFor() != 0) {
                logger.error("error executing command --- {} --- exit value = {}", formSupplicantStatusCommand(iface),
                        proc.exitValue());
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR);
            }

            this.props.load(proc.getInputStream());

            Enumeration<Object> keys = this.props.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                logger.trace("[WpaSupplicant Status] {} = {}", key, this.props.getProperty(key));
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    public String getMode() {
        return this.props.getProperty(PROP_MODE);
    }

    public String getBssid() {
        return this.props.getProperty(PROP_BSSID);
    }

    public String getSsid() {
        return this.props.getProperty(PROP_SSID);
    }

    public String getPairwiseCipher() {
        return this.props.getProperty(PROP_PAIRWISE_CIPHER);
    }

    public String getGroupCipher() {
        return this.props.getProperty(PROP_GROUP_CIPHER);
    }

    public String getKeyMgmt() {
        return this.props.getProperty(PROP_KEY_MGMT);
    }

    public String getWpaState() {
        return this.props.getProperty(PROP_WPA_STATE);
    }

    public String getIpAddress() {
        return this.props.getProperty(PROP_IP_ADDRESS);
    }

    public String getAddress() {
        return this.props.getProperty(PROP_ADDRESS);
    }

    private static String formSupplicantStatusCommand(String iface) {
        StringBuilder sb = new StringBuilder();
        sb.append("wpa_cli -i ");
        sb.append(iface);
        sb.append(" status");
        return sb.toString();
    }
}