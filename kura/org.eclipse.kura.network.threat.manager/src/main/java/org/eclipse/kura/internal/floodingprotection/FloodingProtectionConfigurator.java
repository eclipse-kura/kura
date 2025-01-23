/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.net.admin.FirewallConfigurationService;
import org.eclipse.kura.net.admin.ipv6.FirewallConfigurationServiceIPv6;
import org.eclipse.kura.security.FloodingProtectionConfigurationService;
import org.eclipse.kura.security.ThreatManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FloodingProtectionConfigurator
        implements FloodingProtectionConfigurationService, ThreatManagerService, SelfConfiguringComponent {

    private static final Logger logger = LoggerFactory.getLogger(FloodingProtectionConfigurator.class);
    private FloodingProtectionOptions floodingProtectionOptions;

    private FirewallConfigurationService firewallService;
    private Optional<FirewallConfigurationServiceIPv6> optionalFirewallServiceIPv6 = Optional.empty();

    public synchronized void setFirewallConfigurationService(FirewallConfigurationService firewallService) {
        this.firewallService = firewallService;
    }

    public synchronized void setFirewallConfigurationServiceIPv6(FirewallConfigurationServiceIPv6 firewallServiceIPv6) {
        this.optionalFirewallServiceIPv6 = Optional.of(firewallServiceIPv6);
    }

    public void activate(Map<String, Object> properties) {
        logger.info("Activating FloodingConfigurator...");
        doUpdate(properties);
        logger.info("Activating FloodingConfigurator... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updating FloodingConfigurator...");
        doUpdate(properties);
        logger.info("Updating FloodingConfigurator... Done.");
    }

    public void deactivate() {
        logger.info("Deactivating FloodingConfigurator...");
        logger.info("Deactivating FloodingConfigurator... Done.");
    }

    public FloodingProtectionOptions buildFloodingProtectionOptions(Map<String, Object> properties) {
        return new FloodingProtectionOptions(properties);
    }

    private void doUpdate(Map<String, Object> properties) {
        logger.info("Updating firewall configuration...");
        this.floodingProtectionOptions = buildFloodingProtectionOptions(properties);
        updateFirewallService();
        updateFirewallServiceIPv6();
        updateFragmentFiltering();
        updateFragmentFilteringIPv6();
        logger.info("Updating firewall configuration... Done.");
    }

    private void updateFirewallService() {
        Set<String> filterRules = this.floodingProtectionOptions.getFloodingProtectionFilterRules();
        Set<String> natRules = this.floodingProtectionOptions.getFloodingProtectionNatRules();
        Set<String> mangleRules = this.floodingProtectionOptions.getFloodingProtectionMangleRules();
        this.firewallService.addFloodingProtectionRules(filterRules, natRules, mangleRules);
    }

    private void updateFirewallServiceIPv6() {
        if (this.optionalFirewallServiceIPv6.isPresent()) {
            Set<String> filterRules = this.floodingProtectionOptions.getFloodingProtectionFilterRulesIPv6();
            Set<String> natRules = this.floodingProtectionOptions.getFloodingProtectionNatRulesIPv6();
            Set<String> mangleRules = this.floodingProtectionOptions.getFloodingProtectionMangleRulesIPv6();
            this.optionalFirewallServiceIPv6.get().addFloodingProtectionRules(filterRules, natRules, mangleRules);
        } else {
            logger.warn("This device does not support IPv6.");
        }
    }

    private void updateFragmentFiltering() {
        if (this.floodingProtectionOptions.isIPv4Enabled()) {
            configureThreshold(this.floodingProtectionOptions.getFragmentLowThresholdIPv4FileName(), 0);
            configureThreshold(this.floodingProtectionOptions.getFragmentHighThresholdIPv4FileName(), 0);
        } else {
            configureThreshold(this.floodingProtectionOptions.getFragmentHighThresholdIPv4FileName(),
                    this.floodingProtectionOptions.getFragmentHighThresholdDefault());
            configureThreshold(this.floodingProtectionOptions.getFragmentLowThresholdIPv4FileName(),
                    this.floodingProtectionOptions.getFragmentLowThresholdDefault());
        }
    }

    private void updateFragmentFilteringIPv6() {
        if (this.optionalFirewallServiceIPv6.isPresent()) {
            if (this.floodingProtectionOptions.isIPv6Enabled()) {
                configureThreshold(this.floodingProtectionOptions.getFragmentLowThresholdIPv6FileName(), 0);
                configureThreshold(this.floodingProtectionOptions.getFragmentHighThresholdIPv6FileName(), 0);
            } else {
                configureThreshold(this.floodingProtectionOptions.getFragmentHighThresholdIPv6FileName(),
                        this.floodingProtectionOptions.getFragmentHighThresholdDefault());
                configureThreshold(this.floodingProtectionOptions.getFragmentLowThresholdIPv6FileName(),
                        this.floodingProtectionOptions.getFragmentLowThresholdDefault());
            }
        }
    }

    private void configureThreshold(String fileName, int value) {
        Path sourceFile = Paths.get(fileName);
        if (Files.exists(sourceFile)) {
            try {
                Files.write(sourceFile, Integer.toString(value).getBytes());
            } catch (IOException e) {
                logger.error("Cannot write to " + sourceFile, e);
            }
        } else {
            logger.warn("File {} does not exists.", fileName);
        }
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

    @Override
    public Set<String> getFloodingProtectionFilterRulesIPv6() {
        return this.floodingProtectionOptions.getFloodingProtectionFilterRulesIPv6();
    }

    @Override
    public Set<String> getFloodingProtectionNatRulesIPv6() {
        return this.floodingProtectionOptions.getFloodingProtectionNatRulesIPv6();
    }

    @Override
    public Set<String> getFloodingProtectionMangleRulesIPv6() {
        return this.floodingProtectionOptions.getFloodingProtectionMangleRulesIPv6();
    }

}
