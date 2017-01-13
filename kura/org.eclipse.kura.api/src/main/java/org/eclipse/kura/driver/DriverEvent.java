/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.driver;

import static java.util.Objects.requireNonNull;

import org.eclipse.kura.annotation.NotThreadSafe;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The DriverEvent class represents an event occurred while monitoring specific
 * channel configuration by the driver
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@NotThreadSafe
@ProviderType
public class DriverEvent {

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
