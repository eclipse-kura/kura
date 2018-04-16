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
package org.eclipse.kura.core.status;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.Properties;

public class CloudConnectionStatusURL {

    public static final String NOTIFICATION_TYPE = "notification_type";
    public static final String CCS = "ccs:";
    public static final String LED = "led:";
    public static final String LINUX_LED = "linux_led:";
    public static final String LOG = "log";
    public static final String NONE = "none";

    public static final String INVERTED = ":inverted";
    
    private static final String CCS_NOTIFICATION_URLS_SEPARATOR = ";";

    private CloudConnectionStatusURL() {
    }

    public static Properties parseURL(String ccsUrl) {
        requireNonNull(ccsUrl);

        String urlImage = ccsUrl.toLowerCase(Locale.ENGLISH);

        Properties props = new Properties();

        if (urlImage.startsWith(CCS)) {
            urlImage = urlImage.replace(CCS, "");
            props.put("url", ccsUrl);

            String[] urls = urlImage.split(CCS_NOTIFICATION_URLS_SEPARATOR);
            for (String url : urls) {
                props.putAll(parseUrlType(url));
            }
        } else {
            props.put(NOTIFICATION_TYPE, StatusNotificationTypeEnum.NONE);
        }

        return props;
    }

    private static Properties parseUrlType(String urlImage) {
        Properties props = new Properties();
        if (urlImage.startsWith(LED)) {
            // Cloud Connection Status on LED
            String ledString = urlImage.replace(LED, "").trim();
            try {
                if (ledString.endsWith(INVERTED)) {
                    props.put("inverted", true);
                } else {
                    props.put("inverted", false);
                }
                
                //in case of typo
                if (ledString.contains(":")) {
                    ledString = ledString.substring(0, ledString.indexOf(":"));
                }
                int ledPin = Integer.parseInt(ledString.trim());
                props.put(NOTIFICATION_TYPE, StatusNotificationTypeEnum.LED);
                props.put("led", ledPin);
            } catch (Exception ex) {
                // Do nothing
            }
        } else if (urlImage.startsWith(LINUX_LED)) {
            String ledPath = urlImage.replace(LINUX_LED, "");
            props.put(NOTIFICATION_TYPE, StatusNotificationTypeEnum.LED);
            props.put("linux_led", ledPath);
        } else if (urlImage.startsWith(LOG)) {
            props.put(NOTIFICATION_TYPE, StatusNotificationTypeEnum.LOG);
        } else if (urlImage.startsWith(NONE)) {
            props.put(NOTIFICATION_TYPE, StatusNotificationTypeEnum.NONE);
        }
        return props;
    }
}
