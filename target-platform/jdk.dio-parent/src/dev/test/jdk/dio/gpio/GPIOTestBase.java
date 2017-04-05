/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package dio.gpio;

import dio.runner.gson.GpioPinCollection;
import dio.runner.gson.GpioPinCollection.GpioData;
import dio.shared.TestBase;
import java.util.ArrayList;
import jdk.dio.DeviceConfig;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;
import jdk.dio.gpio.GPIOPortConfig;

/**
 * @title GPIOTestBase class to handle common actions for GPIO testing
 * @author stanislav.smirnov@oracle.com
 */
public class GPIOTestBase implements TestBase {

    protected enum Modes {PIN, PORT}

    protected ArrayList<PinConfig> GPIOpins;
    protected ArrayList<PortConfig> GPIOports;
    protected GPIOPinConfig[][] connectedPinPairsConfig;

    /**
     * Method to validate and decode input arguments
     * @param args to decode and use in configuration
     * @param mode element of Modes enumerator to detect what arguments to
     * decode and what to configure
     * @return
     */
    protected boolean decodeConfig(String[] args, Modes mode){
        boolean result = true;
        if(args == null || args.length == 0){
            result = false;
            System.err.println("No input arguments to decode");
        } else {
            switch (mode) {
                case PIN: {
                    int index = getDataIndex(args, "-gpins");
                    if (index == -1) {
                        result = false;
                        System.err.println("Wrong input gpins argument");
                    } else {
                        setupPinsConfig(args[index+1]);
                    }
                    break;
                }
                case PORT: {
                    int index = getDataIndex(args, "-gports");
                    if (index == -1) {
                        result = false;
                        System.err.println("Wrong input gports argument");
                    } else {
                        setupPinsConfig(args[index - 1]);
                        setupPortsConfig(args[index + 1]);
                    }
                    break;
                }
                default: {
                    System.err.println("Undefined GPIO mode");
                }
            }
        }
        return result;
    }

    /**
     * Method to setup pins configuration
     * @param config input pins configuration wrapped in Json
     */
    protected void setupPinsConfig(String config){
        GpioPinCollection pinsCollection = GpioPinCollection.deserialize(config, GpioPinCollection.class);
        pinsCollection.pins.stream().map((pin) -> {
            if(GPIOpins == null){
                GPIOpins = new ArrayList<>();
            }
            return pin;
        }).forEach((pin) -> {
            GPIOpins.add(new PinConfig(pin.deviceId, pin.deviceName, new GPIOPinConfig(0, pin.pinNumber, pin.direction, pin.mode, pin.trigger, false)));
        });

        GPIOpins.trimToSize();
    }

    /**
     * Method to get pin config by pin number
     * @param pinNumber
     * @return GPIOPinConfig instance
     */
    protected GPIOPinConfig getPinConfigByNumber(int pinNumber){
        GPIOPinConfig result = null;
        for(PinConfig config : GPIOpins){
            if(config.getPinConfig().getPinNumber() == pinNumber){
                result = config.getPinConfig();
                break;
            }
        }
        return result;
    }

    /**
     * Method to setup ports configuration
     * @param config input ports configuration wrapped in Json
     */
    protected void setupPortsConfig(String config){
        GpioPinCollection portsCollection = GpioPinCollection.deserialize(config, GpioPinCollection.class);
        for (GpioData port : portsCollection.ports) {
            if(port.portPins == null || (GPIOpins == null || GPIOpins.size() != port.portPins.size())){
                throw new IllegalArgumentException("No pins were specified for port");
            }

            if (GPIOports == null) {
                GPIOports = new ArrayList<>();
            }

            GPIOPinConfig[] pinsConfigArray = null;

            for (int i = 0; i < port.portPins.size(); i++) {
                if (pinsConfigArray == null) {
                    pinsConfigArray = new GPIOPinConfig[port.portPins.size()];
                }

                int pinConfigId = Integer.parseInt(port.portPins.get(i));

                pinsConfigArray[i] = new GPIOPinConfig(0,
                        GPIOpins.get(pinConfigId).getPinConfig().getPinNumber(),
                        GPIOpins.get(pinConfigId).getPinConfig().getDirection(),
                        GPIOpins.get(pinConfigId).getPinConfig().getDriveMode(),
                        GPIOpins.get(pinConfigId).getPinConfig().getTrigger(),
                        false);
            }

            GPIOports.add(new PortConfig(new GPIOPortConfig(port.direction, port.initValue, pinsConfigArray)));
        }

        GPIOports.trimToSize();
    }

    /**
     * Method to get literal value from direction decimal value
     * @param direction GPIOPinConfig direction
     * @return literal value
     */
    protected String getPinConfigDirection(int direction) {
        String text;
        switch (direction) {
            case GPIOPinConfig.DIR_INPUT_ONLY:
                text = "DIR_INPUT_ONLY";
                break;
            case GPIOPinConfig.DIR_OUTPUT_ONLY:
                text = "DIR_OUTPUT_ONLY";
                break;
            case GPIOPinConfig.DIR_BOTH_INIT_INPUT:
                text = "DIR_BOTH_INIT_INPUT";
                break;
            case GPIOPinConfig.DIR_BOTH_INIT_OUTPUT:
                text = "DIR_BOTH_INIT_OUTPUT";
                break;
            default:
                text = "Wrong direction: " + direction;
        }
        return text;
    }

    /**
     * Method to get literal value from direction decimal value
     * @param dir GPIOPin direction
     * @return literal value
     */
    protected String getPinDirection(int dir) {
        String text;
        switch (dir) {
            case GPIOPin.INPUT:
                text = "INPUT";
                break;
            case GPIOPin.OUTPUT:
                text = "OUTPUT";
                break;
            default:
                text = "UNKNOWN";
        }
        return text;
    }

    /**
     * Method to get literal value from drive modes decimal value
     * @param driveMode GPIOPinConfig drive mode
     * @return literal value
     */
    protected String getPinDriveMode(int driveMode) {
        String text = "";

        if (driveMode == DeviceConfig.DEFAULT) {
            text = "DEFAULT";
        } else {
            if ((driveMode & GPIOPinConfig.MODE_INPUT_PULL_UP) == GPIOPinConfig.MODE_INPUT_PULL_UP) {
                text = text + (text.length() > 0 ? " " : "") + "MODE_INPUT_PULL_UP";
            }
            if ((driveMode & GPIOPinConfig.MODE_INPUT_PULL_DOWN) == GPIOPinConfig.MODE_INPUT_PULL_DOWN) {
                text = text + (text.length() > 0 ? " " : "") + "MODE_INPUT_PULL_DOWN";
            }
            if ((driveMode & GPIOPinConfig.MODE_OUTPUT_PUSH_PULL) == GPIOPinConfig.MODE_OUTPUT_PUSH_PULL) {
                text = text + (text.length() > 0 ? " " : "") + "MODE_OUTPUT_PUSH_PULL";
            }
            if ((driveMode & GPIOPinConfig.MODE_OUTPUT_OPEN_DRAIN) == GPIOPinConfig.MODE_OUTPUT_OPEN_DRAIN) {
                text = text + (text.length() > 0 ? " " : "") + "MODE_OUTPUT_OPEN_DRAIN";
            }
        }
        return text;
    }

    /**
     * Method to get literal value from trigger decimal value
     * @param intTrigger GPIOPinConfig trigger
     * @return literal value
     */
    protected String getPinConfigTrigger(int intTrigger) {
        String text;
        switch (intTrigger) {
            case GPIOPinConfig.TRIGGER_NONE:
                text = "TRIGGER_NONE";
                break;
            case GPIOPinConfig.TRIGGER_FALLING_EDGE:
                text = "TRIGGER_FALLING_EDGE";
                break;
            case GPIOPinConfig.TRIGGER_RISING_EDGE:
                text = "TRIGGER_RISING_EDGE";
                break;
            case GPIOPinConfig.TRIGGER_BOTH_EDGES:
                text = "TRIGGER_BOTH_EDGES";
                break;
            case GPIOPinConfig.TRIGGER_HIGH_LEVEL:
                text = "TRIGGER_HIGH_LEVEL";
                break;
            case GPIOPinConfig.TRIGGER_LOW_LEVEL:
                text = "TRIGGER_LOW_LEVEL";
                break;
            case GPIOPinConfig.TRIGGER_BOTH_LEVELS:
                text = "TRIGGER_BOTH_LEVELS";
                break;
            default:
                text = "UNKNOWN";
        }
        return text;
    }
}