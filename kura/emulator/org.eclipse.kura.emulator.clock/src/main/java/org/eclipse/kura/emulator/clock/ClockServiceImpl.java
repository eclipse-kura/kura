/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.emulator.clock;

import java.util.Date;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.clock.ClockService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClockServiceImpl implements ConfigurableComponent, ClockService {

    private static final Logger logger = LoggerFactory.getLogger(ClockServiceImpl.class);

    @SuppressWarnings("unused")
    private ComponentContext ctx;
    @SuppressWarnings("unused")
    private EventAdmin eventAdmin;
    @SuppressWarnings("unused")
    private Map<String, Object> properties;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        logger.info("Activate. Current Time: {}", new Date());

        // save the bundle context
        this.ctx = componentContext;
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Deactivate...");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated...");
        try {

            // save the properties
            this.properties = properties;
        } catch (Throwable t) {
            logger.error("Error updating ClockService Configuration", t);
        }
    }

    // ----------------------------------------------------------------
    //
    // Master Client Management APIs
    //
    // ----------------------------------------------------------------

    @Override
    public Date getLastSync() throws KuraException {
        return new Date();
    }
}
