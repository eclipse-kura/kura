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
package org.eclipse.kura.device.cloudlet;

import static org.eclipse.kura.device.util.Preconditions.checkCondition;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.device.Channel;
import org.eclipse.kura.device.Device;
import org.eclipse.kura.device.DeviceRecord;
import org.eclipse.kura.device.internal.BaseDevice;
import org.eclipse.kura.device.internal.DeviceConfiguration;
import org.eclipse.kura.device.util.Devices;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.util.TypedValues;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * The Class DeviceCloudlet is used to provide MQTT read/write operations on the
 * device. The application id is configured as "DEV-CLOUD".
 *
 * The available EXEC commands are as follows
 * <ul>
 * <li>read</li> e.g /read/device_name/channel_name
 * <li>write</li> e.g /write/device_name/channel_name topic with payload "value"
 * and "type"
 * </ul>
 *
 * The "value" key in the request payload can be one of the following
 * <ul>
 * <li>INTEGER</li>
 * <li>LONG</li>
 * <li>STRING</li>
 * <li>BOOLEAN</li>
 * <li>BYTE</li>
 * <li>SHORT</li>
 * <li>DUBLE</li>
 * </ul>
 *
 * The available GET commands are as follows
 * <ul>
 * <li>list-devices</li> e.g: /list-devices
 * <li>list-channels</li> e.g: /list-channels/device_name
 * </ul>
 *
 * @see Cloudlet
 * @see CloudClient
 * @see DeviceCloudlet#doGet(CloudletTopic, KuraRequestPayload,
 *      KuraResponsePayload)
 * @see DeviceCloudlet#doExec(CloudletTopic, KuraRequestPayload,
 *      KuraResponsePayload)
 */
@Beta
public final class DeviceCloudlet extends Cloudlet {

	/** Application Identifier for Cloudlet. */
	private static final String APP_ID = "DEV-CLOUD";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DeviceCloudlet.class);

	/** Cloud Service Dependency. */
	private volatile CloudService m_cloudService;

	/** The map of devices present in the OSGi service registry. */
	private Map<String, Device> m_devices;

	/** Device Driver Tracker. */
	private DeviceTracker m_deviceTracker;

	/**
	 * Instantiates a new device cloudlet.
	 *
	 * @param appId
	 *            the application id
	 */
	protected DeviceCloudlet(final String appId) {
		super(APP_ID);
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
		s_logger.debug("Activating Device Cloudlet...");
		super.activate(componentContext);
		try {
			this.m_deviceTracker = new DeviceTracker(componentContext.getBundleContext());
		} catch (final InvalidSyntaxException e) {
			s_logger.error("Error in searching for devices..." + Throwables.getStackTraceAsString(e));
		}
		s_logger.debug("Activating Device Cloudlet...Done");
	}

	/**
	 * Callback to be used while {@link CloudService} is registering.
	 *
	 * @param cloudService
	 *            the cloud service
	 */
	public synchronized void bindCloudService(final CloudService cloudService) {
		if (this.m_cloudService == null) {
			this.m_cloudService = cloudService;
			super.setCloudService(this.m_cloudService);
		}
	}

	/**
	 * Callback method used to trigger when this service component will be
	 * deactivated.
	 *
	 * @param componentContext
	 *            the component context
	 */
	@Override
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.debug("Deactivating Device Cloudlet...");
		super.deactivate(componentContext);
		this.m_cloudService = null;
		s_logger.debug("Deactivating Device Cloudlet...Done");
	}

	/**
	 * The device cloudlet receives a request to perform EXEC operations
	 *
	 * @param reqTopic
	 *            the request topic
	 * @param reqPayload
	 *            the request payload
	 * @param respPayload
	 *            the response payload
	 * @throws KuraException
	 *             the kura exception
	 * @see Cloudlet
	 */
	@Override
	protected void doExec(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
			final KuraResponsePayload respPayload) throws KuraException {
		s_logger.info("Cloudlet EXEC Request received on the Device Cloudlet....");
		// Checks if the operation name "read", the name of the device and the
		// name of the channel are provided
		if ("read".equals(reqTopic.getResources()[0]) && (reqTopic.getResources().length > 2)) {
			// perform a search operation at the beginning
			this.findDevices();
			final String deviceName = reqTopic.getResources()[1];
			final String channelName = reqTopic.getResources()[2];
			final BaseDevice device = (BaseDevice) this.m_devices.get(deviceName);
			final DeviceConfiguration configuration = device.getDeviceConfiguration();
			final Map<String, Channel> deviceConfiguredChannels = configuration.getChannels();
			if ((deviceConfiguredChannels != null) && deviceConfiguredChannels.containsKey(channelName)) {
				final List<DeviceRecord> deviceRecords = device.read(Lists.newArrayList(channelName));
				for (final DeviceRecord deviceRecord : deviceRecords) {
					respPayload.addMetric("Flag", deviceRecord.getDeviceFlag());
					respPayload.addMetric("Timestamp", deviceRecord.getTimestamp());
					respPayload.addMetric("Value", deviceRecord.getValue());
					respPayload.addMetric("Channel_Name", deviceRecord.getChannelName());
				}
			}
		}
		// Checks if the operation name "write", the name of the device and the
		// name of the channel are provided
		if ("write".equals(reqTopic.getResources()[0]) && (reqTopic.getResources().length > 2)) {
			// perform a search operation at the beginning
			this.findDevices();
			final String deviceName = reqTopic.getResources()[1];
			final String channelName = reqTopic.getResources()[2];
			final BaseDevice device = (BaseDevice) this.m_devices.get(deviceName);
			final DeviceConfiguration configuration = device.getDeviceConfiguration();
			final Map<String, Channel> deviceConfiguredChannels = configuration.getChannels();
			if ((deviceConfiguredChannels != null) && deviceConfiguredChannels.containsKey(channelName)) {
				final DeviceRecord deviceRecord = Devices.newDeviceRecord(channelName);
				final String userValue = (String) reqPayload.getMetric("value");
				final String userType = (String) reqPayload.getMetric("type");
				this.wrapValue(deviceRecord, userValue, userType);
				final List<DeviceRecord> deviceRecords = device.write(Lists.newArrayList(deviceRecord));
				for (final DeviceRecord record : deviceRecords) {
					respPayload.addMetric("Flag", record.getDeviceFlag());
					respPayload.addMetric("Timestamp", record.getTimestamp());
					respPayload.addMetric("Value", record.getValue());
					respPayload.addMetric("Channel_Name", record.getChannelName());
				}
			}
		}
		s_logger.info("Cloudlet GET Request received on the Device Cloudlet....Done");
	}

	/**
	 * The device cloudlet receives a request to perform GET operations
	 *
	 * @param reqTopic
	 *            the request topic
	 * @param reqPayload
	 *            the request payload
	 * @param respPayload
	 *            the response payload
	 * @throws KuraException
	 *             the kura exception
	 * @see Cloudlet
	 */
	@Override
	protected void doGet(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
			final KuraResponsePayload respPayload) throws KuraException {
		s_logger.info("Cloudlet GET Request received on the Device Cloudlet");
		if ("list-devices".equals(reqTopic.getResources()[0])) {
			this.findDevices();
			int index = 1;
			for (final String deviceName : this.m_devices.keySet()) {
				final BaseDevice device = (BaseDevice) this.m_devices.get(deviceName);
				respPayload.addMetric(String.valueOf(index++), device.getDeviceConfiguration().getDeviceName());
			}
		}
		if ("list-channels".equals(reqTopic.getResources()[0]) && (reqTopic.getResources().length > 1)) {
			this.findDevices();
			final String deviceName = reqTopic.getResources()[1];
			final BaseDevice device = (BaseDevice) this.m_devices.get(deviceName);
			final DeviceConfiguration configuration = device.getDeviceConfiguration();
			final Map<String, Channel> deviceConfiguredChannels = configuration.getChannels();
			int index = 1;
			for (final String channelName : deviceConfiguredChannels.keySet()) {
				respPayload.addMetric(String.valueOf(index++), channelName);
			}
		}
		s_logger.info("Cloudlet GET Request received on the Device Cloudlet");
	}

	/**
	 * Searches for all the currently available devices in the service registry
	 */
	private void findDevices() {
		this.m_devices = this.m_deviceTracker.getRegisteredDevices();
	}

	/**
	 * Callback to be used while {@link CloudService} is deregistering.
	 *
	 * @param cloudService
	 *            the cloud service
	 */
	public synchronized void unbindCloudService(final CloudService cloudService) {
		if (this.m_cloudService == cloudService) {
			this.m_cloudService = null;
			super.setCloudService(null);
		}
	}

	/**
	 * Wraps the provided user provided value to the an instance of
	 * {@link TypedValue} in the device record
	 *
	 * @param deviceRecord
	 *            the device record to contain the typed value
	 * @param userValue
	 *            the value to wrap
	 * @param userType
	 *            the type to use
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private void wrapValue(final DeviceRecord deviceRecord, final String userValue, final String userType) {
		checkCondition(deviceRecord == null, "Device Record cannot be null");
		checkCondition(userValue == null, "User Provided Value cannot be null");
		checkCondition(userType == null, "User Provided Type cannot be null");

		TypedValue<?> value = null;

		if ("INTEGER".equalsIgnoreCase(userType)) {
			value = TypedValues.newIntegerValue(Integer.valueOf(userValue));
		}
		if ("BOOLEAN".equalsIgnoreCase(userType)) {
			value = TypedValues.newBooleanValue(Boolean.valueOf(userValue));
		}
		if ("BYTE".equalsIgnoreCase(userType)) {
			value = TypedValues.newByteValue(Byte.valueOf(userValue));
		}
		if ("DOUBLE".equalsIgnoreCase(userType)) {
			value = TypedValues.newDoubleValue(Double.valueOf(userValue));
		}
		if ("LONG".equalsIgnoreCase(userType)) {
			value = TypedValues.newLongValue(Long.valueOf(userValue));
		}
		if ("SHORT".equalsIgnoreCase(userType)) {
			value = TypedValues.newShortValue(Short.valueOf(userValue));
		}
		if ("STRING".equalsIgnoreCase(userType)) {
			value = TypedValues.newStringValue(userValue);
		}
		if (userValue != null) {
			deviceRecord.setValue(value);
		}
	}

}
