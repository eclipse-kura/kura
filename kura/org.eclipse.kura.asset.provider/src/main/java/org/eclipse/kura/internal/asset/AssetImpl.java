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
package org.eclipse.kura.internal.asset;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.asset.AssetFlag.READ_SUCCESSFUL;
import static org.eclipse.kura.asset.AssetFlag.UNKNOWN;
import static org.eclipse.kura.asset.AssetFlag.WRITE_SUCCESSFUL;
import static org.eclipse.kura.asset.ChannelType.READ;
import static org.eclipse.kura.asset.ChannelType.READ_WRITE;
import static org.eclipse.kura.asset.ChannelType.WRITE;
import static org.eclipse.kura.driver.DriverConstants.CHANNEL_ID;
import static org.eclipse.kura.driver.DriverConstants.CHANNEL_VALUE_TYPE;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetEvent;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.listener.AssetListener;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.DriverEvent;
import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class AssetImpl is basic implementation of {@code Asset}.
 *
 * @see AssetOptions
 * @see AssetConfiguration
 */
public final class AssetImpl implements Asset {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(AssetImpl.class);

	/** Localization Resource. */
	private static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

	/** The provided asset configuration wrapper instance. */
	private AssetConfiguration m_assetConfiguration;

	/** Container of mapped asset listeners and drivers listener. */
	private final Map<AssetListener, DriverListener> m_assetListeners;

	/** The provided asset options instance. */
	private AssetOptions m_assetOptions;

	/** The Asset Service instance. */
	private final AssetService m_assetService;

	/** The Bundle context. */
	private final BundleContext m_context;

	/** The Driver instance. */
	private volatile Driver m_driver;

	/** The Driver Service instance. */
	private final DriverService m_driverService;

	/** Asset Driver Tracker Customizer. */
	private DriverTrackerCustomizer m_driverTrackerCustomizer;

	/** Synchronization Monitor for driver specific operations. */
	private final Lock m_monitor;

	/** Asset Driver Tracker. */
	private ServiceTracker<Driver, Driver> m_serviceTracker;

	/**
	 * Instantiates a new asset instance.
	 *
	 * @param assetService
	 *            the asset service
	 * @param driverService
	 *            the driver service
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public AssetImpl(final AssetService assetService, final DriverService driverService) {
		checkNull(assetService, s_message.assetHelperNonNull());
		this.m_assetListeners = CollectionUtil.newConcurrentHashMap();
		this.m_monitor = new ReentrantLock();
		this.m_assetService = assetService;
		this.m_driverService = driverService;
		this.m_context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	}

	/**
	 * Tracks the Driver in the OSGi service registry with the specified driver
	 * ID.
	 *
	 * @param driverId
	 *            the identifier of the driver
	 * @throws KuraRuntimeException
	 *             if driver id provided is null
	 */
	private synchronized void attachDriver(final String driverId) {
		checkNull(driverId, s_message.driverIdNonNull());
		s_logger.debug(s_message.driverAttach());
		try {
			this.m_driverTrackerCustomizer = new DriverTrackerCustomizer(this.m_context, this, driverId);
			this.m_serviceTracker = new ServiceTracker<Driver, Driver>(this.m_context, Driver.class.getName(),
					this.m_driverTrackerCustomizer);
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

	/**
	 * Gets the injected driver instance
	 *
	 * @return the driver instance
	 */
	public Driver getDriver() {
		return this.m_driver;
	}

	/** {@inheritDoc} */
	@Override
	public void initialize(final Map<String, Object> properties) {
		s_logger.debug(s_message.updating());
		this.retrieveConfigurationsFromProperties(properties);
		this.attachDriver(this.m_assetConfiguration.getDriverId());
		s_logger.debug(s_message.updatingDone());
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
		final String exceptionMessage = (status.getExceptionMessage() == null) ? "" : status.getExceptionMessage();

		switch (driverFlag) {
		case READ_SUCCESSFUL:
			assetRecord.setAssetFlag(READ_SUCCESSFUL);
			break;
		case WRITE_SUCCESSFUL:
			assetRecord.setAssetFlag(WRITE_SUCCESSFUL);
			break;
		case UNKNOWN:
			assetRecord.setAssetFlag(UNKNOWN);
			break;
		case CUSTOM_ERROR_0:
		case CUSTOM_ERROR_1:
		case CUSTOM_ERROR_2:
		case CUSTOM_ERROR_3:
		case CUSTOM_ERROR_4:
		case CUSTOM_ERROR_5:
		case CUSTOM_ERROR_6:
		case CUSTOM_ERROR_7:
		case CUSTOM_ERROR_8:
		case CUSTOM_ERROR_9:
		case COMM_DEVICE_NOT_CONNECTED:
		case DEVICE_OR_INTERFACE_BUSY:
		case DRIVER_ERROR_CHANNEL_ADDRESS_INVALID:
		case DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE:
		case DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION:
		case DRIVER_ERROR_UNSPECIFIED:
		case DRIVER_THREW_UNKNOWN_EXCEPTION:
		case READ_FAILURE:
		case WRITE_FAILURE:
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, exceptionMessage);
		default:
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

		final Map<Long, Channel> channels = this.m_assetConfiguration.getChannels();
		for (final long channelId : channelIds) {
			final long id = this.checkChannelAvailability(channelId, channels);
			checkCondition(id == 0, s_message.channelUnavailable());

			final Channel channel = channels.get(id);
			checkCondition(!((channel.getType() == READ) || (channel.getType() == READ_WRITE)),
					s_message.channelTypeNotReadable() + channel);

			final DriverRecord driverRecord = this.m_driverService.newDriverRecord();
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
			final AssetRecord assetRecord = this.m_assetService.newAssetRecord(channelId);
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
		final Map<Long, Channel> channels = this.m_assetConfiguration.getChannels();
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
				final AssetRecord assetRecord = AssetImpl.this.m_assetService.newAssetRecord(channelId);
				try {
					AssetImpl.this.prepareAssetRecord(driverRecord, assetRecord);
				} catch (final KuraException e) {
					s_logger.error(s_message.errorPreparingAssetRecord() + ThrowableUtil.stackTraceAsString(e));
				}
				final AssetEvent assetEvent = AssetImpl.this.m_assetService.newAssetEvent(assetRecord);
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

	/** {@inheritDoc} */
	@Override
	public void release() {
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

	/**
	 * Retrieve channels from the provided properties.
	 *
	 * @param properties
	 *            the properties containing the asset specific configuration
	 */
	private void retrieveConfigurationsFromProperties(final Map<String, Object> properties) {
		s_logger.debug(s_message.retrievingConf());
		if (this.m_assetOptions == null) {
			this.m_assetOptions = new AssetOptions(properties, this.m_assetService);
		}
		if ((this.m_assetConfiguration == null) && (this.m_assetOptions != null)) {
			this.m_assetConfiguration = this.m_assetOptions.getAssetConfiguration();
		}
		s_logger.debug(s_message.retrievingConfDone());
	}

	/**
	 * Sets the new driver instance.
	 *
	 * @param driver
	 *            the new driver
	 */
	void setDriver(final Driver driver) {
		this.m_driver = driver;
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

	/** {@inheritDoc} */
	@Override
	public List<AssetRecord> write(final List<AssetRecord> assetRecords) throws KuraException {
		checkNull(assetRecords, s_message.assetRecordsNonNull());
		checkCondition(assetRecords.isEmpty(), s_message.assetRecordsNonEmpty());
		checkNull(this.m_driver, s_message.driverNonNull());

		s_logger.debug(s_message.writing());
		final List<DriverRecord> driverRecords = CollectionUtil.newArrayList();
		final Map<Long, Channel> channels = this.m_assetConfiguration.getChannels();
		for (final AssetRecord assetRecord : assetRecords) {
			final long id = this.checkChannelAvailability(assetRecord.getChannelId(), channels);
			checkCondition(id == 0, s_message.channelUnavailable());

			final Channel channel = channels.get(id);
			checkCondition(!((channel.getType() == WRITE) || (channel.getType() == READ_WRITE)),
					s_message.channelTypeNotWritable() + channel);
			final DriverRecord driverRecord = this.m_driverService.newDriverRecord();

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
