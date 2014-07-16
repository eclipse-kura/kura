package org.eclipse.kura.test.helloworld;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorld {
	
	private static final Logger s_logger = LoggerFactory.getLogger(HelloWorld.class);
	
	private static final String APP_ID = HelloWorld.class.getName();
	
	// ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext) {
		 s_logger.info("Bundle " + APP_ID + " has started");
	}
	
	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Deactivating " + APP_ID + " ...");
	}
}
