/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.watchdog;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * CriticalComponent is an interface that can be used to denote a component of functionality that is
 * 'critical' to the nature of the system. If a component implements CriticalComponent then it must
 * state its name as well as its criticalComponentTimeout. The name is a unique identifier in the
 * system. The timeout is a length of time in milliseconds that the CriticalComponent must check in
 * with the {@link WatchdogService}. If the CriticalComponent goes for a time period of greater
 * than this timeout, based on the (@link WatchdogService } configuration it will perform some action
 * (such as rebooting the system).
 *
 */
@ConsumerType
public interface CriticalComponent {

    /**
     * The unique identifier for this CriticalComponent
     *
     * @return a identifier unique to the {@link WatchdogService }
     */
    public String getCriticalComponentName();

    /**
     * The maximum amount of time in milliseconds that the CriticalComponent should check in
     * with the {@link WatchdogService} before the WatchdogService reboots the device.
     *
     * @return the timeout in milliseconds
     */
    public int getCriticalComponentTimeout();
}
