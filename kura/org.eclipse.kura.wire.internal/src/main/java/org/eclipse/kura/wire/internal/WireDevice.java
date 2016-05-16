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
package org.eclipse.kura.wire.internal;

import static org.eclipse.kura.device.internal.DevicePreconditions.checkCondition;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.device.Channel;
import org.eclipse.kura.device.DeviceRecord;
import org.eclipse.kura.device.internal.BaseDevice;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.timer.Timer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * The Class WireDevice is a wire component which provides all necessary higher
 * level abstraction of a Kura device. This wire device is an integral wire
 * component in Kura Wires topology as it represents an industrial device with a
 * field protocol driver associated to it.
 *
 * The WireRecord to be emitted by every wire device comprises the following
 * keys
 *
 * <ul>
 * <li>Channel_Name</li>
 * <li>Device_Flag</li>
 * <li>Timestamp</li>
 * <li>Value</li>
 * </ul>
 */
public final class WireDevice extends BaseDevice implements WireComponent, WireEmitter, WireReceiver {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(WireDevice.class);

	/** Wire Supporter Component */
	protected WireSupport m_wireSupport;

	/**
	 * Instantiates a new wire device.
	 */
	public WireDevice() {
		this.m_wireSupport = WireSupport.of(this);
	}

	/** {@inheritDoc} */
	@Override
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.debug("Activating Wire Device...");
		super.activate(componentContext, properties);
		s_logger.debug("Activating Wire Device...Done");
	}

	/** {@inheritDoc} */
	@Override
	public void consumersConnected(final Wire[] wires) {
		this.m_wireSupport.consumersConnected(wires);
	}

	/**
	 * Callback used when this service component is deactivating
	 */
	@Override
	protected synchronized void deactivate(final ComponentContext context) {
		s_logger.debug("Deactivating Wire Device...");
		super.deactivate(context);
		s_logger.debug("Deactivating Wire Device...Done");
	}

	/**
	 * Emits the provided list of device records to the associated wires.
	 *
	 * @param recentlyReadRecords
	 *            the list of device records conforming to the aforementioned
	 *            specification
	 * @throws KuraRuntimeException
	 *             if provided records list is null or it is empty
	 */
	private void emitDeviceRecords(final List<DeviceRecord> deviceRecords) {
		checkCondition(deviceRecords == null, "Device records cannot be null");
		checkCondition(deviceRecords.isEmpty(), "Device records cannot be empty");

		final List<WireRecord> wireRecords = Lists.newArrayList();

		for (final DeviceRecord deviceRecord : deviceRecords) {
			final WireField channelWireField = new WireField("Channel_Name",
					new StringValue(deviceRecord.getChannelName()));
			final WireField deviceFlagWireField = new WireField("Device_Flag",
					new StringValue(deviceRecord.getDeviceFlag().name()));
			final WireField timestampWireField = new WireField("Timestamp",
					new LongValue(deviceRecord.getTimetstamp()));
			final WireField valueWireField = new WireField("Value", deviceRecord.getValue());
			final WireRecord wireRecord = new WireRecord(new Timestamp(new Date().getTime()),
					Lists.newArrayList(channelWireField, deviceFlagWireField, timestampWireField, valueWireField));
			wireRecords.add(wireRecord);
		}

		this.m_wireSupport.emit(wireRecords);
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return this.m_deviceConfiguration.getDeviceName();
	}

	/** {@inheritDoc} */
	@Override
	public void onWireReceive(final WireEnvelope wireEnvelope) {
		checkCondition(wireEnvelope == null, "Wire Envelope cannot be null");
		s_logger.debug("Wire Enveloped received..." + this.m_wireSupport);

		final List<DeviceRecord> deviceRecordsToWriteChannels = Lists.newArrayList();
		final List<String> channelsToRead = Lists.newArrayList();

		final Map<String, Channel> channels = this.m_deviceConfiguration.getChannels();

		// determining channels to read
		for (final String channelKey : channels.keySet()) {
			final Channel channel = channels.get(channelKey);
			channelsToRead.add(channel.getName());
		}

		if (wireEnvelope.getRecords().get(0).getFields().get(0).getName().equals(Timer.TIMER_EVENT_FIELD_NAME)) {
			// perform the read operation on timer event receive
			try {
				final List<DeviceRecord> recentlyReadRecords = this.read(channelsToRead);
				this.emitDeviceRecords(recentlyReadRecords);
			} catch (final KuraException e) {
				s_logger.error(
						"Error while performing read from the wire device..." + Throwables.getStackTraceAsString(e));
			}
		}

		// determining channels to write
		for (final WireRecord wireRecord : wireEnvelope.getRecords()) {
			for (final WireField wireField : wireRecord.getFields()) {
				for (final String channelKey : channels.keySet()) {
					final Channel channel = channels.get(channelKey);
					deviceRecordsToWriteChannels.add(this.prepareDeviceRecord(channel, wireField.getValue()));
				}
			}
		}
		// perform the write operation
		try {
			this.write(deviceRecordsToWriteChannels);
		} catch (final KuraException e) {
			s_logger.error(
					"Error while performing write from the wire device..." + Throwables.getStackTraceAsString(e));
		}
	}

	/** {@inheritDoc} */
	@Override
	public Object polled(final Wire wire) {
		return this.m_wireSupport.polled(wire);
	}

	/**
	 * Creates a device record from the provided channel information
	 *
	 * @param channel
	 *            the channel to get the values from
	 * @return the device record
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private DeviceRecord prepareDeviceRecord(final Channel channel, final TypedValue<?> value) {
		checkCondition(channel == null, "Channel cannot be null");
		checkCondition(value == null, "Value cannot be null");

		final DeviceRecord deviceRecord = new DeviceRecord();
		deviceRecord.setChannelName(channel.getName());
		deviceRecord.setValue(value);
		return deviceRecord;
	}

	/** {@inheritDoc} */
	@Override
	public void producersConnected(final Wire[] wires) {
		this.m_wireSupport.producersConnected(wires);
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		this.m_wireSupport.updated(wire, value);
	}

}
