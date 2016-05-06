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

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.device.Device;
import org.eclipse.kura.device.internal.BaseDevice;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.google.common.base.Throwables;

/**
 * The Class DeviceCloudlet is used to provide MQTT read/write operations on the
 * device
 */
@Beta
public final class DeviceCloudlet extends Cloudlet {

	/** Application Identifier for Cloudlet. */
	private static final String APP_ID = "DEVICE-CLOUD";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DeviceCloudlet.class);

	/** Cloud Service Dependency. */
	private volatile CloudService m_cloudService;

	/** The list of devices present in the OSGi service registry. */
	private List<Device> m_devices;

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
		s_logger.debug("Deactivating Device Cloudlet...Done");
	}

	/**
	 * The device cloudlet receives a request to perform GET operations on
	 * following commands.
	 *
	 * The available commands are as follows
	 * <ul>
	 * <li>list-devices</li>
	 * </ul>
	 *
	 * @param reqTopic
	 *            the req topic
	 * @param reqPayload
	 *            the req payload
	 * @param respPayload
	 *            the resp payload
	 * @throws KuraException
	 *             the kura exception
	 * @see Cloudlet
	 */
	@Override
	protected void doGet(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
			final KuraResponsePayload respPayload) throws KuraException {
		s_logger.info("Cloudlet GET Request received on the Device Cloudlet");
		if ("list-devices".equals(reqTopic.getResources()[0])) {
			this.m_devices = this.m_deviceTracker.getDevicesList();
			int index = 1;
			for (final Device device : this.m_devices) {
				respPayload.addMetric(String.valueOf(index++), ((BaseDevice) device).getDeviceName());
			}
		}
		s_logger.info("Cloudlet GET Request received on the Device Cloudlet");
	}

	/**
	 * The device cloudlet receives a request to perform PUT operations on
	 * following commands.
	 *
	 * The available commands are as follows
	 * <ul>
	 * <li></li>
	 * </ul>
	 *
	 * @param reqTopic
	 *            the req topic
	 * @param reqPayload
	 *            the req payload
	 * @param respPayload
	 *            the resp payload
	 * @throws KuraException
	 *             the kura exception
	 * @see Cloudlet
	 */
	@Override
	protected void doPut(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
			final KuraResponsePayload respPayload) throws KuraException {
		// TODO Auto-generated method stub
		super.doPut(reqTopic, reqPayload, respPayload);
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

}
