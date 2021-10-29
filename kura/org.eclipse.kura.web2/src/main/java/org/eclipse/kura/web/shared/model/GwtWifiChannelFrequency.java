/*******************************************************************************
 * Copyright (c) 2021 Sterwen-Technology and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Sterwen-Technology
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtWifiChannelFrequency extends KuraBaseModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FREQUENCY = "frequency";
    private static final String CHANNEL = "channel";
    private static final String DISABLED = "disabled";
    private static final String NO_IR = "no-ir";
    private static final String RADAR_DETECTION = "radar-detection";

    public Integer getChannel() {
        if (get(CHANNEL) != null) {
            return get(CHANNEL);
        } else {
            return 0;
        }
    }

    public void setChannel(int channel) {
        set(CHANNEL, channel);
    }

    public Integer getFrequency() {
        if (get(FREQUENCY) != null) {
            return get(FREQUENCY);
        } else {
            return 0;
        }
    }

    public void setFrequency(int frequency) {
        set(FREQUENCY, frequency);
    }

    public boolean isDisabled() {
        if (get(DISABLED) != null) {
            return get(DISABLED);
        } else {
            return false;
        }
    }

    public void setDisabled(boolean disabled) {
        set(DISABLED, disabled);
    }

    public boolean isNoIrradiation() {
        if (get(NO_IR) != null) {
            return get(NO_IR);
        } else {
            return false;
        }
    }

    public void setNoIrradiation(boolean isNoIr) {
        set(NO_IR, isNoIr);
    }

    public boolean isRadarDetection() {
        if (get(RADAR_DETECTION) != null) {
            return get(RADAR_DETECTION);
        } else {
            return false;
        }
    }

    public void setRadarDetection(boolean radarDetection) {
        set(RADAR_DETECTION, radarDetection);
    }
}
