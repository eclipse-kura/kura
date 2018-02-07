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

public class WireAssetOptions {

    public static final String EMIT_ALL_CHANNELS_PROP_NAME = "emit.all.channels";
    public static final String SINGLE_TIMESTAMP_PROP_NAME = "single.timestamp";
    public static final String EMIT_ERRORS_PROP_NAME = "emit.errors";

    private boolean emitAllChannels;
    private boolean singleTimestamp;
    private boolean emitErrors;

    public WireAssetOptions() {
    }

    public WireAssetOptions(Map<String, Object> properties) {
        final Object emitAllChannels = properties.get(EMIT_ALL_CHANNELS_PROP_NAME);
        final Object singleTimestamp = properties.get(SINGLE_TIMESTAMP_PROP_NAME);
        final Object emitErrors = properties.get(EMIT_ERRORS_PROP_NAME);

        this.emitAllChannels = emitAllChannels instanceof Boolean && (Boolean) emitAllChannels;
        this.singleTimestamp = singleTimestamp instanceof Boolean && (Boolean) singleTimestamp;
        this.emitErrors = emitErrors instanceof Boolean && (Boolean) emitErrors;
    }

    public boolean emitAllChannels() {
        return emitAllChannels;
    }

    public boolean emitSingleTimestamp() {
        return singleTimestamp;
    }

    public boolean emitErrors() {
        return emitErrors;
    }
}
