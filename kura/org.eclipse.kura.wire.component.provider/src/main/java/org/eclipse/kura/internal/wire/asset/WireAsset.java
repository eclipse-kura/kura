/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.wire.asset;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.asset.ChannelType.READ;
import static org.eclipse.kura.asset.ChannelType.READ_WRITE;
import static org.eclipse.kura.asset.ChannelType.WRITE;
import static org.eclipse.kura.wire.SeverityLevel.ERROR;
import static org.eclipse.kura.wire.SeverityLevel.INFO;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetFlag;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.AssetStatus;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.provider.BaseAsset;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.SeverityLevel;
import org.eclipse.kura.wire.TimerWireField;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
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
 * <li>channel_name</li>
 * <li>asset_flag</li>
 * <li>timestamp</li>
 * <li>value</li>
 * <li>exception</li> (This Wire Field is present if and only if asset_flag is
 * set to FAILURE)
 * </ul>
 *
 * @see Asset
 */
public final class WireAsset extends BaseAsset implements WireEmitter, WireReceiver {

	/** Configuration PID Property. */
	private static final String CONF_PID = "org.eclipse.kura.wire.WireAsset";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(WireAsset.class);

	/** Localization Resource. */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The Wire Helper Service. */
	private volatile WireHelperService m_wireHelperService;

	/** Wire Supporter Component. */
	private WireSupport m_wireSupport;

	/**
	 * OSGi service component callback while activation.
	 *
	 * @param componentContext
	 *            the component context
	 * @param properties
	 *            the service properties
	 */
	@Override
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.debug(s_message.activatingWireAsset());
		super.activate(componentContext, properties);
		this.m_wireSupport = this.m_wireHelperService.newWireSupport(this);
		s_logger.debug(s_message.activatingWireAssetDone());
	}

	/**
	 * Binds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == null) {
			this.m_wireHelperService = wireHelperService;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void consumersConnected(final Wire[] wires) {
		this.m_wireSupport.consumersConnected(wires);
	}

	/**
	 * OSGi service component callback while deactivation.
	 *
	 * @param context
	 *            the context
	 */
	@Override
	protected synchronized void deactivate(final ComponentContext context) {
		s_logger.debug(s_message.deactivatingWireAsset());
		super.deactivate(context);
		s_logger.debug(s_message.deactivatingWireAssetDone());
	}

	/**
	 * Emit the provided list of asset records to the associated wires.
	 *
	 * @param assetRecords
	 *            the list of asset records conforming to the aforementioned
	 *            specification
	 * @throws KuraRuntimeException
	 *             if provided records list is null or it is empty
	 */
	private void emitAssetRecords(final List<AssetRecord> assetRecords) {
		checkNull(assetRecords, s_message.assetRecordsNonNull());
		checkCondition(assetRecords.isEmpty(), s_message.assetRecordsNonEmpty());

		final List<WireRecord> wireRecords = CollectionUtil.newArrayList();
		for (final AssetRecord assetRecord : assetRecords) {
			final AssetStatus assetStatus = assetRecord.getAssetStatus();
			final AssetFlag assetFlag = assetStatus.getAssetFlag();
			final SeverityLevel level = (assetFlag == AssetFlag.FAILURE) ? ERROR : INFO;
			final WireField channelIdWireField = new WireField(s_message.channelId(),
					TypedValues.newLongValue(assetRecord.getChannelId()), level);
			final WireField assetFlagWireField = new WireField(s_message.assetFlag(),
					TypedValues.newStringValue(assetFlag.name()), level);
			final WireField timestampWireField = new WireField(s_message.timestamp(),
					TypedValues.newLongValue(assetRecord.getTimestamp()), level);
			final WireField valueWireField = new WireField(s_message.value(), assetRecord.getValue(), level);
			WireRecord wireRecord;
			WireField errorField;
			if (level == ERROR) {
				String errorMessage = "ERROR NOT SPECIFIED";
				final Exception exception = assetStatus.getException();
				final String exceptionMsg = assetStatus.getExceptionMessage();
				if ((exception != null) && (exceptionMsg != null)) {
					errorMessage = exceptionMsg + " " + ThrowableUtil.stackTraceAsString(exception);
				} else if ((exception == null) && (exceptionMsg != null)) {
					errorMessage = exceptionMsg;
				} else if ((exception != null) && (exceptionMsg == null)) {
					errorMessage = ThrowableUtil.stackTraceAsString(exception);
				}
				errorField = new WireField(s_message.error(), TypedValues.newStringValue(errorMessage), level);
				wireRecord = new WireRecord(new Timestamp(new Date().getTime()), Arrays.asList(channelIdWireField,
						assetFlagWireField, timestampWireField, valueWireField, errorField));
			} else {
				wireRecord = new WireRecord(new Timestamp(new Date().getTime()),
						Arrays.asList(channelIdWireField, assetFlagWireField, timestampWireField, valueWireField));
			}
			wireRecords.add(wireRecord);
		}
		this.m_wireSupport.emit(wireRecords);
	}

	/** {@inheritDoc} */
	@Override
	protected String getFactoryPid() {
		return CONF_PID;
	}

	/**
	 * This method is triggered as soon as the wire component receives a Wire
	 * Envelope. After it receives a Wire Envelope, it checks for all associated
	 * channels to read and write and perform the operations accordingly. The
	 * order of executions are performed the following way:
	 *
	 * <ul>
	 * <li>Perform all read operations on associated reading channels</li>
	 * <li>Perform all write operations on associated writing channels</li>
	 * <ul>
	 *
	 * Both of the aforementioned operations are performed as soon as it timer
	 * wire component is also triggered.
	 *
	 * @param wireEnvelope
	 *            the received wire envelope
	 */
	@Override
	public void onWireReceive(final WireEnvelope wireEnvelope) {
		checkNull(wireEnvelope, s_message.wireEnvelopeNonNull());
		s_logger.debug(s_message.wireEnvelopeReceived() + this.m_wireSupport);

		// filtering list of wire records based on the provided severity level
		final List<WireRecord> records = this.m_wireSupport.filter(wireEnvelope.getRecords());
		final List<AssetRecord> assetRecordsToWriteChannels = CollectionUtil.newArrayList();
		final List<Long> channelsToRead = CollectionUtil.newArrayList();
		final Map<Long, Channel> channels = this.m_assetConfiguration.getAssetChannels();
		// determining channels to read
		for (final Map.Entry<Long, Channel> channelEntry : channels.entrySet()) {
			final Channel channel = channelEntry.getValue();
			if ((channel.getType() == READ) || (channel.getType() == READ_WRITE)) {
				channelsToRead.add(channel.getId());
			}
		}
		checkCondition(records.isEmpty(), s_message.wireRecordsNonEmpty());
		final Object field = records.get(0).getFields().get(0);
		if (field instanceof TimerWireField) {
			// perform the read operation on timer event receive
			try {
				final List<AssetRecord> recentlyReadRecords = this.read(channelsToRead);
				this.emitAssetRecords(recentlyReadRecords);
			} catch (final KuraException e) {
				s_logger.error(s_message.errorPerformingRead() + ThrowableUtil.stackTraceAsString(e));
			}
		}
		// determining channels to write
		for (final WireRecord wireRecord : records) {
			for (final WireField wireField : wireRecord.getFields()) {
				for (final Map.Entry<Long, Channel> channelEntry : channels.entrySet()) {
					final Channel channel = channelEntry.getValue();
					if ((channel.getType() == WRITE) || (channel.getType() == READ_WRITE)) {
						final String wireFieldName = wireField.getName();
						// if the channel name is equal to the received wire
						// field name, then write the wire field value to the
						// specific channel
						if (channel.getName().equalsIgnoreCase(wireFieldName)
								&& (wireField.getSeverityLevel() == INFO)) {
							assetRecordsToWriteChannels.add(this.prepareAssetRecord(channel, wireField.getValue()));
						}
					}
				}
			}
		}
		// perform the write operation
		try {
			this.write(assetRecordsToWriteChannels);
		} catch (final KuraException e) {
			s_logger.error(s_message.errorPerformingWrite() + ThrowableUtil.stackTraceAsString(e));
		}
	}

	/** {@inheritDoc} */
	@Override
	public Object polled(final Wire wire) {
		return this.m_wireSupport.polled(wire);
	}

	/**
	 * Create an asset record from the provided channel information.
	 *
	 * @param channel
	 *            the channel to get the values from
	 * @param value
	 *            the value
	 * @return the asset record
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private AssetRecord prepareAssetRecord(final Channel channel, final TypedValue<?> value) {
		checkNull(channel, s_message.channelNonNull());
		checkNull(value, s_message.valueNonNull());

		final AssetRecord assetRecord = new AssetRecord(channel.getId());
		assetRecord.setValue(value);
		return assetRecord;
	}

	/** {@inheritDoc} */
	@Override
	public void producersConnected(final Wire[] wires) {
		this.m_wireSupport.producersConnected(wires);
	}

	/**
	 * Unbinds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == wireHelperService) {
			this.m_wireHelperService = null;
		}
	}

	/**
	 * OSGi service component callback while updation.
	 *
	 * @param properties
	 *            the service properties
	 */
	@Override
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updatingWireAsset());
		super.updated(properties);
		s_logger.debug(s_message.updatingWireAssetDone());
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		this.m_wireSupport.updated(wire, value);
	}

}
