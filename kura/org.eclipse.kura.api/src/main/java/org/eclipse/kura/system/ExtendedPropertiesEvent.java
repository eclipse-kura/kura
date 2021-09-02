/*******************************************************************************
 * Copyright (c) 2021 WinWinIt and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  WinWinIt
 *******************************************************************************/
package org.eclipse.kura.system;

import java.util.HashMap;

import org.osgi.service.event.Event;

/**
 * An event notifying that MQTT birth certificate extended properties has changed.
 */
public class ExtendedPropertiesEvent extends Event {

    /** Topic. */
    public static final String EXTENDED_PROPERTIES_EVENT_TOPIC = "org/eclipse/kura/system/ExtendedPropertiesEvent/CHANGED";

    /**
     * Constructor.
     */
    public ExtendedPropertiesEvent() {
        super(EXTENDED_PROPERTIES_EVENT_TOPIC, new HashMap<>());
    }

}
