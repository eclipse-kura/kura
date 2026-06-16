/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.emulator.watchdog;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Icon;

@ObjectClassDefinition(id = "org.eclipse.kura.watchdog.WatchdogService", name = "WatchdogService", description = "Emulated implementation of the WatchdogService", icon = @Icon(resource = "WatchdogService", size = 32))
public @interface WatchdogServiceOptions {

    @AttributeDefinition(name = "enabled", description = "The emulated WatchdogService is always disabled.")
    boolean enabled() default false;

}


