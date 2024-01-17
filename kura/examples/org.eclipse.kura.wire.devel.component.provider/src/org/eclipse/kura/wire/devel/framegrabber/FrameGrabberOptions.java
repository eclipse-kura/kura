/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.wire.devel.framegrabber;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

public class FrameGrabberOptions {

    // Cloud Application identifier
    private static final boolean DEFAULT_ENABLE = false;
    private static final int DEFAULT_FRAME_RATE = 1;
    private static final int DEFAULT_NATIVE_FRAME_RATE = 1;
    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;
    private static final String DEFAULT_MODE = "polling";

    // Publishing Property Names
    private static final String ENABLE_PROP_NAME = "enable.acquisition";
    private static final String FRAME_RATE_PROP_NAME = "frame.rate";
    private static final String FRAME_RATE_NATIVE_PROP_NAME = "native.frame.rate";
    private static final String WIDTH_PROP_NAME = "width";
    private static final String HEIGHT_PROP_NAME = "height";
    private static final String MODE_PROP_NAME = "acquisition.mode";

    private final Map<String, Object> properties;

    FrameGrabberOptions(final Map<String, Object> properties) {
        requireNonNull(properties);
        this.properties = properties;
    }

    boolean isEnabled() {
        boolean enabled = DEFAULT_ENABLE;
        Object enabledProp = this.properties.get(ENABLE_PROP_NAME);
        if (nonNull(enabledProp) && enabledProp instanceof Boolean) {
            enabled = (boolean) enabledProp;
        }
        return enabled;
    }

    int getFrameRate() {
        int frameRate = DEFAULT_FRAME_RATE;
        Object rate = this.properties.get(FRAME_RATE_PROP_NAME);
        if (nonNull(rate) && rate instanceof Integer) {
            frameRate = (int) rate;
        }
        return frameRate;
    }

    int getNativeFrameRate() {
        int frameRate = DEFAULT_NATIVE_FRAME_RATE;
        Object rate = this.properties.get(FRAME_RATE_NATIVE_PROP_NAME);
        if (nonNull(rate) && rate instanceof Integer) {
            frameRate = (int) rate;
        }
        return frameRate;
    }

    int getFramePeriod() {
        int framePeriod = (int) ((double) (1.0 / DEFAULT_FRAME_RATE) * 1000);
        Object rate = this.properties.get(FRAME_RATE_PROP_NAME);
        if (nonNull(rate) && rate instanceof Integer) {
            framePeriod = (int) ((double) (1.0 / (int) rate) * 1000);
        }
        return framePeriod;
    }

    int getWidth() {
        int width = DEFAULT_WIDTH;
        Object rate = this.properties.get(WIDTH_PROP_NAME);
        if (nonNull(rate) && rate instanceof Integer) {
            width = (int) rate;
        }
        return width;
    }

    int getHeight() {
        int height = DEFAULT_HEIGHT;
        Object rate = this.properties.get(HEIGHT_PROP_NAME);
        if (nonNull(rate) && rate instanceof Integer) {
            height = (int) rate;
        }
        return height;
    }

    String getAcquisitionMode() {
        String mode = DEFAULT_MODE;
        Object rate = this.properties.get(MODE_PROP_NAME);
        if (nonNull(rate) && rate instanceof String) {
            mode = (String) rate;
        }
        return mode;
    }

}
