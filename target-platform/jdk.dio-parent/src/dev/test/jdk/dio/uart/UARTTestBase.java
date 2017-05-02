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

package dio.uart;

import dio.runner.gson.UARTDeviceCollection;
import dio.shared.TestBase;
import java.util.ArrayList;
import jdk.dio.DeviceConfig;
import jdk.dio.uart.UARTConfig;
import jdk.dio.uart.UARTEvent;

/**
 *
 * @author stanislav.smirnov@oracle.com
 */
public class UARTTestBase implements TestBase {

    protected ArrayList<Config> UARTDevices;

    private static ArrayList<Integer> SUPPORTED_BAUD_RATES;
    private static ArrayList<Integer> SUPPORTED_DATA_BITS;
    private static ArrayList<Integer> SUPPORTED_FLOW_CONTROL_MODES;
    private static ArrayList<Integer> SUPPORTED_STOP_BITS;
    private static ArrayList<Integer> SUPPORTED_PARITIES;

    protected ArrayList<Integer> getSupportedBaudRates(){
        if(SUPPORTED_BAUD_RATES == null){
            SUPPORTED_BAUD_RATES = new ArrayList<>();
            SUPPORTED_BAUD_RATES.add(2400);
            SUPPORTED_BAUD_RATES.add(4800);
            SUPPORTED_BAUD_RATES.add(9600);
            SUPPORTED_BAUD_RATES.add(19200);
            SUPPORTED_BAUD_RATES.add(38400);
            SUPPORTED_BAUD_RATES.add(57600);
            SUPPORTED_BAUD_RATES.add(115200);
            SUPPORTED_BAUD_RATES.add(Integer.MAX_VALUE);
        }

        return SUPPORTED_BAUD_RATES;
    }

    protected ArrayList<Integer> getSupportedDataBits() {
        if(SUPPORTED_DATA_BITS == null){
            SUPPORTED_DATA_BITS = new ArrayList<>();
            SUPPORTED_DATA_BITS.add(UARTConfig.DATABITS_5);
            SUPPORTED_DATA_BITS.add(UARTConfig.DATABITS_6);
            SUPPORTED_DATA_BITS.add(UARTConfig.DATABITS_7);
            SUPPORTED_DATA_BITS.add(UARTConfig.DATABITS_8);
            SUPPORTED_DATA_BITS.add(UARTConfig.DATABITS_9);
        }

        return SUPPORTED_DATA_BITS;
    }

    protected ArrayList<Integer> getSupportedFlowControlModes() {
        if (SUPPORTED_FLOW_CONTROL_MODES == null) {
            SUPPORTED_FLOW_CONTROL_MODES = new ArrayList<>();
            SUPPORTED_FLOW_CONTROL_MODES.add(UARTConfig.FLOWCONTROL_NONE);
            SUPPORTED_FLOW_CONTROL_MODES.add(UARTConfig.FLOWCONTROL_RTSCTS_IN);
            SUPPORTED_FLOW_CONTROL_MODES.add(UARTConfig.FLOWCONTROL_RTSCTS_OUT);
            SUPPORTED_FLOW_CONTROL_MODES.add(UARTConfig.FLOWCONTROL_XONXOFF_IN);
            SUPPORTED_FLOW_CONTROL_MODES.add(UARTConfig.FLOWCONTROL_XONXOFF_OUT);
        }

        return SUPPORTED_FLOW_CONTROL_MODES;
    }

    protected ArrayList<Integer> getSupportedStopBits() {
        if(SUPPORTED_STOP_BITS == null){
            SUPPORTED_STOP_BITS = new ArrayList<>();
            SUPPORTED_STOP_BITS.add(UARTConfig.STOPBITS_1);
            SUPPORTED_STOP_BITS.add(UARTConfig.STOPBITS_1_5);
            SUPPORTED_STOP_BITS.add(UARTConfig.STOPBITS_2);
        }

        return SUPPORTED_STOP_BITS;
    }

    protected ArrayList<Integer> getSupportedParities() {
        if(SUPPORTED_PARITIES == null){
            SUPPORTED_PARITIES = new ArrayList<>();
            SUPPORTED_PARITIES.add(UARTConfig.PARITY_EVEN);
            SUPPORTED_PARITIES.add(UARTConfig.PARITY_MARK);
            SUPPORTED_PARITIES.add(UARTConfig.PARITY_NONE);
            SUPPORTED_PARITIES.add(UARTConfig.PARITY_ODD);
            SUPPORTED_PARITIES.add(UARTConfig.PARITY_SPACE);
        }

        return SUPPORTED_PARITIES;
    }

    /**
     * Method to validate and decode input arguments
     * @param args to decode and use in configuration
     * @return
     */
    protected boolean decodeConfig(String[] args){
        boolean result = true;
        if(args == null || args.length == 0){
            result = false;
            System.err.println("No input arguments to decode");
        } else {
            int index = getDataIndex(args, "-uart");
            if (index == -1) {
                result = false;
                System.err.println("Wrong input uart argument");
            } else {
                setupUartConfig(args[index + 1]);
            }
        }
        return result;
    }

    /**
     * Method to setup configuration
     * @param config input configuration wrapped in Json
     */
    protected void setupUartConfig(String config) {
        UARTDeviceCollection uartDevicesCollection = UARTDeviceCollection.deserialize(config, UARTDeviceCollection.class);
        uartDevicesCollection.uartDevices.stream().map((uartDevice) -> {
            if (UARTDevices == null) {
                UARTDevices = new ArrayList<>();
            }
            return uartDevice;
        }).forEach((uartDevice) -> {
            UARTDevices.add(new Config(
                    uartDevice.id,
                    uartDevice.deviceName,
                    uartDevice.deviceType,
                    new UARTConfig(
                            uartDevice.deviceNumber,
                            uartDevice.chanelNumber,
                            uartDevice.baudRate,
                            uartDevice.dataBits,
                            uartDevice.parity,
                            uartDevice.stopBits,
                            uartDevice.flowControlMode,
                            uartDevice.inputBufferSize,
                            uartDevice.outputBufferSize))
            );
        });

        UARTDevices.trimToSize();
    }

    protected String printDataBits(int intDataBits) {
        String text;
        switch (intDataBits) {
            case UARTConfig.DATABITS_5:
                text = "DATABITS_5";
                break;
            case UARTConfig.DATABITS_6:
                text = "DATABITS_6";
                break;
            case UARTConfig.DATABITS_7:
                text = "DATABITS_7";
                break;
            case UARTConfig.DATABITS_8:
                text = "DATABITS_8";
                break;
            case UARTConfig.DATABITS_9:
                text = "DATABITS_9";
                break;
            default:
                text = "! UNKNOWN !";
        }
        return text;
    }

    protected String printParity(int intParity) {
        String text;
        switch (intParity) {
            case UARTConfig.PARITY_ODD:
                text = "PARITY_ODD";
                break;
            case UARTConfig.PARITY_EVEN:
                text = "PARITY_EVEN";
                break;
            case UARTConfig.PARITY_MARK:
                text = "PARITY_MARK";
                break;
            case UARTConfig.PARITY_SPACE:
                text = "PARITY_SPACE";
                break;
            case UARTConfig.PARITY_NONE:
                text = "PARITY_NONE";
                break;
            default:
                text = "! UNKNOWN !";
        }
        return text;
    }

    protected String printStopBits(int intStopBits) {
        String text;
        switch (intStopBits) {
            case UARTConfig.STOPBITS_1:
                text = "STOPBITS_1";
                break;
            case UARTConfig.STOPBITS_1_5:
                text = "STOPBITS_1_5";
                break;
            case UARTConfig.STOPBITS_2:
                text = "STOPBITS_2";
                break;
            default:
                text = "! UNKNOWN !";
        }
        return text;
    }

    protected String printFlowControl(int intFlowControl) {
        String text = "";
        if (intFlowControl == DeviceConfig.DEFAULT) {
            text = "DEFAULT";
        } else {
            if (intFlowControl == UARTConfig.FLOWCONTROL_NONE) {
                text = text + (text.length() > 0 ? " " : "") + "FLOWCONTROL_NONE";
            }
            if ((intFlowControl & UARTConfig.FLOWCONTROL_RTSCTS_IN) == UARTConfig.FLOWCONTROL_RTSCTS_IN) {
                text = text + (text.length() > 0 ? " " : "") + "FLOWCONTROL_RTSCTS_IN";
            }
            if ((intFlowControl & UARTConfig.FLOWCONTROL_RTSCTS_OUT) == UARTConfig.FLOWCONTROL_RTSCTS_OUT) {
                text = text + (text.length() > 0 ? " " : "") + "FLOWCONTROL_RTSCTS_OUT";
            }
            if ((intFlowControl & UARTConfig.FLOWCONTROL_XONXOFF_IN) == UARTConfig.FLOWCONTROL_XONXOFF_IN) {
                text = text + (text.length() > 0 ? " " : "") + "FLOWCONTROL_XONXOFF_IN";
            }
            if ((intFlowControl & UARTConfig.FLOWCONTROL_XONXOFF_OUT) == UARTConfig.FLOWCONTROL_XONXOFF_OUT) {
                text = text + (text.length() > 0 ? " " : "") + "FLOWCONTROL_XONXOFF_OUT";
            }
        }
        return text;
    }

    protected String printEventType(int intEvent) {
        String text;
        switch (intEvent) {
            case UARTEvent.INPUT_DATA_AVAILABLE:
                text = "INPUT_DATA_AVAILABLE";
                break;
            case UARTEvent.OUTPUT_BUFFER_EMPTY:
                text = "OUTPUT_BUFFER_EMPTY";
                break;
            case UARTEvent.INPUT_BUFFER_OVERRUN:
                text = "INPUT_BUFFER_OVERRUN";
                break;
            case UARTEvent.BREAK_INTERRUPT:
                text = "BREAK_INTERRUPT";
                break;
            case UARTEvent.FRAMING_ERROR:
                text = "FRAMING_ERROR";
                break;
            case UARTEvent.PARITY_ERROR:
                text = "PARITY_ERROR";
                break;
            default:
                text = "! UNKNOWN !";
        }
        return text;
    }

}
