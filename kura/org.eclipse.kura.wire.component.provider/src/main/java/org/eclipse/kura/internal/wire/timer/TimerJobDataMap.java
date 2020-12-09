/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.internal.wire.timer;

import org.eclipse.kura.wire.WireSupport;
import org.quartz.JobDataMap;

/**
 * The Class TimerJobDataMap can be used to provide custom Wire Support
 * instances for different Emit Jobs
 */
public final class TimerJobDataMap extends JobDataMap {

    private static final long serialVersionUID = -2191522128203525408L;

    /** The Constant to be used in the map. */
    private static final String WIRE_SUPPORT = "WireSupport";

    /**
     * Gets the wire support.
     *
     * @return the wire support
     */
    public WireSupport getWireSupport() {
        return (WireSupport) super.get(WIRE_SUPPORT);
    }

    /**
     * Put wire support.
     *
     * @param wireSupport
     *            the wire support
     */
    public void putWireSupport(final WireSupport wireSupport) {
        super.put(WIRE_SUPPORT, wireSupport);
    }
}
