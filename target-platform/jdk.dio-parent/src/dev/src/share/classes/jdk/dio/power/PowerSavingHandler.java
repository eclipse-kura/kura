/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.dio.power;

import jdk.dio.Device;

/**
 * The {@code PowerSavingHandler} interface defines methods for getting notified of power state change requests on a
 * specific {@link Device} instance. <br />
 * A {@code PowerSavingHandler} can be registered using the
 * {@link PowerManaged#enablePowerSaving(int, jdk.dio.power.PowerSavingHandler) } method.
 * <p />
 * A power saving handler should implement the following requirements:
 * <ul>
 * <li>it should be implemented to be as fast as possible; for example it should not call any operations that may block,
 * nor pause the current thread.</li>
 * <li>it should not throw any unchecked exception.</li>
 * </ul>
 * A compliant implementation of this specification MUST catch unchecked exceptions that may be thrown by a power saving
 * handler and MAY guard against a hanging handler by timing out in order to guarantee the notification of all the
 * registered power saving handlers. A compliant implementation of this specification MUST ignore such unresponsive
 * or failing power saving handler invocations.
 *
 * @see PowerManaged
 * @since 1.0
 */
public interface PowerSavingHandler {

    /**
     * Invoked to allow the application to handle a <em>vetoable</em> power state change request on the designated
     * {@link Device} instance or {@link PowerManaged.Group} instance. The application may veto the power state
     * change by returning {@code 0}. Otherwise it
     * should return a duration lesser or equal to the proposed state change duration. An application may veto
     * altogether a power state change from {@link PowerManaged#POWER_ON} to {@link PowerManaged#LOWEST_POWER} if for
     * example the application is currently using or is about to use the designated device. An application may grant
     * a power state change duration lesser than the specified duration if for example the application anticipates it
     * will use the designated device earlier than the specified duration.
     * <p />
     * Since a transition to the {@link PowerManaged#POWER_ON} state can never be vetoed such as to not deny device
     * access to other applications this method is never invoked prior to a transition to the
     * {@link PowerManaged#POWER_ON} state.
     * <p />
     * Once this method has been called on all the {@code PowerSavingHandler}s registered for the device the
     * {@link #handlePowerStateChange handlePowerStateChange} method is invoked on these same {@code PowerSavingHandler}s with the smallest
     * of the negotiated durations unless the power state change has been vetoed.
     *
     * @param device
     *            the {@link Device} instance for which a power state change is requested or {@code null} if this
     *            {@link PowerSavingHandler} is registered for group notifications.
     * @param group
     *            the {@link PowerManaged.Group} instance that contains the {@link Device} instance for which a
     *            power state change is requested (never {@code null}).
     * @param currentState
     *            the current power state: {@link PowerManaged#POWER_ON}, {@link PowerManaged#LOW_POWER},
     *            {@link PowerManaged#LOWEST_POWER} or {@link PowerManaged#POWER_OFF}.
     * @param requestedState
     *            the requested power state: {@link PowerManaged#LOW_POWER}, {@link PowerManaged#LOWEST_POWER} or
     *            {@link PowerManaged#POWER_OFF}.
     * @param duration
     *            the expected duration (in milliseconds) of the new requested state;
     *            {@link PowerManaged#UNLIMITED_DURATION} if unlimited or unknown.
     * @return a duration (in milliseconds) lesser or equal to {@code duration} or {@code 0} if the power state change
     *         is vetoed or {@link PowerManaged#UNLIMITED_DURATION} if unlimited or unknown.
     */
    <P extends Device<? super P>> long handlePowerStateChangeRequest(P device, PowerManaged.Group group, int currentState, int requestedState, long duration);

    /**
     * Invoked to allow the application to handle a power state change (confirmation) on the designated {@link Device} instance
     * or {@link PowerManaged.Group} instance.
     * This method is invoked under two circumstances:
     * <ul>
     * <li>once the {@link #handlePowerStateChangeRequest} method of all the {@code PowerSavingHandler}s registered for
     * the device has been called without veto</li>
     * <li>upon an urgent (non-vetoable) power state change requested by the power management facility.</li>
     * </ul>
     *
     * @param device
     *            the {@link Device} instance for which a power state change is requested or {@code null} if this
     *            {@link PowerSavingHandler} is registered for group notifications.
     * @param group
     *            the {@link PowerManaged.Group} instance that contains the {@link Device} instance for which a
     *            power state change is requested (never {@code null}).
     * @param currentState
     *            the current power state: {@link PowerManaged#POWER_ON}, {@link PowerManaged#LOW_POWER},
     *            {@link PowerManaged#LOWEST_POWER} or {@link PowerManaged#POWER_OFF}.
     * @param requestedState
     *            the requested power state: {@link PowerManaged#POWER_ON}, {@link PowerManaged#LOW_POWER},
     *            {@link PowerManaged#LOWEST_POWER} or {@link PowerManaged#POWER_OFF}.
     * @param duration
     *            the duration (in milliseconds) of the new requested state; {@link PowerManaged#UNLIMITED_DURATION} if
     *            unlimited or unknown.
     */
    <P extends Device<? super P>> void handlePowerStateChange(P device, PowerManaged.Group group, int currentState, int requestedState, long duration);
}
