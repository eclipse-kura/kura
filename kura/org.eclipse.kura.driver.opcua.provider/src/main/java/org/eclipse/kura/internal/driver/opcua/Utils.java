/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.kura.internal.driver.opcua;

import static org.eclipse.kura.channel.ChannelFlag.FAILURE;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {

    private static final Logger logger = LoggerFactory.getLogger(OpcUaDriver.class);

    private Utils() {
    }

    public static <T> T tryExtract(final Map<String, Object> properties, final Function<Map<String, Object>, T> func,
            final String errorMessage) {
        try {
            return func.apply(properties);
        } catch (Exception e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static <U> U runSafe(final Future<U> future, final long timeout, final Consumer<Throwable> onFailure)
            throws Exception {
        try {
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if (onFailure != null) {
                onFailure.accept(e);
            }
            throw e;
        }
    }

    public static void fill(final Variant variant, final ChannelRecord record) {
        try {
            record.setValue(DataTypeMapper.map(variant.getValue(), record.getValueType()));
            record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        } catch (Exception e) {
            record.setChannelStatus(new ChannelStatus(FAILURE,
                    "Error while converting the retrieved value to the defined typed", null));
        }
        record.setTimestamp(System.currentTimeMillis());
    }

    public static void checkStatus(final StatusCode status, final ChannelRecord record) {
        try {
            if (status == null) {
                throw new IOException("Operation Result Status cannot be null");
            }
            if (status.isBad()) {
                throw new IOException(status.toString());
            }
            record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        } catch (Exception e) {
            record.setChannelStatus(new ChannelStatus(FAILURE, e.getMessage(), e));
        }
        record.setTimestamp(System.currentTimeMillis());
    }

    public static void fill(final DataValue value, final ChannelRecord record) {
        try {
            if (value == null) {
                throw new IOException("Operation Result cannot be null");
            }
            final StatusCode status = value.getStatusCode();
            if (status.isBad()) {
                throw new IOException(status.toString());
            }
            fill(value.getValue(), record);
        } catch (Exception e) {
            record.setChannelStatus(new ChannelStatus(FAILURE, e.getMessage(), e));
        }

        final DateTime sourceTime = value.getSourceTime();

        if (sourceTime != null && sourceTime.getJavaTime() > 0) {
            record.setTimestamp(sourceTime.getJavaTime());
            return;
        }

        logger.debug("Source time not available, falling back to server time");

        final DateTime serverTime = value.getServerTime();

        if (serverTime != null && serverTime.getJavaTime() > 0) {
            record.setTimestamp(serverTime.getJavaTime());
            return;
        }

        logger.debug("Server time not available, falling back to locally generated timestamp");

        record.setTimestamp(System.currentTimeMillis());
    }

    public static void forEachChunk(final int chunkSize, int size, final BiConsumer<Integer, Integer> consumer) {
        final int chunkCount = size / chunkSize + (size % chunkSize == 0 ? 0 : 1);

        for (int i = 0; i < chunkCount; i++) {
            final int start = i * chunkSize;
            final int end = Math.min(start + chunkSize, size);
            consumer.accept(start, end);
        }
    }
}
