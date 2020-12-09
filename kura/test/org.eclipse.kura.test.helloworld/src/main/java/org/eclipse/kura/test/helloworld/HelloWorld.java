/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
