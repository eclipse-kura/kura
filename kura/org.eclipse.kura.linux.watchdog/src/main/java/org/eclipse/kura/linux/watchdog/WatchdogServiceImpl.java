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
package org.eclipse.kura.linux.watchdog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    private static final Logger s_logger = LoggerFactory.getLogger(WatchdogServiceImpl.class);

    private final static long THREAD_TERMINATION_TOUT = 1; // in seconds

    private static ScheduledFuture<?> s_pollThreadTask;
    private ScheduledExecutorService m_pollThreadExecutor;

    private Map<String, Object> m_properties;

    private int pingInterval = 2000;	// milliseconds
    private static ArrayList<CriticalComponentImpl> s_criticalServiceList;
    private boolean m_configEnabled = false;	// initialized in properties, if false -> no watchdog
    private boolean m_enabled;
    private boolean m_watchdogToStop = false;
    private String watchdogDevice = "/dev/watchdog";

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        this.m_properties = properties;
        if (properties == null) {
            s_logger.debug("activating WatchdogService with null props");
        } else {
            if (this.m_properties.get("enabled") != null) {
                this.m_configEnabled = (Boolean) this.m_properties.get("enabled");
                if (this.m_configEnabled) {
                    s_logger.debug("activating WatchdogService with watchdog enabled");
                } else {
                    s_logger.debug("activating WatchdogService with watchdog disabled");
                }
            }
            if (this.m_properties.get("pingInterval") != null) {
                this.pingInterval = (Integer) this.m_properties.get("pingInterval");
            }
            if (this.m_properties.get("watchdogDevice") != null
                    && !((String) this.m_properties.get("watchdogDevice")).isEmpty()) {
                this.watchdogDevice = (String) this.m_properties.get("watchdogDevice");
            }
        }
        s_criticalServiceList = new ArrayList<CriticalComponentImpl>();
        this.m_enabled = false;

        this.m_pollThreadExecutor = Executors.newSingleThreadScheduledExecutor();

        updated(properties);
    }

    protected void deactivate(ComponentContext componentContext) {

        if (s_pollThreadTask != null && !s_pollThreadTask.isDone()) {
            s_logger.debug("Cancelling WatchdogServiceImpl task ...");
            s_pollThreadTask.cancel(true);
            s_logger.info("WatchdogServiceImpl task cancelled? = {}", s_pollThreadTask.isDone());
            s_pollThreadTask = null;
        }

        disableWatchdog();

        if (this.m_pollThreadExecutor != null) {
            s_logger.debug("Terminating WatchdogServiceImpl Thread ...");
            this.m_pollThreadExecutor.shutdownNow();
            try {
                this.m_pollThreadExecutor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                s_logger.warn("Interrupted", e);
            }
            s_logger.info("WatchdogServiceImpl Thread terminated? - {}", this.m_pollThreadExecutor.isTerminated());
            this.m_pollThreadExecutor = null;
        }

        s_criticalServiceList = null;
    }

    public void updated(Map<String, Object> properties) {
        s_logger.debug("updated...");
        this.m_properties = properties;
        if (this.m_properties != null) {

            // clean up if this is not our first run
            if (s_pollThreadTask != null && !s_pollThreadTask.isCancelled()) {
                s_pollThreadTask.cancel(true);
            }

            if (this.m_properties.get("enabled") != null) {
                this.m_configEnabled = (Boolean) this.m_properties.get("enabled");
            }
            if (!this.m_configEnabled) {
                if (this.m_enabled) {
                    // stop the watchdog
                    this.m_watchdogToStop = true;
                    disableWatchdog();
                }
                return;
            }
            if (this.m_properties.get("pingInterval") != null) {
                this.pingInterval = (Integer) this.m_properties.get("pingInterval");
            }
            if (!((String) this.m_properties.get("watchdogDevice")).isEmpty()) {
                this.watchdogDevice = (String) this.m_properties.get("watchdogDevice");
            }

            s_pollThreadTask = this.m_pollThreadExecutor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("WatchdogServiceImpl");
                    doWatchdogLoop();
                }
            }, 0, this.pingInterval, TimeUnit.MILLISECONDS);

        }
    }

    @Override
    @Deprecated
    public void startWatchdog() {
    }

    @Override
    @Deprecated
    public void stopWatchdog() {
    }

    @Override
    public int getHardwareTimeout() {
        return 0;
    }

    @Override
    public void registerCriticalComponent(CriticalComponent criticalComponent) {
        final CriticalComponentImpl service = new CriticalComponentImpl(criticalComponent.getCriticalComponentName(),
                criticalComponent.getCriticalComponentTimeout());
        synchronized (s_criticalServiceList) {
            // avoid to add same component twice (eg in case of a package updating)
            boolean existing = false;
            for (CriticalComponentImpl csi : s_criticalServiceList) {
                if (criticalComponent.getCriticalComponentName().compareTo(csi.getName()) == 0) {
                    existing = true;
                }
            }
            if (!existing) {
                s_criticalServiceList.add(service);
            }
        }

        s_logger.debug("Added " + criticalComponent.getCriticalComponentName() + ", with timeout = "
                + criticalComponent.getCriticalComponentTimeout() + ", list contains " + s_criticalServiceList.size()
                + " critical services");
    }

    @Override
    @Deprecated
    public void registerCriticalService(CriticalComponent criticalComponent) {
        registerCriticalComponent(criticalComponent);
    }

    @Override
    public void unregisterCriticalComponent(CriticalComponent criticalComponent) {
        synchronized (s_criticalServiceList) {
            for (int i = 0; i < s_criticalServiceList.size(); i++) {
                if (criticalComponent.getCriticalComponentName().compareTo(s_criticalServiceList.get(i).getName()) == 0) {
                    s_criticalServiceList.remove(i);
                    s_logger.debug("Critical service " + criticalComponent.getCriticalComponentName() + " removed, "
                            + System.currentTimeMillis());
                }
            }
        }
    }

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
        synchronized (s_criticalServiceList) {
            for (CriticalComponentImpl csi : s_criticalServiceList) {
                if (criticalService.getCriticalComponentName().compareTo(csi.getName()) == 0) {
                    csi.update();
                }
            }
        }
    }

    private void doWatchdogLoop() {
        if (this.m_enabled) {
            if (this.m_watchdogToStop) {
                disableWatchdog();
                this.m_watchdogToStop = false;
            } else {
                boolean failure = false;
                // Critical Services
                synchronized (s_criticalServiceList) {
                    if (s_criticalServiceList.size() > 0) {
                        for (CriticalComponentImpl csi : s_criticalServiceList) {
                            if (csi.isTimedOut()) {
                                failure = true;
                                s_logger.warn("Critical service {} failed -> SYSTEM REBOOT", csi.getName());
                            }
                        }
                    }
                }

                if (!failure) { // refresh watchdog
                    File f = null;
                    FileWriter bw = null;
                    try {
                        f = new File(this.watchdogDevice);
                        bw = new FileWriter(f);
                        bw.write('w');
                        bw.flush();
                        s_logger.debug("watchdog refreshed");
                    } catch (IOException e) {
                        s_logger.error("IOException on refresh watchdog : {}", e.getMessage());
                    } finally {
                        try {
                            if (bw != null) {
                                bw.close();
                            }
                        } catch (IOException e) {
                            s_logger.error("IOException on closing watchdog file : {}", e.getMessage());
                        }
                    }
                }
            }
        } else { // ! m_enabled
            if (this.m_configEnabled) {
                File f = null;
                FileWriter bw = null;
                try {
                    f = new File(this.watchdogDevice);
                    bw = new FileWriter(f);
                    bw.write('w');
                    bw.flush();
                    bw.close();
                    // m_watchdogToStart=false;
                    this.m_enabled = true;
                    s_logger.info("watchdog started");
                } catch (IOException e) {
                    s_logger.error("IOException on start watchdog : {}", e.getMessage());
                } finally {
                    try {
                        if (bw != null) {
                            bw.close();
                        }
                    } catch (IOException e) {
                        s_logger.error("IOException on closing watchdog file : {}", e.getMessage());
                    }
                }
            }
        }
    }

    private void disableWatchdog() {
        if (this.m_enabled) {
            File f = null;
            FileWriter bw = null;
            try {
                f = new File(this.watchdogDevice);
                bw = new FileWriter(f);
                bw.write('V');
                this.m_enabled = false;
                s_logger.info("watchdog disabled");
            } catch (IOException e) {
                s_logger.error("IOException on disable watchdog : {}", e.getMessage());
            } finally {
                try {
                    if (bw != null) {
                        bw.close();
                    }
                } catch (IOException e) {
                    s_logger.error("IOException on closing watchdog file : {}", e.getMessage());
                }
            }
        }
    }

    public boolean isConfigEnabled() {
        return this.m_configEnabled;
    }

    public void setConfigEnabled(boolean configEnabled) {
        this.m_configEnabled = configEnabled;
    }
}
