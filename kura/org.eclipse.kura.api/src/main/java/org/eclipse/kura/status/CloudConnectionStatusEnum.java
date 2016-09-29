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
package org.eclipse.kura.status;

/**
 * Enum used to store the status of the notification for a {@link CloudConnectionStatusComponent}
 *
 */
public enum CloudConnectionStatusEnum {
    OFF, FAST_BLINKING, SLOW_BLINKING, HEARTBEAT, ON;

    /**
     * Delays in ms for Fast Blinking
     */
    public static final int FAST_BLINKING_ON_TIME = 100;
    public static final int FAST_BLINKING_OFF_TIME = 100;

    /**
     * Delays in ms for Slow Blinking
     */
    public static final int SLOW_BLINKING_ON_TIME = 300;
    public static final int SLOW_BLINKING_OFF_TIME = 300;

    /**
     * Delays in ms for Heartbeat
     */
    public static final int HEARTBEAT_SYSTOLE_DURATION = 150;
    public static final int HEARTBEAT_DIASTOLE_DURATION = 150;
    public static final int HEARTBEAT_PAUSE_DURATION = 600;

    /**
     * Delay in ms for periodic check on fixed statuses (On, Off)
     */
    public static final int PERIODIC_STATUS_CHECK_DELAY = 5000;
}
