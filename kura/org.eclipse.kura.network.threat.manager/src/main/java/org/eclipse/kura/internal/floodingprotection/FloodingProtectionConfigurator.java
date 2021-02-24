/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Eurotech
 ******************************************************************************/
package org.eclipse.kura.internal.floodingprotection;

import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.security.FloodingProtectionConfigurationChangeEvent;
import org.eclipse.kura.security.FloodingProtectionConfigurationService;
import org.eclipse.kura.security.ThreatManagerService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FloodingProtectionConfigurator
        implements FloodingProtectionConfigurationService, ThreatManagerService, SelfConfiguringComponent {

    private static final Logger logger = LoggerFactory.getLogger(FloodingProtectionConfigurator.class);
    private FloodingProtectionOptions floodingProtectionOptions;
    private EventAdmin eventAdmin;

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        if (this.eventAdmin == eventAdmin) {
            this.eventAdmin = null;
        }
    }

    public void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Activating FloodingConfigurator...");
        doUpdate(properties);
        logger.info("Activating FloodingConfigurator... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updating FloodingConfigurator...");
        doUpdate(properties);
        logger.info("Updating FloodingConfigurator... Done.");
    }

    public void deactivate(ComponentContext componentContext) {
        logger.info("Deactivating FloodingConfigurator...");
        logger.info("Deactivating FloodingConfigurator... Done.");
    }

    private void doUpdate(Map<String, Object> properties) {
        this.floodingProtectionOptions = new FloodingProtectionOptions(properties);
        this.eventAdmin.postEvent(new FloodingProtectionConfigurationChangeEvent(properties));
    }

    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
        return new ComponentConfigurationImpl(this.floodingProtectionOptions.getPid(),
                this.floodingProtectionOptions.getDefinition(), this.floodingProtectionOptions.getProperties());
    }

    @Override
    public Set<String> getFloodingProtectionFilterRules() {
        return this.floodingProtectionOptions.getFloodingProtectionFilterRules();
    }

    @Override
    public Set<String> getFloodingProtectionNatRules() {
        return this.floodingProtectionOptions.getFloodingProtectionNatRules();
    }

    @Override
    public Set<String> getFloodingProtectionMangleRules() {
        return this.floodingProtectionOptions.getFloodingProtectionMangleRules();
    }

}
