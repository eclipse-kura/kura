/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.asset;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.channel.ChannelType.READ_WRITE;
import static org.eclipse.kura.channel.ChannelType.WRITE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.provider.BaseAsset;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class WireAsset is a wire component which provides all necessary higher
 * level abstractions of a Kura asset. This wire asset is an integral wire
 * component in Kura Wires topology as it represents an industrial device with a
 * field protocol driver associated to it.<br/>
 * <br/>
 *
 * The WireRecord to be emitted by every wire asset comprises the following keys
 *
 * <ul>
 * <li>{@code <channelName>}</li>
 * <li>{@code <channelName>_assetName}</li>
 * <li>{@code <channelName>_timestamp}</li>
 * </ul>
 *
 * For example, if the processing of data of a channel with a name of {@code LED} becomes
 * <b>successful</b>, the data will be as follows:
 *
 * <pre>
 * 1. LED = true
 * 2. LED_assetName = MODICON_PLC
 * 4. LED_timestamp = 201648274712
 * </pre>
 *
 * <br/>
 * Also note that, if the channel name is equal to the received value of the
 * channel wire field name, then it would be considered as a WRITE wire field
 * value to the specific channel. <br/>
 * <br/>
 * For instance, {@code A} asset sends a {@link WireRecord} to {@code B} asset and the
 * received {@link WireRecord} contains list of Wire Fields. If there exists a Wire
 * Field which signifies the channel name and if this channel name also exists
 * in {@code B}'s list of configured channels, then the Wire Field which
 * contains the typed value of this channel in the received {@link WireRecord} will be
 * considered as a WRITE Value in that specific channel in B and this value will
 * be written to {@code B}'s channel
 *
 * @see Channel
 * @see ChannelRecord
 * @see WireRecord
 * @see Asset
 */
public final class WireAsset extends BaseAsset implements WireEmitter, WireReceiver {

    private static final Logger logger = LogManager.getLogger(WireAsset.class);

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    private WireAssetOptions options = new WireAssetOptions();

    private PreparedEmit preparedEmit;

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * OSGi service component activation callback.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the service properties
     */
    @Override
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug("Activating Wire Asset...");
        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());
        super.activate(componentContext, properties);
        logger.debug("Activating Wire Asset...Done");
    }

    /**
     * OSGi service component update callback.
     *
     * @param properties
     *            the service properties
     */
    @Override
    public void updated(final Map<String, Object> properties) {
        logger.debug("Updating Wire Asset...");
        this.options = new WireAssetOptions(properties);
        super.updated(properties);
        logger.debug("Updating Wire Asset...Done");
    }

    /**
     * OSGi service component deactivate callback.
     *
     * @param context
     *            the context
     */
    @Override
    protected void deactivate(final ComponentContext context) {
        logger.debug("Deactivating Wire Asset...");
        super.deactivate(context);
        logger.debug("Deactivating Wire Asset...Done");
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    protected String getFactoryPid() {
        return CONF_PID;
    }

    /**
     * This method is triggered as soon as the wire component receives a Wire
     * Envelope. After it receives a {@link WireEnvelope}, it checks for all associated
     * channels to read and write and perform the operations accordingly. The
     * order of executions are performed the following way:
     *
     * <ul>
     * <li>Perform all read operations on associated reading channels</li>
     * <li>Perform all write operations on associated writing channels</li>
     * <ul>
     *
     * Both the aforementioned operations are performed as soon as this Wire Component
     * receives {@code Non Null} {@link WireEnvelop} from its upstream Wire Component(s).
     *
     * @param wireEnvelope
     *            the received {@link WireEnvelope}
     * @throws NullPointerException
     *             if {@link WireEnvelope} is null
     */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");

        emitAllReadChannels();

        final List<WireRecord> records = wireEnvelope.getRecords();
        for (WireRecord wireRecord : records) {
            final List<ChannelRecord> channelRecordsToWrite = determineWritingChannels(wireRecord);
            writeChannels(channelRecordsToWrite);
        }
    }

    @Override
    protected void onPreparedReadCreated(PreparedRead preparedRead) {
        this.preparedEmit = new PreparedEmit(preparedRead.getChannelRecords());
    }

    @Override
    protected void onPreparedReadReleased(PreparedRead preparedRead) {
        this.preparedEmit = null;
    }

    private void emitAllReadChannels() {
        if (hasReadChannels()) {
            try {
                emitChannelRecords(readAllChannels());
            } catch (final Exception e) {
                logger.error("Error while performing read from the Wire Asset...", e);
            }
        }
    }

    /**
     * Determine the channels to write
     *
     * @param records
     *            the list of {@link WireRecord}s to parse
     * @return list of Channel Records containing the values to be written
     * @throws NullPointerException
     *             if argument is null
     */
    private List<ChannelRecord> determineWritingChannels(final WireRecord record) {
        requireNonNull(record, "Wire Record cannot be null");

        final List<ChannelRecord> channelRecordsToWrite = CollectionUtil.newArrayList();
        final AssetConfiguration assetConfiguration = getAssetConfiguration();
        final Map<String, Channel> channels = assetConfiguration.getAssetChannels();
        for (final Entry<String, Channel> channelEntry : channels.entrySet()) {
            final Channel channel = channelEntry.getValue();
            final String channelName = channel.getName();
            final ChannelType channelType = channel.getType();

            if (channelType != WRITE && channelType != READ_WRITE) {
                continue;
            }

            Map<String, TypedValue<?>> wireRecordProperties = record.getProperties();

            if (wireRecordProperties.containsKey(channelName)) {
                final TypedValue<?> value = wireRecordProperties.get(channelName);
                if (channel.getValueType() == value.getType()) {
                    channelRecordsToWrite.add(channel.createWriteRecord(value));
                }
            }
        }
        return channelRecordsToWrite;
    }

    /**
     * Emit the provided list of channel records to the associated wires.
     *
     * @param channelRecords
     *            the list of channel records conforming to the aforementioned
     *            specification
     * @throws NullPointerException
     *             if provided records list is null
     * @throws IllegalArgumentException
     *             if provided records list is empty
     */
    private void emitChannelRecords(final List<ChannelRecord> channelRecords) {
        requireNonNull(channelRecords, "List of Channel Records cannot be null");
        if (channelRecords.isEmpty()) {
            throw new IllegalArgumentException("Channel Records cannot be empty");
        }

        final Map<String, TypedValue<?>> wireRecordProperties;

        if (this.preparedEmit != null) {
            wireRecordProperties = this.preparedEmit.execute(channelRecords);
        } else {
            wireRecordProperties = Utils.toWireRecordProperties(channelRecords, options);
        }

        try {
            wireRecordProperties.put(WireAssetConstants.PROP_ASSET_NAME.value(),
                    TypedValues.newStringValue(getKuraServicePid()));
        } catch (KuraException e) {
            logger.error("Configurations cannot be null", e);
        }

        this.wireSupport.emit(Collections.singletonList(new WireRecord(wireRecordProperties)));
    }

    /**
     * Perform Channel Write operation
     *
     * @param channelRecordsToWrite
     *            the list of {@link ChannelRecord}s
     * @throws NullPointerException
     *             if the provided list is null
     */
    private void writeChannels(final List<ChannelRecord> channelRecordsToWrite) {
        requireNonNull(channelRecordsToWrite, "List of Channel Records cannot be null");
        if (channelRecordsToWrite.isEmpty()) {
            return;
        }

        try {
            write(channelRecordsToWrite);
        } catch (final Exception e) {
            logger.error("Error while performing write from the Wire Asset...", e);
        }
    }

    private boolean isListeningChannel(final Map<String, Object> properties) {
        try {
            return Boolean.parseBoolean(properties.get(WireAssetConstants.LISTEN_PROP_NAME.value()).toString());
        } catch (Exception e) {
            logger.warn("Failed to retreive \"listen\" property from channel configuration");
            return false;
        }
    }

    @Override
    protected boolean isChannelListenerValid(final ChannelListenerRegistration reg, final Channel channel) {

        if (!super.isChannelListenerValid(reg, channel)) {
            return false;
        }

        final ChannelListener listener = reg.getChannelListener();

        if (!(listener instanceof EmitterChannelListener)) {
            return true;
        }

        return ((EmitterChannelListener) listener).outer() != this;
    }

    @Override
    protected void updateChannelListenerRegistrations(final Set<ChannelListenerRegistration> listeners,
            final AssetConfiguration config) {

        super.updateChannelListenerRegistrations(listeners, config);

        config.getAssetChannels().entrySet().stream().filter(e -> isListeningChannel(e.getValue().getConfiguration()))
                .map(e -> new ChannelListenerRegistration(e.getKey(), new EmitterChannelListener()))
                .forEach(listeners::add);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Tad> getAssetChannelDescriptor() {
        return (List<Tad>) WireAssetChannelDescriptor.get().getDescriptor();
    }

    @Override
    protected Tocd getOCD() {
        return new WireAssetOCD();
    }

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    private class EmitterChannelListener implements ChannelListener {

        @Override
        public void onChannelEvent(ChannelEvent event) {
            if (options.emitAllChannels()) {
                emitAllReadChannels();
            } else {
                emitChannelRecords(Collections.singletonList(event.getChannelRecord()));
            }
        }

        public WireAsset outer() {
            return WireAsset.this;
        }
    }

    private class PreparedEmit {

        private final List<ChannelRecord> preparedRecords;
        private final List<RecordFiller> recordFillers;

        PreparedEmit(final List<ChannelRecord> records) {
            this.preparedRecords = records;
            this.recordFillers = RecordFillers.create(preparedRecords, options);
        }

        Map<String, TypedValue<?>> execute(final List<ChannelRecord> channelRecords) {

            if (channelRecords != preparedRecords) {
                // driver changed the record list
                // fallback to slow mode
                return Utils.toWireRecordProperties(channelRecords, options);
            }

            return Utils.toWireRecordProperties(channelRecords, options, recordFillers);
        }

    }
}
