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
package org.eclipse.kura.asset.internal;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetEvent;
import org.eclipse.kura.asset.AssetFlag;
import org.eclipse.kura.asset.AssetListener;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.Assets;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.ChannelType;
import org.eclipse.kura.asset.Driver;
import org.eclipse.kura.asset.DriverEvent;
import org.eclipse.kura.asset.DriverFlag;
import org.eclipse.kura.asset.DriverListener;
import org.eclipse.kura.asset.DriverRecord;
import org.eclipse.kura.localization.AssetMessages;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Monitor;

/**
 * The Class BaseAsset is a basic Kura asset implementation of an Industrial
 * Field Device which associates a device driver. All devices which associates a
 * driver either extend this class for more specific functionality or use this
 * to conform to the Kura asset specifications.
 *
 * The basic asset implementation requires a specific set of configurations to
 * be provided by the user. Please check {@see AssetConfiguration} for more
 * information on how to provide the configurations to the basic Kura asset.
 *
 * @see AssetOptions
 * @see AssetConfiguration
 */
public class BaseAsset implements Asset {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(BaseAsset.class);

	/** Localization Resource */
	protected static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

	/** The provided asset configuration wrapper instance. */
	protected AssetConfiguration m_assetConfiguration;

	/** Container of mapped asset listeners and drivers listener. */
	protected final Map<AssetListener, DriverListener> m_assetListeners;

	/** The provided asset options instance. */
	protected AssetOptions m_assetOptions;

	/** The service component context. */
	protected ComponentContext m_context;

	/** The Driver instance. */
	protected volatile Driver m_driver;

	/** Asset Driver Tracker Customizer. */
	private DriverTrackerCustomizer m_driverTrackerCustomizer;

	/** Synchronization Monitor for driver specific operations. */
	private final Monitor m_monitor;

	/** The configurable properties of this asset. */
	protected Map<String, Object> m_properties;

	/** Asset Driver Tracker. */
	private ServiceTracker<Driver, Driver> m_serviceTracker;

	/**
	 * Instantiates a new Base Asset.
	 */
	public BaseAsset() {
		this.m_assetListeners = Maps.newConcurrentMap();
		this.m_monitor = new Monitor();
	}

	/**
	 * Callback method used to trigger when this service component will be
	 * activated.
	 *
	 * @param componentContext
	 *            the component context
	 * @param properties
	 *            the configurable properties
	 */
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.debug(s_message.activating());
		this.m_context = componentContext;
		this.m_properties = properties;
		this.retrieveConfigurationsFromProperties(properties);
		s_logger.debug(s_message.activatingDone());
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
			this.m_driverTrackerCustomizer = new DriverTrackerCustomizer(this.m_context.getBundleContext(), this,
					driverId);
			this.m_serviceTracker = new ServiceTracker<Driver, Driver>(this.m_context.getBundleContext(),
					Driver.class.getName(), this.m_driverTrackerCustomizer);
			this.m_serviceTracker.open();
		} catch (final InvalidSyntaxException e) {
			Throwables.propagate(e);
		}
		s_logger.debug(s_message.driverAttachDone());
	}

	/**
	 * Checks if the provided channel is present in the provided map
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

	/**
	 * Callback method used to trigger when this service component will be
	 * deactivated.
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.debug(s_message.deactivating());
		this.m_monitor.enter();
		try {
			if (this.m_driver != null) {
				this.m_driver.disconnect();
			}
		} catch (final KuraException e) {
			s_logger.error(s_message.errorDriverDisconnection() + Throwables.getStackTraceAsString(e));
		} finally {
			this.m_monitor.leave();
		}
		this.m_driver = null;
		if (this.m_serviceTracker != null) {
			this.m_serviceTracker.close();
		}
		s_logger.debug(s_message.deactivatingDone());
	}

	/**
	 * Gets the asset configuration.
	 *
	 * @return the asset configuration
	 */
	public AssetConfiguration getAssetConfiguration() {
		return this.m_assetConfiguration;
	}

	/**
	 * Returns the injected instance of the Driver
	 *
	 * @return the driver instance
	 */
	public Driver getDriver() {
		return this.m_driver;
	}

	/**
	 * Returns the associated asset listeners
	 *
	 * @return the asset listeners containment
	 */
	public Map<AssetListener, DriverListener> getListeners() {
		return this.m_assetListeners;
	}

	/** {@inheritDoc} */
	@Override
	public List<AssetRecord> read(final List<String> channelNames) throws KuraException {
		checkNull(channelNames, s_message.channelsNonNull());
		checkCondition(channelNames.isEmpty(), s_message.channelsNonEmpty());
		checkNull(this.m_driver, s_message.driverNonNull());

		s_logger.debug(s_message.readingChannels());
		final List<AssetRecord> assetRecords = Lists.newArrayList();
		final List<DriverRecord> driverRecords = Lists.newArrayList();

		final Map<Long, Channel> channels = this.m_assetConfiguration.getChannels();
		for (final String channelName : channelNames) {
			final long id = this.checkChannelAvailability(channelName, channels);
			checkCondition(id == 0, s_message.channelUnavailable());

			final Channel channel = channels.get(id);
			checkCondition((channel.getType() != ChannelType.READ) || (channel.getType() != ChannelType.READ_WRITE),
					s_message.channelTypeNotReadable() + channel);

			final DriverRecord driverRecord = Assets.newDriverRecord(channelName);
			driverRecord.setChannelConfig(channel.getConfiguration());
			driverRecords.add(driverRecord);
		}

		this.m_monitor.enter();
		try {
			this.m_driver.read(driverRecords);
		} finally {
			this.m_monitor.leave();
		}

		for (final DriverRecord driverRecord : driverRecords) {
			final AssetRecord assetRecord = Assets.newAssetRecord(driverRecord.getChannelName());
			final DriverFlag driverFlag = driverRecord.getDriverFlag();

			switch (driverFlag) {
			case READ_SUCCESSFUL:
				assetRecord.setAssetFlag(AssetFlag.READ_SUCCESSFUL);
				break;
			case DRIVER_ERROR_UNSPECIFIED:
				assetRecord.setAssetFlag(AssetFlag.ASSET_ERROR_UNSPECIFIED);
				break;
			case UNKNOWN:
				assetRecord.setAssetFlag(AssetFlag.UNKNOWN);
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
				final AssetRecord assetRecord = Assets.newAssetRecord(this.m_channelName);
				final DriverFlag driverFlag = driverRecord.getDriverFlag();

				switch (driverFlag) {
				case READ_SUCCESSFUL:
					assetRecord.setAssetFlag(AssetFlag.READ_SUCCESSFUL);
					break;
				case WRITE_SUCCESSFUL:
					assetRecord.setAssetFlag(AssetFlag.WRITE_SUCCESSFUL);
					break;
				case DRIVER_ERROR_UNSPECIFIED:
					assetRecord.setAssetFlag(AssetFlag.ASSET_ERROR_UNSPECIFIED);
					break;
				case UNKNOWN:
					assetRecord.setAssetFlag(AssetFlag.UNKNOWN);
					break;
				default:
					break;
				}
				assetRecord.setTimestamp(driverRecord.getTimestamp());
				assetRecord.setValue(driverRecord.getValue());
				final AssetEvent assetEvent = Assets.newAssetEvent(assetRecord);
				this.m_assetListener.onAssetEvent(assetEvent);
			}
		}

		final Channel channel = channels.get(channelName);
		final DriverListener driverListener = new BaseDriverListener(channelName, assetListener);

		this.m_assetListeners.put(assetListener, driverListener);
		checkNull(this.m_driver, s_message.driverNonNull());

		this.m_monitor.enter();
		try {
			this.m_driver.registerDriverListener(ImmutableMap.copyOf(channel.getConfiguration()), driverListener);
		} finally {
			this.m_monitor.leave();
		}

		s_logger.debug(s_message.registeringListenerDone());
	}

	/**
	 * Retrieve channels from the provided properties.
	 *
	 * @param properties
	 *            the properties
	 */
	private void retrieveConfigurationsFromProperties(final Map<String, Object> properties) {
		s_logger.debug(s_message.retrievingConf());
		if (this.m_assetOptions == null) {
			this.m_assetOptions = new AssetOptions(properties);
		}
		if ((this.m_assetConfiguration == null) && (this.m_assetOptions != null)) {
			this.m_assetConfiguration = this.m_assetOptions.getAssetConfiguration();
		}
		s_logger.debug(s_message.retrievingConfDone());
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add(s_message.configuration(), this.m_assetConfiguration).toString();
	}

	/** {@inheritDoc} */
	@Override
	public void unregisterAssetListener(final AssetListener assetListener) throws KuraException {
		checkNull(assetListener, s_message.listenerNonNull());
		checkNull(this.m_driver, s_message.driverNonNull());

		s_logger.debug(s_message.unregisteringListener());
		this.m_monitor.enter();
		try {
			this.m_driver.unregisterDriverListener(this.m_assetListeners.get(assetListener));
		} finally {
			this.m_monitor.leave();
		}
		this.m_assetListeners.remove(assetListener);
		s_logger.debug(s_message.unregisteringListenerDone());
	}

	/**
	 * Callback method used to trigger when this service component will be
	 * updated.
	 *
	 * @param properties
	 *            the configurable properties
	 */
	protected synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updating());
		this.m_properties = properties;
		this.retrieveConfigurationsFromProperties(properties);
		this.attachDriver(this.m_assetConfiguration.getDriverId());
		s_logger.debug(s_message.updatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public List<AssetRecord> write(final List<AssetRecord> assetRecords) throws KuraException {
		checkNull(assetRecords, s_message.assetRecordsNonNull());
		checkCondition(assetRecords.isEmpty(), s_message.assetRecordsNonEmpty());

		s_logger.debug(s_message.writing());
		final List<DriverRecord> driverRecords = Lists.newArrayList();
		final Map<DriverRecord, AssetRecord> mappedRecords = Maps.newHashMap();

		final Map<Long, Channel> channels = this.m_assetConfiguration.getChannels();
		for (final AssetRecord assetRecord : assetRecords) {
			final long id = this.checkChannelAvailability(assetRecord.getChannelName(), channels);
			checkCondition(id == 0, s_message.channelUnavailable());

			final Channel channel = channels.get(id);
			checkCondition((channel.getType() != ChannelType.WRITE) || (channel.getType() != ChannelType.READ_WRITE),
					s_message.channelTypeNotWritable() + channel);
			final DriverRecord driverRecord = Assets.newDriverRecord(channel.getName());
			driverRecord.setChannelConfig(channel.getConfiguration());
			driverRecord.setValue(assetRecord.getValue());
			driverRecords.add(driverRecord);
			mappedRecords.put(driverRecord, assetRecord);
		}

		checkNull(this.m_driver, s_message.driverNonNull());
		this.m_monitor.enter();
		try {
			this.m_driver.write(driverRecords);
		} finally {
			this.m_monitor.leave();
		}

		for (final DriverRecord driverRecord : mappedRecords.keySet()) {
			final AssetRecord assetRecord = mappedRecords.get(driverRecord);
			assetRecord.setChannelName(driverRecord.getChannelName());
			final DriverFlag driverFlag = driverRecord.getDriverFlag();
			switch (driverFlag) {
			case WRITE_SUCCESSFUL:
				assetRecord.setAssetFlag(AssetFlag.WRITE_SUCCESSFUL);
				break;
			case DRIVER_ERROR_UNSPECIFIED:
				assetRecord.setAssetFlag(AssetFlag.ASSET_ERROR_UNSPECIFIED);
				break;
			case UNKNOWN:
				assetRecord.setAssetFlag(AssetFlag.UNKNOWN);
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
