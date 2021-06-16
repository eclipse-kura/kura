/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net.modem;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class ModemGpsEnabledEvent extends Event {

    /** Topic of the ModemGpsEnabledEvent */
    public static final String MODEM_EVENT_GPS_ENABLED_TOPIC = "org/eclipse/kura/net/modem/gps/ENABLED";

    /**
     * @since 2.2
     */
    public static final String PORT = "port";

    /**
     * @since 2.2
     */
    public static final String BAUD_RATE = "baudRate";

    /**
     * @since 2.2
     */
    public static final String DATA_BITS = "bitsPerWord";

    /**
     * @since 2.2
     */
    public static final String STOP_BITS = "stopBits";

    /**
     * @since 2.2
     */
    public static final String PARITY = "parity";

    /**
     * @deprecated Use PORT instead
     */
    @SuppressWarnings("checkstyle:constantName")
    @Deprecated
    public static final String Port = PORT;

    /**
     * @deprecated Use BAUD_RATE instead
     */
    @SuppressWarnings("checkstyle:constantName")
    @Deprecated
    public static final String BaudRate = BAUD_RATE;

    /**
     * @deprecated Use DATA_BITS instead
     */
    @SuppressWarnings("checkstyle:constantName")
    @Deprecated
    public static final String DataBits = DATA_BITS;

    /**
     * @deprecated Use STOP_BITS instead
     */
    @SuppressWarnings("checkstyle:constantName")
    @Deprecated
    public static final String StopBits = STOP_BITS;

    /**
     * @deprecated Use PARITY instead
     */
    @SuppressWarnings("checkstyle:constantName")
    @Deprecated
    public static final String Parity = PARITY;

    public ModemGpsEnabledEvent(Map<String, Object> properties) {
        super(MODEM_EVENT_GPS_ENABLED_TOPIC, properties);
    }
}
