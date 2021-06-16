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
import org.eclipse.kura.security.FloodingProtectionConfigurationService;
import org.eclipse.kura.security.ThreatManagerService;
import org.osgi.service.component.ComponentContext;
import org.eclipse.kura.net.admin.FirewallConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FloodingProtectionConfigurator
        implements FloodingProtectionConfigurationService, ThreatManagerService, SelfConfiguringComponent {

    private static final Logger logger = LoggerFactory.getLogger(FloodingProtectionConfigurator.class);
    private FloodingProtectionOptions floodingProtectionOptions;

    private FirewallConfigurationService firewallService;

    public synchronized void setFirewallConfigurationService(FirewallConfigurationService firewallService) {
        logger.debug("Binding FirewallConfigurationService...");
        this.firewallService = firewallService;
        logger.debug("Binding FirewallConfigurationService... Done.");
    }

    public synchronized void unsetFirewallConfigurationService(FirewallConfigurationService firewallService) {
        if (this.firewallService == firewallService) {
            logger.debug("Unbinding FirewallConfigurationService...");
            this.firewallService = null;
            logger.debug("Unbinding FirewallConfigurationService... Done.");
        }
    }

    public void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("Activating FloodingConfigurator...");
        doUpdate(properties);
        logger.debug("Activating FloodingConfigurator... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.debug("Updating FloodingConfigurator...");
        doUpdate(properties);
        logger.debug("Updating FloodingConfigurator... Done.");
    }

    public void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating FloodingConfigurator...");
        logger.debug("Deactivating FloodingConfigurator... Done.");
    }

    private void doUpdate(Map<String, Object> properties) {
        logger.debug("Updating firewall configuration...");
        this.floodingProtectionOptions = new FloodingProtectionOptions(properties);
        this.firewallService
                .addFloodingProtectionRules(this.floodingProtectionOptions.getFloodingProtectionMangleRules());
        logger.debug("Updating firewall configuration... Done.");
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
