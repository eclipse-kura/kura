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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraErrorCode;
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
import org.eclipse.kura.type.DataType;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * The Class BaseDevice is a basic device implementation of a Kura Field Device
 * which associates a device driver
 */
public class BaseDevice implements Device, SelfConfiguringComponent {

	/** Device Description Property to be used in the configuration */
	private static final String DEVICE_DESC_PROP = "device.desc";

	/** Device Driver Name Property to be used in the configuration */
	private static final String DEVICE_DRIVER_PROP = "driver.id";

	/** Device Name Property to be used in the configuration */
	private static final String DEVICE_ID_PROP = "device.name";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(BaseDevice.class);

	/** The list of channels associated with this device. */
	protected final Map<String, Channel> m_channels;

	/** The service component context. */
	private ComponentContext m_ctx;

	/** The device description */
	protected String m_deviceDescription;

	/** Container of mapped device listeners and drivers listener */
	private final Map<DeviceListener, DriverListener> m_deviceListeners;

	/** The device name */
	protected String m_deviceName;

	/** The Driver instance. */
	protected volatile Driver m_driver;

	/** Name of the driver to be associated with */
	private String m_driverId;

	/** Device Driver Tracker. */
	private DriverTracker m_driverTracker;

	/** Synchronization Monitor for driver specific operations. */
	private final Object m_monitor = new Object();

	/** The configurable properties of this device. */
	private Map<String, Object> m_properties;

	/**
	 * Instantiates a new base device.
	 */
	public BaseDevice() {
		this.m_channels = Maps.newConcurrentMap();
		this.m_deviceListeners = Maps.newConcurrentMap();
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
	 * Adds the channel to the map of all the associated channels
	 *
	 * @param channel
	 *            the channel to be inserted
	 */
	private void addChannel(final Channel channel) {
		this.m_channels.put(channel.getName(), channel);
	}

	/**
	 * Tracks the Driver in the OSGi service registry with the specified driver
	 * id.
	 *
	 * @param driverId
	 *            the identifier of the driver
	 */
	private synchronized void attachDriver(final String driverId) {
		s_logger.debug("Attaching driver instance...");
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
	 */
	private Tad cloneAd(final Tad oldAd, final String prefix) {
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
		try {
			synchronized (this.m_monitor) {
				this.m_driver.disconnect();
			}
		} catch (final KuraException e) {
			s_logger.error("Error in disconnecting driver..." + Throwables.getStackTraceAsString(e));
		}
		this.m_driver = null;
		s_logger.debug("Deactivating Base Device...Done");
	}

	/**
	 * Retrieves the map of configured channels to this device
	 *
	 * @return the channels' map
	 */
	public Map<String, Channel> getChannels() {
		return this.m_channels;
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		final String componentName = this.m_ctx.getProperties().get("service.pid").toString();
		final String CHANNEL_PROPERTY_PREFIX = "CH";
		final String CHANNEL_PROPERTY_POSTFIX = ".";

		final Tocd mainOcd = new Tocd();
		mainOcd.setName(this.m_deviceName);
		mainOcd.setDescription(this.m_deviceDescription);
		mainOcd.setId(this.m_deviceName);

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

			final BaseDeviceChannelDescriptor basicDeviceChanneldescriptor = BaseDeviceChannelDescriptor.getDefault();
			basicDeviceChanneldescriptor.getDefaultConfiguration().addAll(channelConfiguration);

			for (final Tad attribute : channelConfiguration) {
				final Tad newAttribute = this.cloneAd(attribute,
						CHANNEL_PROPERTY_PREFIX + System.currentTimeMillis() + CHANNEL_PROPERTY_POSTFIX);
				mainOcd.addAD(newAttribute);
			}
		}
		return new ComponentConfigurationImpl(componentName, mainOcd, props);
	}

	/**
	 * Returns the name of the device
	 *
	 * @return the name
	 */
	public String getDeviceName() {
		return this.m_deviceName;
	}

	/** {@inheritDoc} */
	@Override
	public List<DeviceRecord> read(final List<String> channelNames) throws KuraException {
		s_logger.debug("Reading device channels...");

		final List<DeviceRecord> deviceRecords = Lists.newArrayList();
		final List<DriverRecord> driverRecords = Lists.newArrayList();

		for (final String channelName : channelNames) {

			if (!this.m_channels.containsKey(channelName)) {
				throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "Channel not available");
			}

			final Channel channel = this.m_channels.get(channelName);
			if (!(channel.getType() == ChannelType.READ) || !(channel.getType() == ChannelType.READ_WRITE)) {
				throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "Channel type not within defined types");
			}

			final DriverRecord driverRecord = new DriverRecord();
			driverRecord.setChannelConfig(ImmutableMap.copyOf(this.m_channels.get(channelName).getConfig()));
			driverRecord.setChannelName(channelName);

			driverRecords.add(driverRecord);
		}

		if (this.m_driver == null) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "Driver not available");
		}
		synchronized (this.m_monitor) {
			this.m_driver.read(driverRecords);
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
			default:
				break;
			}

			deviceRecord.setTimetstamp(driverRecord.getTimetstamp());
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
		s_logger.debug("Registering Device Listener for monitoring...");

		Channel channel = null;

		if (this.m_channels.containsKey(channelName)) {
			channel = this.m_channels.get(channelName);
		}

		final DriverListener driverListener = new BaseDriverListener(deviceListener);

		this.m_deviceListeners.put(deviceListener, driverListener);
		if (this.m_driver == null) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "Driver not available");
		}
		synchronized (this.m_monitor) {
			this.m_driver.registerDriverListener(ImmutableMap.copyOf(channel.getConfig()), driverListener);
		}

		s_logger.debug("Registering Device Listener for monitoring...Done");
	}

	/**
	 * Retrieve channel specific configuration from the provided properties. The
	 * representation in the provided properties signifies a single channel and
	 * it should conform to the following specification.
	 *
	 * The properties should contain the following values
	 * <ul>
	 * <li>name</li>
	 * <li>type</li>
	 * <li>value_type</li>
	 * <li>channel_config</li>
	 * </ul>
	 *
	 * The key "name" must be String. The key "value_type" must be in one of the
	 * following
	 *
	 * <ul>
	 * <li>INTEGER</li>
	 * <li>BOOLEAN</li>
	 * <li>BYTE</li>
	 * <li>DOUBLE</li>
	 * <li>LONG</li>
	 * <li>SHORT</li>
	 * <li>STRING</li>
	 * <li>BYTE_ARRAY</li>
	 * </ul>
	 *
	 * The channel "type" should be one of the following
	 *
	 * <ul>
	 * <li>READ</li>
	 * <li>WRITE</li>
	 * <li>READ_WRITE</li>
	 * </ul>
	 *
	 * The "channel_config" is a map which provides all the channel specific
	 * settings.
	 *
	 * @param properties
	 *            the properties to retrieve channel from
	 * @return the specific channel
	 */
	@SuppressWarnings("unchecked")
	private Channel retrieveChannel(final Map<String, Object> properties) {
		s_logger.debug("Retrieving single channel information from the properties...");

		String channelName = null;
		ChannelType channelType = null;
		DataType dataType = null;
		Map<String, Object> channelConfig = null;

		if (properties != null) {
			if (properties.containsKey("name")) {
				channelName = (String) properties.get("name");
			}
			if (properties.containsKey("type")) {
				final String type = (String) properties.get("type");
				if ("READ".equals(type)) {
					channelType = ChannelType.READ;
				}
				if ("WRITE".equals(type)) {
					channelType = ChannelType.WRITE;
				}
				if ("READ_WRITE".equals(type)) {
					channelType = ChannelType.READ_WRITE;
				}
			}
			if (properties.containsKey("value_type")) {
				final String type = (String) properties.get("value_type");

				if ("INTEGER".equalsIgnoreCase(type)) {
					dataType = DataType.INTEGER;
				}
				if ("BOOLEAN".equalsIgnoreCase(type)) {
					dataType = DataType.BOOLEAN;
				}
				if ("BYTE".equalsIgnoreCase(type)) {
					dataType = DataType.BYTE;
				}
				if ("DOUBLE".equalsIgnoreCase(type)) {
					dataType = DataType.DOUBLE;
				}
				if ("LONG".equalsIgnoreCase(type)) {
					dataType = DataType.LONG;
				}
				if ("SHORT".equalsIgnoreCase(type)) {
					dataType = DataType.SHORT;
				}
				if ("STRING".equalsIgnoreCase(type)) {
					dataType = DataType.STRING;
				}
				if ("BYTE_ARRAY".equalsIgnoreCase(type)) {
					dataType = DataType.BYTE_ARRAY;
				}
			}
			if (properties.containsKey("channel_config")) {
				channelConfig = (Map<String, Object>) properties.get("channel_config");
			}
		}
		s_logger.debug("Retrieving single channel information from the properties...Done");

		return Channel.of(channelName, channelType, dataType, channelConfig);
	}

	/**
	 * Retrieve channels from properties. The properties must conform to the
	 * following specifications. The properties must have the following.
	 *
	 * <ul>
	 * <li>CHx.</li> where x is any no (eg: CH103432214. / CH1124124215. /
	 * CH5654364436. etc) (Note it the format includes a "." at the end)
	 * <li>map object containing a channel configuration</li>
	 * <li>the value associated with "driver.id" key in the map denotes the
	 * driver instance name to be consumed by this device</li>
	 * <li>A value associated with key "device.name" must be present to denote
	 * the device name</li>
	 * <li>A value associated with "device.desc" key denotes the device
	 * description</li>
	 * </ul>
	 *
	 * For further information on how to provide a channel configuration, @see
	 * {@link BaseDevice#retrieveChannel(Map)}
	 *
	 * @param properties
	 *            the properties to be used to retrieve channels from
	 */
	@SuppressWarnings("unchecked")
	private void retrieveConfigurationsFromProperties(final Map<String, Object> properties) {
		s_logger.debug("Retrieving configurations from the properties...");

		final HashSet<Integer> parsedIndexes = Sets.newHashSet();
		// Matching channel information
		final Pattern pattern = Pattern.compile("(CH)\\d{1,}\\.");

		for (final String property : properties.keySet()) {
			try {
				final Matcher matcher = pattern.matcher(property);
				if (matcher.matches()) {
					final String prefix = property.substring(0, property.indexOf('.') + 1);
					final int index = Integer.parseInt(prefix.substring(1, prefix.length() - 1));
					if (!parsedIndexes.contains(index)) {
						parsedIndexes.add(index);
						final Channel channel = this.retrieveChannel((Map<String, Object>) properties.get(index));
						this.addChannel(channel);
					}
				}
				if (properties.containsKey(DEVICE_DRIVER_PROP)) {
					this.m_driverId = (String) properties.get(DEVICE_DRIVER_PROP);
				}
				if (properties.containsKey(DEVICE_ID_PROP)) {
					this.m_deviceName = (String) properties.get(DEVICE_ID_PROP);
				}
				if (properties.containsKey(DEVICE_DESC_PROP)) {
					this.m_deviceDescription = (String) properties.get(DEVICE_DESC_PROP);
				}
			} catch (final Exception ex) {
				s_logger.error("Error while retrieving channels from the provided configurable properties..."
						+ Throwables.getStackTraceAsString(ex));
			}
		}
		s_logger.debug("Retrieving configurations from the properties...Done");

	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("device_name", this.m_deviceName)
				.add("device_description", this.m_deviceDescription).add("driver_name", this.m_driverId)
				.add("associated_channels", this.m_channels).add("associated_properties", this.m_properties).toString();
	}

	/** {@inheritDoc} */
	@Override
	public void unregisterDeviceListener(final DeviceListener deviceListener) throws KuraException {
		s_logger.debug("Unregistering Device Listener...");

		if (this.m_driver == null) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "Driver not available");
		}
		synchronized (this.m_monitor) {
			this.m_driver.unregisterDriverListener(this.m_deviceListeners.get(deviceListener));
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
		this.attachDriver(this.m_driverId);

		s_logger.debug("Updating Base Device Configurations...Done");
	}

	/** {@inheritDoc} */
	@Override
	public List<DeviceRecord> write(final List<DeviceRecord> deviceRecords) throws KuraException {
		s_logger.debug("Writing to channels...");

		final List<DriverRecord> driverRecords = Lists.newArrayList();

		for (final DeviceRecord deviceRecord : deviceRecords) {

			if (!this.m_channels.containsKey(deviceRecord.getChannelName())) {
				throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "Channel not available");
			}

			final Channel channel = this.m_channels.get(deviceRecord.getChannelName());
			if (!(channel.getType() == ChannelType.WRITE) || !(channel.getType() == ChannelType.READ_WRITE)) {
				throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "Channel type not within defined types");
			}

			final DriverRecord driverRecord = new DriverRecord();
			driverRecord.setChannelConfig(ImmutableMap.copyOf(channel.getConfig()));
			driverRecord.setChannelName(channel.getName());
			driverRecord.setValue(deviceRecord.getValue());

			driverRecords.add(driverRecord);
		}

		if (this.m_driver == null) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "Driver not available");
		}
		synchronized (this.m_monitor) {
			this.m_driver.write(driverRecords);
		}

		for (final DriverRecord driverRecord : driverRecords) {
			final DeviceRecord deviceRecord = new DeviceRecord();
			deviceRecord.setChannelName(driverRecord.getChannelName());

			final DriverFlag driverFlag = driverRecord.getDriverFlag();

			switch (driverFlag) {
			case WRITE_SUCCESSFUL:
				deviceRecord.setDeviceFlag(DeviceFlag.WRITE_SUCCESSFUL);
				break;
			case DRIVER_ERROR_UNSPECIFIED:
				deviceRecord.setDeviceFlag(DeviceFlag.DEVICE_ERROR_UNSPECIFIED);
				break;
			default:
				break;
			}

			deviceRecord.setTimetstamp(driverRecord.getTimetstamp());
			deviceRecords.add(deviceRecord);
		}
		s_logger.debug("Writing to channels...Done");
		return deviceRecords;
	}

}
