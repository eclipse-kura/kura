/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.asset.provider.test;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;

/**
 * Stub Driver implementation required for test
 */
public final class StubDriver implements Driver {

    /** flag to check if driver is connected */
    private boolean isConnected;

    /** {@inheritDoc} */
    @Override
    public void connect() throws ConnectionException {
        this.isConnected = true;
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect() throws ConnectionException {
        this.isConnected = false;
    }

    /** {@inheritDoc} */
    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new StubChannelDescriptor();
    }

    @Override
    public void read(List<ChannelRecord> records) throws ConnectionException {
        if (!this.isConnected) {
            this.connect();
        }

        for (final ChannelRecord record : records) {
            switch (record.getValueType()) {
            case BOOLEAN:
                record.setValue(TypedValues.newBooleanValue(true));
                break;
            case FLOAT:
                record.setValue(TypedValues.newFloatValue(1f));
                break;
            case LONG:
                record.setValue(TypedValues.newLongValue(1L));
                break;
            case BYTE_ARRAY:
                record.setValue(TypedValues.newByteArrayValue("dummy".getBytes()));
                break;
            case DOUBLE:
                record.setValue(TypedValues.newDoubleValue(1.0));
                break;
            case STRING:
                record.setValue(TypedValues.newStringValue("dummy"));
                break;
            case INTEGER:
                record.setValue(TypedValues.newIntegerValue(1));
                break;
            default:
                break;
            }
            record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        }
    }

    @Override
    public void registerChannelListener(Map<String, Object> channelConfig, ChannelListener listener)
            throws ConnectionException {

        final ChannelRecord record = ChannelRecord.createWriteRecord((String) channelConfig.get("+name"),
                TypedValues.newIntegerValue(1));
        record.setChannelConfig(channelConfig);
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setTimestamp(System.currentTimeMillis());

        listener.onChannelEvent(new ChannelEvent(record));
    }

    @Override
    public void unregisterChannelListener(ChannelListener listener) throws ConnectionException {

        final ChannelRecord record = ChannelRecord.createReadRecord("unregister", DataType.BOOLEAN);
        record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        record.setTimestamp(System.currentTimeMillis());

        try {
            listener.onChannelEvent(new ChannelEvent(record));
        } catch (IllegalArgumentException e) {
            throw new ConnectionException("test");
        }
    }

    @Override
    public void write(List<ChannelRecord> records) throws ConnectionException {
        if (!this.isConnected) {
            this.connect();
        }

        for (final ChannelRecord record : records) {
            record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        }
    }

    @Override
    public PreparedRead prepareRead(List<ChannelRecord> records) {
        return null;
    }

}
