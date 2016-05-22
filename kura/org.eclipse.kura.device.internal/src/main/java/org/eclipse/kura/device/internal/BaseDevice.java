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

import static org.eclipse.kura.device.internal.DeviceConfiguration.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.device.internal.DeviceConfiguration.CHANNEL_PROPERTY_PREFIX;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_DESC_PROP;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_DRIVER_PROP;
import static org.eclipse.kura.device.internal.DeviceConfiguration.DEVICE_ID_PROP;
import static org.eclipse.kura.device.internal.Preconditions.checkCondition;

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
import org.eclipse.kura.device.Driver;
import org.eclipse.kura.device.DriverFlag;
import org.eclipse.kura.device.DriverListener;
import org.eclipse.kura.device.DriverRecord;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Monitor;

/**
 * The Class BaseDevice is a basic device implementation of a Kura Field Device
 * which associates a device driver. All devices which associates a driver must
 * extend this class to conform to the kura device specifications.
 */
public class BaseDevice implements Device, SelfConfiguringComponent {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(BaseDevice.class);

	/** The service component context. */
	private ComponentContext m_ctx;

	/** The provided device configuration wrapper instance. */
	protected DeviceConfiguration m_deviceConfiguration;

	/** Container of mapped device listeners and drivers listener. */
	private final Map<DeviceListener, DriverListener> m_deviceListeners;

	/** The Driver instance. */
	protected volatile Driver m_driver;

	/** Device Driver Tracker. */
	private DriverTracker m_driverTracker;

	/** Synchronization Monitor for driver specific operations. */
	private final Monitor m_monitor;

	/** The configurable properties of this device. */
	private Map<String, Object> m_properties;

	/**
	 * Instantiates a new base device.
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
		s_logger.debug("Activating Base Device...");
		this.m_ctx = componentContext;
		this.m_properties = properties;
		this.retrieveConfigurationsFromProperties(properties);
		s_logger.debug("Activating Base Device...Done");
	}

	/**
	 * Tracks the Driver in the OSGi service registry with the specified driver
	 * id.
	 *
	 * @param driverId
	 *            the identifier of the driver
	 * @throws KuraRuntimeException
	 *             if driver id provided is null
	 */
	private synchronized void attachDriver(final String driverId) {
		s_logger.debug("Attaching driver instance...");
		checkCondition(driverId == null, "Driver ID cannot be null");

		try {
			this.m_driverTracker = new DriverTracker(this.m_ctx.getBundleContext(), this, driverId);
			this.m_driverTracker.open();
		} catch (final InvalidSyntaxException e) {
			s_logger.error("Error in searching for drivers..." + Throwables.getStackTraceAsString(e));
		}
		s_logger.debug("Attaching driver instance...Done");
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
		checkCondition(oldAd == null, "Old Attribute Definition cannot be null");
		checkCondition(prefix == null, "Attribute Definition Prefix cannot be null");

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
		s_logger.debug("Deactivating Base Device...");
		this.m_monitor.enter();
		try {
			if (this.m_driver != null) {
				this.m_driver.disconnect();
			}
		} catch (final KuraException e) {
			s_logger.error("Error in disconnecting driver..." + Throwables.getStackTraceAsString(e));
		} finally {
			this.m_monitor.leave();
		}
		this.m_driver = null;
		s_logger.debug("Deactivating Base Device...Done");
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		final String componentName = this.m_ctx.getProperties().get("service.pid").toString();

		final Tocd mainOcd = new Tocd();
		mainOcd.setName(this.m_deviceConfiguration.getDeviceName());
		mainOcd.setDescription(this.m_deviceConfiguration.getDeviceDescription());
		mainOcd.setId(this.m_deviceConfiguration.getDeviceName());

		final Tad deviceNameAd = new Tad();
		deviceNameAd.setId(DEVICE_ID_PROP);
		deviceNameAd.setName(DEVICE_ID_PROP);
		deviceNameAd.setCardinality(0);
		deviceNameAd.setType(Tscalar.STRING);
		deviceNameAd.setDescription("Device Name");
		deviceNameAd.setRequired(true);

		final Tad deviceDescriptionAd = new Tad();
		deviceDescriptionAd.setId(DEVICE_DESC_PROP);
		deviceDescriptionAd.setName(DEVICE_DESC_PROP);
		deviceDescriptionAd.setCardinality(0);
		deviceDescriptionAd.setType(Tscalar.STRING);
		deviceDescriptionAd.setDescription("Device Description");
		deviceDescriptionAd.setRequired(true);

		final Tad driverNameAd = new Tad();
		driverNameAd.setId(DEVICE_DRIVER_PROP);
		driverNameAd.setName(DEVICE_DRIVER_PROP);
		driverNameAd.setCardinality(0);
		driverNameAd.setType(Tscalar.STRING);
		driverNameAd.setDescription("Driver Name");
		driverNameAd.setRequired(true);

		mainOcd.addAD(deviceDescriptionAd);
		mainOcd.addAD(deviceNameAd);
		mainOcd.addAD(driverNameAd);

		final Map<String, Object> props = Maps.newHashMap();

		for (final String key : this.m_properties.keySet()) {
			props.put(key, this.m_properties.get(key));
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
							CHANNEL_PROPERTY_PREFIX + System.currentTimeMillis() + CHANNEL_PROPERTY_POSTFIX);
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

	/** {@inheritDoc} */
	@Override
	public List<DeviceRecord> read(final List<String> channelNames) throws KuraException {
		checkCondition((channelNames == null), "List of channel names cannot be null");
		checkCondition(channelNames.isEmpty(), "List of channel names cannot be empty");
		checkCondition(this.m_driver == null, "Driver cannot be null");

		s_logger.debug("Reading device channels...");

		final List<DeviceRecord> deviceRecords = Lists.newArrayList();
		final List<DriverRecord> driverRecords = Lists.newArrayList();

		final Map<String, Channel> channels = this.m_deviceConfiguration.getChannels();

		for (final String channelName : channelNames) {
			checkCondition(!channels.containsKey(channelName), "Channel not available");

			final Channel channel = channels.get(channelName);
			checkCondition((channel.getType() != ChannelType.READ) || !(channel.getType() != ChannelType.READ_WRITE),
					"Channel type not within defined types (READ OR READ_WRITE) : " + channel);

			final DriverRecord driverRecord = new DriverRecord();
			driverRecord.setChannelConfig(ImmutableMap.copyOf(channel.getConfig()));
			driverRecord.setChannelName(channelName);

			driverRecords.add(driverRecord);
		}

		this.m_monitor.enter();
		try {
			this.m_driver.read(driverRecords);
		} finally {
			this.m_monitor.leave();
		}

		for (final DriverRecord driverRecord : driverRecords) {
			final DeviceRecord deviceRecord = new DeviceRecord();
			deviceRecord.setChannelName(driverRecord.getChannelName());

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
		s_logger.debug("Reading device channels...Done");
		return deviceRecords;
	}

	/** {@inheritDoc} */
	@Override
	public void registerDeviceListener(final String channelName, final DeviceListener deviceListener)
			throws KuraException {
		checkCondition(channelName == null, "Channel name cannot be null");
		checkCondition(deviceListener == null, "Device Listener cannot be null");

		s_logger.debug("Registering Device Listener for monitoring...");

		Channel channel = null;
		final Map<String, Channel> channels = this.m_deviceConfiguration.getChannels();

		if (channels.containsKey(channelName)) {
			channel = channels.get(channelName);
		}

		final DriverListener driverListener = new BaseDriverListener(deviceListener);
		this.m_deviceListeners.put(deviceListener, driverListener);
		checkCondition(this.m_driver == null, "Driver cannot be null");

		this.m_monitor.enter();
		try {
			if (channel != null) {
				this.m_driver.registerDriverListener(ImmutableMap.copyOf(channel.getConfig()), driverListener);
			}
		} finally {
			this.m_monitor.leave();
		}

		s_logger.debug("Registering Device Listener for monitoring...Done");
	}

	/**
	 * Retrieve channels from the provided properties.
	 *
	 * @param properties
	 *            the properties
	 */
	private void retrieveConfigurationsFromProperties(final Map<String, Object> properties) {
		s_logger.debug("Retrieving configurations from the properties...");
		this.m_deviceConfiguration = DeviceConfiguration.of(properties);
		s_logger.debug("Retrieving configurations from the properties...Done");

	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("Device Configuration", this.m_deviceConfiguration).toString();
	}

	/** {@inheritDoc} */
	@Override
	public void unregisterDeviceListener(final DeviceListener deviceListener) throws KuraException {
		checkCondition(deviceListener == null, "Device Listener cannot be null");
		s_logger.debug("Unregistering Device Listener...");

		checkCondition(this.m_driver == null, "Driver cannot be null");
		this.m_monitor.enter();
		try {
			this.m_driver.unregisterDriverListener(this.m_deviceListeners.get(deviceListener));
		} finally {
			this.m_monitor.leave();
		}
		this.m_deviceListeners.remove(deviceListener);
		s_logger.debug("Unregistering Device Listener...Done");
	}

	/**
	 * Callback method used to trigger when this service component will be
	 * updated.
	 *
	 * @param properties
	 *            the configurable properties
	 */
	protected synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug("Updating Base Device Configurations...");

		this.m_properties = properties;
		this.retrieveConfigurationsFromProperties(properties);
		this.attachDriver(this.m_deviceConfiguration.getDriverId());

		s_logger.debug("Updating Base Device Configurations...Done");
	}

	/** {@inheritDoc} */
	@Override
	public List<DeviceRecord> write(final List<DeviceRecord> deviceRecords) throws KuraException {
		s_logger.debug("Writing to channels...");
		checkCondition((deviceRecords == null) || deviceRecords.isEmpty(), "Device Records cannot be null or empty");

		final List<DriverRecord> driverRecords = Lists.newArrayList();
		final Map<DriverRecord, DeviceRecord> mappedRecords = Maps.newHashMap();

		final Map<String, Channel> channels = this.m_deviceConfiguration.getChannels();
		for (final DeviceRecord deviceRecord : deviceRecords) {
			checkCondition(!channels.containsKey(deviceRecord.getChannelName()), "Channel not available");

			final Channel channel = channels.get(deviceRecord.getChannelName());
			checkCondition((channel.getType() != ChannelType.WRITE) || !(channel.getType() != ChannelType.READ_WRITE),
					"Channel type not within defined types (WRITE OR READ_WRITE) : " + channel);

			final DriverRecord driverRecord = new DriverRecord();
			driverRecord.setChannelConfig(ImmutableMap.copyOf(channel.getConfig()));
			driverRecord.setChannelName(channel.getName());
			driverRecord.setValue(deviceRecord.getValue());

			driverRecords.add(driverRecord);
			mappedRecords.put(driverRecord, deviceRecord);
		}

		checkCondition(this.m_driver == null, "Driver cannot be null");
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
		s_logger.debug("Writing to channels...Done");
		return deviceRecords;
	}
}
