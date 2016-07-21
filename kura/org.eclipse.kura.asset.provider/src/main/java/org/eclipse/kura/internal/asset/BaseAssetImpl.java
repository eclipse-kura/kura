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
import static org.eclipse.kura.asset.AssetFlag.ASSET_ERROR_UNSPECIFIED;
import static org.eclipse.kura.asset.AssetFlag.READ_SUCCESSFUL;
import static org.eclipse.kura.asset.AssetFlag.UNKNOWN;
import static org.eclipse.kura.asset.AssetFlag.WRITE_SUCCESSFUL;
import static org.eclipse.kura.asset.ChannelType.READ;
import static org.eclipse.kura.asset.ChannelType.READ_WRITE;
import static org.eclipse.kura.asset.ChannelType.WRITE;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetEvent;
import org.eclipse.kura.asset.AssetHelperService;
import org.eclipse.kura.asset.AssetListener;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.BaseAsset;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.Driver;
import org.eclipse.kura.asset.DriverEvent;
import org.eclipse.kura.asset.DriverFlag;
import org.eclipse.kura.asset.DriverListener;
import org.eclipse.kura.asset.DriverRecord;
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
 * The Class BaseAsset is a basic Kura asset implementation.
 *
 * @see AssetOptions
 * @see AssetConfiguration
 */
public final class BaseAssetImpl implements BaseAsset {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(BaseAssetImpl.class);

	/** Localization Resource. */
	private static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

	/** The provided asset configuration wrapper instance. */
	private AssetConfiguration m_assetConfiguration;

	/** The Asset Helper Service instance. */
	private final AssetHelperService m_assetHelper;

	/** Container of mapped asset listeners and drivers listener. */
	private final Map<AssetListener, DriverListener> m_assetListeners;

	/** The provided asset options instance. */
	private AssetOptions m_assetOptions;

	/** The Bundle context. */
	private final BundleContext m_context;

	/** The Driver instance. */
	private volatile Driver m_driver;

	/** Asset Driver Tracker Customizer. */
	private DriverTrackerCustomizer m_driverTrackerCustomizer;

	/** Synchronization Monitor for driver specific operations. */
	private final Lock m_monitor;

	/** Asset Driver Tracker. */
	private ServiceTracker<Driver, Driver> m_serviceTracker;

	/**
	 * Instantiates a new base asset implementation.
	 *
	 * @param helperService
	 *            the asset helper service
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public BaseAssetImpl(final AssetHelperService helperService) {
		checkNull(helperService, s_message.assetHelperNonNull());
		this.m_assetListeners = CollectionUtil.newConcurrentHashMap();
		this.m_monitor = new ReentrantLock();
		this.m_assetHelper = helperService;
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
	 * @param channelName
	 *            the name of channel
	 * @param channels
	 *            the provided container of channels
	 * @return the id of the channel if found or else 0
	 * @throws KuraRuntimeException
	 *             if driver id provided is null
	 */
	private long checkChannelAvailability(final String channelName, final Map<Long, Channel> channels) {
		checkNull(channelName, s_message.channelNameNonNull());
		checkNull(channels, s_message.channelsNonNull());
		checkCondition(channels.isEmpty(), s_message.channelsNonEmpty());

		for (final Map.Entry<Long, Channel> channel : channels.entrySet()) {
			final String chName = channel.getValue().getName();
			if (channelName.equals(chName)) {
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

	/** {@inheritDoc} */
	@Override
	public Driver getDriver() {
		return this.m_driver;
	}

	/**
	 * Returns the associated asset listeners.
	 *
	 * @return the asset listeners containment
	 */
	public Map<AssetListener, DriverListener> getListeners() {
		return this.m_assetListeners;
	}

	/** {@inheritDoc} */
	@Override
	public void initialize(final Map<String, Object> properties) {
		s_logger.debug(s_message.updating());
		this.retrieveConfigurationsFromProperties(properties);
		this.attachDriver(this.m_assetConfiguration.getDriverId());
		s_logger.debug(s_message.updatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public List<AssetRecord> read(final List<String> channelNames) throws KuraException {
		checkNull(channelNames, s_message.channelsNonNull());
		checkCondition(channelNames.isEmpty(), s_message.channelsNonEmpty());
		checkNull(this.m_driver, s_message.driverNonNull());

		s_logger.debug(s_message.readingChannels());
		final List<AssetRecord> assetRecords = CollectionUtil.newArrayList();
		final List<DriverRecord> driverRecords = CollectionUtil.newArrayList();

		final Map<Long, Channel> channels = this.m_assetConfiguration.getChannels();
		for (final String channelName : channelNames) {
			final long id = this.checkChannelAvailability(channelName, channels);
			checkCondition(id == 0, s_message.channelUnavailable());

			final Channel channel = channels.get(id);
			checkCondition((channel.getType() != READ) || (channel.getType() != READ_WRITE),
					s_message.channelTypeNotReadable() + channel);

			final DriverRecord driverRecord = this.m_assetHelper.newDriverRecord(channelName);
			driverRecord.setChannelConfig(channel.getConfiguration());
			driverRecords.add(driverRecord);
		}

		this.m_monitor.lock();
		try {
			this.m_driver.read(driverRecords);
		} finally {
			this.m_monitor.unlock();
		}

		for (final DriverRecord driverRecord : driverRecords) {
			final AssetRecord assetRecord = this.m_assetHelper.newAssetRecord(driverRecord.getChannelName());
			final DriverFlag driverFlag = driverRecord.getDriverFlag();

			switch (driverFlag) {
			case READ_SUCCESSFUL:
				assetRecord.setAssetFlag(READ_SUCCESSFUL);
				break;
			case DRIVER_ERROR_UNSPECIFIED:
				assetRecord.setAssetFlag(ASSET_ERROR_UNSPECIFIED);
				break;
			case UNKNOWN:
				assetRecord.setAssetFlag(UNKNOWN);
				break;
			default:
				break;
			}

			assetRecord.setTimestamp(driverRecord.getTimestamp());
			assetRecord.setValue(driverRecord.getValue());
			assetRecords.add(assetRecord);
		}
		s_logger.debug(s_message.readingChannelsDone());
		return assetRecords;
	}

	/** {@inheritDoc} */
	@Override
	public void registerAssetListener(final String channelName, final AssetListener assetListener)
			throws KuraException {
		checkNull(channelName, s_message.channelNameNonNull());
		checkNull(assetListener, s_message.listenerNonNull());

		s_logger.debug(s_message.registeringListener());
		final Map<Long, Channel> channels = this.m_assetConfiguration.getChannels();
		final long id = this.checkChannelAvailability(channelName, channels);
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

			/** The channel name. */
			private final String m_channelName;

			/**
			 * Instantiates a new base driver listener.
			 *
			 * @param channelName
			 *            the channel name as provided
			 * @param assetListener
			 *            the asset listener
			 * @throws KuraRuntimeException
			 *             if any of the arguments is null
			 */
			BaseDriverListener(final String channelName, final AssetListener assetListener) {
				checkNull(channelName, s_message.channelNameNonNull());
				checkNull(assetListener, s_message.listenerNonNull());

				this.m_channelName = channelName;
				this.m_assetListener = assetListener;
			}

			/** {@inheritDoc} */
			@Override
			public void onDriverEvent(final DriverEvent event) {
				checkNull(event, s_message.driverEventNonNull());
				final DriverRecord driverRecord = event.getDriverRecord();
				final AssetRecord assetRecord = BaseAssetImpl.this.m_assetHelper.newAssetRecord(this.m_channelName);
				final DriverFlag driverFlag = driverRecord.getDriverFlag();

				switch (driverFlag) {
				case READ_SUCCESSFUL:
					assetRecord.setAssetFlag(READ_SUCCESSFUL);
					break;
				case WRITE_SUCCESSFUL:
					assetRecord.setAssetFlag(WRITE_SUCCESSFUL);
					break;
				case DRIVER_ERROR_UNSPECIFIED:
					assetRecord.setAssetFlag(ASSET_ERROR_UNSPECIFIED);
					break;
				case UNKNOWN:
					assetRecord.setAssetFlag(UNKNOWN);
					break;
				default:
					break;
				}
				assetRecord.setTimestamp(driverRecord.getTimestamp());
				assetRecord.setValue(driverRecord.getValue());
				final AssetEvent assetEvent = BaseAssetImpl.this.m_assetHelper.newAssetEvent(assetRecord);
				this.m_assetListener.onAssetEvent(assetEvent);
			}
		}

		final Channel channel = channels.get(channelName);
		final DriverListener driverListener = new BaseDriverListener(channelName, assetListener);

		this.m_assetListeners.put(assetListener, driverListener);
		checkNull(this.m_driver, s_message.driverNonNull());

		this.m_monitor.lock();
		try {
			this.m_driver.registerDriverListener(CollectionUtil.newHashMap(channel.getConfiguration()), driverListener);
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
		} catch (final KuraException e) {
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
			this.m_assetOptions = new AssetOptions(properties, this.m_assetHelper);
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
			this.m_driver.unregisterDriverListener(this.m_assetListeners.get(assetListener));
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

		s_logger.debug(s_message.writing());
		final List<DriverRecord> driverRecords = CollectionUtil.newArrayList();
		final Map<DriverRecord, AssetRecord> mappedRecords = CollectionUtil.newHashMap();

		final Map<Long, Channel> channels = this.m_assetConfiguration.getChannels();
		for (final AssetRecord assetRecord : assetRecords) {
			final long id = this.checkChannelAvailability(assetRecord.getChannelName(), channels);
			checkCondition(id == 0, s_message.channelUnavailable());

			final Channel channel = channels.get(id);
			checkCondition((channel.getType() != WRITE) || (channel.getType() != READ_WRITE),
					s_message.channelTypeNotWritable() + channel);
			final DriverRecord driverRecord = this.m_assetHelper.newDriverRecord(channel.getName());
			driverRecord.setChannelConfig(channel.getConfiguration());
			driverRecord.setValue(assetRecord.getValue());
			driverRecords.add(driverRecord);
			mappedRecords.put(driverRecord, assetRecord);
		}

		checkNull(this.m_driver, s_message.driverNonNull());
		this.m_monitor.lock();
		try {
			this.m_driver.write(driverRecords);
		} finally {
			this.m_monitor.unlock();
		}

		for (final DriverRecord driverRecord : mappedRecords.keySet()) {
			final AssetRecord assetRecord = mappedRecords.get(driverRecord);
			assetRecord.setChannelName(driverRecord.getChannelName());
			final DriverFlag driverFlag = driverRecord.getDriverFlag();
			switch (driverFlag) {
			case WRITE_SUCCESSFUL:
				assetRecord.setAssetFlag(WRITE_SUCCESSFUL);
				break;
			case DRIVER_ERROR_UNSPECIFIED:
				assetRecord.setAssetFlag(ASSET_ERROR_UNSPECIFIED);
				break;
			case UNKNOWN:
				assetRecord.setAssetFlag(UNKNOWN);
				break;
			default:
				break;
			}
			assetRecord.setTimestamp(driverRecord.getTimestamp());
		}
		s_logger.debug(s_message.writingDone());
		return assetRecords;
	}

}
