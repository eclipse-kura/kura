/*******************************************************************************
 * Copyright (c) 2019, 2024 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/
package ${package}.test;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleComponentTest {

	private static final Logger logger = LoggerFactory.getLogger(ExampleComponentTest.class);

    private static CountDownLatch dependencies = new CountDownLatch(1);

    private static ConfigurableComponent exampleComponent;

    public void setExampleComponent(final ConfigurableComponent component) {
        exampleComponent = component;
        dependencies.countDown();
    }

    public void unsetExampleComponent() {
        exampleComponent = null;
    }

    public void activate() {
    	logger.info("Activating");
    	logger.info("Activated");
    }

    public void deactivate() {
    	logger.info("Deactivating");
    	logger.info("Deactivated");
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
