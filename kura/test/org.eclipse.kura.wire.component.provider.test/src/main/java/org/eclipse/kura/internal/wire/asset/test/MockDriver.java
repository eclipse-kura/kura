/*******************************************************************************
 * Copyright (c) 2022, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.asset.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.type.TypedValue;

class MockDriver implements Driver {

    private final Map<String, List<TypedValue<?>>> values = new HashMap<>();
    final Map<String, ChannelListener> listeners = new HashMap<>();
    CompletableFuture<Void> preparedReadCalled = new CompletableFuture<>();

    @Override
    public void connect() throws ConnectionException {
    }

    @Override
    public void disconnect() throws ConnectionException {
    }

    @Override
    public ChannelDescriptor getChannelDescriptor() {

        return new ChannelDescriptor() {

            @Override
            public Object getDescriptor() {
                return Collections.emptyList();
            }
        };
    }

    @Override
    public void read(List<ChannelRecord> records) throws ConnectionException {
        for (final ChannelRecord record : records) {
            final Optional<TypedValue<?>> value = Optional.ofNullable(values.get(record.getChannelName()))
                    .flatMap(l -> {
                        if (l.isEmpty()) {
                            return Optional.empty();
                        } else {
                            return Optional.of(l.remove(0));
                        }
                    });

            if (value.isPresent()) {
                record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
                record.setValue(value.get());
            } else {
                record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE));
            }

            record.setTimestamp(System.currentTimeMillis());
        }

    }

    @Override
    public synchronized void registerChannelListener(Map<String, Object> channelConfig, ChannelListener listener)
            throws ConnectionException {
        synchronized (listeners) {
            listeners.put((String) channelConfig.get("+name"), listener);
            listeners.notifyAll();
        }
    }

    @Override
    public void unregisterChannelListener(ChannelListener listener) throws ConnectionException {
        synchronized (listeners) {
            final Iterator<Entry<String, ChannelListener>> iter = listeners.entrySet().iterator();

            while (iter.hasNext()) {
                final Entry<String, ChannelListener> e = iter.next();

                if (e.getValue() == listener) {
                    iter.remove();
                }
            }
        }
    }

    @Override
    public void write(List<ChannelRecord> records) throws ConnectionException {
    }

    synchronized void addReadResult(final String channelName, final TypedValue<?> value) {
        this.values.computeIfAbsent(channelName, a -> new ArrayList<>()).add(value);
    }

    synchronized void emitChannelEvent(final String channelName, final TypedValue<?> value) {
        for (final Entry<String, ChannelListener> e : listeners.entrySet()) {

            if (!e.getKey().equals(channelName)) {
                continue;
            }

            final ChannelRecord record = ChannelRecord.createReadRecord(channelName, value.getType());
            record.setValue(value);
            record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
            record.setTimestamp(System.currentTimeMillis());

            e.getValue().onChannelEvent(new ChannelEvent(record));
        }
    }

    @Override
    public PreparedRead prepareRead(List<ChannelRecord> records) {
        preparedReadCalled.complete(null);

        throw new UnsupportedOperationException();
    }

}