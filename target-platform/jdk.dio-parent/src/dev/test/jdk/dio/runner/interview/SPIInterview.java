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
import dio.runner.gson.SPIDeviceCollection;
import dio.runner.gson.SPIDeviceCollection.SPIDeviceData;
import java.util.ArrayList;
import java.util.Map;

/**
 * @title JT Harness interview, SPI specific questions
 * @author stanislav.smirnov@oracle.com
 */
public class SPIInterview extends Interview {

    static final String[] TOPO_CHOICE = {"Device ID", "PeripheralConfig"};

    static final String[] CLOCK_MODES = {"Active-high clocks, sampling occurs at odd edges",
        "Active-high clocks, sampling occurs at even edges",
        "Active-low clocks, sampling occurs at odd edges",
        "Active-low clocks, sampling occurs at even edges"};

    static final String[] CLOCK_MODES_INT = {"0", "1", "2", "3"};

    static final String[] CHIP_SELECT = {"CS_ACTIVE_HIGH",
        "CS_ACTIVE_LOW",
        "CS_NOT_CONTROLLED",
        "PeripheralConfig.DEFAULT"};

    static final String[] CHIP_SELECT_INT = {"0",
        "1",
        "2",
        "-1"};

    static final String[] BIT_ORDERING = {"Big endian",
        "Little endian",
        "Mixed endian"};

    static final String[] BIT_ORDERING_INT = {"1",
        "0",
        "2"};

    protected String KEY_SPI = "spi.interface";

    private BasicInterview basicInterview;

    protected SPIInterview(Interview parent, String baseTag) {
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
            return basicInterview.isPeripheralInterfaceSupported(BasicInterview.PI_SPIB);
        }

        @Override
        public Question getNext() {
            if (isEnabled()) {
                return qSpiConfig;
            } else {
                return qEnd;
            }
        }
    };

    private ListQuestion qSpiConfig = new SpiConfigQuestion(this, "spiConfig");

    private class SpiConfigQuestion extends ListQuestion {

        public SpiConfigQuestion(Interview i, String s) {
            super(i, s);
        }

        @Override
        public Body createBody(int i) {
            return new SpiConfigQuestionBody(this, i);
        }

        @Override
        protected Question getNext() {
            return qEnd;
        }

        @Override
        public Object[] getEndTextArgs() {
            return null;
        }

        @Override
        protected void export(Map data) {
            ListQuestion.Body[] bodies = getBodies();

            SPIDeviceCollection spiDevicesCollection = new SPIDeviceCollection();
            for (ListQuestion.Body b : bodies) {

                SpiConfigQuestionBody spiConfigQuestionBody = (SpiConfigQuestionBody) b;

                SPIDeviceData spiDeviceData = new SPIDeviceData();
                spiDeviceData.id = spiConfigQuestionBody.getId();
                spiDeviceData.deviceName = spiConfigQuestionBody.getDeviceName();
                spiDeviceData.deviceNumber = spiConfigQuestionBody.getDeviceNumber();
                spiDeviceData.address = spiConfigQuestionBody.getAddress();
                spiDeviceData.clockFrequency = spiConfigQuestionBody.getClockFrequency();
                spiDeviceData.clockMode = spiConfigQuestionBody.getClockMode();
                spiDeviceData.wordLength = spiConfigQuestionBody.getWordLength();
                spiDeviceData.bitOrdering = spiConfigQuestionBody.getBitOrdering();
                spiDeviceData.chipSelectActive = spiConfigQuestionBody.getChipSelectActive();
                spiDeviceData.transmitRequest = spiConfigQuestionBody.getTransmitRequest();
                spiDeviceData.transmitData = spiConfigQuestionBody.getTransmitData();
                spiDeviceData.receiveData = spiConfigQuestionBody.getReceiveData();

                if (spiDevicesCollection.spiDevices == null) {
                    spiDevicesCollection.spiDevices = new ArrayList<>();
                }

                spiDevicesCollection.spiDevices.add(spiDeviceData);
            }

            if (bodies.length == 0) {
                data.put(basicInterview.KEY_SPI, "false");
            } else {
                data.put(basicInterview.KEY_SPI, "true");
            }

            data.put(KEY_SPI, spiDevicesCollection.serialize());
        }

    }

    private class SpiConfigQuestionBody extends ListQuestion.Body {

        int index = 0;
        int id = -1;
        String deviceName = "";
        int deviceNumber = -1;
        int address = -1;
        int clockFrequency = -1;
        int clockMode = -1;
        int wordLength = -1;
        int bitOrdering = -1;
        int chipSelectActive = -1;
        int transmitRequest = -1;
        String transmitData = "";
        String receiveData = "";

        protected StringQuestion qDeviceId;
        protected ChoiceQuestion qDeviceConfig;
        protected StringQuestion qDeviceNumber;
        protected StringQuestion qDeviceName;
        protected StringQuestion qAddress;
        protected StringQuestion qClockFrequency;
        protected ChoiceQuestion qClockMode;
        protected StringQuestion qWordLength;
        protected ChoiceQuestion qBitOrdering;
        protected ChoiceQuestion qChipSelectActive;
        protected StringQuestion qTransmitRequest;
        protected StringQuestion qTransmitData;
        protected StringQuestion qReceiveData;

        public SpiConfigQuestionBody(ListQuestion q, int i) {
            super(q, i);
            this.index = i;

            init();
        }

        @Override
        public String getSummary() {
            return "spi" + this.index;
        }

        private void init() {
            qDeviceConfig = new ChoiceQuestion(this, "deviceConfig") {
                {
                    setChoices(TOPO_CHOICE, false);
                }

                @Override
                protected Question getNext() {
                    if (getStringValue() != null
                            && getStringValue().contains("ID")) {
                        return qDeviceId;
                    } else {
                        return qDeviceNumber;
                    }
                }
            };

            qDeviceId = new StringQuestion(this, "deviceId") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    id = Integer.valueOf(getStringValue());
                    return qTransmitRequest;
                }
            };

            qDeviceNumber = new StringQuestion(this, "deviceNumber") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    deviceNumber = Integer.valueOf(getStringValue());
                    return qDeviceName;
                }
            };

            qDeviceName = new StringQuestion(this, "deviceName") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    deviceName = getStringValue();
                    return qAddress;
                }
            };

            qAddress = new StringQuestion(this, "address") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    address = Integer.valueOf(getStringValue());
                    return qClockFrequency;
                }
            };

            qClockFrequency = new StringQuestion(this, "clockFrequency") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    clockFrequency = Integer.valueOf(getStringValue());
                    return qClockMode;
                }
            };

            qClockMode = new ChoiceQuestion(this, "clockMode") {
                {
                    setChoices(CLOCK_MODES_INT, CLOCK_MODES);
                }

                @Override
                protected Question getNext() {
                    clockMode = Integer.valueOf(getStringValue());
                    return qWordLength;
                }
            };

            qWordLength = new StringQuestion(this, "wordLength") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    wordLength = Integer.valueOf(getStringValue());
                    return qBitOrdering;
                }
            };

            qBitOrdering = new ChoiceQuestion(this, "bitOrdering") {
                {
                    setChoices(BIT_ORDERING_INT, BIT_ORDERING);
                }

                @Override
                protected Question getNext() {
                    bitOrdering = Integer.valueOf(getStringValue());
                    return qChipSelectActive;
                }
            };

            qChipSelectActive = new ChoiceQuestion(this, "chipSelectActive") {
                {
                    setChoices(CHIP_SELECT_INT, CHIP_SELECT);
                }

                @Override
                protected Question getNext() {
                    chipSelectActive = Integer.valueOf(getStringValue());
                    return qTransmitRequest;
                }
            };

            qTransmitRequest = new StringQuestion(this, "transmitRequest") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    transmitRequest = Integer.valueOf(getStringValue());
                    return qTransmitData;
                }
            };

            qTransmitData = new StringQuestion(this, "transmitData") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    transmitData = getStringValue();
                    return qReceiveData;
                }
            };

            qReceiveData = new StringQuestion(this, "receiveData") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    receiveData = getStringValue();
                    return qListEnd;
                }
            };

            if (basicInterview.isTopologyOpen()) {
                setFirstQuestion(qDeviceConfig);
            } else {
                setFirstQuestion(qDeviceId);
            }

        }

        String getName() {
            return "name";
        }

        public int getId() {
            return id;
        }

        public int getDeviceNumber() {
            return deviceNumber;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public int getAddress() {
            return address;
        }

        public int getClockFrequency() {
            return clockFrequency;
        }

        public int getClockMode() {
            return clockMode;
        }

        public int getWordLength() {
            return wordLength;
        }

        public int getBitOrdering() {
            return bitOrdering;
        }

        public int getChipSelectActive() {
            return chipSelectActive;
        }

        public int getTransmitRequest() {
            return transmitRequest;
        }

        public String getTransmitData() {
            return transmitData;
        }

        public String getReceiveData() {
            return receiveData;
        }

        private final FinalQuestion qListEnd = new FinalQuestion(this);

    }

    protected Question qEnd = new FinalQuestion(this);

}
