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

import com.google.gson.Gson;
import com.sun.interview.ChoiceArrayQuestion;
import com.sun.interview.ChoiceQuestion;
import com.sun.interview.FinalQuestion;
import com.sun.interview.Interview;
import com.sun.interview.ListQuestion;
import com.sun.interview.NullQuestion;
import com.sun.interview.Question;
import com.sun.interview.StringQuestion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import dio.runner.gson.GpioPinCollection;
import dio.runner.gson.GpioPinCollection.GpioData;

/**
 * @title JT Harness interview, GPIO specific questions
 * @author stanislav.smirnov@oracle.com
 */
public class GPIOInterview extends Interview {

    private BasicInterview basicInterview;
    //private final GPIOInterview gpioInterview;

    //private static final String[] TOPO_CHOICE = {"Device ID", "PeripheralConfig"};
    private static final String[] PIN_MODES = {"MODE_INPUT_PULL_UP",
        "MODE_INPUT_PULL_DOWN",
        "MODE_OUTPUT_PUSH_PULL",
        "MODE_OUTPUT_OPEN_DRAIN"};
    private static final int[] PIN_MODES_INT = {1,
        2,
        4,
        8};
    private static final String[] PIN_DIRECTIONS = {"DIR_INPUT_ONLY",
        "DIR_OUTPUT_ONLY",
        "DIR_BOTH_INIT_INPUT",
        "DIR_BOTH_INIT_OUTPUT"};
    private static final String[] PIN_DIRECTIONS_INT = {"0",
        "1",
        "2",
        "3"};
    private static final String[] PIN_TRIGGERS = {"TRIGGER_BOTH_EDGES",
        "TRIGGER_RISING_EDGE",
        "TRIGGER_FALLING_EDGE",
        "TRIGGER_BOTH_LEVELS",
        "TRIGGER_HIGH_LEVEL",
        "TRIGGER_LOW_LEVEL",
        "TRIGGER_NONE"};

    private static final String[] PIN_TRIGGERS_INT = {"3",
        "2",
        "1",
        "6",
        "4",
        "5",
        "0"};

    protected String KEY_GPINS = "gpin.interface";
    protected String KEY_GPORTS = "gport.interface";

    protected GPIOInterview(Interview parent, String baseTag) {
        super(parent, baseTag);
        basicInterview = (BasicInterview) parent;
        init();
    }

    private void init() {
        setFirstQuestion(qWelcome);
    }

    private NullQuestion qWelcome = new NullQuestion(this, "welcome") {

        @Override
        public boolean isEnabled() {
            return basicInterview.isPeripheralInterfaceSupported(BasicInterview.PI_GPIO);
        }

        @Override
        public Question getNext() {
            if (isEnabled()) {
                return qPinConfig;
            } else {
                return qEnd;
            }
        }
    };

    private ListQuestion qPinConfig = new PinConfigQuestion(this, "gpioPinConfig");

    private class PinConfigQuestion extends ListQuestion {

        public PinConfigQuestion(Interview i, String s) {
            super(i, s);
        }

        @Override
        public Question getNext() {
            return qPortConfig;
        }

        @Override
        public Object[] getEndTextArgs() {
            return null;
        }

        @Override
        public Body createBody(int index) {
            return new PinConfigBody(this, index);
        }

        @Override
        public void export(Map data) {
            ListQuestion.Body[] bodies = getBodies();

            GpioPinCollection pinsCollection = new GpioPinCollection();
            for (ListQuestion.Body b : bodies) {

                PinConfigBody pinConfigBody = ((PinConfigBody) b);

                GpioData pin = new GpioData();

                pin.deviceId = basicInterview.getString(pinConfigBody.getId());
                pin.deviceNumber = basicInterview.getString(pinConfigBody.getDeviceNumber());
                pin.pinNumber = basicInterview.getString(pinConfigBody.getPinNumber());
                pin.mode = basicInterview.getString(pinConfigBody.getMode());
                pin.direction = basicInterview.getString(pinConfigBody.getDirection());
                pin.trigger = basicInterview.getString(pinConfigBody.getTriggers());
                pin.initValue = basicInterview.getString(pinConfigBody.getInitValue());

                if(pinsCollection.pins == null){
                    pinsCollection.pins = new ArrayList<>();
                }

                pinsCollection.pins.add(pin);
            }

            if (bodies.length == 0) {
                data.put(basicInterview.KEY_GPINS, "false");
            } else {
                data.put(basicInterview.KEY_GPINS, "true");
            }

            data.put(KEY_GPINS, pinsCollection.serialize());
        }

    }

    private class PinConfigBody extends ListQuestion.Body {

        Interview iparent = null;
        int index = 0;
        String id = "";
        String directions = "";
        String strigger = "";
        String ports = "";
        String pins = "";
        String initValues = "";
        boolean[] mode;

        private final NullQuestion qBodyStart = new NullQuestion(this, "pinConfigStart") {

            @Override
            protected Question getNext() {
                if (basicInterview.isTopologyOpen()) {
                    return qPinDeviceConfig;
                } else {
                    return qPinId;
                }
            }
        };
        protected ChoiceQuestion qPinDeviceConfig;
        protected StringQuestion qPinDeviceNumber;
        protected StringQuestion qPinNumber;
        protected ChoiceArrayQuestion qPinMode;
        protected StringQuestion qPinInitValue;
        protected StringQuestion qPinId;
        protected ChoiceQuestion qPinDirection;
        protected ChoiceQuestion qPinTrigger;

        PinConfigBody(ListQuestion q, int index) {
            super(q, index);
            this.index = index;
            //this.iparent = iparent;

            init();

        }

        private void init() {

            setFirstQuestion(qBodyStart);

            //qPinDeviceConfig = new ChoiceQuestion(this, "gpioPinDeviceConfig", TOPO_CHOICE) {
            qPinDeviceConfig = new ChoiceQuestion(this, "gpioPinDeviceConfig", new String[]{"PeripheralConfig"}) {

                @Override
                protected Question getNext() {
                    if (getStringValue() != null
                            && getStringValue().contains("ID")) {
                        return qPinId;
                    }

                    return qPinNumber;
                }
            };

            qPinDeviceNumber = new StringQuestion(this, "gpioPinDeviceNumber") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    ports = getStringValue();

                    return qPinNumber;
                }
            };
            qPinNumber = new StringQuestion(this, "gpioPinNumber") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    pins = getStringValue();

                    return qPinMode;
                }
            };
            qPinMode = new ChoiceArrayQuestion(this, "gpioPinMode", PIN_MODES) {

                @Override
                protected Question getNext() {

                    mode = getValue();

                    return qPinInitValue;
                }
            };
            qPinInitValue = new StringQuestion(this, "gpioPinInitValue") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    initValues = getStringValue();

                    return qPinDirection;
                }
            };
            qPinId = new StringQuestion(this, "gpioPinId") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    id = getStringValue();

                    return qPinDirection;
                }
            };
            qPinDirection = new ChoiceQuestion(this, "gpioPinDirection", PIN_DIRECTIONS_INT) {

                @Override
                protected void setChoices(String[] choices, String[] displayChoices) {
                    super.setChoices(choices, PIN_DIRECTIONS);
                }

                @Override
                protected Question getNext() {

                    directions = getStringValue();

                    return qPinTrigger;
                }
            };
            qPinTrigger = new ChoiceQuestion(this, "gpioPinTrigger", PIN_TRIGGERS_INT) {

                @Override
                protected void setChoices(String[] choices, String[] displayChoices) {
                    super.setChoices(choices, PIN_TRIGGERS);
                }

                @Override
                protected Question getNext() {

                    strigger = getStringValue();

                    return qListEnd;
                }
            };
        }

        @Override
        public String getSummary() {
            return String.valueOf(index);
        }

        String getName() {
            return "name";
        }

        public String getId() {
            return id;
        }

        public String getDirection() {
            return directions;
        }

        public String getTrigger() {
            return "2";
        }

        public String getTriggers() {
            return strigger;
        }

        public String getDeviceNumber() {
            return ports;
        }

        public String getPinNumber() {
            return pins;
        }

        public String getMode() {
            if (mode == null) {
                return null;
            }

            int m = 0;
            for (int i = 0; i < mode.length; i++) {
                if (mode[i]) {
                    m += PIN_MODES_INT[i];
                }
            }

            return Integer.toString(m);
        }

        public ArrayList getModes() {
            if (mode == null) {
                return null;
            }
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0; i < mode.length; i++) {
                if (mode[i]) {
                    list.add(PIN_MODES[i]);
                }
            }

            if (list.isEmpty()) {
                return null;
            }
            return list;
        }

        public String getInitValue() {
            return initValues;
        }

        private final FinalQuestion qListEnd = new FinalQuestion(this);
    }

    private final ListQuestion qPortConfig = new ListQuestion(this, "gpioPortConfig") {
        @Override
        public Question getNext() {
            return qEnd;
        }

        @Override
        public Body createBody(int index) {
            return new PortConfigBody(this, index);
        }

        @Override
        public void export(Map data) {
            ListQuestion.Body[] bodies = getBodies();

            GpioPinCollection pinsCollection = new GpioPinCollection();
            for (ListQuestion.Body b : bodies) {

                PortConfigBody portConfigBody = ((PortConfigBody) b);

                GpioData port = new GpioData();
                port.deviceId = basicInterview.getString(portConfigBody.getId());
                port.direction = basicInterview.getString(portConfigBody.getDirection());
                port.initValue = basicInterview.getString(portConfigBody.getInitValue());
                port.portPins = portConfigBody.getPortPins();

                if(pinsCollection.ports == null){
                    pinsCollection.ports = new ArrayList<>();
                }

                pinsCollection.ports.add(port);
            }

            if (bodies.length == 0) {
                data.put(basicInterview.KEY_GPORTS, "false");
            } else {
                data.put(basicInterview.KEY_GPORTS, "true");
            }

            data.put(KEY_GPORTS, pinsCollection.serialize());
        }
    };

    private class PortConfigBody extends ListQuestion.Body {

        String id = "";
        String directions = "";
        String initValue = "";
        ArrayList<String> portPins = null;
        int index = 0;

        private final NullQuestion qBodyStart = new NullQuestion(this, "portConfigStart") {

            @Override
            protected Question getNext() {
                if (basicInterview.isTopologyOpen()) {
                    return qPortDeviceConfig;
                } else {
                    return qPortId;
                }
            }
        };
        private ChoiceQuestion qPortDeviceConfig;
        private StringQuestion qPortId;
        private StringQuestion qPortInitValue;
        private ChoiceQuestion qPortDirection;
        private StringQuestion qPortPins;

        PortConfigBody(ListQuestion q, int index) {
            super(q, index);

            this.index = index;
            init();
        }

        private void init() {

            setFirstQuestion(qBodyStart);

            //qBodyStart.setEnabled(false);
            //qPortDeviceConfig = new ChoiceQuestion(this, "gpioPortDeviceConfig", TOPO_CHOICE) {
            qPortDeviceConfig = new ChoiceQuestion(this, "gpioPortDeviceConfig", new String[]{"PeripheralConfig"}) {

                @Override
                protected Question getNext() {
                    if (getStringValue() != null
                            && getStringValue().contains("ID")) {
                        return qPortId;
                    } else {
                        return qPortDirection;
                    }
                }
            };

            qPortDirection = new ChoiceQuestion(this, "gpioPortDirection", PIN_DIRECTIONS_INT) {

                @Override
                protected void setChoices(String[] choices, String[] displayChoices) {
                    super.setChoices(choices, PIN_DIRECTIONS);
                }

                @Override
                protected Question getNext() {
                    directions = getStringValue();
                    if (basicInterview.isTopologyOpen() && (qPortDeviceConfig.getStringValue() != null
                            && qPortDeviceConfig.getStringValue().contains("ID"))) {
                        return qListEnd;
                    } else {
                        return qPortInitValue;
                    }
                }
            };

            qPortId = new StringQuestion(this, "gpioPortId") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    id = getStringValue();

                    return qPortDirection;
                }
            };

            qPortInitValue = new StringQuestion(this, "gpioPortInitValue") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    initValue = getStringValue();

                    return qPortPins;
                }
            };

            qPortPins = new StringQuestion(this, "gpioPortPins") {

                @Override
                protected Question getNext() {
                    if (basicInterview.isEmpty(getStringValue())) {
                        return basicInterview.qInvalid;
                    }

                    String v = getStringValue();
                    if (v != null) {
                        portPins = new ArrayList<>(Arrays.asList(v.split(",")));
                    }

                    return qListEnd;
                }
            };

        }

        @Override
        public String getSummary() {
            return String.valueOf(index);
        }

        public String getId() {
            return id;
        }

        public String getDirection() {
            return directions;
        }

        public String getInitValue() {
            return initValue;
        }

        public ArrayList<String> getPortPins() {
            return portPins;
        }

        private final FinalQuestion qListEnd = new FinalQuestion(this);
    }

    private Question qEnd = new FinalQuestion(this);

}