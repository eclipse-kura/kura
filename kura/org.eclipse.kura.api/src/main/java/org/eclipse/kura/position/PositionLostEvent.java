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
 * PositionLostEvent is raised when GPS position has been lost.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class PositionLostEvent extends Event {

    /** Topic of the PositionLostEvent */
    public static final String POSITION_LOST_EVENT_TOPIC = "org/eclipse/kura/position/lost";

    public PositionLostEvent(Map<String, ?> properties) {
        super(POSITION_LOST_EVENT_TOPIC, properties);
    }

}
