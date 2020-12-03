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
 *******************************************************************************/
package org.eclipse.kura.emulator.gpio;

import java.io.IOException;

import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.gpio.PinStatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmulatedPin implements KuraGPIOPin {

    private static final Logger logger = LoggerFactory.getLogger(EmulatedPin.class);

    private boolean internalValue = false;
    String pinName = null;
    int pinIndex = -1;

    private KuraGPIODirection direction = KuraGPIODirection.OUTPUT;
    private KuraGPIOMode mode = KuraGPIOMode.OUTPUT_OPEN_DRAIN;
    private KuraGPIOTrigger trigger = KuraGPIOTrigger.NONE;

    public EmulatedPin(String pinName) {
        super();
        this.pinName = pinName;
    }

    public EmulatedPin(int pinIndex) {
        super();
        this.pinIndex = pinIndex;
    }

    public EmulatedPin(String pinName, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger) {
        super();
        this.pinName = pinName;
        this.direction = direction;
        this.mode = mode;
        this.trigger = trigger;
    }

    public EmulatedPin(int pinIndex, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger) {
        super();
        this.pinIndex = pinIndex;
        this.direction = direction;
        this.mode = mode;
        this.trigger = trigger;
    }

    @Override
    public void setValue(boolean active) throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException {
        this.internalValue = active;

        logger.debug("Emulated GPIO Pin {} changed to {}", this.pinName != null ? this.pinName : this.pinIndex,
                active ? "on" : "off");
    }

    @Override
    public boolean getValue() throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException {
        return this.internalValue;
    }

    @Override
    public void addPinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException {
    }

    @Override
    public void removePinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException {
    }

    @Override
    public void open() throws KuraGPIODeviceException, KuraUnavailableDeviceException, IOException {
        logger.info("Emulated GPIO Pin {} open.", this.pinName != null ? this.pinName : this.pinIndex);
    }

    @Override
    public void close() throws IOException {
        logger.info("Emulated GPIO Pin {} closed.", this.pinName != null ? this.pinName : this.pinIndex);
    }

    @Override
    public String toString() {
        return this.pinName != null ? "GPIO Pin: " + this.pinName : "Gpio PIN #" + String.valueOf(this.pinIndex);
    }

    @Override
    public KuraGPIODirection getDirection() {
        return this.direction;
    }

    @Override
    public KuraGPIOMode getMode() {
        return this.mode;
    }

    @Override
    public KuraGPIOTrigger getTrigger() {
        return this.trigger;
    }

    @Override
    public String getName() {
        return this.pinName != null ? this.pinName : String.valueOf(this.pinIndex);
    }

    @Override
    public int getIndex() {
        return this.pinIndex;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

}
