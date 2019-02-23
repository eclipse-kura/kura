/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.emulator.watchdog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.watchdog.CriticalComponent;
import org.eclipse.kura.watchdog.WatchdogService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchdogServiceImpl implements WatchdogService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(WatchdogServiceImpl.class);

    private static List<CriticalServiceImpl> criticalServiceList;

    private Map<String, Object> properties;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private int pingInterval = 10000;	// milliseconds
    private boolean configEnabled = false;	// initialized in properties, if false -> no watchdog
    private boolean enabled;

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        this.properties = properties;
        if (properties == null) {
            logger.debug("activating WatchdogService with null props");
        } else {
            logger.debug("activating WatchdogService with {}", properties.toString());
        }
        criticalServiceList = new ArrayList<CriticalServiceImpl>();
        this.enabled = false;

        // clean up if this is not our first run
        if (this.executor != null) {
            this.executor.shutdown();
            while (!this.executor.isTerminated()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.debug(e.getMessage(), e);
                }
            }
            this.executor = null;
        }

        this.executor = Executors.newSingleThreadScheduledExecutor();

        this.future = this.executor.scheduleAtFixedRate(() -> {
            Thread.currentThread().setName(getClass().getSimpleName());
            if (WatchdogServiceImpl.this.configEnabled) {
                doWatchdogLoop();
            }
        }, 0, this.pingInterval, TimeUnit.MILLISECONDS);
    }

    protected void deactivate(ComponentContext componentContext) {
        this.executor.shutdown();
        while (!this.executor.isTerminated()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug(e.getMessage(), e);
            }
        }
        this.executor = null;
        criticalServiceList = null;
    }

    public void updated(Map<String, Object> properties) {
        logger.debug("updated...");
        this.properties = properties;
        if (this.properties != null) {

            Object enabled = this.properties.get("enabled");
            if (enabled != null) {
                this.configEnabled = (Boolean) enabled;
            }
            if (!this.configEnabled) {
                return;
            }
            if (this.properties.get("pingInterval") != null) {
                this.pingInterval = (Integer) this.properties.get("pingInterval");
                if (this.future != null) {
                    this.future.cancel(false);
                    while (!this.future.isDone()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            logger.debug(e.getMessage(), e);
                        }
                    }
                }
                this.future = this.executor.scheduleAtFixedRate(() -> {
                    Thread.currentThread().setName(getClass().getSimpleName());
                    if (WatchdogServiceImpl.this.configEnabled) {
                        doWatchdogLoop();
                    }
                }, 0, this.pingInterval, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void startWatchdog() {
        this.enabled = true;
    }

    @Override
    public void stopWatchdog() {
        this.enabled = false;
    }

    @Override
    public int getHardwareTimeout() {
        return 0;
    }

    @Override
    public void registerCriticalComponent(CriticalComponent criticalComponent) {
        final CriticalServiceImpl service = new CriticalServiceImpl(criticalComponent.getCriticalComponentName(),
                criticalComponent.getCriticalComponentTimeout());
        synchronized (criticalServiceList) {
            // avoid to add same component twice (eg in case of a package updating)
            boolean existing = false;
            for (CriticalServiceImpl csi : criticalServiceList) {
                if (criticalComponent.getCriticalComponentName().compareTo(csi.getName()) == 0) {
                    existing = true;
                }
            }
            if (!existing) {
                criticalServiceList.add(service);
            }
        }

        logger.debug("Added {} , with timeout = {}, list contains {} critical services",
                criticalComponent.getCriticalComponentName(), criticalComponent.getCriticalComponentTimeout(),
                criticalServiceList.size());
    }

    /**
     * @deprecated use {@link WatchdogServiceImpl#registerCriticalComponent(CriticalComponent)}
     */
    @Override
    @Deprecated
    public void registerCriticalService(CriticalComponent criticalComponent) {
        registerCriticalComponent(criticalComponent);
    }

    @Override
    public void unregisterCriticalComponent(CriticalComponent criticalComponent) {
        synchronized (criticalServiceList) {
            for (int i = 0; i < criticalServiceList.size(); i++) {
                if (criticalComponent.getCriticalComponentName().compareTo(criticalServiceList.get(i).getName()) == 0) {
                    criticalServiceList.remove(i);
                    logger.debug("Critical service {} removed, {}", criticalComponent.getCriticalComponentName(),
                            System.currentTimeMillis());
                }
            }
        }
    }

    /**
     * @deprecated use {@link WatchdogServiceImpl#unregisterCriticalComponent(CriticalComponent)}
     */
    @Override
    @Deprecated
    public void unregisterCriticalService(CriticalComponent criticalComponent) {
        unregisterCriticalComponent(criticalComponent);
    }

    @Override
    public List<CriticalComponent> getCriticalComponents() {
        return null;
    }

    @Override
    public void checkin(CriticalComponent criticalService) {
        synchronized (criticalServiceList) {
            for (CriticalServiceImpl csi : criticalServiceList) {
                if (criticalService.getCriticalComponentName().compareTo(csi.getName()) == 0) {
                    csi.update();
                }
            }
        }
    }

    private void doWatchdogLoop() {
        if (!this.enabled) {
            return;
        }

        boolean failure = false;
        // Critical Services
        synchronized (criticalServiceList) {
            if (!criticalServiceList.isEmpty()) {
                for (CriticalServiceImpl csi : criticalServiceList) {
                    if (csi.isTimedOut()) {
                        failure = true;
                        logger.warn("Critical service {} failed -> SYSTEM REBOOT", csi.getName());
                    }
                }
            }
        }
        if (!failure) {
            refreshWatchdog();
        }
    }

    private void refreshWatchdog() {
        File f = new File("/dev/watchdog");
        if (f.exists()) {
            try (FileOutputStream fos = new FileOutputStream(f); PrintWriter pw = new PrintWriter(fos);) {
                pw.write("w");
                pw.flush();
                fos.getFD().sync();
            } catch (IOException e) {
                logger.info(e.getMessage(), e);
            }
        }
    }

    public boolean isConfigEnabled() {
        return this.configEnabled;
    }

    public void setConfigEnabled(boolean configEnabled) {
        this.configEnabled = configEnabled;
    }
}
