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

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class ExampleComponentItTest {

	private static final Logger logger = LoggerFactory.getLogger(ExampleComponentItTest.class);

    private static final CountDownLatch dependencies = new CountDownLatch(1);

    // needs to be static for being available to JUnit Runner
    private static ConfigurableComponent exampleComponent;

    @Reference(cardinality = ReferenceCardinality.MANDATORY, //
        policy = ReferencePolicy.STATIC, //
        target = "(kura.service.pid=${package}.ExampleComponent)" //
    )
    public void setExampleComponent(final ConfigurableComponent componentUnderTest) {
        exampleComponent = componentUnderTest;
        dependencies.countDown();
        logger.info("Got service reference {}", exampleComponent.getClass().getSimpleName());
    }

    @BeforeClass
    public static void awaitDependencies() throws InterruptedException {
        if (!dependencies.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("dependencies not resolved in 30 seconds");
        }
    }

    @Test
    public void shouldHaveTrackedExampleComponent() {
    	assertNotNull(exampleComponent);
    }

}
