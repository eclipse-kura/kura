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

import java.util.Map;

class ExampleComponentOptions {

    private static final Property<String> EXAMPLE_PROPERTY = new Property<>("example.property", "example");

    private final String exampleProperty;

    public ExampleComponentOptions(final Map<String, Object> properties) {
        this.exampleProperty = EXAMPLE_PROPERTY.getOrDefault(properties);
    }

    public String getExampleProperty() {
        return exampleProperty;
    }

}
