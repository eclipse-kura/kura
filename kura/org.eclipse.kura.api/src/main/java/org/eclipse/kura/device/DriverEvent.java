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

import static org.eclipse.kura.Preconditions.checkNull;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;

import com.google.common.base.MoreObjects;

/**
 * The DriverEvent class represents an event occurred while monitoring specific
 * channel configuration by the driver.
 */
@Immutable
@ThreadSafe
public final class DriverEvent {

	/**
	 * Represents the driver record as triggered due to the driver monitor
	 * operation.
	 */
	private final DriverRecord driverRecord;

	/**
	 * Instantiates a new driver event.
	 *
	 * @param driverRecord
	 *            the driver record
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public DriverEvent(final DriverRecord driverRecord) {
		checkNull(driverRecord, "Driver record cannot be null");
		this.driverRecord = driverRecord;
	}

	/**
	 * Returns the associated driver record.
	 *
	 * @return the driver record
	 */
	public DriverRecord getDriverRecord() {
		return this.driverRecord;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("driver_record", this.driverRecord).toString();
	}

}
