/*******************************************************************************
 * Copyright (c) 2016, 2023 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.camel.type;

import static org.eclipse.kura.type.TypedValues.newTypedValue;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Converter;
import org.apache.camel.TypeConverters;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;

@Converter
public final class TypeConverter implements TypeConverters {

    private static final WireRecord[] EMPTY_RECORDS = new WireRecord[0];

    @Converter
    public KuraPayload fromMap(final Map<String, ?> data) {
        if (data == null) {
            return null;
        }

        final KuraPayload result = new KuraPayload();
        result.setTimestamp(new Date());

        for (final Map.Entry<String, ?> entry : data.entrySet()) {
            result.addMetric(entry.getKey(), entry.getValue());
        }

        return result;
    }

    @Converter
    public WireRecord[] recordsFromEnvelope(final WireEnvelope envelope) {
        return recordsFromList(envelope.getRecords());
    }

    @Converter
    public WireRecord[] recordsFromRecord(final WireRecord wireRecord) {
        return new WireRecord[] { wireRecord };
    }

    @Converter
    public WireRecord[] recordsFromList(final List<WireRecord> wireRecords) {
        return wireRecords.toArray(new WireRecord[wireRecords.size()]);
    }

    @Converter
    public WireRecord[] recordsFromMap(final Map<?, ?> map) {

        if (map.isEmpty()) {
            return EMPTY_RECORDS;
        }

        final Map<String, TypedValue<?>> result = new HashMap<>();

        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            final Object keyValue = entry.getKey();
            if (keyValue == null) {
                continue;
            }

            if (entry.getValue() instanceof TypedValue<?>) {
                result.put(keyValue.toString(), (TypedValue<?>) entry.getValue());
            } else {
                result.put(keyValue.toString(), newTypedValue(entry.getValue()));
            }
        }

        return recordsFromRecord(new WireRecord(result));
    }

    @Converter
    public Map<Object, Object> mapFromRecord(final WireRecord wireRecord) {

        final Map<Object, Object> result = new HashMap<>(wireRecord.getProperties().size());

        for (final Map.Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {
            result.put(entry.getKey(), entry.getValue().getValue());
        }

        return result;

    }

    @Converter
    public static Map<Object, Object> mapFromRecords(final WireRecord[] wireRecords) {

        final Map<Object, Object> result = new HashMap<>();

        for (final WireRecord wireRecord : wireRecords) {
            for (final Map.Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {
                result.put(entry.getKey(), entry.getValue().getValue());
            }
        }

        return result;

    }
}
