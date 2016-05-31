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

import static org.eclipse.kura.device.util.Preconditions.checkCondition;

import org.eclipse.kura.device.DeviceEvent;
import org.eclipse.kura.device.DeviceFlag;
import org.eclipse.kura.device.DeviceListener;
import org.eclipse.kura.device.DeviceRecord;
import org.eclipse.kura.device.DriverEvent;
import org.eclipse.kura.device.DriverFlag;
import org.eclipse.kura.device.DriverListener;
import org.eclipse.kura.device.DriverRecord;
import org.eclipse.kura.device.util.Devices;

/**
 * This is a basic driver listener used to listen for driver events so that it
 * can be propagated upwards to the respective device listener
 *
 * @see DeviceListener
 * @see DriverListener
 * @see DeviceEvent
 * @see DriverEvent
 */
public final class BaseDriverListener implements DriverListener {

	/** The channel name. */
	private final String m_channelName;

	/** The device listener instance. */
	private final DeviceListener m_deviceListener;

	/**
	 * Instantiates a new base driver listener.
	 *
	 * @param channelName
	 *            the channel name as provided
	 * @param deviceListener
	 *            the device listener
	 */
	public BaseDriverListener(final String channelName, final DeviceListener deviceListener) {
		this.m_channelName = channelName;
		this.m_deviceListener = deviceListener;
	}

	/** {@inheritDoc} */
	@Override
	public void onDriverEvent(final DriverEvent event) {
		checkCondition(event == null, "Driver Event cannot be null");
		final DriverRecord driverRecord = event.getDriverRecord();
		final DeviceRecord deviceRecord = Devices.newDeviceRecord(this.m_channelName);
		final DriverFlag driverFlag = driverRecord.getDriverFlag();

		switch (driverFlag) {
		case READ_SUCCESSFUL:
			deviceRecord.setDeviceFlag(DeviceFlag.READ_SUCCESSFUL);
			break;
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
		deviceRecord.setValue(driverRecord.getValue());
		final DeviceEvent deviceEvent = new DeviceEvent(deviceRecord);
		this.m_deviceListener.onDeviceEvent(deviceEvent);
	}
}