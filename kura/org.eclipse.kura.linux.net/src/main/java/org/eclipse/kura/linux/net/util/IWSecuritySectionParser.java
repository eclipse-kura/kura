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

package org.eclipse.kura.linux.net.util;

import java.util.EnumSet;

import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumes AP property lines from iw scan command.
 */
class IWSecuritySectionParser {

    private static final Logger logger = LoggerFactory.getLogger(IWSecuritySectionParser.class);

    private boolean foundGroup = false;
    private boolean foundPairwise = false;
    private boolean foundAuthSuites = false;

    private final EnumSet<WifiSecurity> security = EnumSet.noneOf(WifiSecurity.class);

    public EnumSet<WifiSecurity> getWifiSecurityFlags() {
        return this.security;
    }

    /**
     * @param line
     *            A trimmed line from a subsection in iw scan.
     * @return true if we've got all of the security information for this parser
     */
    public boolean parsePropLine(String line) {
        if (line.contains("Group cipher:")) {
            this.foundGroup = true;
            if (line.contains("CCMP")) {
                this.security.add(WifiSecurity.GROUP_CCMP);
            }
            if (line.contains("TKIP")) {
                this.security.add(WifiSecurity.GROUP_TKIP);
            }
            if (line.contains("WEP104")) {
                this.security.add(WifiSecurity.GROUP_WEP104);
            }
            if (line.contains("WEP40")) {
                this.security.add(WifiSecurity.GROUP_WEP40);
            }
        } else if (line.contains("Pairwise ciphers:")) {
            this.foundPairwise = true;
            if (line.contains("CCMP")) {
                this.security.add(WifiSecurity.PAIR_CCMP);
            }
            if (line.contains("TKIP")) {
                this.security.add(WifiSecurity.PAIR_TKIP);
            }
            if (line.contains("WEP104")) {
                this.security.add(WifiSecurity.PAIR_WEP104);
            }
            if (line.contains("WEP40")) {
                this.security.add(WifiSecurity.PAIR_WEP40);
            }
        } else if (line.contains("Authentication suites:")) {
            this.foundAuthSuites = true;
            if (line.contains("802_1X")) {
                this.security.add(WifiSecurity.KEY_MGMT_802_1X);
            }
            if (line.contains("PSK")) {
                this.security.add(WifiSecurity.KEY_MGMT_PSK);
            }
        } else {
            logger.debug("Ignoring line in section: {}", line);
        }

        return this.foundGroup && this.foundPairwise && this.foundAuthSuites;
    }
}
