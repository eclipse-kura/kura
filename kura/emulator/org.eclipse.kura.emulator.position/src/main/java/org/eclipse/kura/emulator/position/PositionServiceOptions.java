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
package org.eclipse.kura.emulator.position;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Icon;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.position.PositionService", name = "PositionService", description = "Emulated implementation of the PositionService.", icon = @Icon(resource = "PositionService", size = 32))
public @interface PositionServiceOptions {

    @AttributeDefinition(name = "enabled", description = "The emulated PositionService is always enabled and using sample GPS positions.")
    boolean enabled() default true;

    @AttributeDefinition(name = "useGpsd", description = "If true uses the gpsd service daemon. This implies that this daemon must be installed and active first.")
    boolean useGpsd() default false;

    @AttributeDefinition(name = "source", options = { @Option(label = "Boston", value = "boston"), @Option(label = "Denver", value = "denver"), @Option(label = "Paris", value = "paris"), @Option(label = "Test", value = "test") }, description = "Select the data source file to use.")
    String source() default "boston";

}


