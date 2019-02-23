/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.position;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * PositionLockedEvent is raised when a valid GPS position has been acquired.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class PositionLockedEvent extends Event {

    /** Topic of the PositionLockedEvent */
    public static final String POSITION_LOCKED_EVENT_TOPIC = "org/eclipse/kura/position/locked";

    public PositionLockedEvent(Map<String, ?> properties) {
        super(POSITION_LOCKED_EVENT_TOPIC, properties);
    }

}
