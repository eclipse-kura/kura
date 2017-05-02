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

package jdk.dio.atcmd;

import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;
import java.io.IOException;

/**
 * The {@code ATDevice} interface provides methods for controlling a Data Communication Equipment
 * such as a modem or a cellular module using AT commands.
 * <p />
 * An AT device may be identified by the numeric ID, by the name (if any defined) and by an
 * optional set of capabilities (properties) that correspond to its registered configuration. An
 * {@code ATDevice} instance can be opened by a call to one of the
 * {@link DeviceManager#open(int) DeviceManager.open(id,...)} methods using its ID or by a
 * call to one of the
 * {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[])
 * DeviceManager.open(name,...)} methods using its name and capabilities. <br />
 * The defined property keywords include {@code jdk.dio.atcmd.config},
 * {@code jdk.dio.atcmd.csd}, {@code jdk.dio.atcmd.psd},
 * {@code jdk.dio.atcmd.voice}, {@code jdk.dio.atcmd.sms},
 * {@code jdk.dio.atcmd.sim}, {@code jdk.dio.atcmd.phonebook}. Their meaning
 * is defined as follows:
 * <dl>
 * <dt>{@code jdk.dio.atcmd.config}</dt>
 * <dd>Supports access to configuration, control and identification commands.</dd>
 * <dt>{@code jdk.dio.atcmd.csd}</dt>
 * <dd>Supports access to circuit switched data (CSD) related AT commands.</dd>
 * <dt>{@code jdk.dio.atcmd.psd}</dt>
 * <dd>Supports access to packet switched data, such as GPRS or EDGE, related AT commands.</dd>
 * <dt>{@code jdk.dio.atcmd.voice}</dt>
 * <dd>Supports access to voice call related AT commands.</dd>
 * <dt>{@code jdk.dio.atcmd.sms}</dt>
 * <dd>Supports access to SMS related AT commands.</dd>
 * <dt>{@code jdk.dio.atcmd.sim}</dt>
 * <dd>Supports access to SIM related AT commands.</dd>
 * <dt>{@code jdk.dio.atcmd.phonebook}</dt>
 * <dd>Supports access to phonebook related AT commands.
 * </dl>
 * This list may be extended to designate other (possibly proprietary) capabilities (properties).
 * <p />
 * As per the convention, when one of this capabilities is supported by an AT device it must be
 * assigned as a positively asserted boolean capability: <blockquote>
 * <em>&lt;keyword&gt;</em>{@code =true} </blockquote>
 * For example: {@code jdk.dio.atcmd.phonebook=true}. <br />
 * When a capability is not supported by an AT device negatively asserting the boolean capability is
 * optional.
 * <p />
 * Commands can be issued to an {@code ATDevice} either synchronously or asynchronously. When
 * submitted synchronously using the {@link #sendCommand(java.lang.String) sendCommand(String)}, the
 * response string will be available as the return value to the call. When submitted asynchronously
 * using the
 * {@link #sendCommand(java.lang.String, jdk.dio.atcmd.CommandResponseHandler)
 * sendCommand(String, CommandResponseHandler)} a {@link CommandResponseHandler} instance must be
 * provided to handle the response when available.
 * <p />
 * The command strings passed as parameter to the {@code sendCommand} methods are the
 * complete AT command lines including the {@code AT} prefix and a termination character.
 * <p />
 * An {@code ATDevice} can only handle one command at a time. Commands cannot be sent (or queued)
 * while a command is already being handled. Nevertheless, if supported by the underlying AT device,
 * several commands may be concatenated in a single command line.
 * <p />
 * An {@code ATDevice} may report responses that are either information text or result codes. A
 * result code can be one of three types: final, intermediate, and unsolicited. A final result code
 * (e.g; {@code OK} or {@code ERROR}) indicates the completion of command and the readiness for
 * accepting new commands. An intermediate result code (e.g. {@code CONNECT}) is a report of the
 * progress of a command. An unsolicited result code (e.g. {@code RING}) indicates the occurrence of
 * an event not directly associated with the issuance of a command. <br />
 * Information text, final result code and intermediate result code responses are reported as return
 * values of calls to the {@code sendCommand(String)} method or as the parameter to the
 * {@code processResponse} method of a {@code CommandResponseHandler} instance provided as parameter
 * to a call to {@code sendCommand(String, CommandResponseHandler)}. <br />
 * Such response strings may include command echos unless command echo has been disabled
 * (such as with an {@code ATE0} command). <br />
 * Unsolicited result code responses are reported and passed as parameter to the
 * {@link UnsolicitedResponseHandler#processResponse processResponse} method of a
 * {@link UnsolicitedResponseHandler} instance.
 * <p />
 * A data connection can be established by calling the {@link #openDataConnection
 * openDataConnection} with a dedicated AT command such as {@code ATD}. The state of the connection
 * can be monitored by additionally providing an {@link DataConnectionHandler} instance.
 * <p />
 * When done, an application should call the {@link #close() close} method to close the AT device.
 * Any further attempt to use an {@code ATDevice} instance which has been closed will result in a
 * {@link ClosedDeviceException} been thrown.
 * <p />
 * Opening an {@code ATDevice} instance is subject to permission checks (see {@link ATPermission}).
 * <p />
 * Note: The {@code sendCommand} methods of {@code ATDevice} do not parse the provided AT commands.
 * The AT command line should include the {@code AT} prefix and the proper termination character
 * when and where needed.
 *
 * @see ATPermission
 * @see CommandResponseHandler
 * @see UnsolicitedResponseHandler
 * @see DataConnection
 * @since 1.0
 */
public interface ATDevice extends Device<ATDevice> {

    /**
     * Aborts the currently executing command by sending the provided {@code abortSequence}.
     * Abortion depends on the command's definition (abortability). Calling this method does NOT
     * guarantee abortion of the currently executing command. It only aborts if the command supports
     * abortion and it is currently in a proper state for abortion.
     *
     * @param abortSequence
     *            the character sequence for aborting; if {@code null} the {@code ESC} (abort)
     *            character is sent out by default.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void abortCommand(String abortSequence) throws IOException, UnavailableDeviceException,
            ClosedDeviceException;

    /**
     * When in data mode, calling this method will try to switch to command mode such as (depending
     * on the underlying AT device) by sending {@code "+++"} escape sequence.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void escapeToCommandMode() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Returns the maximum length of the command string that can be processed by the underlying AT
     * parser. Command string exceeding this value may be cut off without warning as this is a
     * default behavior of modems.
     *
     * @return maximum length of command line string that can be processed
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    int getMaxCommandLength() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Queries if this AT device has an opened data connection.
     *
     * @return {@code true} if connected; {@code false} otherwise.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    boolean isConnected() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Queries if this AT device is in command mode. When in command mode, a new command can be sent
     * provided no command is currently being processed.
     *
     * @return {@code true} if in command mode; {@code false} otherwise.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    boolean isInCommandMode() throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Opens a data connection by issuing the specified AT command and optionally handles the
     * response and the opened connection asynchronously. The call will return immediately and the
     * provided {@link CommandResponseHandler} and {@link DataConnectionHandler} instances will be
     * invoked to handle respectively the response (error or intermediate and final result codes)
     * when available and the connection when opened then when subsequently closed.
     *
     * @param cmd
     *            the complete command line including the {@code AT} prefix and the termination
     *            character when and where needed.
     * @param crHandler
     *            the {@code CommandResponseHandler} instance to handle the response to the command
     *            or {@code null} if notification of specific error, intermediate and final response
     *            codes are not requested.
     * @param dcHandler
     *            the {@code DataConnectionHandler} instance to handle the data connection or
     *            {@code null} if notification of connection state is not requested.
     * @return the opened data connection or {@code null} if no data connection has been opened.
     * @throws UnsupportedOperationException
     *             if opening data connections is not supported by this device.
     * @exception IllegalStateException
     *                if in data mode or if a command is already being executed.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws SecurityException
     *             if the caller does not have the required permission.
     */
    DataConnection openDataConnection(String cmd, CommandResponseHandler crHandler, DataConnectionHandler dcHandler)
            throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sends an AT command and wait for the response. If the command line includes payload text, it
     * must be properly terminated with e.g. {@code Ctrl-Z} otherwise the operation may block. In
     * which case it may be canceled by a call to {@link #abortCommand abortCommand}. <br />
     * The returned response string may include the command echo unless command echo has
     * been disabled (such as with an {@code ATE0} command).
     *
     * @param cmd
     *            the complete command line including the {@code AT} prefix and the termination
     *            character when and where needed.
     * @return the response to the command.
     * @exception IllegalStateException
     *                if in data mode or if a command is already being executed.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    String sendCommand(String cmd) throws IOException, UnavailableDeviceException, ClosedDeviceException;

    /**
     * Sends an AT command and handle the response asynchronously. The call will return immediately
     * and the provided {@link CommandResponseHandler} instance will be invoked to handle the
     * response when available. The command line may or may not include payload text (such as SMS
     * body text); in which case the provided {@code CommandResponseHandler} instance will be
     * invoked to provide the additional input text (text prompt mode). If the command line includes
     * payload text, it must be properly terminated with e.g. {@code Ctrl-Z}.
     *
     * @param cmd
     *            the complete command line including the {@code AT} prefix and the termination
     *            character when and where needed.
     * @param handler
     *            the {@code CommandResponseHandler} instance to handle the response to the command.
     * @throws NullPointerException
     *             if {@code cmd} or {@code handler} is {@code null}.
     * @exception IllegalStateException
     *                if in data mode or if a command is already being executed.
     * @throws IOException
     *             if some other I/O error occurs.
     * @throws UnavailableDeviceException
     *             if this device is not currently available - such as it is locked by another
     *             application.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     */
    void sendCommand(String cmd, CommandResponseHandler handler) throws IOException, UnavailableDeviceException,
            ClosedDeviceException;

    /**
     * Registers a handler for handling Unsolicited Result Code responses.
     * <p />
     * If this {@code ATDevice} is open in {@link DeviceManager#SHARED} access mode the handlers
     * registered by all the applications sharing the underlying device will get invoked to handle
     * Unsolicited Result Code responses.
     * <p />
     * If {@code handler} is {@code null} then the previously registered handler will be removed.
     * <p />
     * Only one handler can be registered at a particular time.
     *
     * @param handler
     *            the {@code UnsolicitedResponseHandler} instance to handle Unsolicited Result Code
     *            responses.
     * @throws NullPointerException
     *             if {@code handler} is {@code null}.
     * @throws IllegalStateException
     *             if {@code handler} is not {@code null} and a handler is already registered.
     * @throws ClosedDeviceException
     *             if the device has been closed.
     * @throws IOException
     *             if some other I/O error occurs.
     */
    void setUnsolicitedResponseHandler(UnsolicitedResponseHandler handler) throws IOException,
            UnavailableDeviceException, ClosedDeviceException;

    /**
     * {@inheritDoc}
     * <p />
     * Closing an {@code ATDevice} will also close the device's {@code DataConnection}.
     *
     * @throws IOException
     *             if some other I/O error occurs.
     */
    @Override
    void close() throws IOException;
}
