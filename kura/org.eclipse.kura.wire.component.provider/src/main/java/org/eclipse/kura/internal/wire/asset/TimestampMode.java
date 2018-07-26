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
import java.util.function.Function;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

enum TimestampMode {

    NO_TIMESTAMPS(None::new),
    PER_CHANNEL(None::new),
    SINGLE_ASSET_GENERATED(AssetGenerated::new),
    SINGLE_DRIVER_GENERATED_MAX(DriverGeneratedMax::new),
    SINGLE_DRIVER_GENERATED_MIN(DriverGeneratedMin::new);

    private Function<Map<String, TypedValue<?>>, TimestampFiller> supplier;

    private TimestampMode(Function<Map<String, TypedValue<?>>, TimestampFiller> supplier) {
        this.supplier = supplier;
    }

    public TimestampFiller createFiller(final Map<String, TypedValue<?>> wireRecordProperties) {
        return this.supplier.apply(wireRecordProperties);
    }

    private static final class None implements TimestampFiller {

        public None(final Map<String, TypedValue<?>> wireRecordProperties) {
        }

        @Override
        public void processRecord(ChannelRecord record) {
            // nothing to do
        }

        @Override
        public void fillSingleTimestamp() {
            // nothing to do
        }

    }

    private static final class AssetGenerated implements TimestampFiller {

        private final Map<String, TypedValue<?>> wireRecordProperties;

        public AssetGenerated(final Map<String, TypedValue<?>> wireRecordProperties) {
            this.wireRecordProperties = wireRecordProperties;
        }

        @Override
        public void processRecord(ChannelRecord record) {
            // nothing to do
        }

        @Override
        public void fillSingleTimestamp() {
            wireRecordProperties.put(WireAssetConstants.PROP_SINGLE_TIMESTAMP_NAME.value(),
                    TypedValues.newLongValue(System.currentTimeMillis()));
        }
    }

    private static final class DriverGeneratedMax implements TimestampFiller {

        private final Map<String, TypedValue<?>> wireRecordProperties;

        private long timestamp;

        public DriverGeneratedMax(final Map<String, TypedValue<?>> wireRecordProperties) {
            this.wireRecordProperties = wireRecordProperties;
        }

        @Override
        public void processRecord(ChannelRecord record) {
            timestamp = Math.max(timestamp, record.getTimestamp());
        }

        @Override
        public void fillSingleTimestamp() {
            wireRecordProperties.put(WireAssetConstants.PROP_SINGLE_TIMESTAMP_NAME.value(),
                    TypedValues.newLongValue(timestamp));
        }
    }

    private static final class DriverGeneratedMin implements TimestampFiller {

        private final Map<String, TypedValue<?>> wireRecordProperties;

        private long timestamp = Long.MAX_VALUE;

        public DriverGeneratedMin(final Map<String, TypedValue<?>> wireRecordProperties) {
            this.wireRecordProperties = wireRecordProperties;
        }

        @Override
        public void processRecord(ChannelRecord record) {
            timestamp = Math.min(timestamp, record.getTimestamp());
        }

        @Override
        public void fillSingleTimestamp() {
            wireRecordProperties.put(WireAssetConstants.PROP_SINGLE_TIMESTAMP_NAME.value(),
                    TypedValues.newLongValue(timestamp));
        }
    }
}
