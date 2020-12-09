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
package org.eclipse.kura.gpio;

import java.io.IOException;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The <b>KuraGPIOPin</b> class is used to access the GPIO resource.<br>
 * The pin can be programmed either as an input or as an output. The way this is handled is implementation dependent.
 * <br>
 * <br>
 * Pins must be opened and closed before setting or getting values. Implementations, however, could automatically open a
 * pin if it is closed when accessing it, or automatically close it when it is not needed anymore.<br>
 * <br>
 * Status of input pins can be retrieved either with a call to {@link #getValue()} or by attaching a
 * {@link PinStatusListener}
 * to the pin.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface KuraGPIOPin {

    /**
     * Used to set the value of an output pin.
     *
     * @param active
     *            New state of the pin.
     * @throws KuraUnavailableDeviceException
     *             when the GPIO resource is not available
     * @throws KuraClosedDeviceException
     *             when the GPIO resource has not yet been opened
     * @throws IOException
     *             if an I/O error occurs
     */
    public void setValue(boolean active) throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException;

    /**
     * Used to get the current value of a pin, either output or input
     *
     * @return true if the pin is in active state
     * @throws KuraUnavailableDeviceException
     *             when the GPIO resource is not available
     * @throws KuraClosedDeviceException
     *             when the GPIO resource has not yet been opened
     * @throws IOException
     *             if an I/O error occurs
     */
    public boolean getValue() throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException;

    /**
     * Adds a {@link PinStatusListener} to this input pin. The listener will be notified when the status of this input
     * changes.<br>
     * Attaching a listener to an output pin should not raise an exception, but will have no result.
     *
     * @param listener
     *            Listener to be added to this pin
     * @throws KuraClosedDeviceException
     *             when the GPIO resource has not yet been opened
     * @throws IOException
     *             if an I/O error occurs
     */
    public void addPinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException;

    /**
     * Removes a {@link PinStatusListener} from this input pin.<br>
     * If the pin has no listeners attached, this method should fail silently.<br>
     *
     * @param listener
     *            Listener to be removed from this pin
     * @throws KuraClosedDeviceException
     *             when the GPIO resource has not yet been opened
     * @throws IOException
     *             if an I/O error occurs
     */
    public void removePinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException;

    /**
     * Opens the pin and allocates the needed resources to communicate with it.
     *
     * @throws KuraGPIODeviceException
     *             when an exception occurs opening the pin
     * @throws KuraUnavailableDeviceException
     *             when the GPIO resource is not available
     * @throws IOException
     *             if a generic I/O error occurs
     */
    public void open() throws KuraGPIODeviceException, KuraUnavailableDeviceException, IOException;

    /**
     * Closes this pin and deallocates the resources needed to communicate with it.<br>
     * <br>
     * If there is a {@link PinStatusListener} attached to this pin, the implementation should remove it
     * before closing the resource.
     *
     * @throws IOException
     *             if a generic I/O error occurs
     */
    public void close() throws IOException;

    /**
     *
     * @return {@link KuraGPIODirection} representing the direction (Input/Output) of the PIN
     */
    public KuraGPIODirection getDirection();

    /**
     *
     * @return {@link KuraGPIOMode} representing the mode of the pun.<br>
     *         Open Drain / Push Pull for outputs, Pull Up / Pull Down for inputs.
     */
    public KuraGPIOMode getMode();

    /**
     *
     * @return {@link KuraGPIOTrigger} representing the trigger mode for this pin.
     */
    public KuraGPIOTrigger getTrigger();

    /**
     *
     * @return the name associated with the the pin
     */
    public String getName();

    /**
     *
     * @return the numeric index of the pin's terminal
     */
    public int getIndex();

    /**
     *
     * @return true if the pin has been previously opened for use.
     */
    public boolean isOpen();
}
