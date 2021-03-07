/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.security.tamper.detection;

import static java.util.Objects.requireNonNull;

import java.util.Collections;

import org.osgi.service.event.Event;

/**
 * Represents a EventAdmin event that reports a change in tamper status.
 * The changes that can originate a {@link TamperEvent} are the following:
 *
 * <ul>
 * <li>The tamper status flag changes. This can happen because a tamper event has been detected by the implementation or
 * the tamper status has been reset by the user. In this case the implementation must send an event.</li>
 * <li>The tamper status properties change. In this case sending the event is optional.</li>
 * </ul>
 *
 * This class contains a {@link TamperStatus} instance representing the new status.
 *
 * @since 2.2
 * @noimplement This interface is not intended to be implemented by clients.
 */
public class TamperEvent extends Event {

    /**
     * The EventAdmin topic of this event.
     */
    public static final String TAMPER_EVENT_TOPIC = "org/eclipse/kura/security/tamper/detection/TamperEvent/TAMPER_STATUS_CHANGED";
    /**
     * The key of a Event property containing the new {@link TamperStatus}
     */
    public static final String TAMPER_STATUS_PROPERTY_KEY = "tamper.status";

    /**
     * Creates a new {@link TamperEvent} instance.
     *
     * @param tamperStatus
     *            the new {@link TamperStatus}, cannot be <code>null</code>.
     */
    public TamperEvent(final TamperStatus tamperStatus) {
        super(TAMPER_EVENT_TOPIC, Collections.singletonMap(TAMPER_STATUS_PROPERTY_KEY,
                requireNonNull(tamperStatus, "Tamper status cannot be null")));
    }

    /**
     * Returns the new {@link TamperStatus}.
     *
     * @return the new {@link TamperStatus}, cannot be <code>null</code>.
     */
    public TamperStatus getTamperStatus() {
        return (TamperStatus) getProperty(TAMPER_STATUS_PROPERTY_KEY);
    }

}
