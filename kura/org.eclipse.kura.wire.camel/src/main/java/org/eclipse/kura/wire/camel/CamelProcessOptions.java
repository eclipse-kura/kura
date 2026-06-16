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
package org.eclipse.kura.wire.camel;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.wire.camel.CamelProcess", name = "Camel Processor", description = "Call an endpoint and extract its result")
public @interface CamelProcessOptions {

    @AttributeDefinition(name = "ID", description = "The ID of the Camel Context")
    String id();

    @AttributeDefinition(name = "Endpoint URI", description = "The URI to the Camel endpoint the component will to call.")
    String endpointUri();

}


