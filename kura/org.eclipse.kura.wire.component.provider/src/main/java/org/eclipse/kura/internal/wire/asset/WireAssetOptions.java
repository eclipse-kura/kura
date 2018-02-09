/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.wire.asset;

import java.util.Map;

class WireAssetOptions {

    public static final String EMIT_ALL_CHANNELS_PROP_NAME = "emit.all.channels";
    public static final String TIMESTAMP_MODE_PROP_NAME = "timestamp.mode";
    public static final String EMIT_ERRORS_PROP_NAME = "emit.errors";

    private boolean emitAllChannels;
    private TimestampMode timestampMode;
    private boolean emitErrors;

    public WireAssetOptions() {
    }

    public WireAssetOptions(Map<String, Object> properties) {
        final Object emitAllChannels = properties.get(EMIT_ALL_CHANNELS_PROP_NAME);
        final Object emitErrors = properties.get(EMIT_ERRORS_PROP_NAME);

        this.emitAllChannels = emitAllChannels instanceof Boolean && (Boolean) emitAllChannels;
        this.emitErrors = emitErrors instanceof Boolean && (Boolean) emitErrors;

        this.timestampMode = extractTimestampMode(properties);
    }

    public boolean emitAllChannels() {
        return emitAllChannels;
    }

    public TimestampMode getTimestampMode() {
        return timestampMode;
    }

    public boolean emitErrors() {
        return emitErrors;
    }

    private static TimestampMode extractTimestampMode(final Map<String, Object> properties) {
        try {
            return TimestampMode.valueOf(properties.get(TIMESTAMP_MODE_PROP_NAME).toString());
        } catch (Exception e) {
            return TimestampMode.PER_CHANNEL;
        }
    }

}
