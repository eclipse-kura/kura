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
package org.eclipse.kura.clock;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * ClockEvent is raised when a clock synchronization has been performed.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class ClockEvent extends Event {

    /** Topic of the ClockEvent */
    public static final String CLOCK_EVENT_TOPIC = "org/eclipse/kura/clock";

    public ClockEvent(Map<String, ?> properties) {
        super(CLOCK_EVENT_TOPIC, properties);
    }
}
