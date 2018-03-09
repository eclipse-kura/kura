/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.kura.internal.driver.opcua.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;

public class Request<T> {

    private final T params;
    private final ChannelRecord record;

    protected Request(final T params, final ChannelRecord record) {
        this.params = params;
        this.record = record;
    }

    private static <T> Optional<T> fromRecord(final ChannelRecord record, final Function<ChannelRecord, T> func) {
        try {
            return Optional.of(func.apply(record));
        } catch (Exception e) {
            record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, e.getMessage(), e));
            record.setTimestamp(System.currentTimeMillis());
            return Optional.empty();
        }
    }

    public static Optional<Request<ReadParams>> extractReadRequest(final ChannelRecord record) {
        return fromRecord(record, r -> new Request<>(new ReadParams(r.getChannelConfig()), r));
    }

    public static Optional<Request<WriteParams>> extractWriteRequest(final ChannelRecord record) {
        return fromRecord(record,
                r -> new Request<>(new WriteParams(r.getChannelConfig(), r.getValue().getValue()), r));
    }

    public static List<Request<ReadParams>> extractReadRequests(final List<ChannelRecord> records) {
        final ArrayList<Request<ReadParams>> result = new ArrayList<>(records.size());
        for (final ChannelRecord record : records) {
            final Optional<Request<ReadParams>> request = extractReadRequest(record);
            if (request.isPresent()) {
                result.add(request.get());
            }
        }
        return result;
    }

    public static List<Request<WriteParams>> extractWriteRequests(final List<ChannelRecord> records) {
        final ArrayList<Request<WriteParams>> result = new ArrayList<>(records.size());
        for (final ChannelRecord record : records) {
            final Optional<Request<WriteParams>> request = extractWriteRequest(record);
            if (request.isPresent()) {
                result.add(request.get());
            }
        }
        return result;
    }

    public T getParameters() {
        return params;
    }

    public ChannelRecord getRecord() {
        return record;
    }
}
