/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
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

    public WpaSupplicantStatus(String iface, CommandExecutorService executorService) throws KuraException {

        this.props = new Properties();
        String[] cmd = formSupplicantStatusCommand(iface);
        Command command = new Command(cmd);
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            if (logger.isErrorEnabled()) {
                logger.error("error executing command --- {} --- exit value = {}", String.join(" ", cmd),
                        status.getExitStatus().getExitCode());
            }
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, "Failed to get wpa supplicant status");
        }

        try {
            this.props.load(new ByteArrayInputStream(((ByteArrayOutputStream) status.getOutputStream()).toByteArray()));
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, "Failed to parse wpa supplicant status");
        }

        Enumeration<Object> keys = this.props.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            logger.trace("[WpaSupplicant Status] {} = {}", key, this.props.getProperty(key));
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

    private static String[] formSupplicantStatusCommand(String iface) {
        return new String[] { "wpa_cli", "-i", iface, "status" };
    }
}