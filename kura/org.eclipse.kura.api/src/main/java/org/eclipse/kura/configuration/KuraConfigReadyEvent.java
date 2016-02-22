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
package org.eclipse.kura.configuration;

import java.util.Map;

import org.osgi.service.event.Event;

public class KuraConfigReadyEvent extends Event {

	/** Topic of the KuraConfigurationReadyEvent */
	public static final String KURA_CONFIG_EVENT_READY_TOPIC = "org/eclipse/kura/configuration/ConfigEvent/READY";

	public KuraConfigReadyEvent(Map<String, ?> properties) {
		super(KURA_CONFIG_EVENT_READY_TOPIC, properties);
	}
}
