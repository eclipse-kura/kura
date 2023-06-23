/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.nm.status;

import java.util.List;

import org.eclipse.kura.net.wifi.WifiChannel;

public class SupportedChannelsProperties {

    private final String countryCode;
    private final List<WifiChannel> supportedChannels;

    public SupportedChannelsProperties(String countryCode, List<WifiChannel> supportedChannels) {
        this.countryCode = countryCode;
        this.supportedChannels = supportedChannels;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public List<WifiChannel> getSupportedChannels() {
        return this.supportedChannels;
    }
}
