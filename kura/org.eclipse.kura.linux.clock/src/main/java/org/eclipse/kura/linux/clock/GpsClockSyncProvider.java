/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - fix build warnigns
 *******************************************************************************/
package org.eclipse.kura.linux.clock;

import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.linux.clock.ClockSyncListener;
import org.eclipse.kura.linux.clock.ClockSyncProvider;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpsClockSyncProvider implements ClockSyncProvider, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(GpsClockSyncProvider.class);

    private PositionService positionService;
    protected Map<String, Object> properties;
    protected ClockSyncListener listener;
    protected int refreshInterval;
    protected Date lastSync;
    protected boolean waitForLocked;
    protected ScheduledExecutorService scheduler;

    public GpsClockSyncProvider() {
    }

    // ----------------------------------------------------------------
    //
    // Wait for GPS locked event if single clock update
    //
    // ----------------------------------------------------------------

    @Override
    public void handleEvent(Event event) {
        if (PositionLockedEvent.POSITION_LOCKED_EVENT_TOPIC.equals(event.getTopic())) {
            if (this.waitForLocked && this.refreshInterval == 0) {
                logger.info("Received Position Locked event");
                try {
                    synchClock();
                } catch (KuraException e) {
                    logger.error("Error Synchronizing Clock", e);
                }
            }
        }
    }

    @Override
    public void init(Map<String, Object> properties, ClockSyncListener listener) throws KuraException {
        logger.debug("initing the GPS clock sync provider");
        this.properties = properties;
        this.listener = listener;

        this.waitForLocked = false;

        this.refreshInterval = 0;
        // FIXME? is it weird, that GPS provider uses NTP setting?
        if (this.properties.containsKey("clock.ntp.refresh-interval")) {
            this.refreshInterval = (Integer) this.properties.get("clock.ntp.refresh-interval");
        }

        try {
            // looking for a valid PositionService from SCR
            BundleContext bundleContext = getBundleContext();
            ServiceReference<PositionService> scrServiceRef = bundleContext.getServiceReference(PositionService.class);
            this.positionService = bundleContext.getService(scrServiceRef);

            // install event listener for GPS locked event
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            String[] topic = { PositionLockedEvent.POSITION_LOCKED_EVENT_TOPIC };
            props.put(EventConstants.EVENT_TOPIC, topic);
            bundleContext.registerService(EventHandler.class.getName(), this, props);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, "Failed to initialize the GpsClockSyncProvider");
        }

        logger.debug("done initiing the GPS clock sync provider");
    }

    @Override
    public void start() throws KuraException {

        if (this.refreshInterval < 0) {
            // Never do any update. So Nothing to do.
            logger.info("No clock update required");
        } else if (this.refreshInterval == 0) {
            // Perform a single clock update.
            logger.info("Perform single clock update.");
            try {
                synchClock();
            } catch (KuraException e) {
                logger.error("Error Synchronizing Clock", e);
            }
        } else {
            // Perform periodic clock updates.
            logger.info("Perform periodic clock updates every {} sec", this.refreshInterval);
            if (this.scheduler != null) {
                this.scheduler.shutdown();
                this.scheduler = null;
            }
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.scheduler.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("GpsClockSyncProvider");
                    try {
                        synchClock();
                    } catch (KuraException e) {
                        logger.error("Error Synchronizing Clock", e);
                    }
                }
            }, 0, this.refreshInterval, TimeUnit.SECONDS);
        }
    }

    @Override
    public void stop() throws KuraException {
        if (this.scheduler != null) {
            this.scheduler.shutdown();
            this.scheduler = null;
        }
        this.positionService = null;
    }

    @Override
    public Date getLastSync() {
        return this.lastSync;
    }

    protected BundleContext getBundleContext() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        return bundleContext;
    }

    // ----------------------------------------------------------------
    //
    // The actual time sync method
    // The GPS can give time but not date
    //
    // ----------------------------------------------------------------

    protected void synchClock() throws KuraException {
        SafeProcess procDate = null;
        SafeProcess procTime = null;
        try {
            if (this.positionService != null) {
                if (this.positionService.isLocked()) {
                    String gpsTime = this.positionService.getNmeaTime();
                    String gpsDate = this.positionService.getNmeaDate();
                    // Execute a native Linux command to perform the set time and date.
                    if (!gpsDate.isEmpty()) {
                        String YY = gpsDate.substring(4, 6);
                        String MM = gpsDate.substring(2, 4);
                        String DD = gpsDate.substring(0, 2);
                        String commandDate = "date +%Y%m%d -s \"20" + YY + MM + DD + "\"";
                        procDate = exec(commandDate);
                        procDate.waitFor();
                        if (procDate.exitValue() == 0) {
                            logger.info("System Clock Synchronized with GPS, date = {} ", gpsDate);
                            this.lastSync = new Date();
                            if (!gpsTime.isEmpty()) {
                                String hh = gpsTime.substring(0, 2);
                                String mm = gpsTime.substring(2, 4);
                                String ss = gpsTime.substring(4, 6);

                                // time is in UTC => -u
                                String commandTime = "date +%T -u -s \"" + hh + ":" + mm + ":" + ss + "\"";
                                procTime = exec(commandTime);
                                procTime.waitFor();
                                if (procTime.exitValue() == 0) {
                                    logger.info("System Clock Synchronized with GPS, time = {}", gpsTime);
                                    this.lastSync = new Date();
                                    this.waitForLocked = false;
                                } else {
                                    logger.error("Unexpected error while Synchronizing System Clock with GPS");

                                    // FIXME? this value gets overwritten immediately and it likely makes no sense as
                                    // the date command failed, not GPS
                                    this.waitForLocked = true;
                                }
                            }
                            this.waitForLocked = false;
                            // Call update method with 0 offset to ensure the clock event gets fired and the HW clock
                            // is updated if desired.
                            this.listener.onClockUpdate(0);
                        } else {
                            logger.error("Unexpected error while Synchronizing System Clock with GPS");
                            this.waitForLocked = true;
                        }
                    }
                } else {
                    this.waitForLocked = true;
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (procDate != null) {
                ProcessUtil.destroy(procDate);
            }
            if (procTime != null) {
                ProcessUtil.destroy(procTime);
            }
        }
    }

    protected SafeProcess exec(String command) throws IOException {
        return ProcessUtil.exec(command);
    }
}
