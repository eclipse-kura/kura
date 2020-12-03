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
package org.eclipse.kura.web.shared.model;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GwtWifiWirelessModeModel extends KuraBaseModel {

    private static final Logger logger = Logger.getLogger(GwtWifiWirelessModeModel.class.getSimpleName());

    private static final long serialVersionUID = -6095963356000494663L;

    public static final String NAME = "name";
    public static final String MODE = "mode";
    public static final String TOOLTIP = "tooltip";

    protected GwtWifiWirelessModeModel() {

    }

    public GwtWifiWirelessModeModel(GwtWifiWirelessMode mode, String name, String tooltip) {
        set(MODE, mode.name());
        set(NAME, name);
        set(TOOLTIP, tooltip);
    }

    public String getName() {
        return get(NAME);
    }

    public GwtWifiWirelessMode getMode() {
        GwtWifiWirelessMode mode = null;
        String modeStr = get(MODE);

        try {
            mode = GwtWifiWirelessMode.valueOf(modeStr);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting Wifi Wireless Mode.", e);
        }

        return mode;
    }

    public String getTooltip() {
        return get(TOOLTIP);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof GwtWifiWirelessModeModel)) {
            return false;
        }

        GwtWifiWirelessModeModel other = (GwtWifiWirelessModeModel) obj;

        if (getMode() != null) {
            if (!getMode().equals(other.getMode())) {
                return false;
            }
        } else if (other.getMode() != null) {
            return false;
        }

        if (getTooltip() != null) {
            if (!getTooltip().equals(other.getTooltip())) {
                return false;
            }
        } else if (other.getTooltip() != null) {
            return false;
        }

        return true;
    }
}
