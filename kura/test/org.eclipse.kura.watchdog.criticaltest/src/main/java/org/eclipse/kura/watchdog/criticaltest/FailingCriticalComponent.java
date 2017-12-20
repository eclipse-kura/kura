/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.watchdog.criticaltest;

import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.watchdog.CriticalComponent;
import org.eclipse.kura.watchdog.WatchdogService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that 'immediately' causes watchdog service to reboot the system, if it is enabled. It is registered as a
 * critical component and never checks in.
 */
public class FailingCriticalComponent implements ConfigurableComponent, CriticalComponent {

    private static final Logger logger = LoggerFactory.getLogger(FailingCriticalComponent.class);

    private String pid;

    private int timeout = 30000;

    private WatchdogService watchdogService;

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        this.pid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        logger.info("Activating {} and registering with watchdog service...", this.pid);

        this.watchdogService.registerCriticalComponent(this);

        logger.info("Activated {}...", this.pid);
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Deactivating {}...", this.pid);

        this.watchdogService.unregisterCriticalComponent(this);

        logger.info("Deactivated {}...", this.pid);
    }

    @Override
    public String getCriticalComponentName() {
        return this.pid;
    }

    @Override
    public int getCriticalComponentTimeout() {
        return this.timeout;
    }

    public void bindWatchdogService(WatchdogService watchdogService) {
        this.watchdogService = watchdogService;
    }

    public void unbindWatchdogService(WatchdogService watchdogService) {
        this.watchdogService = null;
    }
}
