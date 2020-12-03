/*******************************************************************************
 * Copyright (c) 2016, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.camel.type;

import static org.eclipse.kura.type.TypedValues.newTypedValue;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Converter;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;

@Converter
public final class TypeConverter {

    private static final WireRecord[] EMPTY_RECORDS = new WireRecord[0];

    private TypeConverter() {
    }

    @Converter
    public static KuraPayload fromMap(final Map<String, ?> data) {
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
    public static WireRecord[] recordsFromEnvelope(final WireEnvelope envelope) {
        return recordsFromList(envelope.getRecords());
    }

    @Converter
    public static WireRecord[] recordsFromRecord(final WireRecord record) {
        return new WireRecord[] { record };
    }

    @Converter
    public static WireRecord[] recordsFromList(final List<WireRecord> records) {
        return records.toArray(new WireRecord[records.size()]);
    }

    @Converter
    public static WireRecord[] recordsFromMap(final Map<?, ?> map) {

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
    public static Map<?, ?> mapFromRecord(final WireRecord record) {

        final Map<Object, Object> result = new HashMap<>(record.getProperties().size());

        for (final Map.Entry<String, TypedValue<?>> entry : record.getProperties().entrySet()) {
            result.put(entry.getKey(), entry.getValue().getValue());
        }

        return result;

    }

    @Converter
    public static Map<?, ?> mapFromRecords(final WireRecord[] records) {

        final Map<Object, Object> result = new HashMap<>();

        for (final WireRecord record : records) {
            for (final Map.Entry<String, TypedValue<?>> entry : record.getProperties().entrySet()) {
                result.put(entry.getKey(), entry.getValue().getValue());
            }
        }

        return result;

    }
}
