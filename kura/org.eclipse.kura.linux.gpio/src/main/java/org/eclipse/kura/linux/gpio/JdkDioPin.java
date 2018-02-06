/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.gpio;

import java.io.IOException;

import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.gpio.PinStatusListener;

import jdk.dio.ClosedDeviceException;
import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceNotFoundException;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.UnsupportedDeviceTypeException;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;
import jdk.dio.gpio.PinEvent;
import jdk.dio.gpio.PinListener;

public class JdkDioPin implements KuraGPIOPin {

    private GPIOPin thePin;
    private final int pinIndex;
    private String pinName = null;

    private KuraGPIODirection direction = null;
    private KuraGPIOMode mode = null;
    private KuraGPIOTrigger trigger = null;

    PinStatusListener localListener;

    public JdkDioPin(int pinIndex) {
        super();
        this.pinIndex = pinIndex;
    }

    public JdkDioPin(int pinIndex, String pinName, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        super();
        this.pinIndex = pinIndex;
        this.pinName = pinName;
        this.direction = direction;
        this.mode = mode;
        this.trigger = trigger;
    }

    public static JdkDioPin parseFromProperty(Object key, String property) {
        try {
            int index = Integer.parseInt((String) key);

            String[] tokens = property.split(",");

            String name = getValueByToken("name", tokens);
            String deviceType = getValueByToken("deviceType", tokens);
            if (deviceType != null && "gpio.GPIOPin".equals(deviceType.trim())) {
                KuraGPIODirection d = parseDirection(getValueByToken("direction", tokens));
                KuraGPIOMode m = parseMode(getValueByToken("mode", tokens));
                KuraGPIOTrigger t = parseTrigger(getValueByToken("trigger", tokens));
                return new JdkDioPin(index, name, d, m, t);
            }

        } catch (Exception e) {
            // Invalid property. Not a GPIO Pin
            return null;
        }

        return null;
    }

    private static String getValueByToken(String token, String[] tokens) {
        try {
            for (String e : tokens) {
                String[] elements = e.split(":");
                if (elements[0].trim().equals(token)) {
                    return elements[1];
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @Override
    public void setValue(boolean active) throws KuraClosedDeviceException, KuraUnavailableDeviceException, IOException {
        try {
            this.thePin.setValue(active);
        } catch (UnavailableDeviceException ex) {
            throw new KuraUnavailableDeviceException(ex, this.pinName != null ? this.pinName : this.pinIndex);
        } catch (ClosedDeviceException ex) {
            throw new KuraClosedDeviceException(ex, this.pinName != null ? this.pinName : this.pinIndex);
        }
    }

    @Override
    public boolean getValue() throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException {
        try {
            return this.thePin.getValue();
        } catch (UnavailableDeviceException e) {
            throw new KuraUnavailableDeviceException(e, this.pinName != null ? this.pinName : this.pinIndex);
        } catch (ClosedDeviceException e) {
            throw new KuraClosedDeviceException(e, this.pinName != null ? this.pinName : this.pinIndex);
        }
    }

    @Override
    public void addPinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException {
        this.localListener = listener;
        try {
            this.thePin.setInputListener(this.privateListener);
        } catch (ClosedDeviceException e) {
            throw new KuraClosedDeviceException(e, this.pinName != null ? this.pinName : this.pinIndex);
        }
    }

    @Override
    public void removePinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException {
        this.localListener = null;

        try {
            this.thePin.setInputListener(null);
        } catch (ClosedDeviceException e) {
            throw new KuraClosedDeviceException(e, this.pinName != null ? this.pinName : this.pinIndex);
        }
    }

    @Override
    public void open() throws KuraGPIODeviceException, KuraUnavailableDeviceException, IOException {
        if (this.direction != null) {
            GPIOPinConfig config = new GPIOPinConfig(DeviceConfig.DEFAULT, getPinIndex(), getDirectionInternal(),
                    getModeInternal(), getTriggerInternal(), false);
            try {
                this.thePin = DeviceManager.open(GPIOPin.class, config);
            } catch (InvalidDeviceConfigException e) {
                throw new KuraGPIODeviceException(e, getPinIndex());
            } catch (UnsupportedDeviceTypeException e) {
                throw new KuraGPIODeviceException(e, getPinIndex());
            } catch (DeviceNotFoundException e) {
                throw new KuraGPIODeviceException(e, getPinIndex());
            } catch (UnavailableDeviceException e) {
                throw new KuraUnavailableDeviceException(e, getPinIndex());
            }
        } else {
            try {
                this.thePin = DeviceManager.open(getPinIndex());
            } catch (InvalidDeviceConfigException e) {
                throw new KuraGPIODeviceException(e, getPinIndex());
            } catch (UnsupportedDeviceTypeException e) {
                throw new KuraGPIODeviceException(e, getPinIndex());
            } catch (DeviceNotFoundException e) {
                throw new KuraGPIODeviceException(e, getPinIndex());
            } catch (UnavailableDeviceException e) {
                throw new KuraUnavailableDeviceException(e, getPinIndex());
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (this.localListener != null) {
            try {
                removePinStatusListener(this.localListener);
            } catch (Exception ex) {
                // Do nothing
            }
        }
        if (this.thePin != null && this.thePin.isOpen()) {
            this.thePin.close();
        }
    }

    private final PinListener privateListener = new PinListener() {

        @Override
        public void valueChanged(PinEvent pinEvent) {

            if (JdkDioPin.this.localListener != null) {
                JdkDioPin.this.localListener.pinStatusChange(pinEvent.getValue());
            }
        }
    };

    private int getPinIndex() {
        return this.pinIndex;
    }

    private int getDirectionInternal() {
        switch (this.direction) {
        case INPUT:
            return GPIOPinConfig.DIR_INPUT_ONLY;
        case OUTPUT:
            return GPIOPinConfig.DIR_OUTPUT_ONLY;
        default:
            return -1;
        }
    }

    private static KuraGPIODirection parseDirection(String d) {
        try {
            switch (Integer.decode(d)) {
            case GPIOPinConfig.DIR_BOTH_INIT_INPUT:
            case GPIOPinConfig.DIR_INPUT_ONLY:
                return KuraGPIODirection.INPUT;
            case GPIOPinConfig.DIR_BOTH_INIT_OUTPUT:
            case GPIOPinConfig.DIR_OUTPUT_ONLY:
                return KuraGPIODirection.OUTPUT;
            default:
                return KuraGPIODirection.OUTPUT;
            }
        } catch (Exception e) {
        }
        return KuraGPIODirection.OUTPUT;
    }

    private static KuraGPIOMode parseMode(String m) {
        try {
            switch (Integer.decode(m)) {
            case GPIOPinConfig.MODE_INPUT_PULL_DOWN:
                return KuraGPIOMode.INPUT_PULL_DOWN;
            case GPIOPinConfig.MODE_INPUT_PULL_UP:
                return KuraGPIOMode.INPUT_PULL_UP;
            case GPIOPinConfig.MODE_OUTPUT_OPEN_DRAIN:
                return KuraGPIOMode.OUTPUT_OPEN_DRAIN;
            case GPIOPinConfig.MODE_OUTPUT_PUSH_PULL:
                return KuraGPIOMode.OUTPUT_PUSH_PULL;
            default:
                return KuraGPIOMode.OUTPUT_OPEN_DRAIN;
            }
        } catch (Exception e) {
        }
        return KuraGPIOMode.OUTPUT_OPEN_DRAIN;
    }

    private static KuraGPIOTrigger parseTrigger(String t) {
        try {
            switch (Integer.decode(t)) {
            case GPIOPinConfig.TRIGGER_BOTH_EDGES:
                return KuraGPIOTrigger.BOTH_EDGES;
            case GPIOPinConfig.TRIGGER_BOTH_LEVELS:
                return KuraGPIOTrigger.BOTH_LEVELS;
            case GPIOPinConfig.TRIGGER_FALLING_EDGE:
                return KuraGPIOTrigger.FALLING_EDGE;
            case GPIOPinConfig.TRIGGER_HIGH_LEVEL:
                return KuraGPIOTrigger.HIGH_LEVEL;
            case GPIOPinConfig.TRIGGER_LOW_LEVEL:
                return KuraGPIOTrigger.LOW_LEVEL;
            case GPIOPinConfig.TRIGGER_NONE:
                return KuraGPIOTrigger.NONE;
            case GPIOPinConfig.TRIGGER_RISING_EDGE:
                return KuraGPIOTrigger.RAISING_EDGE;
            default:
                return KuraGPIOTrigger.NONE;
            }
        } catch (Exception e) {
        }
        return KuraGPIOTrigger.NONE;
    }

    private int getModeInternal() {
        switch (this.mode) {
        case INPUT_PULL_DOWN:
            return GPIOPinConfig.MODE_INPUT_PULL_DOWN;
        case INPUT_PULL_UP:
            return GPIOPinConfig.MODE_INPUT_PULL_UP;
        case OUTPUT_OPEN_DRAIN:
            return GPIOPinConfig.MODE_OUTPUT_OPEN_DRAIN;
        case OUTPUT_PUSH_PULL:
            return GPIOPinConfig.MODE_OUTPUT_PUSH_PULL;
        default:
            return -1;
        }
    }

    private int getTriggerInternal() {
        switch (this.trigger) {
        case BOTH_EDGES:
            return GPIOPinConfig.TRIGGER_BOTH_EDGES;
        case BOTH_LEVELS:
            return GPIOPinConfig.TRIGGER_BOTH_LEVELS;
        case FALLING_EDGE:
            return GPIOPinConfig.TRIGGER_FALLING_EDGE;
        case HIGH_LEVEL:
            return GPIOPinConfig.TRIGGER_HIGH_LEVEL;
        case LOW_LEVEL:
            return GPIOPinConfig.TRIGGER_LOW_LEVEL;
        case NONE:
            return GPIOPinConfig.TRIGGER_NONE;
        case RAISING_EDGE:
            return GPIOPinConfig.TRIGGER_RISING_EDGE;
        default:
            return -1;
        }
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
        return this.thePin != null ? this.thePin.isOpen() : false;
    }

    @Override
    public String toString() {
        return "JdkDioPin [pinIndex=" + this.pinIndex + ", pinName=" + this.pinName + "]";
    }

}
