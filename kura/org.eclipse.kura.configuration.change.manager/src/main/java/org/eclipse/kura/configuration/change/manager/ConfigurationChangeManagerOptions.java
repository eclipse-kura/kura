/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.configuration.change.manager;

import java.util.Map;

public class ConfigurationChangeManagerOptions {

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_SEND_DELAY = "send.delay";
    public static final boolean DEFAULT_ENABLED = false;
    public static final long DEFAULT_SEND_DELAY = 10;

    private final boolean enabled;
    private final long sendDelay;

    public ConfigurationChangeManagerOptions(Map<String, Object> properties) {
        this.enabled = (boolean) properties.getOrDefault(KEY_ENABLED, DEFAULT_ENABLED);
        this.sendDelay = (long) properties.getOrDefault(KEY_SEND_DELAY, DEFAULT_SEND_DELAY);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public long getSendDelay() {
        return this.sendDelay;
    }

}
