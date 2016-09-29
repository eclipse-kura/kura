/**
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.wire.timer;

import org.eclipse.kura.wire.WireSupport;
import org.quartz.JobDataMap;

/**
 * The Class TimerJobDataMap can be used to provide custom Wire Support
 * instances for different Emit Jobs
 */
public final class TimerJobDataMap extends JobDataMap {

    /** The Constant serialVersionUID. */
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
