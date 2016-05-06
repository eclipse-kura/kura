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
package org.eclipse.kura.device;

import com.google.common.base.MoreObjects;

/**
 * This class represents an event occurred while monitoring specific channel
 * configuration by the driver
 */
public final class DriverEvent {

	/**
	 * Represents the driver record as triggered due to the driver monitor
	 * operation
	 */
	private final DriverRecord m_driverRecord;

	/** Constructor */
	public DriverEvent(final DriverRecord driverRecord) {
		super();
		this.m_driverRecord = driverRecord;
	}

	/**
	 * Returns the associated driver record
	 */
	public DriverRecord getDriverRecord() {
		return this.m_driverRecord;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("driver_record", this.m_driverRecord).toString();
	}

}
