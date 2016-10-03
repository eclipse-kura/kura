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

    private static final Logger s_logger = LoggerFactory.getLogger(ClockServiceImpl.class);

    @SuppressWarnings("unused")
    private ComponentContext m_ctx;
    @SuppressWarnings("unused")
    private EventAdmin m_eventAdmin;
    @SuppressWarnings("unused")
    private Map<String, Object> m_properties;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.m_eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        this.m_eventAdmin = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        s_logger.info("Activate. Current Time: {}", new Date());

        // save the bundle context
        this.m_ctx = componentContext;
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("Deactivate...");
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updated...");
        try {

            // save the properties
            this.m_properties = properties;
        } catch (Throwable t) {
            s_logger.error("Error updating ClockService Configuration", t);
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
