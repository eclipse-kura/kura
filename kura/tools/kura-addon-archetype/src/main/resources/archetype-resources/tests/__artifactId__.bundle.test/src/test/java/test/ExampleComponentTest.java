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
package ${package}.test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ${package}.ExampleComponent;

public class ExampleComponentTest {

    private ExampleComponent exampleComponent = new ExampleComponent();
    private Map<String, Object> properties = new HashMap<>();

    @Test
    public void shouldActivate() {
        givenExampleComponent();
        givenProperties("example.property", "test");

        whenActivatingExampleComponent();

        thenExampleOptionIs("test");
    }

    private void givenExampleComponent() {
        this.exampleComponent = new ExampleComponent();
    }

    private void givenProperties(String key, Object value) {
        this.properties.put(key, value);
    }

    private void whenActivatingExampleComponent() {
        this.exampleComponent.activate(this.properties);
    }

    private void thenExampleOptionIs(String examplePropertyValue) {
        assertEquals(examplePropertyValue, this.exampleComponent.getOptions().getExampleProperty());
    }

}