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

import com.sun.javatest.Status;
import com.sun.javatest.Test;
import static dio.shared.TestBase.STATUS_OK;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Policy;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jdk.dio.DeviceAlreadyExistsException;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceMgmtPermission;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.uart.UART;
import jdk.dio.uart.UARTConfig;
import jdk.dio.uart.UARTPermission;

/**
 *
 * @test
 * @sources UARTConfigureTest.java
 * @executeClass dio.uart.UARTConfigureTest
 *
 * @title UART new configuration testing
 *
 * @author stanislav.smirnov@oracle.com
 */
public class UARTConfigureTest extends UARTTestBase implements Test {

    /**
     * Standard command-line entry point.
     *
     * @param args command line args (ignored)
     */
    public static void main(String[] args) {
        PrintWriter err = new PrintWriter(System.err, true);
        Test t = new UARTConfigTest();
        Status s = t.run(args, null, err);
        s.exit();
    }

    /**
     * Main test method. The test consists of a series of test cases; the test
     * passes only if all the individual test cases pass.
     *
     * @param args ignored
     * @param out ignored
     * @param err a stream to which to write details about test failures
     * @return a Status object indicating if the test passed or failed
     */
    @Override
    public Status run(String[] args, PrintWriter out, PrintWriter err) {
        if(!decodeConfig(args)){
            return printFailedStatus("Error occured while decoding input arguments");
        }

        Policy.setPolicy(new UARTPolicy(
                new DeviceMgmtPermission("*:*", "open"),
                new UARTPermission("*:*", "open")));

        List<Integer> result = new LinkedList<>();

        result.add(testNewUARTBaudRate().getType());

        result.add(testNewUARTDataBits().getType());

        result.add(testNewUARTParity().getType());

        result.add(testNewUARTStopBits().getType());

        result.add(testNewUARTFlowControl().getType());

        return (result.contains(Status.FAILED)
                ? printFailedStatus("some test cases failed") : Status.passed("OK"));
    }

    /**
     * Testcase to create config and open device using all supported baud rates
     * @return Status passed/failed
     */
    private Status testNewUARTBaudRate() {
        Status result = Status.passed(STATUS_OK);
        start("Verify UART config against initial configuration");
        Config initialConfig;
        UARTConfig newConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            for(Integer param : getSupportedBaudRates()){
                newConfig = new UARTConfig(
                        devCfg.getControllerNumber(),
                        devCfg.getChannelNumber(),
                        param,
                        devCfg.getDataBits(),
                        devCfg.getParity(),
                        devCfg.getStopBits(),
                        devCfg.getFlowControlMode());
                try (UART device = (UART) DeviceManager.open(newConfig)) {
                    System.out.println("New UART configuration was open OK");
                } catch (InvalidDeviceConfigException e) {
                    result = printFailedStatus("UART configuration is not valid/supported.");
                } catch (DeviceAlreadyExistsException e) {
                    result = printFailedStatus("ID is already assigned to a Device device.");
                } catch (IOException e) {
                    result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
                }
                System.out.println("New UART configuration was closed OK");
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to create config and open device using all supported data bits
     * @return Status passed/failed
     */
    private Status testNewUARTDataBits() {
        Status result = Status.passed(STATUS_OK);
        start("Verify UART config against initial configuration");
        Config initialConfig;
        UARTConfig newConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            for(Integer param : getSupportedDataBits()){
                newConfig = new UARTConfig(
                        devCfg.getControllerNumber(),
                        devCfg.getChannelNumber(),
                        devCfg.getBaudRate(),
                        param,
                        devCfg.getParity(),
                        devCfg.getStopBits(),
                        devCfg.getFlowControlMode());
                try (UART device = (UART) DeviceManager.open(newConfig)) {
                    System.out.println("New UART configuration was open OK");
                } catch (InvalidDeviceConfigException e) {
                    result = printFailedStatus("UART configuration is not valid/supported.");
                } catch (DeviceAlreadyExistsException e) {
                    result = printFailedStatus("ID is already assigned to a Device device.");
                } catch (IOException e) {
                    result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
                }
                System.out.println("New UART configuration was closed OK");
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to create config and open device using all supported parities
     * @return Status passed/failed
     */
    private Status testNewUARTParity() {
        Status result = Status.passed(STATUS_OK);
        start("Verify UART config against initial configuration");
        Config initialConfig;
        UARTConfig newConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            for(Integer param : getSupportedParities()){
                newConfig = new UARTConfig(
                        devCfg.getControllerNumber(),
                        devCfg.getChannelNumber(),
                        devCfg.getBaudRate(),
                        devCfg.getDataBits(),
                        param,
                        devCfg.getStopBits(),
                        devCfg.getFlowControlMode());
                try (UART device = (UART) DeviceManager.open(newConfig)) {
                    System.out.println("New UART configuration was open OK");
                } catch (InvalidDeviceConfigException e) {
                    result = printFailedStatus("UART configuration is not valid/supported.");
                } catch (DeviceAlreadyExistsException e) {
                    result = printFailedStatus("ID is already assigned to a Device device.");
                } catch (IOException e) {
                    result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
                }
                System.out.println("New UART configuration was closed OK");
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to create config and open device using all stop bits
     * @return Status passed/failed
     */
    private Status testNewUARTStopBits() {
        Status result = Status.passed(STATUS_OK);
        start("Verify UART config against initial configuration");
        Config initialConfig;
        UARTConfig newConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            for(Integer param : getSupportedStopBits()){
                newConfig = new UARTConfig(
                        devCfg.getControllerNumber(),
                        devCfg.getChannelNumber(),
                        devCfg.getBaudRate(),
                        devCfg.getDataBits(),
                        devCfg.getParity(),
                        param,
                        devCfg.getFlowControlMode());
                try (UART device = (UART) DeviceManager.open(newConfig)) {
                    System.out.println("New UART configuration was open OK");
                } catch (InvalidDeviceConfigException e) {
                    result = printFailedStatus("UART configuration is not valid/supported.");
                } catch (DeviceAlreadyExistsException e) {
                    result = printFailedStatus("ID is already assigned to a Device device.");
                } catch (IOException e) {
                    result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
                }
                System.out.println("New UART configuration was closed OK");
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to create config and open device using all flow control modes
     * @return Status passed/failed
     */
    private Status testNewUARTFlowControl() {
        Status result = Status.passed(STATUS_OK);
        start("Verify UART config against initial configuration");
        Config initialConfig;
        UARTConfig newConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            for(Integer param : getSupportedFlowControlModes()){
                newConfig = new UARTConfig(
                        devCfg.getControllerNumber(),
                        devCfg.getChannelNumber(),
                        devCfg.getBaudRate(),
                        devCfg.getDataBits(),
                        devCfg.getParity(),
                        devCfg.getStopBits(),
                        param);
                try (UART device = (UART) DeviceManager.open(newConfig)) {
                    System.out.println("New UART configuration was open OK");
                } catch (InvalidDeviceConfigException e) {
                    result = printFailedStatus("UART configuration is not valid/supported.");
                } catch (DeviceAlreadyExistsException e) {
                    result = printFailedStatus("ID is already assigned to a Device device.");
                } catch (IOException e) {
                    result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
                }
                System.out.println("New UART configuration was closed OK");
            }
        }
        stop();
        return result;
    }
}
