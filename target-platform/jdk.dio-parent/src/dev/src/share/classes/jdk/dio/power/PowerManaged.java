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

import jdk.dio.ClosedDeviceException;
import jdk.dio.Device;
import java.io.IOException;

/**
 * The {@code PowerManaged} interface provides methods that a {@link Device} class may implement to control how the
 * underlying device hardware resource is managed by the power management facility of the device.
 * <p />
 * The power management states defined are device as well as host device-dependent. For devices on a
 * microcontroller unit, there may be no distinction between {@link #POWER_OFF}, {@link #LOW_POWER} and
 * {@link #LOWEST_POWER} and they may for example all be supported by clock-gating the unused devices. Conversely, a
 * device external to the host device could support the four power management modes and could especially be
 * powered off.
 * <p />
 * A power state change may be dictated by the power management facility of the device or it may be requested by the
 * power management facility on behalf of the application itself or of another application (see
 * {@link #requestPowerStateChange requestPowerStateChange}). A power state change for a specific device may be requested
 * by another application if the device or some of the underlying device hardware resources are
 * shared. This is, for example, the case on a {@link GPIOPin} instance: another application may have opened a different
 * GPIO pin controlled by the same GPIO controller; the application will get notified of any power state changes
 * requested by the other application. Devices currently open by an application whose power managements
 * are logically or physically dependent belong to the same power management group (see {@link Group}).
 * <p />
 * An application may register to get notified of power state changes. When notified, the application may take the
 * following actions:
 * <ol>
 * <li>the application may save or restore the state/configuration of the device if needed. Saving the device's
 * state/configuration may be needed when the application is being notified of a power state change requested by another
 * application on a device hardware resources shared with the current application. Saving/Restoration of the
 * device's state/configuration may be needed when changing from/to {@link PowerManaged#POWER_OFF} or
 * {@link PowerManaged#LOWEST_POWER} to/from {@link PowerManaged#POWER_ON}, as the device state/context may not be
 * preserved.</li>
 * <li>the application may veto a power state change. For example, the application may veto a power state change from
 * {@link PowerManaged#POWER_ON} to {@link PowerManaged#LOWEST_POWER} if the application is currently using or is about
 * to use the designated device.</li>
 * <li>the application may grant a shorter power state change duration. For example, the application may grant a
 * duration of a power state change from {@link PowerManaged#POWER_ON} to {@link PowerManaged#LOWEST_POWER} shorter than
 * the specified duration if the application anticipates it will use the designated device earlier than the
 * specified duration.</li>
 * </ol>
 * <p />
 * If application-dictated power saving for a device is not explicitly enabled by a call to one of the
 * {@link #enablePowerSaving enablePowerSaving} methods the default power saving strategy of the platform applies.
 * This strategy is <em>platform as well as implementation-dependent</em>. It may define power saving rules (changing
 * the power state of a device when certain conditions are met) that may or may not differ from device
 * device to device. It may, for example, consist in forcefully changing all devices' power state to
 * {@link #LOWEST_POWER} upon some condition; in such a situation, attempting to access the device without restoring
 * its state/configuration may result in unexpected behavior. Therefore an application should always either:
 * <ol>
 * <li>register for power state changes on the devices it uses</li>
 * <li>or, register for system-wide power state changes (if supported by the platform) and close the devices when
 * going to power saving modes that may not preserve the device state/context and then open again the devices
 * when returning from such power saving modes.</li>
 * </ol>
 * <p />
 * <br />
 * When an application has enabled application-dictated power saving for several devices that belong to the same
 * power management group (by a call to one of the {@link #enablePowerSaving enablePowerSaving} methods) the effective
 * set of enabled power states is the intersection of the individually sets of enabled power states (bitwise AND). This
 * corresponds to the power states enabled for the group. If one of the device is subsequently closed the
 * power states enabled for the group MUST reflect the change.
 * <h3><a name="transitions">Valid Power State Transitions</a></h3>
 * The valid power state transitions are as follows:
 * <dl>
 * <dt>From the higher power states (lesser power saving states) to the lower power states (higher power saving states):
 * </dt>
 * <dd>
 * <ul>
 * <li>from {@link #POWER_ON} to {@link #LOW_POWER}</li>
 * <li>from {@link #POWER_ON} to {@link #LOWEST_POWER}</li>
 * <li>from {@link #POWER_ON} to {@link #POWER_OFF}</li>
 * <li>from {@link #LOW_POWER} to {@link #LOWEST_POWER}</li>
 * <li>from {@link #LOW_POWER} to {@link #POWER_OFF}</li>
 * <li>from {@link #LOWEST_POWER} to {@link #POWER_OFF}</li>
 * </ul>
 * </dd>
 * <dt>From any of the lower power states (higher power saving state) back to the highest power state (lesser power
 * saving state):</dt>
 * <dd>
 * <ul>
 * <li>from {@link #LOW_POWER} to {@link #POWER_ON}</li>
 * <li>from {@link #LOWEST_POWER} to {@link #POWER_ON}</li>
 * <li>from {@link #POWER_OFF} to {@link #POWER_ON}</li>
 * </ul>
 * Subsequently, after the duration of a power state change requested by a call to
 * {@link #requestPowerStateChange requestPowerStateChange} has expired the device's power state always transitions to
 * {@link #POWER_ON}.</dd>
 * </dl>
 * <p />
 *
 * @see PowerSavingHandler
 * @since 1.0
 */
public interface PowerManaged {
    /*
     * Consider renaming enablePowerSaving and disablePowerSaving to setPowerSavingDirectives and
     * clearPowerSavingDirectives
     */

    /*
     * Note: the power states below are freely adapted from the Device Power Sates defined by the Device power management
     * in the ACPI spec (http://www.acpi.info/DOWNLOADS/ACPIspec30a.pdf).
     */

    /**
     * Low power mode (may save less power while preserving more device context/state than
     * {@link #LOWEST_POWER}, hence allowing for a faster return to full performance).
     * <p />
     * When transitioning from this state to {@link #POWER_ON} no state/configuration restoration of the device
     * device must be needed.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other power state bit flags.
     */
    int LOW_POWER = 2;

    /**
     * Lowest power mode (may save more power while preserving less device context/state than
     * {@link #LOW_POWER}, hence only allowing for a slower return to full performance).
     * <p />
     * When transitioning from this state to {@link #POWER_ON} some state/configuration restoration of the device
     * device may be needed. This state/configuration restoration of the device may be handled by a
     * {@link PowerSavingHandler}.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other power state bit flags.
     */
    int LOWEST_POWER = 4;

    /**
     * Power has been fully removed from the device (for example from an external device).
     * <p />
     * When transitioning from this state to {@link #POWER_ON} a complete state/configuration restoration of the
     * device may be needed. This state/configuration restoration of the device may be handled by
     * a {@link PowerSavingHandler}.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other power state bit flags.
     */
    int POWER_OFF = 8;

    /**
     * Fully powered on.
     * <p />
     * This bit flag can be bitwise-combined (OR) with other power state bit flags.
     */
    int POWER_ON = 1;

    /**
     * Unlimited or unknown power state change requested duration.
     */
    long UNLIMITED_DURATION = -1;

    /**
     * Disables application-dictated power saving for the {@link Device} instance. The power saving strategy of the
     * platform applies.
     * <p />
     * If a {@link PowerSavingHandler} instance was registered using
     * {@link #enablePowerSaving(int, jdk.dio.power.PowerSavingHandler)} it will be unregistered.
     *
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws SecurityException
     *             if the caller does not have the required permission.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void disablePowerSaving() throws IOException, ClosedDeviceException;

    /**
     * Enables application-dictated power saving for the {@link Device} instance. Only the specified power states
     * are enabled for the {@link Device} instance. Any attempt by another application to change the power state of
     * the {@link Device} instance to a state different from those specified will be automatically vetoed. The power
     * management facility will not forcefully change the power state of the {@link Device} instance to a state
     * different from those specified unless in case of urgency.
     * <p />
     * By using the {@link #requestPowerStateChange requestPowerStateChange} method an application may override this directive and
     * request a power state change to a state other than those specified.
     * <p />
     * The {@link #POWER_ON} state is always implicitly enabled.
     * <p />
     * Subsequent calls to this method silently override any previous settings of enabled power states as well as
     * registered {@link PowerSavingHandler}.
     *
     * @param powerStates
     *            bitwise OR of enabled power states: {@link #LOW_POWER}, {@link #LOWEST_POWER} or {@link #POWER_OFF}.
     * @throws IllegalArgumentException
     *             if {@code powerStates} is not a bitwise OR of the defined power state values.
     * @throws SecurityException
     *             if the caller does not have the required permission.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void enablePowerSaving(int powerStates) throws IOException, ClosedDeviceException;

    /**
     * Enables application-dictated power saving for the {@link Device} instance and registers a
     * {@link PowerSavingHandler} instance to get asynchronously notified when the power management facility is about to
     * change the power state of the {@link Device} instance, hence allowing the application to veto the power state
     * change on the device. Only the specified power states are enabled for the {@link Device} instance. Any
     * attempt by another application to change the power state of the {@link Device} instance to a state different
     * from those specified will be automatically vetoed. The power management facility will not forcefully change the
     * power state of the {@link Device} instance to a state different from those specified unless in case of
     * urgency.
     * <p />
     * By using the {@link #requestPowerStateChange requestPowerStateChange} method an application may override this directive and
     * request a power state change to a state other than those specified.
     * <p />
     * The {@link #POWER_ON} state is always implicitly enabled.
     * <p />
     * Since only the specified power states are enabled, the application will only be notified of changes to these
     * states (including the {@link #POWER_ON} state).
     * <p />
     * Subsequent calls to this method silently override any previous settings of enabled power states as well as
     * registered {@link PowerSavingHandler}.
     *
     * @param powerStates
     *            bitwise OR of enabled power states: {@link #LOW_POWER}, {@link #LOWEST_POWER} or {@link #POWER_OFF}.
     * @param handler
     *            the {@link PowerSavingHandler} instance to be notified when a power state change is requested or confirmed for the
     *            device.
     * @throws IllegalArgumentException
     *             if {@code powerStates} is not a bitwise OR of the defined power state values.
     * @throws SecurityException
     *             if the caller does not have the required permission.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void enablePowerSaving(int powerStates, PowerSavingHandler handler) throws IOException, ClosedDeviceException;

    /**
     * Returns the current power state of the {@link Device} instance. If application-dictated power saving is
     * disabled (see {@link #disablePowerSaving disablePowerSaving} the power state depends on the power saving strategy of the platform.
     *
     * @return the current power state of the {@link Device} instance: {@link #POWER_ON}, {@link #LOW_POWER},
     *         {@link #LOWEST_POWER} or {@link #POWER_OFF}.
     * @throws SecurityException
     *             if the caller does not have the required permission.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    int getPowerState() throws IOException, ClosedDeviceException;

    /**
     * Requests the change of the device's current power state to the specified power state for the specified
     * duration. The power state change may be vetoed by the system or by other applications, or the actual duration of
     * the power state change may be lesser than the requested duration.
     * <p />
     * If a {@link PowerSavingHandler} instance has been registered by the current application, it will not get notified
     * of the requested power state change (neither its request nor its confirmation); it will only get notified when returning back to the {@link #POWER_ON} state
     * or of any interleaving power state changes requested by the system or by other applications.
     * <p />
     * If the device is already in the requested power state no notification will be performed and the remaining
     * duration of the current state will be set to the smallest of the requested duration and the currently remaining
     * duration.
     * <p />
     * Any invalid power state request (see <a href="#transitions">Valid Power State Transitions</a>) is automatically
     * vetoed and {@code 0} is returned.
     * <p />
     * A transition to the {@link #POWER_ON} state can never be vetoed such as to not deny device access to other
     * applications and the {@code duration} parameter is ignored and always assumed to be {@link #UNLIMITED_DURATION}.
     *
     * @param powerState
     *            the new power state of the {@link Device} instance: {@link #POWER_ON}, {@link #LOW_POWER},
     *            {@link #LOWEST_POWER} or {@link #POWER_OFF}.
     * @param duration
     *            the expected duration (in milliseconds) of the requested power state change or
     *            {@link #UNLIMITED_DURATION} if unlimited or unknown.
     * @return the actual (negotiated) duration (in milliseconds) - which may be lesser than or equal to the requested
     *         duration ( {@link #UNLIMITED_DURATION} if an unlimited/unknown duration was requested) or {@code 0} if
     *         the requested power state change has been vetoed.
     * @throws IllegalArgumentException
     *             if {@code powerState} is not one of the defined power state values or if {@code duration} is not
     *             greater than {@code 0} or equal to {@link #UNLIMITED_DURATION}.
     * @throws SecurityException
     *             if the caller does not have the required permission.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    long requestPowerStateChange(int powerState, long duration) throws IOException, ClosedDeviceException;

    /**
     * Returns the power management {@code Group} this device belongs to.
     * <p />
     * A compliant implementation MAY return the same {@code Group} instance (application-wise)
     * upon subsequent calls
     * to this {@code PowerManaged} {@code Device} instance or to other
     * {@code PowerManaged} {@code Device} instances belonging to that same
     * power management {@code Group} (until that {@code Group} is closed - see {@link Group}).
     *
     * @return the {@code Group} this device belongs to.
     *
     * @throws SecurityException
     *             if the caller does not have the required permission.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    Group getGroup() throws IOException, ClosedDeviceException;

    /**
     * The {@code Group} interface provides methods for registering for power state changes of devices belonging
     * to the same power management group. Devices currently open by an application belong to the same power management group when their power managements
     * are logically or physically dependent. For example, different GPIO pins may be controlled by the same GPIO controller
     * and their power managements are physically dependent: when a power state change is requested on one pin it results in
     * applications that have open any of the other pins to be notified of that power state change request, including
     * the requesting application. Registering for power state change request notifications on a power management group
     * rather than on individual devices belonging to that group allows for being notified only once for each
     * of the devices opened by the application belonging to that group. If an
     * application has registered for both notifications on an individual device belonging to a group and for notifications
     * on that same group notifications will happen on all registered handlers. Notification on the group happens last.
     * <p />
     * Devices belonging to the same group may not necessarily be of the same type.
     * <p />
     * When all the {@code Device} instances open by an application and belonging to the same power management group
     * have been closed the {@code Group} instance corresponding to that group enters an irreversible {@code closed} state:
     * power state changes are no longer notified on that {@code Group}.
     * <p />
     * A compliant implementation MUST guarantee the thread-safety of {@code Group} instances.
     *
     * @since 1.0
     */
    public interface Group {

        /**
         * Registers a {@code PowerSavingHandler} instance to get asynchronously notified when the power management
         * facility is about to change the power state of this group, hence allowing the application to veto the power
         * state change.
         * <p />
         * If {@code handler} is {@code null} then the previously registered {@code PowerSavingHandler} is unregistered.
         *
         * @param handler the {@link PowerSavingHandler} instance to be notified when a power state change is requested
         * for this group.
         *
         * @throws IllegalStateException if {@code handler} is not {@code null} and a {@code PowerSavingHandler} is
         * already registered.
         * @throws SecurityException if the caller does not have the required permission.
         * @throws ClosedDeviceException if this {@code Group} is closed; that is all the devices belonging to this {@code Group} have been closed.
         * @throws IOException if some other I/O error occurs.
         */
        void setPowerSavingHandler(PowerSavingHandler handler) throws IOException, ClosedDeviceException;

        /**
         * Check whether the provided {@code Device} is part of this group. To be part of a group
         * the {@code Device} must be open. As soon as a {@code Device} is closed it is removed
         * from the group.
         *
         * @param device the device to be checked.
         *
         * @return {@code true} if the provided {@code Device} is part of this group; {@code false} otherwise.
         *
         * @throws NullPointerException if {@code device} is {@code null}.
         * @throws SecurityException if the caller does not have the required permission.
         * @throws ClosedDeviceException if this {@code Group} is closed; that is all the devices belonging to this {@code Group} have been closed.
         * @throws IOException if some other I/O error occurs.
         */
        boolean contains(PowerManaged device) throws IOException, ClosedDeviceException;
    }
}
