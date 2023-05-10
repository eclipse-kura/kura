package org.eclipse.kura.remote.service.demo.provider;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.kura.remote.service.demo.api.ExampleService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleServiceImpl implements ExampleService {

    private static final Logger logger = LoggerFactory.getLogger(ExampleService.class);

    public void activate() {
        logger.info("Activating ExampleClient...");
        update();
        logger.info("Activating ExampleClient... Done.");
    }

    public void update() {
        logger.info("Registering remote service: ExampleService...");

        // Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("service.exported.interfaces", "*");
        props.put("service.exported.configs", "ecf.generic.server");
        bundleContext.registerService(ExampleService.class, new ExampleServiceImpl(), props);

        logger.info("Registering remote service: ExampleService... Done.");
    }

    public void deactivate() {
        logger.info("Deactivating ExampleServiceImpl...");
        logger.info("Deactivating ExampleServiceImpl... Done.");
    }

    @Override
    public String exampleMethod() {
        return "Hello World!";
    }

}
