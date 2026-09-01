package org.eclipse.soda.dk.comm;

/*************************************************************************
 * Copyright (c) 1999, 2009 IBM.                                         *
 * All rights reserved. This program and the accompanying materials      *
 * are made available under the terms of the Eclipse Public License v1.0 *
 * which accompanies this distribution, and is available at              *
 * http://www.eclipse.org/legal/epl-v10.html                             *
 *                                                                       *
 * Contributors:                                                         *
 *     IBM - initial API and implementation                              *
 ************************************************************************/
import java.io.IOException;
import javax.comm.CommDriver;
import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;
import org.eclipse.soda.dk.comm.internal.Library;

/**
 * @author IBM
 * @version 1.2.0
 * @since 1.0
 */
public class NSCommDriver implements CommDriver {
	static {
		try {
			Library.load_dkcomm();
		} catch (final UnsatisfiedLinkError exception) {
			exception.printStackTrace();
		}
	}

	/**
	 * Define the device list (DeviceList) field.
	 */
	DeviceList devicelist = new DeviceList();

	/**
	 * Add device to list with the specified port name, port type, device name and sem id parameters.
	 * @param portName
	 *		The port name (<code>String</code>) parameter.
	 * @param portType
	 *		The port type (<code>int</code>) parameter.
	 * @param deviceName
	 *		The device name (<code>String</code>) parameter.
	 * @param semID
	 *		The sem ID (<code>int</code>) parameter.
	 */
	protected void addDeviceToList(final String portName, final int portType, final String deviceName, final int semID) {
		DeviceListEntry cur = this.devicelist.headEntry;
		DeviceListEntry prev = null;
		while (cur != null) {
			prev = cur;
			cur = cur.next;
		}
		cur = new DeviceListEntry();
		cur.logicalName = portName;
		cur.physicalName = deviceName;
		cur.portType = portType;
		cur.semID = semID;
		cur.next = null;
		if (prev == null) {
			this.devicelist.headEntry = cur;
		} else {
			prev.next = cur;
		}
	}

	/**
	 * Discover devices nc.
	 */
	private native void discoverDevicesNC();

	/**
	 * Get comm port with the specified port name and port type parameters and return the CommPort result.
	 * @param portName
	 *		The port name (<code>String</code>) parameter.
	 * @param portType
	 *		The port type (<code>int</code>) parameter.
	 * @return Results of the get comm port (<code>CommPort</code>) value.
	 */
	public CommPort getCommPort(final String portName, final int portType) {
		CommPort port = null;
		try {
			switch (portType) {
			case CommPortIdentifier.PORT_SERIAL:
				port = new NSSerialPort(portName, this);
				break;
			case CommPortIdentifier.PORT_PARALLEL:
				port = new NSParallelPort(portName, this);
				break;
			}
		} catch (final IOException exception) {
			exception.printStackTrace();
			/* port is being used by another app? */
		}
		return port;
	}

	/**
	 * Gets the first dle (DeviceListEntry) value.
	 * @return The first dle (<code>DeviceListEntry</code>) value.
	 */
	DeviceListEntry getFirstDLE() {
		return this.devicelist.headEntry;
	}

	/**
	 * Get next dle with the specified dle parameter and return the DeviceListEntry result.
	 * @param dle
	 *		The dle (<code>DeviceListEntry</code>) parameter.
	 * @return Results of the get next dle (<code>DeviceListEntry</code>) value.
	 */
	DeviceListEntry getNextDLE(final DeviceListEntry dle) {
		DeviceListEntry cur = this.devicelist.headEntry;
		DeviceListEntry ndle = null;
		while (cur != null) {
			if (cur == dle) {
				ndle = cur.next;
				break;
			}
			cur = cur.next;
		}
		return ndle;
	}

	/**
	 * Initialize.
	 */
	public void initialize() {
		discoverDevicesNC();
		for (DeviceListEntry cur = getFirstDLE(); cur != null; cur = getNextDLE(cur)) {
			CommPortIdentifier.addPortName(cur.logicalName, cur.portType, this);
		}
	}
}
