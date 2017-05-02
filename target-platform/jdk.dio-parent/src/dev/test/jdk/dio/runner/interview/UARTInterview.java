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
package dio.runner.interview;

import com.sun.interview.ChoiceQuestion;
import com.sun.interview.FinalQuestion;
import com.sun.interview.Interview;
import com.sun.interview.ListQuestion;
import com.sun.interview.NullQuestion;
import com.sun.interview.Question;
import com.sun.interview.StringQuestion;
import dio.runner.gson.UARTDeviceCollection;
import dio.runner.gson.UARTDeviceCollection.UARTDeviceData;
import java.util.ArrayList;
import java.util.Map;

/**
 * @title JT Harness interview, UART specific questions
 * @author stanislav.smirnov@oracle.com
 */
public class UARTInterview extends Interview {

    private static final String[] TOPO_CHOICE = {"Device ID", "PeripheralConfig"};
    private static final String[] UART_CHOICE = {"UART", "ModemUART"};

    private static final String[] DATA_BITS_CHOICE = {
        "DATABITS_5", "DATABITS_6", "DATABITS_7", "DATABITS_8", "DATABITS_9"
    };

    private static final String[] DATA_BITS_CHOICE_INT = {
        "5", "6", "7", "8", "9"
    };

    private static final String[] PARITIES_CHOICE = {
        "PARITY_ODD", "PARITY_EVEN", "PARITY_MARK", "PARITY_SPACE", "PARITY_NONE"
    };

    private static final String[] PARITIES_CHOICE_INT = {
        "1", "2", "3", "4", "0"
    };

    private static final String[] STOP_BITS_CHOICE = {
        "STOPBITS_1", "STOPBITS_1_5", "STOPBITS_2"
    };

    private static final String[] STOP_BITS_CHOICE_INT = {
        "1", "2", "3"
    };

    private static final String[] FLOW_CONTROL_MODES_CHOICE = {
        "FLOWCONTROL_NONE", "FLOWCONTROL_RTSCTS_IN", "FLOWCONTROL_RTSCTS_OUT", "FLOWCONTROL_XONXOFF_IN", "FLOWCONTROL_XONXOFF_OUT"
    };

    private static final String[] FLOW_CONTROL_MODES_CHOICE_INT = {
        "0", "1", "2", "4", "8"
    };

    protected String KEY_UART = "uart.interface";

    private BasicInterview basicInterview;

    protected UARTInterview(Interview parent, String baseTag) {
        super(parent, baseTag);
        basicInterview = (BasicInterview) parent;
        init();
    }

    private void init() {
        setFirstQuestion(qWelcome);
    }

    private final NullQuestion qWelcome = new NullQuestion(this, "welcome") {

        @Override
        public boolean isEnabled() {
            return basicInterview.isPeripheralInterfaceSupported(BasicInterview.PI_UART);
        }

        @Override
        public Question getNext() {
            if (isEnabled()) {
                return qUartConfig;
            } else {
                return qEnd;
            }
        }
    };

    private ListQuestion qUartConfig = new UARTConfigQuestion(this, "uartConfig");

    private class UARTConfigQuestion extends ListQuestion {

        public UARTConfigQuestion(Interview i, String s) {
            super(i, s);
        }

        @Override
        public Body createBody(int index) {
            return new UARTConfigBody(this, index);
        }

        @Override
        protected Question getNext() {
            return qEnd;
        }

        @Override
        protected void export(Map data) {
            ListQuestion.Body[] bodies = getBodies();

            UARTDeviceCollection uartDevicesCollection = new UARTDeviceCollection();
            for (ListQuestion.Body b : bodies) {

                UARTConfigBody uartConfig = (UARTConfigBody) b;

                UARTDeviceData uartDeviceData = new UARTDeviceData();
                uartDeviceData.id = uartConfig.getId();
                uartDeviceData.deviceType = uartConfig.getDeviceType();
                uartDeviceData.deviceName = uartConfig.getDeviceName();
                uartDeviceData.deviceNumber = uartConfig.getDeviceNumber();
                uartDeviceData.chanelNumber = uartConfig.getChannelNumber();
                uartDeviceData.baudRate = uartConfig.getBaudRate();
                uartDeviceData.dataBits = uartConfig.getDataBits();
                uartDeviceData.parity = uartConfig.getParity();
                uartDeviceData.stopBits = uartConfig.getStopBits();
                uartDeviceData.flowControlMode = uartConfig.getFlowcontrol();
                uartDeviceData.inputBufferSize = uartConfig.getInputBufferSize();
                uartDeviceData.outputBufferSize = uartConfig.getOutputBufferSize();

                if (uartDevicesCollection.uartDevices == null) {
                    uartDevicesCollection.uartDevices = new ArrayList<>();
                }

                uartDevicesCollection.uartDevices.add(uartDeviceData);
            }

            if (bodies.length == 0) {
                data.put(basicInterview.KEY_UART, "false");
            } else {
                data.put(basicInterview.KEY_UART, "true");
            }

            data.put(KEY_UART, uartDevicesCollection.serialize());
        }

    }

    private class UARTConfigBody extends ListQuestion.Body {

        int index = 0;
        int id = -1;
        String deviceType = "";
        int deviceNumber = -1;
        String deviceName = "";
        int channelNumber = -1;
        int baudRates = -1;
        int dataBits = -1;
        int parity = -1;
        int stopBits = -1;
        int flowcontrol = -1;
        int inputBufferSize = -1;
        int outputBufferSize = -1;
        String envParamName = "";

        protected StringQuestion qId;
        protected ChoiceQuestion qUARTDeviceConfig;
        protected ChoiceQuestion qUARTType;
        protected StringQuestion qUARTDeviceNumber;
        protected StringQuestion qUARTDeviceName;
        protected StringQuestion qChannelNumber;
        protected StringQuestion qBaudRate;
        protected ChoiceQuestion qDataBits;
        protected ChoiceQuestion qParity;
        protected ChoiceQuestion qStopBits;
        protected ChoiceQuestion qFlowControlMode;
        protected StringQuestion qInputBufferSize;
        protected StringQuestion qOutputBufferSize;

        UARTConfigBody(ListQuestion q, int index) {
            super(q, index);
            this.index = index;
            init();
        }

        private void init() {

            qUARTDeviceConfig = new ChoiceQuestion(this, "uartDeviceConfig", TOPO_CHOICE) {

                @Override
                protected Question getNext() {
                    if (getStringValue() != null
                            && getStringValue().contains("ID")) {
                        return qId;
                    }

                    return qUARTDeviceNumber;
                }
            };
            qId = new StringQuestion(this, "uartId") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    try {
                        id = Integer.parseInt(getStringValue());
                    } catch (NumberFormatException ex) {
                        return basicInterview.qInvalidInt;
                    }

                    return qUARTType;
                }
            };
            qUARTDeviceNumber = new StringQuestion(this, "uartDeviceNumber") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    try {
                        deviceNumber = Integer.parseInt(getStringValue());
                    } catch (NumberFormatException ex) {
                        return basicInterview.qInvalidInt;
                    }
                    return qUARTDeviceName;
                }
            };
            qUARTDeviceName = new StringQuestion(this, "uartDeviceName") {

                @Override
                protected Question getNext() {
                    deviceName = getStringValue();
                    return qChannelNumber;
                }
            };
            qChannelNumber = new StringQuestion(this, "channelNumber") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    try {
                        channelNumber = Integer.parseInt(getStringValue());
                    } catch (NumberFormatException ex) {
                        return basicInterview.qInvalidInt;
                    }
                    return qUARTType;
                }
            };
            qUARTType = new ChoiceQuestion(this, "uartType") {
                    {
                        setChoices(UART_CHOICE, false);
                    }
                @Override
                protected Question getNext() {
                    deviceType = getStringValue();
                    if (deviceType.equals(UART_CHOICE[0])) {
                        envParamName = "uart" + index;
                    } else {
                        envParamName = "modemUart" + index;
                    }

                    if(qUARTDeviceConfig.getStringValue().contains("ID")){
                        return qListEnd;
                    }

                    return qBaudRate;
                }
            };
            qBaudRate = new StringQuestion(this, "baudRate") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    try {
                        baudRates = Integer.parseInt(getStringValue());
                    } catch (NumberFormatException ex) {
                        return basicInterview.qInvalidInt;
                    }
                    return qDataBits;
                }
            };
            qDataBits = new ChoiceQuestion(this, "dataBits", DATA_BITS_CHOICE_INT) {

                @Override
                protected void setChoices(String[] choices, String[] displayChoices) {
                    super.setChoices(choices, DATA_BITS_CHOICE);
                }

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    try {
                        dataBits = Integer.parseInt(getStringValue());
                    } catch (NumberFormatException ex) {
                        return basicInterview.qInvalidInt;
                    }
                    return qParity;
                }
            };
            qParity = new ChoiceQuestion(this, "parity", PARITIES_CHOICE_INT) {

                @Override
                protected void setChoices(String[] choices, String[] displayChoices) {
                    super.setChoices(choices, PARITIES_CHOICE);
                }

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    try {
                        parity = Integer.parseInt(getStringValue());
                    } catch (NumberFormatException ex) {
                        return basicInterview.qInvalidInt;
                    }
                    return qStopBits;
                }
            };
            qStopBits = new ChoiceQuestion(this, "stopBits", STOP_BITS_CHOICE_INT) {

                @Override
                protected void setChoices(String[] choices, String[] displayChoices) {
                    super.setChoices(choices, STOP_BITS_CHOICE);
                }

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    try {
                        stopBits = Integer.parseInt(getStringValue());
                    } catch (NumberFormatException ex) {
                        return basicInterview.qInvalidInt;
                    }
                    return qFlowControlMode;
                }
            };
            qFlowControlMode = new ChoiceQuestion(this, "flowControlMode", FLOW_CONTROL_MODES_CHOICE_INT) {

                @Override
                protected void setChoices(String[] choices, String[] displayChoices) {
                    super.setChoices(choices, FLOW_CONTROL_MODES_CHOICE);
                }

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    try {
                        flowcontrol = Integer.parseInt(getStringValue());
                    } catch (NumberFormatException ex) {
                        return basicInterview.qInvalidInt;
                    }
                    return qInputBufferSize;
                }
            };
            qInputBufferSize = new StringQuestion(this, "inputBufferSize") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    try {
                        inputBufferSize = Integer.parseInt(getStringValue());
                    } catch (NumberFormatException ex) {
                        return basicInterview.qInvalidInt;
                    }
                    return qOutputBufferSize;
                }
            };
            qOutputBufferSize = new StringQuestion(this, "outputBufferSize") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    try {
                        outputBufferSize = Integer.parseInt(getStringValue());
                    } catch (NumberFormatException ex) {
                        return basicInterview.qInvalidInt;
                    }
                    return qListEnd;
                }
            };

            if (basicInterview.isTopologyOpen()) {
                setFirstQuestion(qUARTDeviceConfig);
            } else {
                setFirstQuestion(qUARTType);
            }
        }

        private final FinalQuestion qListEnd = new FinalQuestion(this);

        @Override
        public String getSummary() {
            return getEnvParamName();
        }

        public int getId() {
            return id;
        }

        public String getDeviceType() {
            return deviceType;
        }

        public int getDeviceNumber() {
            return deviceNumber;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public int getChannelNumber() {
            return channelNumber;
        }

        public int getBaudRate() {
            return baudRates;
        }

        public int getStopBits() {
            return stopBits;
        }


        public int getDataBits() {
            return dataBits;
        }

        public int getParity() {
            return parity;
        }

        public int getFlowcontrol() {
            return flowcontrol;
        }

        public int getInputBufferSize() {
            return inputBufferSize;
        }

        public int getOutputBufferSize() {
            return outputBufferSize;
        }

        public String getEnvParamName() {
            return envParamName;
        }

    }

    private Question qEnd = new FinalQuestion(this);
}
