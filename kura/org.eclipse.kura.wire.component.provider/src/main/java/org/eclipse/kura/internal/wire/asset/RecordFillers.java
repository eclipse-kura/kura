/*******************************************************************************
 * Copyright (c) 2018, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.asset;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public final class RecordFillers {

    private RecordFillers() {
    }

    public static RecordFiller create(final ChannelRecord record, final WireAssetOptions options) {

        final FillerChain result = new FillerChain();

        createInternal(record, options, result::add);

        return result;
    }

    public static List<RecordFiller> create(final List<ChannelRecord> records, final WireAssetOptions options) {
        final List<RecordFiller> result = new ArrayList<>(records.size());

        for (final ChannelRecord record : records) {
            result.add(create(record, options));
        }

        return result;
    }

    public static void fill(final ChannelRecord record, final WireAssetOptions options,
            final Map<String, TypedValue<?>> envelopeProperties) {

        createInternal(record, options, f -> f.fill(envelopeProperties, record));
    }

    private static void createInternal(final ChannelRecord record, final WireAssetOptions options,
            final Consumer<RecordFiller> consumer) {
        final ValueFiller valueFiller = new ValueFiller(record);
        consumer.accept(valueFiller);

        final boolean isPerChannel = options.getTimestampMode() == TimestampMode.PER_CHANNEL;
        final boolean emitErrors = options.emitErrors();

        if (!isPerChannel && !emitErrors) {
            return;
        }

        if (isPerChannel) {
            consumer.accept(new TimestampFiller(record));
        }

        if (emitErrors) {
            consumer.accept(new ErrorFiller(record));
        }
    }

    private static class ValueFiller implements RecordFiller {

        private final String valueKey;

        public ValueFiller(final ChannelRecord record) {
            this.valueKey = record.getChannelName();
        }

        @Override
        public void fill(Map<String, TypedValue<?>> envelopeProperties, ChannelRecord record) {
            if (record.getChannelStatus().getChannelFlag() == ChannelFlag.SUCCESS) {
                envelopeProperties.put(this.valueKey, record.getValue());
                String unit = record.getUnit();
                if (!isNull(unit) && !unit.trim().isEmpty()) {
                    envelopeProperties.put(this.valueKey + WireAssetConstants.PROP_SUFFIX_UNIT.value(),
                            new StringValue(unit));
                }
            }
        }
    }

    private static class ErrorFiller implements RecordFiller {

        private final String errorKey;

        public ErrorFiller(ChannelRecord record) {
            this.errorKey = record.getChannelName() + WireAssetConstants.PROP_SUFFIX_ERROR.value();
        }

        @Override
        public void fill(Map<String, TypedValue<?>> envelopeProperties, ChannelRecord record) {

            envelopeProperties.put(this.errorKey,
                    TypedValues.newStringValue(getErrorMessage(record.getChannelStatus())));
        }

        private String getErrorMessage(final ChannelStatus channelStatus) {
            if (channelStatus.getChannelFlag() == ChannelFlag.SUCCESS) {
                return WireAssetConstants.PROP_VALUE_NO_ERROR.value();
            }
            String errorMessage = WireAssetConstants.ERROR_NOT_SPECIFIED_MESSAGE.value();
            final Exception exception = channelStatus.getException();
            final String exceptionMsg = channelStatus.getExceptionMessage();
            if (nonNull(exception) && nonNull(exceptionMsg)) {
                errorMessage = exceptionMsg + " " + exception.toString();
            } else if (isNull(exception) && nonNull(exceptionMsg)) {
                errorMessage = exceptionMsg;
            } else if (nonNull(exception)) {
                errorMessage = exception.toString();
            }
            return errorMessage;
        }
    }

    private static class TimestampFiller implements RecordFiller {

        private final String timestampKey;

        public TimestampFiller(final ChannelRecord record) {
            this.timestampKey = record.getChannelName() + WireAssetConstants.PROP_SUFFIX_TIMESTAMP.value();
        }

        @Override
        public void fill(Map<String, TypedValue<?>> envelopeProperties, ChannelRecord record) {
            envelopeProperties.put(this.timestampKey, TypedValues.newLongValue(record.getTimestamp()));
        }
    }

    private static class FillerChain implements RecordFiller {

        private final RecordFiller[] fillers = new RecordFiller[3];
        private int len = 0;

        void add(final RecordFiller filler) {
            this.fillers[this.len] = filler;
            this.len++;
        }

        @Override
        public void fill(Map<String, TypedValue<?>> envelopeProperties, ChannelRecord record) {
            for (int i = 0; i < this.fillers.length && this.fillers[i] != null; i++) {
                this.fillers[i].fill(envelopeProperties, record);
            }
        }
    }
}
