/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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

/**
 * Describes the supervision state of the hardware watchdog managed by the {@link WatchdogService}.
 *
 * @since 2.9
 */
public enum WatchdogState {

    /**
     * The {@link WatchdogService} is disabled by configuration and does not supervise the system.
     */
    DISABLED,

    /**
     * The watchdog device is open and it is periodically refreshed. This is the only state in which the
     * system is under hardware watchdog supervision.
     */
    SUPERVISING,

    /**
     * The {@link WatchdogService} is enabled but the watchdog device does not exist or cannot be opened. The system
     * is not under hardware watchdog supervision.
     */
    DEVICE_UNAVAILABLE,

    /**
     * A system reboot has been requested, either because a {@link CriticalComponent} timed out or because the
     * watchdog device is unavailable and the service is configured to fail on unavailable device. The
     * state is kept while a failed reboot is being retried.
     */
    REBOOT_REQUESTED
}
