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
package org.eclipse.kura.device.internal;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.device.internal.DeviceConfiguration.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.device.internal.DeviceConfiguration.CHANNEL_PROPERTY_PREFIX;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_DESC_PROP;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_DRIVER_PROP;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_ID_PROP;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.device.Channel;
import org.eclipse.kura.device.ChannelDescriptor;
import org.eclipse.kura.device.ChannelType;
import org.eclipse.kura.device.Device;
import org.eclipse.kura.device.DeviceFlag;
import org.eclipse.kura.device.DeviceListener;
import org.eclipse.kura.device.DeviceRecord;
import org.eclipse.kura.device.Devices;
import org.eclipse.kura.device.Driver;
import org.eclipse.kura.device.DriverFlag;
import org.eclipse.kura.device.DriverListener;
import org.eclipse.kura.device.DriverRecord;
import org.eclipse.kura.localization.DeviceMessages;
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
 * The Class BaseDevice is a basic Kura device implementation of an Industrial
 * Field Device which associates a device driver. All devices which associates a
 * driver either extend this class for more specific functionality or use this
 * to conform to the Kura device specifications.
 *
 * The basic device implementation requires a specific set of configurations to
 * be provided by the user. Please check {@see DeviceConfiguration} for more
 * information on how to provide the configurations to the basic Kura device.
 */
public class BaseDevice implements Device, SelfConfiguringComponent {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(BaseDevice.class);

	/** Localization Resource */
	protected static final DeviceMessages s_message = LocalizationAdapter.adapt(DeviceMessages.class);

	/** The service component context. */
	private ComponentContext m_context;

	/** The provided device configuration wrapper instance. */
	protected DeviceConfiguration m_deviceConfiguration;

	/** Container of mapped device listeners and drivers listener. */
	protected final Map<DeviceListener, DriverListener> m_deviceListeners;

	/** The Driver instance. */
	protected volatile Driver m_driver;

	/** Device Driver Tracker Customizer. */
	private DriverTrackerCustomizer m_driverTrackerCustomizer;

	/** Synchronization Monitor for driver specific operations. */
	private final Monitor m_monitor;

	/** The configurable properties of this device. */
	private Map<String, Object> m_properties;

	/** Device Driver Tracker. */
	private ServiceTracker<Driver, Driver> m_serviceTracker;

	/**
	 * Instantiates a new Base Device.
	 */
	public BaseDevice() {
		this.m_deviceListeners = Maps.newConcurrentMap();
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
		// We don't retrieve any configuration while activating device component
		// because the user would provide the necessary configuration while
		// updating. Let's consider the scenario for Wire Device. The user will
		// drag the Wire Device Component to the Kura Wires composer UI and at
		// that point of time, the device component is already activated but the
		// configuration of the driver to be attached is not at all provided.
		// So, we don't need to retrieve any configuration while activating a
		// device component.
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

		final Tad result = new Tad();
		result.setId(prefix + oldAd.getId());
		result.setName(prefix + oldAd.getName());
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

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		final String componentName = this.m_context.getProperties().get("service.pid").toString();

		final Tocd mainOcd = new Tocd();
		mainOcd.setId(this.getClass().getName());
		mainOcd.setName(s_message.ocdName());
		mainOcd.setDescription(s_message.ocdDescription());

		final Tad deviceNameAd = new Tad();
		deviceNameAd.setId(DEVICE_ID_PROP);
		deviceNameAd.setName(DEVICE_ID_PROP);
		deviceNameAd.setCardinality(0);
		deviceNameAd.setType(Tscalar.STRING);
		deviceNameAd.setDescription(s_message.name());
		deviceNameAd.setRequired(true);

		final Tad deviceDescriptionAd = new Tad();
		deviceDescriptionAd.setId(DEVICE_DESC_PROP);
		deviceDescriptionAd.setName(DEVICE_DESC_PROP);
		deviceDescriptionAd.setCardinality(0);
		deviceDescriptionAd.setType(Tscalar.STRING);
		deviceDescriptionAd.setDescription(s_message.description());
		deviceDescriptionAd.setRequired(true);

		final Tad driverNameAd = new Tad();
		driverNameAd.setId(DEVICE_DRIVER_PROP);
		driverNameAd.setName(DEVICE_DRIVER_PROP);
		driverNameAd.setCardinality(0);
		driverNameAd.setType(Tscalar.STRING);
		driverNameAd.setDescription(s_message.driverName());
		driverNameAd.setRequired(true);

		mainOcd.addAD(deviceDescriptionAd);
		mainOcd.addAD(deviceNameAd);
		mainOcd.addAD(driverNameAd);

		final Map<String, Object> props = Maps.newHashMap();

		for (final Map.Entry<String, Object> entry : this.m_properties.entrySet()) {
			props.put(entry.getKey(), entry.getValue());
		}

		if ((this.m_driver != null) && (this.m_driver.getChannelDescriptor() != null)) {
			List<Tad> channelConfiguration = null;
			final ChannelDescriptor channelDescriptor = this.m_driver.getChannelDescriptor();
			final Object descriptor = channelDescriptor.getDescriptor();
			if (descriptor instanceof List<?>) {
				channelConfiguration = (List<Tad>) descriptor;
			}

			if (channelConfiguration != null) {
				final BaseDeviceChannelDescriptor basicDeviceChanneldescriptor = BaseDeviceChannelDescriptor
						.getDefault();
				basicDeviceChanneldescriptor.getDefaultConfiguration().addAll(channelConfiguration);
				for (final Tad attribute : channelConfiguration) {
					final Tad newAttribute = this.cloneAd(attribute,
							CHANNEL_PROPERTY_PREFIX + System.nanoTime() + CHANNEL_PROPERTY_POSTFIX);
					mainOcd.addAD(newAttribute);
				}
			}
		}
		return new ComponentConfigurationImpl(componentName, mainOcd, props);
	}

	/**
	 * Gets the device configuration.
	 *
	 * @return the device configuration
	 */
	public DeviceConfiguration getDeviceConfiguration() {
		return this.m_deviceConfiguration;
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
	 * Returns the associated device listeners
	 *
	 * @return the device listeners containment
	 */
	public Map<DeviceListener, DriverListener> getListeners() {
		return this.m_deviceListeners;
	}

	/** {@inheritDoc} */
	@Override
	public List<DeviceRecord> read(final List<String> channelNames) throws KuraException {
		checkNull(channelNames, s_message.channelsNonNull());
		checkCondition(channelNames.isEmpty(), s_message.channelsNonEmpty());
		checkNull(this.m_driver, s_message.driverNonNull());

		s_logger.debug(s_message.readingChannels());
		final List<DeviceRecord> deviceRecords = Lists.newArrayList();
		final List<DriverRecord> driverRecords = Lists.newArrayList();

		final Map<String, Channel> channels = this.m_deviceConfiguration.getChannels();
		for (final String channelName : channelNames) {
			checkCondition(!channels.containsKey(channelName), s_message.channelUnavailable());

			final Channel channel = channels.get(channelName);
			checkCondition((channel.getType() != ChannelType.READ) || (channel.getType() != ChannelType.READ_WRITE),
					s_message.channelTypeNotReadable() + channel);

			final DriverRecord driverRecord = Devices.newDriverRecord(channelName);
			driverRecord.setChannelConfig(channel.getConfig());
			driverRecords.add(driverRecord);
		}

		this.m_monitor.enter();
		try {
			this.m_driver.read(driverRecords);
		} finally {
			this.m_monitor.leave();
		}

		for (final DriverRecord driverRecord : driverRecords) {
			final DeviceRecord deviceRecord = Devices.newDeviceRecord(driverRecord.getChannelName());
			final DriverFlag driverFlag = driverRecord.getDriverFlag();

			switch (driverFlag) {
			case READ_SUCCESSFUL:
				deviceRecord.setDeviceFlag(DeviceFlag.READ_SUCCESSFUL);
				break;
			case DRIVER_ERROR_UNSPECIFIED:
				deviceRecord.setDeviceFlag(DeviceFlag.DEVICE_ERROR_UNSPECIFIED);
				break;
			case UNKNOWN:
				deviceRecord.setDeviceFlag(DeviceFlag.UNKNOWN);
				break;
			default:
				break;
			}

			deviceRecord.setTimestamp(driverRecord.getTimestamp());
			deviceRecord.setValue(driverRecord.getValue());
			deviceRecords.add(deviceRecord);
		}
		s_logger.debug(s_message.readingChannelsDone());
		return deviceRecords;
	}

	/** {@inheritDoc} */
	@Override
	public void registerDeviceListener(final String channelName, final DeviceListener deviceListener)
			throws KuraException {
		checkNull(channelName, s_message.channelNameNonNull());
		checkNull(deviceListener, s_message.listenerNonNull());

		s_logger.debug(s_message.registeringListener());
		final Map<String, Channel> channels = this.m_deviceConfiguration.getChannels();
		checkCondition(!channels.containsKey(channelName), s_message.channelUnavailable());

		final Channel channel = channels.get(channelName);
		final DriverListener driverListener = new BaseDriverListener(channelName, deviceListener);
		this.m_deviceListeners.put(deviceListener, driverListener);
		checkNull(this.m_driver, s_message.driverNonNull());

		this.m_monitor.enter();
		try {
			this.m_driver.registerDriverListener(ImmutableMap.copyOf(channel.getConfig()), driverListener);
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
		this.m_deviceConfiguration = DeviceConfiguration.of(properties);
		s_logger.debug(s_message.retrievingConfDone());

	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add(s_message.configuration(), this.m_deviceConfiguration).toString();
	}

	/** {@inheritDoc} */
	@Override
	public void unregisterDeviceListener(final DeviceListener deviceListener) throws KuraException {
		checkNull(deviceListener, s_message.listenerNonNull());
		checkNull(this.m_driver, s_message.driverNonNull());

		s_logger.debug(s_message.unregisteringListener());
		this.m_monitor.enter();
		try {
			this.m_driver.unregisterDriverListener(this.m_deviceListeners.get(deviceListener));
		} finally {
			this.m_monitor.leave();
		}
		this.m_deviceListeners.remove(deviceListener);
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
		// As mentioned in the comment in activate method, we only extract
		// configuration while updating. Please refer to the previous comment
		// for more details.
		this.retrieveConfigurationsFromProperties(properties);
		this.attachDriver(this.m_deviceConfiguration.getDriverId());
		s_logger.debug(s_message.updatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public List<DeviceRecord> write(final List<DeviceRecord> deviceRecords) throws KuraException {
		checkNull(deviceRecords, s_message.deviceRecordsNonNull());
		checkCondition(deviceRecords.isEmpty(), s_message.deviceRecordsNonEmpty());

		s_logger.debug(s_message.writing());
		final List<DriverRecord> driverRecords = Lists.newArrayList();
		final Map<DriverRecord, DeviceRecord> mappedRecords = Maps.newHashMap();

		final Map<String, Channel> channels = this.m_deviceConfiguration.getChannels();
		for (final DeviceRecord deviceRecord : deviceRecords) {
			checkCondition(!channels.containsKey(deviceRecord.getChannelName()), s_message.channelUnavailable());

			final Channel channel = channels.get(deviceRecord.getChannelName());
			checkCondition((channel.getType() != ChannelType.WRITE) || (channel.getType() != ChannelType.READ_WRITE),
					s_message.channelTypeNotWritable() + channel);
			final DriverRecord driverRecord = Devices.newDriverRecord(channel.getName());
			driverRecord.setChannelConfig(channel.getConfig());
			driverRecord.setValue(deviceRecord.getValue());
			driverRecords.add(driverRecord);
			mappedRecords.put(driverRecord, deviceRecord);
		}

		checkNull(this.m_driver, s_message.driverNonNull());
		this.m_monitor.enter();
		try {
			this.m_driver.write(driverRecords);
		} finally {
			this.m_monitor.leave();
		}

		for (final DriverRecord driverRecord : mappedRecords.keySet()) {
			final DeviceRecord deviceRecord = mappedRecords.get(driverRecord);
			deviceRecord.setChannelName(driverRecord.getChannelName());
			final DriverFlag driverFlag = driverRecord.getDriverFlag();
			switch (driverFlag) {
			case WRITE_SUCCESSFUL:
				deviceRecord.setDeviceFlag(DeviceFlag.WRITE_SUCCESSFUL);
				break;
			case DRIVER_ERROR_UNSPECIFIED:
				deviceRecord.setDeviceFlag(DeviceFlag.DEVICE_ERROR_UNSPECIFIED);
				break;
			case UNKNOWN:
				deviceRecord.setDeviceFlag(DeviceFlag.UNKNOWN);
				break;
			default:
				break;
			}
			deviceRecord.setTimestamp(driverRecord.getTimestamp());
		}
		s_logger.debug(s_message.writingDone());
		return deviceRecords;
	}
}
