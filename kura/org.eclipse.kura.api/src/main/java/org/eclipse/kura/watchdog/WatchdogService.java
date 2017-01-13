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
package org.eclipse.kura.watchdog;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface provides methods for starting, stopping, and updating a
 * hardware watchdog present on the system. Updating the watchdog, once
 * started, prevents the system from rebooting.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface WatchdogService {

    /**
     * Starts the hardware watchdog on the device. If a timeout value has not been
     * set, the watchdog will use its default timeout.<br>
     * This call is no longer used. The life-cycle of the watchdog is controlled
     * by the configuration parameters of the WatchdogSerivce. The API call
     * is retained for compatibility reasons.
     */
    @Deprecated
    public void startWatchdog();

    /**
     * Stops the hardware Watchdog on the device
     * This call is no longer used. The life-cycle of the watchdog is controlled
     * by the configuration parameters of the WatchdogSerivce. The API call
     * is retained for compatibility reasons.
     */
    @Deprecated
    public void stopWatchdog();

    /**
     * Returns the timeout value for the hardware watchdog in increments of milliseconds.
     *
     * @return An int representing the hardware timeout value in milliseconds.
     */
    public int getHardwareTimeout();

    /**
     * Register a critical service with the Critical Service Check-in.
     * Once registered, the critical service must call the checkin()
     * method (at a frequency higher than 1/timeout) to prevent a system reboot.
     *
     * @param criticalComponent
     *            The CriticalComponent to be registered.
     */
    @Deprecated
    public void registerCriticalService(CriticalComponent criticalComponent);

    /**
     * Unregister a critical service with the Critical Service Check-in.
     * Once unregistered, the critical service will no longer call the
     * checkin() method.
     *
     * @param criticalComponent
     *            The CriticalComponent to be unregistered.
     */
    @Deprecated
    public void unregisterCriticalService(CriticalComponent criticalComponent);

    /**
     * Register a critical component with the WatchdogService Check-in.
     * Once registered, the critical component must call the checkin()
     * method (at a frequency higher than 1/timeout) to prevent a system reboot.
     *
     * @param criticalComponent
     *            The CriticalComponent to be registered.
     */
    public void registerCriticalComponent(CriticalComponent criticalComponent);

    /**
     * Unregister a critical component with the WatchdogService Check-in.
     * Once unregistered, the critical component will no longer call the
     * checkin() method.
     *
     * @param criticalComponent
     *            The CriticalComponent to be unregistered.
     */
    public void unregisterCriticalComponent(CriticalComponent criticalComponent);

    /**
     * Returns the list of the currently registered CriticalComponents
     *
     * @return A List of CriticalComponents
     */
    public List<CriticalComponent> getCriticalComponents();

    /**
     * This method is used to notify the Watchdog Service that a critical service
     * has 'checked in' and the reboot timer should be reset.
     *
     * @param criticalComponent
     *            The criticalComponent to be updated.
     */
    public void checkin(CriticalComponent criticalComponent);

}
