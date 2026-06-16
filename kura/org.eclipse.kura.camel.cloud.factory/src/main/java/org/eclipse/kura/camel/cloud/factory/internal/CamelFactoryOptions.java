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
package org.eclipse.kura.camel.cloud.factory.internal;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.camel.cloud.factory.CamelFactory", name = "Camel Cloud Client", description = "Camel Cloud Client factory")
public @interface CamelFactoryOptions {

    @AttributeDefinition(name = "Router XML", cardinality = 1, description = "The camel XML router configuration|TextArea")
    String xml();

    @AttributeDefinition(name = "JavaScript init code", cardinality = 1, required = false, description = "JavaScript code which is called when the router is initialized first. The camel context is avaiable in the variable 'camelContext'.|TextArea")
    String initCode();

    @AttributeDefinition(name = "Enable Camel JMX support", cardinality = 1, description = "This setting controls if JMX support for the Camel context will be activated or not.")
    boolean enableJmx() default true;

}


