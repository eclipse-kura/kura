/*******************************************************************************
 * Copyright (c) 2011, 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.status;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.Properties;

import org.eclipse.kura.core.status.GpioLedManager.GpioIdentifier;
import org.eclipse.kura.core.status.GpioLedManager.GpioName;
import org.eclipse.kura.core.status.GpioLedManager.GpioTerminal;

public class CloudConnectionStatusURL {

    public static final String NOTIFICATION_TYPE = "notification_type";
    public static final String CCS = "ccs:";
    public static final String LED = "led:";
    public static final String LED_NAME_PREFIX = "name:";
    public static final String LED_TERMINAL_PREFIX = "terminal:";
    public static final String LINUX_LED = "linux_led:";
    public static final String LOG = "log";
    public static final String NONE = "none";

    public static final String INVERTED = ":inverted";

    private static final String CASE_INSENSITIVE_PREFIX = "(?i)";
    private static final String CCS_NOTIFICATION_URLS_SEPARATOR = ";";

    private CloudConnectionStatusURL() {
    }

    public static Properties parseURL(String ccsUrl) {
        requireNonNull(ccsUrl);

        String urlImage = ccsUrl;

        Properties props = new Properties();

        if (urlImage.toLowerCase(Locale.ENGLISH).startsWith(CCS)) {
            urlImage = urlImage.replaceAll(CASE_INSENSITIVE_PREFIX + CCS, "");
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
        String urlLowerCase = urlImage.toLowerCase(Locale.ENGLISH);
        if (urlLowerCase.startsWith(LED)) {
            // Cloud Connection Status on LED
            String ledString = urlImage.substring(4);
            try {

                final boolean inverted = urlLowerCase.endsWith(INVERTED);

                if (inverted) {
                    ledString = ledString.substring(0, ledString.length() - INVERTED.length());
                }

                props.put("inverted", inverted);

                final GpioIdentifier identifier = parseLedIdentifier(ledString);

                props.put(NOTIFICATION_TYPE, StatusNotificationTypeEnum.LED);
                props.put("led", identifier);
            } catch (Exception ex) {
                // Do nothing
            }
        } else if (urlLowerCase.startsWith(LINUX_LED)) {
            String ledPath = urlImage.substring(LINUX_LED.length());
            props.put(NOTIFICATION_TYPE, StatusNotificationTypeEnum.LED);
            props.put("linux_led", ledPath);
        } else if (urlLowerCase.startsWith(LOG)) {
            props.put(NOTIFICATION_TYPE, StatusNotificationTypeEnum.LOG);
        } else if (urlLowerCase.startsWith(NONE)) {
            props.put(NOTIFICATION_TYPE, StatusNotificationTypeEnum.NONE);
        }
        return props;
    }

    private static GpioIdentifier parseLedIdentifier(final String identifier) {
        if (identifier.toLowerCase().startsWith(LED_NAME_PREFIX)) {
            final String name = identifier.substring(LED_NAME_PREFIX.length());

            if (!name.isEmpty()) {
                return new GpioName(name);
            }
        } else {
            final String number;

            if (identifier.toLowerCase().startsWith(LED_TERMINAL_PREFIX)) {
                number = identifier.substring(LED_TERMINAL_PREFIX.length());
            } else {
                number = identifier;
            }

            return new GpioTerminal(Integer.parseInt(number.trim()));
        }

        throw new IllegalArgumentException("invalid GPIO identifier " + identifier);
    }
}
