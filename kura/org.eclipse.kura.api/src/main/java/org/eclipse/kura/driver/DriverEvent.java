/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.driver;

import static java.util.Objects.requireNonNull;

import org.eclipse.kura.annotation.NotThreadSafe;

/**
 * The DriverEvent class represents an event occurred while monitoring specific
 * channel configuration by the driver
 */
@NotThreadSafe
public final class DriverEvent {

    /**
     * Represents the driver record as triggered due to the driver monitor
     * operation.
     */
    private final DriverRecord driverRecord;

    /**
     * Instantiates a new driver event
     *
     * @param driverRecord
     *            the driver record
     * @throws NullPointerException
     *             if the argument is null
     */
    public DriverEvent(final DriverRecord driverRecord) {
        requireNonNull(driverRecord, "Driver record cannot be null");
        this.driverRecord = driverRecord;
    }

    /**
     * Returns the associated driver record
     *
     * @return the driver record
     */
    public DriverRecord getDriverRecord() {
        return this.driverRecord;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DriverEvent [driverRecord=" + this.driverRecord + "]";
    }

}
