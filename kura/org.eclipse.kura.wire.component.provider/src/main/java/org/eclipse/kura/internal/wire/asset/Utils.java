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
package org.eclipse.kura.internal.wire.asset;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.type.TypedValue;

final class Utils {

    private Utils() {
    }

    static Map<String, TypedValue<?>> toWireRecordProperties(final List<ChannelRecord> channelRecords,
            final WireAssetOptions options, final List<RecordFiller> recordFillers) {
        final Iterator<ChannelRecord> recordIter = channelRecords.iterator();
        final Iterator<RecordFiller> fillerIter = recordFillers.iterator();

        final Map<String, TypedValue<?>> wireRecordProperties = new HashMap<>(channelRecords.size() * 2);

        final TimestampFiller timestampFiller = options.getTimestampMode().createFiller(wireRecordProperties);

        while (recordIter.hasNext()) {
            final ChannelRecord record = recordIter.next();
            final RecordFiller filler = fillerIter.next();

            filler.fill(wireRecordProperties, record);
            timestampFiller.processRecord(record);
        }

        timestampFiller.fillSingleTimestamp();

        return wireRecordProperties;
    }

    static Map<String, TypedValue<?>> toWireRecordProperties(final List<ChannelRecord> channelRecords,
            final WireAssetOptions options) {
        final Map<String, TypedValue<?>> wireRecordProperties = new HashMap<>(channelRecords.size() * 2);

        final TimestampFiller timestampFiller = options.getTimestampMode().createFiller(wireRecordProperties);

        for (final ChannelRecord record : channelRecords) {
            RecordFillers.create(record, options).fill(wireRecordProperties, record);
            timestampFiller.processRecord(record);
        }

        timestampFiller.fillSingleTimestamp();

        return wireRecordProperties;
    }
}
