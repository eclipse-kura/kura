/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package ${package};

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

// allow using _ in method names, it is needed for having ids containing '.'
@SuppressWarnings("checkstyle:MethodName")
@ObjectClassDefinition(id = "${package}.ExampleComponent", //
    name = "ExampleComponent", //
    description = "An example configurable component implementation." //
)
public interface ExampleComponentOCD {

    @AttributeDefinition(
        name = "An example property", //
        defaultValue = "foo", //
        description = "An example string property.", //
        required = true, //
        cardinality = 0 //
    )
    public String example_property();

}