/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
@Component(
    name = "org.eclipse.kura.clock.ClockService",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = { org.eclipse.kura.clock.ClockService.class, org.eclipse.kura.configuration.ConfigurableComponent.class })
@Designate(ocd = ClockServiceOptions.class)
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

    @Reference(name = "EventAdmin", service = org.osgi.service.event.EventAdmin.class, unbind = "unsetEventAdmin")
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

    @Activate
    protected void activate(ComponentContext componentContext) {
        logger.info("Activate. Current Time: {}", new Date());

        // save the bundle context
        this.ctx = componentContext;
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        logger.info("Deactivate...");
    }

    @Modified
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
