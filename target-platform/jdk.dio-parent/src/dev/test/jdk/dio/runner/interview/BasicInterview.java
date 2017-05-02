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

import com.sun.interview.ChoiceArrayQuestion;
import com.sun.interview.ChoiceQuestion;
import com.sun.interview.DirectoryFileFilter;
import com.sun.interview.ErrorQuestion;
import com.sun.interview.ExtensionFileFilter;
import com.sun.interview.FileFilter;
import com.sun.interview.FileListQuestion;
import com.sun.interview.FileQuestion;
import com.sun.interview.NullQuestion;
import com.sun.interview.Question;
import com.sun.interview.StringQuestion;
import com.sun.javatest.Parameters.EnvParameters;
import com.sun.javatest.TestEnvironment;
import com.sun.javatest.interview.BasicInterviewParameters;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @title JT Harness interview
 * @author stanislav.smirnov@oracle.com
 */
public class BasicInterview
        extends BasicInterviewParameters
        implements EnvParameters {

    private GPIOInterview gpioInterview;
    private SPIInterview spiInterview;
    private UARTInterview uartInterview;

    public String ARG_GPINS = " -gpins ";
    public String ARG_GPORTS = " -gports ";
    public String ARG_SPI = " -spi ";
    public String ARG_UART = " -uart ";

    public String KEY_GPINS = "gpio.pins.enabled";
    public String KEY_GPORTS = "gpio.ports.enabled";
    public String KEY_SPI = "spi.enabled";
    public String KEY_UART = "uart.enabled";

    public BasicInterview() throws Fault {
        super("dio");
        init();
    }

    private void init() throws Fault {
        setResourceBundle("i18n");
        ResourceBundle rb = getResourceBundle();

        if (rb != null) {
            setTitle(rb.getString("main.interview.title"));
        }

        gpioInterview = new GPIOInterview(this, "gpioConfig");
        spiInterview = new SPIInterview(this, "spiConfig");
        uartInterview = new UARTInterview(this, "uartConfig");
    }

    protected Question qInvalid = new ErrorQuestion(this, "invalidResponse");
    protected Question qInvalidInt = new ErrorQuestion(this, "invalidIntResponse");

    @Override
    public TestEnvironment getEnv() {
        HashMap envProps = new HashMap();
        export(envProps);

        String name = qName.getStringValue();
        if (name == null || name.length() == 0)
            name = getResourceBundle().getString("main.error.noName");
        String desc = getResourceBundle().getString("main.interview.name");

        try {
            return new TestEnvironment(name, envProps, desc);
        } catch (TestEnvironment.Fault e) {
            throw new Error("Unexpected problem while creating DIO testsuite environment: " + e);
        }
    }

    @Override
    public EnvParameters getEnvParameters() {
        return this;
    }

    @Override
    public Question getEnvFirstQuestion() {
        return qName;
    }

    //----------------------------------------------------------------------
    //
    // Give a name for this configuration
    private StringQuestion qName = new StringQuestion(this, "envName") {

        @Override
        public Question getNext() {
            if (value == null || value.length() == 0) {
                return null;
            } else {
                return qDesc;
            }
        }
    };

    //----------------------------------------------------------------------
    //
    // Give a description for this configuration
    private Question qDesc = new StringQuestion(this, "envDesc") {
        @Override
        public Question getNext() {
            if (value == null || value.length() == 0) {
                return null;
            } else {
                return qJVM;
            }
        }

        @Override
        public void export(Map data) {
            data.put("description", String.valueOf(value));
        }
    };

    //----------------------------------------------------------------------
    //
    // How do you with to execute tests:
    //   OTHER_VM:  on the same system as JT Harness, in separate process
    //   AGENT:   on a different system, using JT Harness Agent
    private static final String AGENT = "agent";
    private static final String OTHER_VM = "otherVM";

    private Question qCmdType = new ChoiceQuestion(this, "cmdType") {
        {
            setChoices(new String[]{null, OTHER_VM, AGENT}, true);
        }

        @Override
        public Question getNext() {
            if (value == null || value.length() == 0) {
                return null;
            } else {
                return qJavaLibraryPath;
            }
        }

        @Override
        public void export(Map data) {
            StringBuilder cmd = new StringBuilder();
            if (value.equals(AGENT)) {
                cmd = getAgentCommand();
            }

            cmd.append(" com.sun.javatest.lib.ExecStdTestOtherJVMCmd ");

            File jvm = qJVM.getValue();
            cmd.append(jvm == null ? " unknown_jvm " : jvm.getPath());

            cmd.append(" -Djava.library.path=").append(qJavaLibraryPath.getStringValue());

            cmd.append(" -Djava.security.policy=").append(qJavaPolicyFile.getStringValue());

            cmd.append(" -Djdk.dio.registry=").append(qDioPropertiesFile.getStringValue());

            cmd.append(" ").append(qVmOptions.getStringValue());

            cmd.append(" -classpath ").append(qClassPath.getStringValue());

            cmd.append(" $testExecuteClass $testExecuteArgs");

            if ("true".equals(data.get(KEY_GPINS))) {
                cmd.append(ARG_GPINS).append(data.get(gpioInterview.KEY_GPINS));
            }

            if ("true".equals(data.get(KEY_GPORTS))) {
                cmd.append(ARG_GPORTS).append(data.get(gpioInterview.KEY_GPORTS));
            }

            if("true".equals(data.get(KEY_SPI))){
                cmd.append(ARG_SPI).append(data.get(spiInterview.KEY_SPI));
            }

            if("true".equals(data.get(KEY_UART))){
                cmd.append(ARG_UART).append(data.get(uartInterview.KEY_UART));
            }

            data.put("command.execute", cmd.toString());
        }
    };

    private StringBuilder getAgentCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append(" com.sun.javatest.agent.ActiveAgentCommand ");
        sb.append(" -mapArgs ");

        sb.append(" -classpath ").append(qClassPath.getStringValue());

        return sb;
    }

    //----------------------------------------------------------------------
    //
    // What is the path for the JVM you wish to use to execute the tests?
    private FileQuestion qJVM = new FileQuestion(this, "jvm") {
        @Override
        public Question getNext() {
            if (value == null || value.getPath().length() == 0) {
                return null;
            } else if (!(value.exists() && value.isFile() && value.canRead())) {
                return qInvalid;
            } else {
                return peripheralTopology;
            }
        }
    };

    public static final String PI_GPIO = "gpio";
    public static final String PI_SPIB = "spibus";
    public static final String PI_UART = "uart";

    public static final String[] PI_CHOICES = new String[]{
        PI_GPIO,
        PI_SPIB,
        PI_UART
    };

    /*public static String[] TOPOLOGIES = {"Closed Peripheral Device Topology",
        "Open Peripheral Device Topology"};*/
    public static String[] TOPOLOGIES = {"Open Peripheral Device Topology"};

    public boolean isPeripheralInterfaceSupported(String piName) {
        boolean bRet = false;
        String value = peripheralInterfaces.getStringValue();
        if (value.contains(piName)) {
            bRet = true;
        }
        return bRet;
    }

    private boolean topologyOpen = false;

    public boolean isTopologyOpen() {
        return topologyOpen;
    }

    private ChoiceQuestion peripheralTopology = new ChoiceQuestion(this, "peripheralTopology") {
        {
            setChoices(TOPOLOGIES, true);
        }

        @Override
        public void export(Map data) {
            if (topologyOpen) {
                data.put("device.topology", "open");
            } else {
                data.put("device.topology", "closed");
            }
        }

        @Override
        public void setValue(String value) {
            super.setValue(value);

            if (getStringValue().contains("Open")) {
                topologyOpen = true;
            } else {
                topologyOpen = false;
            }
            updatePath();
        }

        @Override
        public Question getNext() {
            return peripheralInterfaces;
        }
    };

    private ChoiceArrayQuestion peripheralInterfaces = new ChoiceArrayQuestion(this, "peripheralInterfaces") {
        {
            setChoices(PI_CHOICES, true);
        }

        @Override
        public Question getNext() {
            if (getStringValue().length() > 0) {
                return callInterview(gpioInterview, qGPIOInterviewEnd);
            }

            return qInvalid;
        }

        @Override
        public void export(Map data) {
            String choices = getStringValue();
            String packages = "";
            for (String PI_CHOICE : PI_CHOICES) {
                if (choices.contains(PI_CHOICE)) {
                    data.put(PI_CHOICE, "true");
                    packages += "jdk.dio." + PI_CHOICE + " ";
                } else {
                    data.put(PI_CHOICE, "false");
                }
            }
            data.put("packages", packages);

        }
    };

    private NullQuestion qGPIOInterviewEnd = new NullQuestion(this, "gpioInterviewEnd") {

        @Override
        public Question getNext() {
            return callInterview(spiInterview, qSPIInterviewEnd);
        }
    };

    private NullQuestion qSPIInterviewEnd = new NullQuestion(this, "spiInterviewEnd") {

        @Override
        public Question getNext() {
            return callInterview(uartInterview, qUARTInterviewEnd);
        }
    };

    private NullQuestion qUARTInterviewEnd = new NullQuestion(this, "uartInterviewEnd") {

        @Override
        public Question getNext() {
            return qCmdType;
        }
    };

    //----------------------------------------------------------------------
    //
    //Specify java library path to use in tests
    //Setting it to FileQuestion not StringQuestion, because otherwise slashes
    //are incorrect, needs to be fixed
    private StringQuestion qJavaLibraryPath = new StringQuestion(this, "javaLibraryPath") {

        @Override
        public Question getNext() {
            if (isEmpty(getStringValue())) {
                return qInvalid;
            }

            return qJavaPolicyFile;
        }

        @Override
        public void export(Map data) {
            data.put("javaLibraryPath", String.valueOf(value));
        }
    };

    //----------------------------------------------------------------------
    //
    // Specify java policy file to use
    private FileQuestion qJavaPolicyFile = new FileQuestion(this, "javaPolicyFile") {

        @Override
        public Question getNext() {
            if (isEmpty(getStringValue())) {
                return qInvalid;
            }

            return qDioPropertiesFile;
        }

        @Override
        public void export(Map data) {
            data.put("javaPolicyFile", String.valueOf(value));
        }
    };

    //----------------------------------------------------------------------
    //
    // Specify dio properties file to use
    private FileQuestion qDioPropertiesFile = new FileQuestion(this, "dioPropertiesFile") {

        @Override
        public Question getNext() {
            if (isEmpty(getStringValue())) {
                return qInvalid;
            }

            return qVmOptions;
        }

        @Override
        public void export(Map data) {
            data.put("dioPropertiesFile", String.valueOf(value));
        }
    };

    //----------------------------------------------------------------------
    //
    // Specify arguments to pass to the virtual machine
    private FileQuestion qVmOptions = new FileQuestion(this, "vmOptions") {

        @Override
        public Question getNext() {
            return qClassPath;
        }

        @Override
        public void export(Map data) {
            data.put("vmOptions", String.valueOf(value));
        }
    };

    //----------------------------------------------------------------------
    //
    // Specify classpath to use
    private FileListQuestion qClassPath = new FileListQuestion(this, "classPath") {
        {
            FileFilter[] filters = {
                new DirectoryFileFilter("Directories"),
                new ExtensionFileFilter(".zip", "ZIP Files"),
                new ExtensionFileFilter(".jar", "JAR Files"),};
            setFilters(filters);
        }

        @Override
        public Question getNext() {
            return qEnvEnd;
        }

    };

    //----------------------------------------------------------------------
    private Question qEnvEnd = new NullQuestion(this, "envEnd") {
        @Override
        public Question getNext() {
            return getEnvSuccessorQuestion();
        }
    };

    protected boolean isEmpty(String inputStr) {
        return inputStr == null || inputStr.isEmpty();
    }

    protected int getString(String string){
        return (string == null || string.isEmpty()) ? -1 : Integer.parseInt(string);
    }
}