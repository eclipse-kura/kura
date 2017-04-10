/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
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
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.channel.ChannelType.READ_WRITE;
import static org.eclipse.kura.channel.ChannelType.WRITE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.provider.BaseAsset;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final String ERROR_NOT_SPECIFIED_MESSAGE = "ERROR NOT SPECIFIED";

    private static final String ASSET_NAME = "assetName";

    private static final String TIMESTAMP = "timestamp";

    private static final String PROPERTY_SEPARATOR = "_";

    /** Configuration PID Property. */
    private static final String CONF_PID = "org.eclipse.kura.wire.WireAsset";

    private static final Logger logger = LoggerFactory.getLogger(WireAsset.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

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
        logger.debug(message.activatingWireAsset());
        super.activate(componentContext, properties);
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        logger.debug(message.activatingWireAssetDone());
    }

    /**
     * OSGi service component update callback.
     *
     * @param properties
     *            the service properties
     */
    @Override
    public void updated(final Map<String, Object> properties) {
        logger.debug(message.updatingWireAsset());
        super.updated(properties);
        logger.debug(message.updatingWireAssetDone());
    }

    /**
     * OSGi service component deactivate callback.
     *
     * @param context
     *            the context
     */
    @Override
    protected void deactivate(final ComponentContext context) {
        logger.debug(message.deactivatingWireAsset());
        super.deactivate(context);
        logger.debug(message.deactivatingWireAssetDone());
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
        requireNonNull(wireEnvelope, message.wireEnvelopeNonNull());
        logger.debug(message.wireEnvelopeReceived(), this.wireSupport);

        if (hasReadChannels()) {
            try {
                emitChannelRecords(readAllChannels());
            } catch (final KuraException e) {
                logger.error(message.errorPerformingRead(), e);
            }
        }

        final List<WireRecord> records = wireEnvelope.getRecords();
        for (WireRecord wireRecord : records) {
            final List<ChannelRecord> channelRecordsToWrite = determineWritingChannels(wireRecord);
            writeChannels(channelRecordsToWrite);
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
        requireNonNull(record, message.wireRecordNonNull());

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
        requireNonNull(channelRecords, message.channelRecordsNonNull());
        if (channelRecords.isEmpty()) {
            throw new IllegalArgumentException(message.channelRecordsNonEmpty());
        }

        final Map<String, TypedValue<?>> wireRecordProperties = new HashMap<>();
        try {
            wireRecordProperties.put(ASSET_NAME, TypedValues.newStringValue(getKuraServicePid()));
        } catch (KuraException e) {
            logger.error(message.configurationNonNull(), e);
        }

        for (final ChannelRecord channelRecord : channelRecords) {
            final ChannelStatus channelStatus = channelRecord.getChannelStatus();
            final ChannelFlag channelFlag = channelStatus.getChannelFlag();
            final String channelName = channelRecord.getChannelName();

            final TypedValue<?> typedValue;
            if (channelFlag == ChannelFlag.FAILURE) {
                logErrorMessage(channelStatus);
                continue;
            } else {
                typedValue = channelRecord.getValue();
            }

            wireRecordProperties.put(channelName, typedValue);

            wireRecordProperties.put(channelName + PROPERTY_SEPARATOR + TIMESTAMP,
                    TypedValues.newLongValue(channelRecord.getTimestamp()));
        }
        final WireRecord wireRecord = new WireRecord(wireRecordProperties);
        this.wireSupport.emit(Arrays.asList(wireRecord));
    }

    private void logErrorMessage(final ChannelStatus channelStatus) {
        String errorMessage = ERROR_NOT_SPECIFIED_MESSAGE;
        final Exception exception = channelStatus.getException();
        final String exceptionMsg = channelStatus.getExceptionMessage();
        if (nonNull(exception) && nonNull(exceptionMsg)) {
            errorMessage = exceptionMsg + " " + exception.toString();
        } else if (isNull(exception) && nonNull(exceptionMsg)) {
            errorMessage = exceptionMsg;
        } else if (nonNull(exception)) {
            errorMessage = exception.toString();
        }
        logger.error(errorMessage);
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
        requireNonNull(channelRecordsToWrite, message.channelRecordsNonNull());
        if (channelRecordsToWrite.isEmpty()) {
            return;
        }

        try {
            write(channelRecordsToWrite);
        } catch (final KuraException e) {
            logger.error(message.errorPerformingWrite(), e);
        }
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
}
