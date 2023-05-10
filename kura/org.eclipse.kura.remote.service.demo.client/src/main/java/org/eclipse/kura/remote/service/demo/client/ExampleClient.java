package org.eclipse.kura.remote.service.demo.client;

import org.eclipse.kura.remote.service.demo.api.ExampleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleClient {

    private static final Logger logger = LoggerFactory.getLogger(ExampleClient.class);

    private ExampleService service;

    public void bindExampleService(ExampleService service) {
        logger.info("Found ExampleService remote service.");
        this.service = service;
    }

    public void unbindExampleService(ExampleService service) {
        if (this.service == service) {
            this.service = null;
        }
    }

    public void activate() {
        logger.info("Activating ExampleClient...");
        update();
        logger.info("Activating ExampleClient... Done.");
    }

    public void update() {
        if (this.service != null) {
            logger.info("Message from remote service: {}", this.service.exampleMethod());
        }
    }

    public void deactivate() {
        logger.info("Deactivating ExampleClient...");
        logger.info("Deactivating ExampleClient... Done.");
    }

}
