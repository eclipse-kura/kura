/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.devel.driver.dummy;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.channel.ChannelFlag.FAILURE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.devel.DataTypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyDriver implements Driver, ConfigurableComponent {

    static final ChannelStatus SUCCESS = new ChannelStatus(ChannelFlag.SUCCESS);

    private static final Logger logger = LoggerFactory.getLogger(DummyDriver.class);

    private final Map<String, TypedValue<?>> values = new HashMap<>();
    private final ChannelListenerManager channelListenerManager = new ChannelListenerManager(this);
    private final ConnectionManager connectionManager = new ConnectionManager();

    private DummyDriverOptions options;

    public void activate(Map<String, Object> properties) {
        logger.info("activating...");

        updated(properties);
        this.channelListenerManager.start();

        logger.info("activating...done");
    }

    public void deactivate() {
        logger.info("deactivating...");

        connectionManager.shutdown();
        this.channelListenerManager.shutdown();

        logger.info("deactivating...done");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updating..");

        values.clear();
        this.options = new DummyDriverOptions(properties);

        this.connectionManager.setOptions(options);

        // some drivers might need to reconnect due to changes to connection parameters
        // in configuration, driver should not block the configuration update thread
        this.connectionManager.reconnectAsync();

        logger.info("updating...done");
    }

    @Override
    public void connect() throws ConnectionException {
        connectionManager.connectSync();
    }

    @Override
    public void disconnect() throws ConnectionException {
        connectionManager.disconnectSync();
    }

    @Override
    public ChannelDescriptor getChannelDescriptor() {
        final ChannelDescriptorIssue issue = this.options.getChannelDescriptorIssues();

        if (issue == ChannelDescriptorIssue.NONE) {
            return DummyChannelDescriptor.instance();
        } else if (issue == ChannelDescriptorIssue.THROW) {
            throw new IllegalArgumentException();
        } else if (issue == ChannelDescriptorIssue.RETURN_INVALID_OBJECT) {
            return () -> "invalid object";
        }

        return null;
    }

    @Override
    public void registerChannelListener(Map<String, Object> channelConfig, ChannelListener listener)
            throws ConnectionException {
        this.channelListenerManager.registerChannelListener(channelConfig, listener);
        // the driver should try to connect to the remote device and start sending notifications to the listener,
        // but it should avoid performing blocking operations in this method
        this.connectionManager.connectAsync();
    }

    @Override
    public void unregisterChannelListener(ChannelListener listener) throws ConnectionException {
        this.channelListenerManager.unregisterChannelListener(listener);
    }

    @Override
    public void read(List<ChannelRecord> records) throws ConnectionException {
        logger.debug("reading...");

        // read() should trigger a connect()
        connect();

        for (final ChannelRecord record : records) {
            try {
                logger.debug("channel name: {}", record.getChannelName());
                logger.debug("channel config: {}", record.getChannelConfig());

                record.setValue(readInternal(new ReadRequest(record)));
                record.setChannelStatus(SUCCESS);
            } catch (Exception e) {
                record.setChannelStatus(new ChannelStatus(FAILURE, "failed to read channel", e));
            } finally {
                record.setTimestamp(System.currentTimeMillis());
            }
        }

        logger.debug("reading...done");
    }

    @Override
    public void write(List<ChannelRecord> records) throws ConnectionException {
        logger.debug("writing...");

        // write() should trigger a connect()
        connect();

        for (final ChannelRecord record : records) {
            try {
                final String channelName = record.getChannelName();
                final TypedValue<?> value = requireNonNull(record.getValue(), "supplied value cannot be null");

                logger.debug("channel name: {}", channelName);
                logger.debug("value: {}", value);

                values.put(record.getChannelName(), record.getValue());
                record.setChannelStatus(SUCCESS);
            } catch (Exception e) {
                record.setChannelStatus(new ChannelStatus(FAILURE, "failed to write channel", e));
            } finally {
                record.setTimestamp(System.currentTimeMillis());
            }
        }

        logger.debug("writing...done");
    }

    @Override
    public PreparedRead prepareRead(final List<ChannelRecord> records) {
        final PreparedReadIssue issue = this.options.getPreparedReadIssues();

        if (issue == PreparedReadIssue.NONE) {
            return new PreparedReadImpl(records);
        } else if (issue == PreparedReadIssue.THROW) {
            throw new NullPointerException();
        }

        return null;
    }

    TypedValue<?> readInternal(final BaseRequest request) {

        final TypedValue<?> writtenValue = this.values.get(request.channelName);

        if (writtenValue == null) {
            return request.valueFromConfig;
        } else if (writtenValue.getType() == request.valueFromConfig.getType()) {
            return writtenValue;
        }

        throw new IllegalArgumentException("Channel " + request.channelName
                + " has been previously written but type does not match type in request");
    }

    boolean isConnected() {
        return this.connectionManager.isConnected();
    }

    private class PreparedReadImpl implements PreparedRead {

        private final List<ChannelRecord> records;
        private final List<ReadRequest> validRequests;

        public PreparedReadImpl(final List<ChannelRecord> records) {
            // A driver can perform optimizations here to allow faster execution of
            // the read request. At minimum it should validate the supplied records
            // during the preparedRead() method, and convert the request parameters
            // into a more efficient representation than the channel configuration map
            this.records = records;
            this.validRequests = new ArrayList<>(records.size());
            for (final ChannelRecord record : records) {
                try {
                    // records with valid configuration will be processed during the execute() method
                    validRequests.add(new ReadRequest(record));
                } catch (Exception e) {
                    // requests with invalid configuration can be immediately marked as failed
                    // invalid records should be returned by execute() anyway
                    record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, e.getMessage(), e));
                    record.setTimestamp(System.currentTimeMillis());
                }
            }
        }

        @Override
        public void close() throws Exception {
            // no need to close anything
        }

        @Override
        public List<ChannelRecord> execute() throws ConnectionException, KuraException {
            connect();

            for (final ReadRequest request : validRequests) {
                try {
                    request.record.setValue(readInternal(request));
                    request.record.setChannelStatus(SUCCESS);
                } catch (Exception e) {
                    request.record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, e.getMessage(), e));
                } finally {
                    request.record.setTimestamp(System.currentTimeMillis());
                }
            }
            // returns all records, not only the ones with valid configuration
            return records;
        }

        @Override
        public List<ChannelRecord> getChannelRecords() {
            return records;
        }

    }

    static class BaseRequest {

        // these parameters can be retrieved from a ChannelRecord in a more convenient
        // way using the getChannelName() and getValueType() methods, however, when
        // a ChannelListener is registered a raw map is passed as argument
        private static final String CHANNEL_NAME_PROPERTY_KEY = "+name";
        private static final String CHANNEL_VALUE_TYPE_PROPERY_KEY = "+value.type";

        final String channelName;
        final TypedValue<?> valueFromConfig;

        public BaseRequest(final Map<String, Object> channelConfig) {
            this.channelName = (String) channelConfig.get(CHANNEL_NAME_PROPERTY_KEY);
            final DataType valueType = DataType.valueOf((String) channelConfig.get(CHANNEL_VALUE_TYPE_PROPERY_KEY));
            this.valueFromConfig = DataTypeHelper.parseTypedValue(valueType,
                    DummyChannelDescriptor.getValue(channelConfig));
        }
    }

    static final class ReadRequest extends BaseRequest {

        final ChannelRecord record;

        public ReadRequest(final ChannelRecord record) {
            super(record.getChannelConfig());

            this.record = record;
        }
    }
}
