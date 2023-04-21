/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.nm.configuration.monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpServerMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DhcpServerMonitor.class);

    private final Map<String, Boolean> dhcpServerInterfaceConfiguration;
    protected DhcpServerManager dhcpServerManager;
    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    public DhcpServerMonitor(CommandExecutorService commandExecutorService) {
        this.dhcpServerInterfaceConfiguration = new ConcurrentHashMap<>();
        setDhcpServerManager(new DhcpServerManager(commandExecutorService));
    }

    protected void setDhcpServerManager(DhcpServerManager dhcpServerManager) {
        this.dhcpServerManager = dhcpServerManager;
    }

    public void start() {
        this.worker = Executors.newSingleThreadScheduledExecutor();
        this.handle = this.worker.scheduleAtFixedRate(this::monitor, 0, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        if (this.handle != null) {
            this.handle.cancel(true);
        }
        if (this.worker != null) {
            this.worker.shutdown();
        }
    }

    public void putDhcpServerInterfaceConfiguration(String interfaceName, boolean enable) {
        this.dhcpServerInterfaceConfiguration.put(interfaceName, enable);
    }

    public void clear() {
        this.dhcpServerInterfaceConfiguration.clear();
    }

    private void monitor() {
        this.dhcpServerInterfaceConfiguration.entrySet().forEach(entry -> {
            String interfaceName = entry.getKey();
            boolean enable = entry.getValue();
            try {
                if (enable && !this.dhcpServerManager.isRunning(interfaceName)) {
                    startDhcpServer(interfaceName);
                } else if (!enable && this.dhcpServerManager.isRunning(interfaceName)) {
                    stopDhcpServer(interfaceName);
                }
            } catch (KuraException e) {
                logger.warn("Failed to chech DHCP server status for the interface " + interfaceName, e);
            }
        });
    }

    private void startDhcpServer(String interfaceName) {
        logger.debug("Starting DHCP server for {}", interfaceName);
        try {
            this.dhcpServerManager.enable(interfaceName);
        } catch (KuraException e) {
            logger.warn("Failed to start DHCP server for the interface " + interfaceName, e);
        }
        logger.debug("Starting DHCP server for {}. Done.", interfaceName);
    }

    private void stopDhcpServer(String interfaceName) {
        logger.debug("Stopping DHCP server for {}", interfaceName);
        try {
            this.dhcpServerManager.disable(interfaceName);
        } catch (KuraException e) {
            logger.warn("Failed to stop DHCP server for the interface " + interfaceName, e);
        }
        logger.debug("Stopping DHCP server for {}. Done.", interfaceName);
    }

}
