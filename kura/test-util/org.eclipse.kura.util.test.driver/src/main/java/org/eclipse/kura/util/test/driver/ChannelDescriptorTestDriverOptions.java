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
package org.eclipse.kura.util.test.driver;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.util.test.driver.ChannelDescriptorTestDriver", name = "ChannelDescriptorTestDriver", description = "A driver for testing channel descriptor properties")
public @interface ChannelDescriptorTestDriverOptions {

    @AttributeDefinition(name = "Test Property", description = "A test property")
    String test_property() default "test value";

}


