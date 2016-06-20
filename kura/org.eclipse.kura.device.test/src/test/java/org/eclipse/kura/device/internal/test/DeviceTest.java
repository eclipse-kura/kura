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
package org.eclipse.kura.device.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.device.Device;
import org.eclipse.kura.device.DeviceEvent;
import org.eclipse.kura.device.DeviceFlag;
import org.eclipse.kura.device.DeviceListener;
import org.eclipse.kura.device.DeviceRecord;
import org.eclipse.kura.device.Devices;
import org.eclipse.kura.device.internal.BaseDevice;
import org.eclipse.kura.device.internal.DeviceConfiguration;
import org.eclipse.kura.type.TypedValues;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.Lists;

/**
 * The Class DeviceTest is responsible for testing base device implementation
 */
public final class DeviceTest {

	/** The Device Configuration instance. */
	public DeviceConfiguration m_configuration;

	/** Device Reference */
	private volatile Device m_device;

	/** Service Component Registration Callback */
	protected void activate(final ComponentContext conext, final Map<String, Object> properties) {
		this.m_configuration = ((BaseDevice) this.m_device).getDeviceConfiguration();
	}

	/**
	 * Service Registration Callback
	 */
	protected synchronized void bindDevice(final Device device) {
		if (this.m_device == null) {
			this.m_device = device;
		}
	}

	/** Service Component Deregistration Callback */
	protected void deactivate() throws Exception {
		this.m_device = null;
	}

	/**
	 * Test device configuration presence.
	 */
	@Test
	public void testDeviceConfiguration() throws KuraException {
		assertNotNull(this.m_configuration);
		assertEquals(3, this.m_configuration.getChannels().size());
	}

	/**
	 * Test device read operation.
	 */
	@Test
	public void testRead() throws KuraException {
		final String channelName = "sample.channel1";
		final List<String> channelNames = Lists.newArrayList(channelName);
		final List<DeviceRecord> deviceRecords = this.m_device.read(channelNames);
		assertNotNull(deviceRecords);
		assertEquals(false, deviceRecords.isEmpty());
		assertEquals(true, deviceRecords.get(0).getValue().getValue());
		assertEquals(DeviceFlag.READ_SUCCESSFUL, deviceRecords.get(0).getDeviceFlag());
	}

	/**
	 * Test device read operation when channel names are null
	 */
	@Test(expected = KuraRuntimeException.class)
	public void testReadExceptionChannelNamesEmpty() throws KuraException {
		final List<String> channelNames = Lists.newArrayList();
		this.m_device.read(channelNames);
	}

	/**
	 * Test device read operation when channel names are null
	 */
	@Test(expected = KuraRuntimeException.class)
	public void testReadExceptionChannelNamesNull() throws KuraException {
		this.m_device.read(null);
	}

	/**
	 * Test device listen operation.
	 */
	@Test
	public void testRegisterUnregisterListener() throws KuraException {
		final String channelName = "sample.channel3";
		final DeviceListener deviceListener = new DeviceListener() {
			/** {@inheritDoc} */
			@Override
			public void onDeviceEvent(final DeviceEvent event) {
				final DeviceRecord deviceRecord = event.getDeviceRecord();
				assertNotNull(deviceRecord);
				assertEquals(20, deviceRecord.getValue().getValue());
				assertNotNull(deviceRecord.getChannelName());
				assertEquals(channelName, deviceRecord.getChannelName());
				assertEquals(DeviceFlag.READ_SUCCESSFUL, deviceRecord.getDeviceFlag());
				assertTrue(deviceRecord.getTimestamp() != 0);
			}
		};
		this.m_device.registerDeviceListener(channelName, deviceListener);
		assertEquals(1, ((DriverStub) (((BaseDevice) this.m_device).getDriver())).m_listeners.size());
		assertEquals(1, ((BaseDevice) this.m_device).getListeners().size());
		this.m_device.unregisterDeviceListener(deviceListener);
		assertEquals(0, ((DriverStub) (((BaseDevice) this.m_device).getDriver())).m_listeners.size());
		assertEquals(0, ((BaseDevice) this.m_device).getListeners().size());
	}

	/**
	 * Test device write operation.
	 */
	@Test
	public void testWrite() throws KuraException {
		final DeviceRecord deviceRecord = Devices.newDeviceRecord("sample.channel2");
		deviceRecord.setValue(TypedValues.newDoubleValue(23.23));
		final List<DeviceRecord> deviceRecords = Lists.newArrayList(deviceRecord);
		this.m_device.write(deviceRecords);
		assertEquals(DeviceFlag.WRITE_SUCCESSFUL, deviceRecords.get(0).getDeviceFlag());
	}

	/**
	 * Test device write operation when channel is not available
	 */
	@Test(expected = KuraRuntimeException.class)
	public void testWriteExceptionChannelNotAvailable() throws KuraException {
		final DeviceRecord deviceRecord = Devices.newDeviceRecord("sample.channel5");
		deviceRecord.setValue(TypedValues.newDoubleValue(23.23));
		final List<DeviceRecord> deviceRecords = Lists.newArrayList(deviceRecord);
		this.m_device.write(deviceRecords);
	}

	/**
	 * Test device write operation when channel type is to read
	 */
	@Test(expected = KuraRuntimeException.class)
	public void testWriteExceptionChannelTypeRead() throws KuraException {
		final DeviceRecord deviceRecord = Devices.newDeviceRecord("sample.channel1");
		deviceRecord.setValue(TypedValues.newDoubleValue(23.23));
		final List<DeviceRecord> deviceRecords = Lists.newArrayList(deviceRecord);
		this.m_device.write(deviceRecords);
	}

	/**
	 * Test device write operation when device records are null
	 */
	@Test(expected = KuraRuntimeException.class)
	public void testWriteExceptionDeviceRecordsNull() throws KuraException {
		this.m_device.write(null);
	}

	/**
	 * Service Deregistration Callback
	 */
	protected synchronized void unbindDevice(final Device device) {
		if (this.m_device == device) {
			this.m_device = null;
		}
	}

}
