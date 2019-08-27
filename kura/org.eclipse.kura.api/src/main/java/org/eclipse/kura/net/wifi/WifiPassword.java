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
package org.eclipse.kura.net.wifi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class WifiPassword extends Password {

    private static final int WPA_KEY_UPPER_BOUND = 63;
    private static final int WPA_KEY_LOWER_BOUND = 8;
    private static final String WEP_KEY_ERROR_MESSAGE = "the WEP key (passwd) must be all HEX characters "
            + "(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f)";
    private static final int WEP_PASSKEY_SIZE_32 = 32;
    private static final int WEP_PASSKEY_SIZE_26 = 26;
    private static final int BASE_16 = 16;
    private static final int WEP_PASSKEY_SIZE_10 = 10;

    private static final List<Integer> WEP_SUPPORTED_SIZES = new ArrayList<>(Arrays.asList(5, 13, 16));

    /**
     * WifiPassword constructor
     *
     * @param password
     *            - WiFi password as {@link String}
     */
    public WifiPassword(String password) {
        super(password);
    }

    /**
     * WifiPassword constructor
     *
     * @param password
     *            - - WiFi password as {@link char[]}
     */
    public WifiPassword(char[] password) {
        super(password);
    }

    /**
     * Validates WiFi password
     *
     * @param wifiSecurity
     *            - WiFi security as {@link WifiSecurity}
     * @throws KuraException
     */
    @SuppressWarnings("checkstyle:magicNumber")
    public void validate(WifiSecurity wifiSecurity) throws KuraException {
        if (getPassword() == null) {
            throw KuraException.internalError("the passwd can not be null");
        }
        String passKey = new String(getPassword()).trim();
        if (wifiSecurity == WifiSecurity.SECURITY_WEP) {
            if (passKey.length() == WEP_PASSKEY_SIZE_10) {
                // check to make sure it is all hex
                try {
                    Long.parseLong(passKey, BASE_16);
                } catch (Exception e) {
                    throw KuraException.internalError(WEP_KEY_ERROR_MESSAGE);
                }
            } else if (passKey.length() == WEP_PASSKEY_SIZE_26) {
                String part1 = passKey.substring(0, 13);
                String part2 = passKey.substring(13);

                try {
                    Long.parseLong(part1, BASE_16);
                    Long.parseLong(part2, BASE_16);
                } catch (Exception e) {
                    throw KuraException.internalError(WEP_KEY_ERROR_MESSAGE);
                }
            } else if (passKey.length() == WEP_PASSKEY_SIZE_32) {
                String part1 = passKey.substring(0, 10);
                String part2 = passKey.substring(10, 20);
                String part3 = passKey.substring(20);
                try {
                    Long.parseLong(part1, BASE_16);
                    Long.parseLong(part2, BASE_16);
                    Long.parseLong(part3, BASE_16);
                } catch (Exception e) {
                    throw KuraException.internalError(WEP_KEY_ERROR_MESSAGE);
                }
            } else {
                int passKeyLenght = passKey.length();
                if (!WEP_SUPPORTED_SIZES.contains(passKeyLenght)) {
                    // not 5, 13, or 16 ASCII characters
                    throw KuraException
                            .internalError("the WEP key (passwd) must be 10, 26, or 32 HEX characters in length");
                }
            }
        } else if (wifiSecurity == WifiSecurity.SECURITY_WPA || wifiSecurity == WifiSecurity.SECURITY_WPA2
                || wifiSecurity == WifiSecurity.SECURITY_WPA_WPA2) {
            if (passKey.length() < WPA_KEY_LOWER_BOUND || passKey.length() > WPA_KEY_UPPER_BOUND) {
                throw KuraException.internalError(
                        "the WPA passphrase (passwd) must be between 8 (inclusive) and 63 (inclusive) characters in length: "
                                + passKey);
            }
        }
    }
}
