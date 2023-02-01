/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkAdminConfigurationMessages {

    private static final Logger logger = LoggerFactory.getLogger(NetworkAdminConfigurationMessages.class);

    @SuppressWarnings("checkstyle:lineLength")
    private static final String NETWORK_ADMIN_CONFIGURATION_MESSAGES_BUNDLE = "org.eclipse.kura.net.admin.messages.NetworkAdminConfigurationMessagesBundle";

    private NetworkAdminConfigurationMessages() {

    }

    public static String getMessage(NetworkAdminConfiguration code) {
        return getLocalizedMessage(Locale.getDefault(), code);
    }

    private static String getLocalizedMessage(Locale locale, NetworkAdminConfiguration code) {

        //
        // Load the message pattern from the bundle
        String message = null;
        ResourceBundle resourceBundle = null;
        try {
            resourceBundle = ResourceBundle.getBundle(NETWORK_ADMIN_CONFIGURATION_MESSAGES_BUNDLE, locale);
            message = resourceBundle.getString(code.name());
            if (message == null) {
                logger.warn("Could not find Configuration Message for Locale {} and code {}", locale, code);
            }
        } catch (MissingResourceException mre) {
            // log the failure to load a message bundle
            logger.warn("Could not find Messages Bundle for Locale {}", locale);
            mre.printStackTrace();
        }

        return message;
    }
}
