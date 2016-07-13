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
package org.eclipse.kura.wire.internal;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.asset.internal.AssetOptions.ASSET_DESC_PROP;
import static org.eclipse.kura.asset.internal.AssetOptions.ASSET_DRIVER_PROP;
import static org.eclipse.kura.asset.internal.AssetOptions.ASSET_ID_PROP;
import static org.eclipse.kura.asset.internal.AssetOptions.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.asset.internal.AssetOptions.CHANNEL_PROPERTY_PREFIX;
import static org.eclipse.kura.asset.internal.AssetOptions.DRIVER_PROPERTY_PREFIX;
import static org.eclipse.kura.asset.internal.BaseChannelDescriptor.NAME;
import static org.eclipse.kura.asset.internal.BaseChannelDescriptor.TYPE;
import static org.eclipse.kura.asset.internal.BaseChannelDescriptor.VALUE_TYPE;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.Assets;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.ChannelDescriptor;
import org.eclipse.kura.asset.ChannelType;
import org.eclipse.kura.asset.internal.BaseAsset;
import org.eclipse.kura.asset.internal.BaseChannelDescriptor;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.Wires;
import org.eclipse.kura.wire.timer.Timer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * The Class WireAsset is a wire component which provides all necessary higher
 * level abstractions of a Kura asset. This wire asset is an integral wire
 * component in Kura Wires topology as it represents an industrial device with a
 * field protocol driver associated to it.
 *
 * The WireRecord to be emitted by every wire asset comprises the following keys
 *
 * <ul>
 * <li>channel_name</li>
 * <li>asset_flag</li>
 * <li>timestamp</li>
 * <li>value</li>
 * </ul>
 *
 * @see BaseAsset
 */
public final class WireAsset extends BaseAsset implements WireEmitter, WireReceiver, SelfConfiguringComponent {

	/** Configuration PID Property */
	private static final String CONF_PID = "org.eclipse.kura.wire.WireAsset";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(WireAsset.class);

	/** Wire Supporter Component */
	private final WireSupport m_wireSupport;

	/**
	 * Instantiate a new wire asset.
	 */
	public WireAsset() {
		super();
		this.m_wireSupport = Wires.newWireSupport(this);
	}

	/** {@inheritDoc} */
	@Override
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.debug(s_message.activatingWireAsset());
		super.activate(componentContext, properties);
		s_logger.debug(s_message.activatingWireAssetDone());
	}

	/**
	 * Clones provided Attribute Definition by prepending the provided prefix.
	 *
	 * @param oldAd
	 *            the old Attribute Definition
	 * @param prefix
	 *            the prefix to be prepended
	 * @return the new attribute definition
	 * @throws KuraRuntimeException
	 *             if any of the provided argument is null
	 */
	private Tad cloneAd(final Tad oldAd, final String prefix) {
		checkNull(oldAd, s_message.oldAdNonNull());
		checkNull(prefix, s_message.adPrefixNonNull());
		String pref = prefix;
		if ((oldAd.getId() != ASSET_DESC_PROP) || (oldAd.getId() != ASSET_DRIVER_PROP)
				|| (oldAd.getId() != ASSET_ID_PROP) || (oldAd.getId() != NAME) || (oldAd.getId() != TYPE)
				|| (oldAd.getId() != VALUE_TYPE)) {
			pref = prefix + DRIVER_PROPERTY_PREFIX;
		}
		final Tad result = new Tad();
		result.setId(pref + oldAd.getId());
		result.setName(pref + oldAd.getName());
		result.setCardinality(oldAd.getCardinality());
		result.setType(Tscalar.fromValue(oldAd.getType().value()));
		result.setDescription(oldAd.getDescription());
		result.setDefault(oldAd.getDefault());
		result.setMax(oldAd.getMax());
		result.setMin(oldAd.getMin());
		result.setRequired(oldAd.isRequired());
		for (final Option option : oldAd.getOption()) {
			final Toption newOption = new Toption();
			newOption.setLabel(option.getLabel());
			newOption.setValue(option.getValue());
			result.getOption().add(newOption);
		}
		return result;

	}

	/** {@inheritDoc} */
	@Override
	public void consumersConnected(final Wire[] wires) {
		this.m_wireSupport.consumersConnected(wires);
	}

	/**
	 * Callback used when this service component is deactivating
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

		final List<WireRecord> wireRecords = Lists.newArrayList();
		for (final AssetRecord assetRecord : assetRecords) {
			final WireField channelWireField = Wires.newWireField(s_message.channelName(),
					TypedValues.newStringValue(assetRecord.getChannelName()));
			final WireField assetFlagWireField = Wires.newWireField(s_message.assetFlag(),
					TypedValues.newStringValue(assetRecord.getAssetFlag().name()));
			final WireField timestampWireField = Wires.newWireField(s_message.timestamp(),
					TypedValues.newLongValue(assetRecord.getTimestamp()));
			final WireField valueWireField = Wires.newWireField(s_message.value(), assetRecord.getValue());
			final WireRecord wireRecord = Wires.newWireRecord(new Timestamp(new Date().getTime()),
					Lists.newArrayList(channelWireField, assetFlagWireField, timestampWireField, valueWireField));
			wireRecords.add(wireRecord);
		}
		this.m_wireSupport.emit(wireRecords);
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		final String componentName = this.m_context.getProperties().get(SERVICE_PID).toString();

		final Tocd mainOcd = new Tocd();
		mainOcd.setId(CONF_PID);
		mainOcd.setName(s_message.ocdName());
		mainOcd.setDescription(s_message.ocdDescription());

		final Tad assetNameAd = new Tad();
		assetNameAd.setId(ASSET_ID_PROP);
		assetNameAd.setName(ASSET_ID_PROP);
		assetNameAd.setCardinality(0);
		assetNameAd.setType(Tscalar.STRING);
		assetNameAd.setDescription(s_message.name());
		assetNameAd.setRequired(true);

		final Tad assetDescriptionAd = new Tad();
		assetDescriptionAd.setId(ASSET_DESC_PROP);
		assetDescriptionAd.setName(ASSET_DESC_PROP);
		assetDescriptionAd.setCardinality(0);
		assetDescriptionAd.setType(Tscalar.STRING);
		assetDescriptionAd.setDescription(s_message.description());
		assetDescriptionAd.setRequired(true);

		final Tad driverNameAd = new Tad();
		driverNameAd.setId(ASSET_DRIVER_PROP);
		driverNameAd.setName(ASSET_DRIVER_PROP);
		driverNameAd.setCardinality(0);
		driverNameAd.setType(Tscalar.STRING);
		driverNameAd.setDescription(s_message.driverName());
		driverNameAd.setRequired(true);

		mainOcd.addAD(assetDescriptionAd);
		mainOcd.addAD(assetNameAd);
		mainOcd.addAD(driverNameAd);

		final Map<String, Object> props = Maps.newHashMap();

		for (final Map.Entry<String, Object> entry : this.m_properties.entrySet()) {
			props.put(entry.getKey(), entry.getValue());
		}
		ChannelDescriptor channelDescriptor = null;
		if (this.m_driver != null) {
			channelDescriptor = this.m_driver.getChannelDescriptor();
		}
		if (channelDescriptor != null) {
			List<Tad> driverSpecificChannelConfiguration = null;
			final Object descriptor = channelDescriptor.getDescriptor();
			if (descriptor instanceof List<?>) {
				driverSpecificChannelConfiguration = (List<Tad>) descriptor;
			}
			if (driverSpecificChannelConfiguration != null) {
				final ChannelDescriptor basicChanneldescriptor = new BaseChannelDescriptor();
				List<Tad> channelConfiguration = null;
				final Object baseChannelDescriptor = basicChanneldescriptor.getDescriptor();
				if (baseChannelDescriptor instanceof List<?>) {
					channelConfiguration = (List<Tad>) baseChannelDescriptor;
				}
				if (channelConfiguration != null) {
					channelConfiguration.addAll(driverSpecificChannelConfiguration);
				}
				for (final Tad attribute : channelConfiguration) {
					for (final String prefix : this.retrieveChannelPrefixes(this.m_assetConfiguration.getChannels())) {
						final Tad newAttribute = this.cloneAd(attribute, prefix);
						mainOcd.addAD(newAttribute);
					}
				}
			}
		}
		return new ComponentConfigurationImpl(componentName, mainOcd, props);
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return this.m_assetConfiguration.getName();
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

		final List<AssetRecord> assetRecordsToWriteChannels = Lists.newArrayList();
		final List<String> channelsToRead = Lists.newArrayList();
		final Map<Long, Channel> channels = this.m_assetConfiguration.getChannels();
		// determining channels to read
		for (final Map.Entry<Long, Channel> channelEntry : channels.entrySet()) {
			final Channel channel = channelEntry.getValue();
			if ((channel.getType() == ChannelType.READ) || (channel.getType() == ChannelType.READ_WRITE)) {
				channelsToRead.add(channel.getName());
			}
		}
		checkCondition(wireEnvelope.getRecords().isEmpty(), s_message.wireRecordsNonEmpty());
		if (wireEnvelope.getRecords().get(0).getFields().get(0).getName().equals(Timer.TIMER_EVENT_FIELD_NAME)) {
			// perform the read operation on timer event receive
			try {
				final List<AssetRecord> recentlyReadRecords = this.read(channelsToRead);
				this.emitAssetRecords(recentlyReadRecords);
			} catch (final KuraException e) {
				s_logger.error(s_message.errorPerformingRead() + Throwables.getStackTraceAsString(e));
			}
		}
		// determining channels to write
		for (final WireRecord wireRecord : wireEnvelope.getRecords()) {
			for (final WireField wireField : wireRecord.getFields()) {
				for (final Map.Entry<Long, Channel> channelEntry : channels.entrySet()) {
					final Channel channel = channelEntry.getValue();
					if ((channel.getType() == ChannelType.WRITE) || (channel.getType() == ChannelType.READ_WRITE)) {
						assetRecordsToWriteChannels.add(this.prepareAssetRecord(channel, wireField.getValue()));
					}
				}
			}
		}
		// perform the write operation
		try {
			this.write(assetRecordsToWriteChannels);
		} catch (final KuraException e) {
			s_logger.error(s_message.errorPerformingWrite() + Throwables.getStackTraceAsString(e));
		}
	}

	/** {@inheritDoc} */
	@Override
	public Object polled(final Wire wire) {
		return this.m_wireSupport.polled(wire);
	}

	/**
	 * Create an asset record from the provided channel information
	 *
	 * @param channel
	 *            the channel to get the values from
	 * @return the asset record
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private AssetRecord prepareAssetRecord(final Channel channel, final TypedValue<?> value) {
		checkNull(channel, s_message.channelNonNull());
		checkNull(value, s_message.valueNonNull());

		final AssetRecord assetRecord = Assets.newAssetRecord(channel.getName());
		assetRecord.setValue(value);
		return assetRecord;
	}

	/** {@inheritDoc} */
	@Override
	public void producersConnected(final Wire[] wires) {
		this.m_wireSupport.producersConnected(wires);
	}

	/**
	 * Retrieves the set of prefixes of the channels from the map of channels
	 *
	 * @param channels
	 *            the properties to parse
	 * @return the list of channel IDs
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private Set<String> retrieveChannelPrefixes(final Map<Long, Channel> channels) {
		checkNull(channels, s_message.propertiesNonNull());
		final Set<String> channelPrefixes = Sets.newHashSet();
		for (final Map.Entry<Long, Channel> entry : channels.entrySet()) {
			final Long key = entry.getKey();
			final String prefix = key + CHANNEL_PROPERTY_POSTFIX + CHANNEL_PROPERTY_PREFIX + CHANNEL_PROPERTY_POSTFIX;
			channelPrefixes.add(prefix);
		}
		return channelPrefixes;
	}

	/** {@inheritDoc} */
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
