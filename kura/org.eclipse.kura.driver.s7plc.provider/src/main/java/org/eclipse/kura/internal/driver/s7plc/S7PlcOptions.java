/**
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 *   Eurotech
 */

package org.eclipse.kura.internal.driver.s7plc;

import java.util.Map;

final class S7PlcOptions {

    private static final String IP_PROP_NAME = "host.ip";
    private static final String AUTHENTICATE_PROP_NAME = "authenticate";
    private static final String PASSWORD_PROP_NAME = "password";
    private static final String RACK_PROP_NAME = "rack";
    private static final String SLOT_PROP_NAME = "slot";
    private static final String MINIMUM_GAP_SIZE_PROP_NAME = "read.minimum.gap.size";

    private static final String IP_DEFAULT = "";
    private static final boolean AUTHENTICATE_DEFAULT = false;
    private static final String PASSWORD_DEFAULT = "";
    private static final int RACK_DEFAULT = 0;
    private static final int SLOT_DEFAULT = 2;
    private static final int MINIMUM_GAP_SIZE_DEFAULT = 0;

    private final Map<String, Object> properties;

    S7PlcOptions(final Map<String, Object> properties) {
        this.properties = properties;
    }

    String getIp() {
        return (String) properties.getOrDefault(IP_PROP_NAME, IP_DEFAULT);
    }

    boolean shouldAuthenticate() {
        return (Boolean) properties.getOrDefault(AUTHENTICATE_PROP_NAME, AUTHENTICATE_DEFAULT);
    }

    String getPassword() {
        return (String) properties.getOrDefault(PASSWORD_PROP_NAME, PASSWORD_DEFAULT);
    }

    int getRack() {
        return (Integer) properties.getOrDefault(RACK_PROP_NAME, RACK_DEFAULT);
    }

    int getSlot() {
        return (Integer) properties.getOrDefault(SLOT_PROP_NAME, SLOT_DEFAULT);
    }

    int getMinimumGapSize() {
        return (Integer) properties.getOrDefault(MINIMUM_GAP_SIZE_PROP_NAME, MINIMUM_GAP_SIZE_DEFAULT);
    }

}
