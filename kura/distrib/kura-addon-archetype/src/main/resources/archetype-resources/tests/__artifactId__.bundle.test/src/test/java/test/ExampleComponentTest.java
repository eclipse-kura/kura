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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ${package}.ExampleComponent;
import ${package}.ExampleDependencyService;

public class ExampleComponentTest {

    private static final Logger logger = LoggerFactory.getLogger(ExampleComponent.class);

    private ExampleComponent exampleComponent = new ExampleComponent();
    private Map<String, Object> properties = new HashMap<>();
    private ExampleDependencyService dependencyService;

    @Test
    public void shouldActivate() {
        givenDependencyService();
        givenExampleComponent();
        givenProperties("example.property", "test");

        whenActivatingExampleComponent();

        thenExampleOptionIs("test");
    }

    private void givenDependencyService() {
        this.dependencyService = mock(ExampleDependencyService.class);
        doAnswer(answer -> {
            logger.info("I'm in a mock ExampleDependencyService");
            return null;
        }).when(this.dependencyService).run();
    }

    private void givenExampleComponent() {
        this.exampleComponent = new ExampleComponent();
        this.exampleComponent.setExampleDependencyService(this.dependencyService);
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