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

import static java.util.Objects.nonNull;

import java.util.Map;

class WireAssetOptions {

    public static final String EMIT_ALL_CHANNELS_PROP_NAME = "emit.all.channels";
    public static final String EMIT_THREAD_COUNT_PROP_NAME = "threadCount";
    public static final String EMIT_THREAD_TIMEOUT_PROP_NAME = "threadTimeout";
    public static final String EMIT_MUTIPLE_THREAD_PROP_NAME = "emit.mutiplethread";
    public static final String TIMESTAMP_MODE_PROP_NAME = "timestamp.mode";
    public static final String EMIT_ERRORS_PROP_NAME = "emit.errors";

    private boolean emitAllChannels;
    private boolean emitMutipleThread;
    private TimestampMode timestampMode;
    private boolean emitErrors;
    private int threadCount;
    private int threadTimeout;

    public WireAssetOptions() {
    }

    public WireAssetOptions(Map<String, Object> properties) {
        final Object emitAllChannels = properties.get(EMIT_ALL_CHANNELS_PROP_NAME);
        final Object emitErrors = properties.get(EMIT_ERRORS_PROP_NAME);
        final Object emitMutipleThread = properties.get(EMIT_MUTIPLE_THREAD_PROP_NAME);
        final Object threadCount = properties.get(EMIT_THREAD_COUNT_PROP_NAME);
        final Object threadTimeout = properties.get(EMIT_THREAD_TIMEOUT_PROP_NAME);

        this.emitAllChannels = emitAllChannels instanceof Boolean && (Boolean) emitAllChannels;
        this.emitErrors = emitErrors instanceof Boolean && (Boolean) emitErrors;
        this.emitMutipleThread = emitMutipleThread instanceof Boolean && (Boolean) emitMutipleThread;
        this.threadCount = 2;
        this.threadTimeout = 10000;
        if (nonNull(threadCount) && threadCount instanceof Integer) {
            this.threadCount = (Integer) threadCount;
        }
        if (nonNull(threadTimeout) && threadTimeout instanceof Integer) {
            this.threadTimeout = (Integer) threadTimeout;
        }
        this.timestampMode = extractTimestampMode(properties);
    }

    public boolean emitAllChannels() {
        return emitAllChannels;
    }

    public boolean emitMutipleThread() {
        return emitMutipleThread;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public int getThreadTimeout() {
        return threadTimeout;
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
            return TimestampMode.NO_TIMESTAMPS;
        }
    }

}
