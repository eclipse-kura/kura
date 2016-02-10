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
package org.eclipse.kura.clock;

import java.util.Map;

import org.osgi.service.event.Event;

/**
 * ClockEvent is raised when a clock synchronization has been performed. 
 */
public class ClockEvent extends Event
{
	/** Topic of the ClockEvent */
	public static final String CLOCK_EVENT_TOPIC = "org/eclipse/kura/clock";
	
	public ClockEvent(Map<String, ?> properties) {
		super(CLOCK_EVENT_TOPIC, properties);
	}
}
