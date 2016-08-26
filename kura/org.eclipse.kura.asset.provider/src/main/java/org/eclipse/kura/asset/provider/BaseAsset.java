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
package org.eclipse.kura.asset.provider;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.asset.AssetConstants.ASSET_DESC_PROP;
import static org.eclipse.kura.asset.AssetConstants.ASSET_DRIVER_PROP;
import static org.eclipse.kura.asset.AssetConstants.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.asset.AssetConstants.CHANNEL_PROPERTY_PREFIX;
import static org.eclipse.kura.asset.AssetConstants.DRIVER_PROPERTY_POSTFIX;
import static org.eclipse.kura.asset.AssetConstants.NAME;
import static org.eclipse.kura.asset.AssetConstants.SEVERITY_LEVEL;
import static org.eclipse.kura.asset.AssetConstants.TYPE;
import static org.eclipse.kura.asset.AssetConstants.VALUE_TYPE;
import static org.eclipse.kura.asset.AssetFlag.FAILURE;
import static org.eclipse.kura.asset.AssetFlag.READ_SUCCESSFUL;
import static org.eclipse.kura.asset.AssetFlag.WRITE_SUCCESSFUL;
import static org.eclipse.kura.asset.ChannelType.READ;
import static org.eclipse.kura.asset.ChannelType.READ_WRITE;
import static org.eclipse.kura.asset.ChannelType.WRITE;
import static org.eclipse.kura.driver.DriverConstants.CHANNEL_ID;
import static org.eclipse.kura.driver.DriverConstants.CHANNEL_VALUE_TYPE;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetEvent;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.AssetStatus;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.listener.AssetListener;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.DriverEvent;
import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.internal.asset.AssetOptions;
import org.eclipse.kura.internal.asset.DriverTrackerCustomizer;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class BaseAsset is basic implementation of {@code Asset}.
 *
 * @see AssetOptions
 * @see AssetConfiguration
 */
public class BaseAsset implements Asset, SelfConfiguringComponent {

	/** Configuration PID Property. */
	private static final String CONF_PID = "org.eclipse.kura.asset";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(BaseAsset.class);

	/** Localization Resource. */
	private static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

	/** The provided asset configuration wrapper instance. */
	protected AssetConfiguration m_assetConfiguration;

	/** Container of mapped asset listeners and drivers listener. */
	private final Map<AssetListener, DriverListener> m_assetListeners;

	/** The provided asset options instance. */
	private AssetOptions m_assetOptions;

	/** The service component context. */
	private ComponentContext m_context;

	/** The Driver instance. */
	public volatile Driver m_driver;

	/** Asset Driver Tracker Customizer. */
	private DriverTrackerCustomizer m_driverTrackerCustomizer;

	/** Synchronization Monitor for driver specific operations. */
	private final Lock m_monitor;

	/** The configurable properties of this asset. */
	private Map<String, Object> m_properties;

	/** Asset Driver Tracker. */
	private ServiceTracker<Driver, Driver> m_serviceTracker;

	/**
	 * Instantiates a new asset instance.
	 */
	public BaseAsset() {
		this.m_assetListeners = CollectionUtil.newConcurrentHashMap();
		this.m_monitor = new ReentrantLock();
	}

	/**
	 * OSGi service component callback while activation.
	 *
	 * @param componentContext
	 *            the component context
	 * @param properties
	 *            the service properties
	 */
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.debug(s_message.activating());
		this.m_context = componentContext;
		this.m_properties = properties;
		this.retrieveConfigurationsFromProperties(properties);
		this.attachDriver(this.m_assetConfiguration.getDriverPid());
		s_logger.debug(s_message.activatingDone());
	}

	/**
	 * Tracks the Driver in the OSGi service registry with the specified driver
	 * PID.
	 *
	 * @param driverId
	 *            the identifier of the driver
	 * @throws KuraRuntimeException
	 *             if driver id provided is null
	 */
	private synchronized void attachDriver(final String driverId) {
		checkNull(driverId, s_message.driverPidNonNull());
		s_logger.debug(s_message.driverAttach());
		try {
			this.m_driverTrackerCustomizer = new DriverTrackerCustomizer(this.m_context.getBundleContext(), this,
					driverId);
			this.m_serviceTracker = new ServiceTracker<Driver, Driver>(this.m_context.getBundleContext(),
					Driver.class.getName(), this.m_driverTrackerCustomizer);
			this.m_serviceTracker.open();
		} catch (final InvalidSyntaxException e) {
			s_logger.error(ThrowableUtil.stackTraceAsString(e));
		}
		s_logger.debug(s_message.driverAttachDone());
	}

	/**
	 * Checks if the provided channel is present in the provided map.
	 *
	 * @param channelId
	 *            the name of channel
	 * @param channels
	 *            the provided container of channels
	 * @return the id of the channel if found or else 0
	 * @throws KuraRuntimeException
	 *             any of the arguments is null or the provided is empty or the
	 *             provided channel ID cannot be represented as an integer
	 */
	private long checkChannelAvailability(final long channelId, final Map<Long, Channel> channels) {
		checkNull(channelId, s_message.channelNameNonNull());
		checkNull(channels, s_message.channelsNonNull());
		checkCondition(channels.isEmpty(), s_message.channelsNonEmpty());

		for (final Map.Entry<Long, Channel> channel : channels.entrySet()) {
			final long chId = channel.getValue().getId();
			if (channelId == chId) {
				return channel.getKey();
			}
		}
		return 0;
	}

	/**
	 * Clones provided Attribute Definition by prepending the provided prefix.
	 *
	 * @param oldAd
	 *            the old Attribute Definition
	 * @param prefix
	 *            the prefix to be prepended (this will be in the format of
	 *            {@code x.CH.} or {@code x.CH.DRIVER.} where {@code x} is
	 *            channel identifier number. {@code x.CH.} will be used for the
	 *            channel specific properties except the driver specific
	 *            properties. The driver specific properties in the channel will
	 *            use the {@code x.CH.DRIVER.} prefix)
	 * @return the new attribute definition
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private Tad cloneAd(final Tad oldAd, final String prefix) {
		checkNull(oldAd, s_message.oldAdNonNull());
		checkNull(prefix, s_message.adPrefixNonNull());

		String pref = prefix;
		final String oldAdId = oldAd.getId();
		if ((oldAdId != ASSET_DESC_PROP.value()) && (oldAdId != ASSET_DRIVER_PROP.value()) && (oldAdId != NAME.value())
				&& (oldAdId != TYPE.value()) && (oldAdId != VALUE_TYPE.value())
				&& (oldAdId != SEVERITY_LEVEL.value())) {
			pref = prefix + DRIVER_PROPERTY_POSTFIX.value() + CHANNEL_PROPERTY_POSTFIX.value();
		}
		final Tad result = new Tad();
		result.setId(pref + oldAdId);
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

	/**
	 * OSGi service component callback while deactivation.
	 *
	 * @param context
	 *            the context
	 */
	protected synchronized void deactivate(final ComponentContext context) {
		s_logger.debug(s_message.deactivating());
		this.m_monitor.lock();
		try {
			if (this.m_driver != null) {
				this.m_driver.disconnect();
			}
		} catch (final ConnectionException e) {
			s_logger.error(s_message.errorDriverDisconnection() + ThrowableUtil.stackTraceAsString(e));
		} finally {
			this.m_monitor.unlock();
		}
		this.m_driver = null;
		if (this.m_serviceTracker != null) {
			this.m_serviceTracker.close();
		}
		s_logger.debug(s_message.deactivatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public AssetConfiguration getAssetConfiguration() {
		return this.m_assetConfiguration;
	}

	/**
	 * Retrieves the specific asset record by driver record from the list of
	 * provided asset records
	 *
	 * @param assetRecords
	 *            the provided list of driver records
	 * @param driverRecord
	 *            the specific driver record
	 * @return the found asset record or null
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null or the provided list is empty
	 */
	private AssetRecord getAssetRecordByDriverRecord(final List<AssetRecord> assetRecords,
			final DriverRecord driverRecord) {
		checkNull(assetRecords, s_message.assetRecordsNonNull());
		checkCondition(assetRecords.isEmpty(), s_message.assetRecordsNonEmpty());
		checkNull(driverRecord, s_message.driverRecordNonNull());

		for (final AssetRecord assetRecord : assetRecords) {
			final long channelId = Long.valueOf(driverRecord.getChannelConfig().get(CHANNEL_ID.value()).toString());
			if (channelId == assetRecord.getChannelId()) {
				return assetRecord;
			}
		}
		return null;
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		final String componentName = this.m_context.getProperties().get(ConfigurationService.KURA_SERVICE_PID)
				.toString();

		final Tocd mainOcd = new Tocd();
		mainOcd.setId(this.getFactoryPid());
		mainOcd.setName(s_message.ocdName());
		mainOcd.setDescription(s_message.ocdDescription());

		final Tad assetDescriptionAd = new Tad();
		assetDescriptionAd.setId(ASSET_DESC_PROP.value());
		assetDescriptionAd.setName(ASSET_DESC_PROP.value());
		assetDescriptionAd.setCardinality(0);
		assetDescriptionAd.setType(Tscalar.STRING);
		assetDescriptionAd.setDescription(s_message.description());
		assetDescriptionAd.setRequired(true);

		final Tad driverNameAd = new Tad();
		driverNameAd.setId(ASSET_DRIVER_PROP.value());
		driverNameAd.setName(ASSET_DRIVER_PROP.value());
		driverNameAd.setCardinality(0);
		driverNameAd.setType(Tscalar.STRING);
		driverNameAd.setDescription(s_message.driverName());
		driverNameAd.setRequired(true);

		final Tad severityLevelAd = new Tad();
		severityLevelAd.setId(SEVERITY_LEVEL.value());
		severityLevelAd.setName(SEVERITY_LEVEL.value());
		// default severity level is ERROR
		severityLevelAd.setDefault(s_message.error());
		severityLevelAd.setCardinality(0);
		severityLevelAd.setType(Tscalar.STRING);
		severityLevelAd.setDescription(s_message.driverName());
		severityLevelAd.setRequired(true);

		final Toption infoLevel = new Toption();
		infoLevel.setValue(s_message.info());
		infoLevel.setLabel(s_message.info());
		severityLevelAd.getOption().add(infoLevel);

		final Toption configLevel = new Toption();
		configLevel.setValue(s_message.error());
		configLevel.setLabel(s_message.error());
		severityLevelAd.getOption().add(configLevel);

		final Toption errorLevel = new Toption();
		errorLevel.setValue(s_message.config());
		errorLevel.setLabel(s_message.config());
		severityLevelAd.getOption().add(errorLevel);

		mainOcd.addAD(assetDescriptionAd);
		mainOcd.addAD(severityLevelAd);

		final Map<String, Object> props = CollectionUtil.newHashMap();
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
					for (final String prefix : this
							.retrieveChannelPrefixes(this.m_assetConfiguration.getAssetChannels())) {
						final Tad newAttribute = this.cloneAd(attribute, prefix);
						mainOcd.addAD(newAttribute);
					}
				}
			}
		}
		return new ComponentConfigurationImpl(componentName, mainOcd, props);
	}

	/**
	 * Return the Factory PID of the component
	 *
	 * @return the factory PID
	 */
	protected String getFactoryPid() {
		return CONF_PID;
	}

	/**
	 * Prepares the provided asset record with the relevant values from the
	 * provided asset record
	 *
	 * @param driverRecord
	 *            the provided driver record
	 * @param assetRecord
	 *            the provided asset record
	 * @throws KuraException
	 *             if any driver flag is error specific
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	private void prepareAssetRecord(final DriverRecord driverRecord, final AssetRecord assetRecord)
			throws KuraException {
		checkNull(driverRecord, s_message.driverRecordNonNull());
		checkNull(driverRecord, s_message.assetRecordNonNull());

		final DriverStatus status = driverRecord.getDriverStatus();
		final DriverFlag driverFlag = status.getDriverFlag();

		AssetStatus assetStatus;
		switch (driverFlag) {
		case READ_SUCCESSFUL:
			assetStatus = new AssetStatus(READ_SUCCESSFUL);
			assetRecord.setAssetStatus(assetStatus);
			break;
		case WRITE_SUCCESSFUL:
			assetStatus = new AssetStatus(WRITE_SUCCESSFUL);
			assetRecord.setAssetStatus(assetStatus);
			break;
		default:
			assetStatus = new AssetStatus(FAILURE, status.getExceptionMessage(), status.getException());
			assetRecord.setAssetStatus(assetStatus);
			break;
		}
		assetRecord.setTimestamp(driverRecord.getTimestamp());
		assetRecord.setValue(driverRecord.getValue());
	}

	/** {@inheritDoc} */
	@Override
	public List<AssetRecord> read(final List<Long> channelIds) throws KuraException {
		checkNull(channelIds, s_message.channelsNonNull());
		checkCondition(channelIds.isEmpty(), s_message.channelsNonEmpty());
		checkNull(this.m_driver, s_message.driverNonNull());

		s_logger.debug(s_message.readingChannels());
		final List<AssetRecord> assetRecords = CollectionUtil.newArrayList();
		final List<DriverRecord> driverRecords = CollectionUtil.newArrayList();

		final Map<Long, Channel> channels = this.m_assetConfiguration.getAssetChannels();
		for (final long channelId : channelIds) {
			final long id = this.checkChannelAvailability(channelId, channels);
			checkCondition(id == 0, s_message.channelUnavailable());

			final Channel channel = channels.get(id);
			checkCondition(!((channel.getType() == READ) || (channel.getType() == READ_WRITE)),
					s_message.channelTypeNotReadable() + channel);

			final DriverRecord driverRecord = new DriverRecord();
			// Copy the configuration of the channel and put the channel ID and
			// channel value type
			final Map<String, Object> channelConf = CollectionUtil.newHashMap(channel.getConfiguration());
			channelConf.put(CHANNEL_ID.value(), channel.getId());
			channelConf.put(CHANNEL_VALUE_TYPE.value(), channel.getValueType());
			driverRecord.setChannelConfig(channelConf);
			driverRecords.add(driverRecord);
		}

		this.m_monitor.lock();
		try {
			this.m_driver.read(driverRecords);
		} catch (final ConnectionException ce) {
			throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
		} finally {
			this.m_monitor.unlock();
		}

		for (final DriverRecord driverRecord : driverRecords) {
			final Map<String, Object> driverRecordConf = driverRecord.getChannelConfig();
			long channelId = 0;
			if (driverRecordConf.containsKey(CHANNEL_ID.value())) {
				channelId = Long.valueOf(driverRecordConf.get(CHANNEL_ID.value()).toString());
			}
			final AssetRecord assetRecord = new AssetRecord(channelId);
			this.prepareAssetRecord(driverRecord, assetRecord);
			assetRecords.add(assetRecord);
		}
		s_logger.debug(s_message.readingChannelsDone());
		return assetRecords;
	}

	/** {@inheritDoc} */
	@Override
	public void registerAssetListener(final long channelId, final AssetListener assetListener) throws KuraException {
		checkCondition(channelId <= 0, s_message.channelIdNotLessThanZero());
		checkNull(assetListener, s_message.listenerNonNull());
		checkNull(this.m_driver, s_message.driverNonNull());

		s_logger.debug(s_message.registeringListener());
		final Map<Long, Channel> channels = this.m_assetConfiguration.getAssetChannels();
		final long id = this.checkChannelAvailability(channelId, channels);
		checkCondition(id == 0, s_message.channelUnavailable());

		/**
		 * This is a basic driver listener used to listen for driver events so
		 * that it can be propagated upwards to the respective asset listener
		 *
		 * @see AssetListener
		 * @see DriverListener
		 * @see AssetEvent
		 * @see DriverEvent
		 */
		final class BaseDriverListener implements DriverListener {

			/** The asset listener instance. */
			private final AssetListener m_assetListener;

			/**
			 * Instantiates a new base driver listener.
			 *
			 * @param assetListener
			 *            the asset listener
			 * @throws KuraRuntimeException
			 *             if the argument is null
			 */
			BaseDriverListener(final AssetListener assetListener) {
				checkNull(assetListener, s_message.listenerNonNull());
				this.m_assetListener = assetListener;
			}

			/** {@inheritDoc} */
			@Override
			public void onDriverEvent(final DriverEvent event) {
				checkNull(event, s_message.driverEventNonNull());
				final DriverRecord driverRecord = event.getDriverRecord();
				final Map<String, Object> driverRecordConf = driverRecord.getChannelConfig();
				long channelId = 0;
				if (driverRecordConf.containsKey(CHANNEL_ID.value())) {
					channelId = Long.valueOf(driverRecordConf.get(CHANNEL_ID.value()).toString());
				}
				final AssetRecord assetRecord = new AssetRecord(channelId);
				try {
					BaseAsset.this.prepareAssetRecord(driverRecord, assetRecord);
				} catch (final KuraException e) {
					s_logger.error(s_message.errorPreparingAssetRecord() + ThrowableUtil.stackTraceAsString(e));
				}
				final AssetEvent assetEvent = new AssetEvent(assetRecord);
				this.m_assetListener.onAssetEvent(assetEvent);
			}
		}
		final Channel channel = channels.get(channelId);
		// Copy the configuration of the channel and put the channel ID and
		// channel value type
		final Map<String, Object> channelConf = CollectionUtil.newHashMap(channel.getConfiguration());
		channelConf.put(CHANNEL_ID.value(), channel.getId());
		channelConf.put(CHANNEL_VALUE_TYPE.value(), channel.getValueType());

		final DriverListener driverListener = new BaseDriverListener(assetListener);
		this.m_assetListeners.put(assetListener, driverListener);

		this.m_monitor.lock();
		try {
			this.m_driver.registerDriverListener(channelConf, driverListener);
		} catch (final ConnectionException ce) {
			throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
		} finally {
			this.m_monitor.unlock();
		}
		s_logger.debug(s_message.registeringListenerDone());
	}

	/**
	 * Retrieves the set of prefixes of the channels from the map of channels.
	 *
	 * @param channels
	 *            the properties to parse
	 * @return the list of channel IDs
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private Set<String> retrieveChannelPrefixes(final Map<Long, Channel> channels) {
		checkNull(channels, s_message.propertiesNonNull());
		final Set<String> channelPrefixes = CollectionUtil.newHashSet();
		for (final Map.Entry<Long, Channel> entry : channels.entrySet()) {
			final Long key = entry.getKey();
			final String prefix = key + CHANNEL_PROPERTY_POSTFIX.value() + CHANNEL_PROPERTY_PREFIX.value()
					+ CHANNEL_PROPERTY_POSTFIX.value();
			channelPrefixes.add(prefix);
		}
		return channelPrefixes;
	}

	/**
	 * Retrieve channels from the provided properties.
	 *
	 * @param properties
	 *            the properties containing the asset specific configuration
	 */
	private void retrieveConfigurationsFromProperties(final Map<String, Object> properties) {
		s_logger.debug(s_message.retrievingConf());
		if (this.m_assetOptions == null) {
			this.m_assetOptions = new AssetOptions(properties);
		} else {
			this.m_assetOptions.update(properties);
		}
		if ((this.m_assetConfiguration == null) && (this.m_assetOptions != null)) {
			this.m_assetConfiguration = this.m_assetOptions.getAssetConfiguration();
		}
		s_logger.debug(s_message.retrievingConfDone());
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "BaseAsset [Asset Configuration=" + this.m_assetConfiguration + "]";
	}

	/** {@inheritDoc} */
	@Override
	public void unregisterAssetListener(final AssetListener assetListener) throws KuraException {
		checkNull(assetListener, s_message.listenerNonNull());
		checkNull(this.m_driver, s_message.driverNonNull());

		s_logger.debug(s_message.unregisteringListener());
		this.m_monitor.lock();
		try {
			if (this.m_assetListeners.containsKey(assetListener)) {
				try {
					this.m_driver.unregisterDriverListener(this.m_assetListeners.get(assetListener));
				} catch (final ConnectionException ce) {
					throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
				}
			}
		} finally {
			this.m_monitor.unlock();
		}
		this.m_assetListeners.remove(assetListener);
		s_logger.debug(s_message.unregisteringListenerDone());
	}

	/**
	 * OSGi service component callback while updation.
	 *
	 * @param properties
	 *            the service properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updating());
		this.retrieveConfigurationsFromProperties(properties);
		this.attachDriver(this.m_assetConfiguration.getDriverPid());
		s_logger.debug(s_message.updatingDone());

	}

	/** {@inheritDoc} */
	@Override
	public List<AssetRecord> write(final List<AssetRecord> assetRecords) throws KuraException {
		checkNull(assetRecords, s_message.assetRecordsNonNull());
		checkCondition(assetRecords.isEmpty(), s_message.assetRecordsNonEmpty());
		checkNull(this.m_driver, s_message.driverNonNull());

		s_logger.debug(s_message.writing());
		final List<DriverRecord> driverRecords = CollectionUtil.newArrayList();
		final Map<Long, Channel> channels = this.m_assetConfiguration.getAssetChannels();
		for (final AssetRecord assetRecord : assetRecords) {
			final long id = this.checkChannelAvailability(assetRecord.getChannelId(), channels);
			checkCondition(id == 0, s_message.channelUnavailable());

			final Channel channel = channels.get(id);
			checkCondition(!((channel.getType() == WRITE) || (channel.getType() == READ_WRITE)),
					s_message.channelTypeNotWritable() + channel);
			final DriverRecord driverRecord = new DriverRecord();

			// Copy the configuration of the channel and put the channel ID and
			// channel value type
			final Map<String, Object> channelConf = CollectionUtil.newHashMap(channel.getConfiguration());
			channelConf.put(CHANNEL_ID.value(), channel.getId());
			channelConf.put(CHANNEL_VALUE_TYPE.value(), channel.getValueType());
			driverRecord.setChannelConfig(channelConf);
			driverRecord.setValue(assetRecord.getValue());
			driverRecords.add(driverRecord);
		}

		this.m_monitor.lock();
		try {
			this.m_driver.write(driverRecords);
		} catch (final ConnectionException ce) {
			throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
		} finally {
			this.m_monitor.unlock();
		}

		for (final DriverRecord driverRecord : driverRecords) {
			final AssetRecord assetRecord = this.getAssetRecordByDriverRecord(assetRecords, driverRecord);
			checkNull(assetRecord, s_message.assetRecordNonNull());
			this.prepareAssetRecord(driverRecord, assetRecord);
		}
		s_logger.debug(s_message.writingDone());
		return assetRecords;
	}

}
