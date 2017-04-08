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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class WifiPassword extends Password {

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
    public void validate(WifiSecurity wifiSecurity) throws KuraException {
        if (getPassword() == null) {
            throw KuraException.internalError("the passwd can not be null");
        }
        String passKey = new String(getPassword()).trim();
        if (wifiSecurity == WifiSecurity.SECURITY_WEP) {
            if (passKey.length() == 10) {
                // check to make sure it is all hex
                try {
                    Long.parseLong(passKey, 16);
                } catch (Exception e) {
                    throw KuraException.internalError(
                            "the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
                }
            } else if (passKey.length() == 26) {
                String part1 = passKey.substring(0, 13);
                String part2 = passKey.substring(13);

                try {
                    Long.parseLong(part1, 16);
                    Long.parseLong(part2, 16);
                } catch (Exception e) {
                    throw KuraException.internalError(
                            "the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
                }
            } else if (passKey.length() == 32) {
                String part1 = passKey.substring(0, 10);
                String part2 = passKey.substring(10, 20);
                String part3 = passKey.substring(20);
                try {
                    Long.parseLong(part1, 16);
                    Long.parseLong(part2, 16);
                    Long.parseLong(part3, 16);
                } catch (Exception e) {
                    throw KuraException.internalError(
                            "the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
                }
            } else if (!(passKey.length() == 5 || passKey.length() == 13 || passKey.length() == 16)) {
                // not 5, 13, or 16 ASCII characters
                throw KuraException
                        .internalError("the WEP key (passwd) must be 10, 26, or 32 HEX characters in length");
            }
        } else if (wifiSecurity == WifiSecurity.SECURITY_WPA || wifiSecurity == WifiSecurity.SECURITY_WPA2
                || wifiSecurity == WifiSecurity.SECURITY_WPA_WPA2) {
            if (passKey.length() < 8 || passKey.length() > 63) {
                throw KuraException.internalError(
                        "the WPA passphrase (passwd) must be between 8 (inclusive) and 63 (inclusive) characters in length: "
                                + passKey);
            }
        }
    }
}
