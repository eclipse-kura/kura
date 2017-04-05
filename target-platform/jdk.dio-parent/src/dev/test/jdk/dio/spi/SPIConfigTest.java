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
package dio.spi;

import com.sun.javatest.Status;
import com.sun.javatest.Test;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Policy;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jdk.dio.DeviceConfig;
import jdk.dio.DeviceDescriptor;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceMgmtPermission;
import jdk.dio.spibus.SPIDevice;
import jdk.dio.spibus.SPIDeviceConfig;
import jdk.dio.spibus.SPIPermission;

/**
 *
 * @test
 * @sources SPIConfigTest.java
 * @executeClass dio.spi.SPIConfigTest
 *
 * @title SPI configuration testing
 *
 * @author stanislav.smirnov@oracle.com
 */
public class SPIConfigTest extends SPITestBase implements Test {

    /**
     * Standard command-line entry point.
     *
     * @param args command line args (ignored)
     */
    public static void main(String[] args) {
        PrintWriter err = new PrintWriter(System.err, true);
        Test t = new SPIConfigTest();
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

        Policy.setPolicy(new SPIPolicy(
                new DeviceMgmtPermission("*:*", "open"),
                new SPIPermission("*:*", "open")));

        List<Integer> result = new LinkedList<>();
        result.add(testSPIDesc().getType());

        result.add(testSPIDefault().getType());

        return (result.contains(Status.FAILED)
                ? printFailedStatus("some test cases failed") : Status.passed("OK"));
    }

    private Status testSPIDesc() {
        Status result = Status.passed("OK");
        start("Checking SPI device configs against declared configuration (DeviceDescriptor methods)");
        SPIConfig spiConfig;
        SPIDeviceConfig deviceConfig;
        DeviceDescriptor deviceDecriptor;
        SPIDeviceConfig deviceConfiguration;

        Iterator<SPIConfig> devicesList = SPIDevices.iterator();
        while (devicesList.hasNext()) {
            try {
                spiConfig = devicesList.next();
                deviceConfig = spiConfig.getDevCfg();
                try (SPIDevice spiDevice = DeviceManager.open(spiConfig.getID())) {
                    deviceDecriptor = (DeviceDescriptor) spiDevice.getDescriptor();
                    deviceConfiguration = (SPIDeviceConfig) deviceDecriptor.getConfiguration();
                    if (deviceConfiguration.getControllerNumber() == deviceConfig.getControllerNumber()) {
                        System.out.println("Device number is correct: " + deviceConfiguration.getControllerNumber());
                    } else {
                        result = printFailedStatus("Not expected Device number: " + deviceConfiguration.getControllerNumber());
                    }
                    if (deviceConfiguration.getControllerName() == null) {
                        if (deviceConfig.getControllerName() == null) {
                            System.out.println("Device name is null");
                        }
                    } else if (deviceConfiguration.getControllerName().equals(deviceConfig.getControllerName())) {
                        System.out.println("Device name is correct: " + deviceConfiguration.getControllerName());
                    } else {
                        result = printFailedStatus("Not expected Device name: " + deviceConfiguration.getControllerName());
                    }
                    if (deviceConfiguration.getAddress() == deviceConfig.getAddress()) {
                        System.out.println("Device address is correct: " + deviceConfiguration.getAddress());
                    } else {
                        result = printFailedStatus("Not expected Device address: " + deviceConfiguration.getAddress());
                    }
                    if (deviceConfiguration.getClockFrequency() == deviceConfig.getClockFrequency()) {
                        System.out.println("Device clock frequency is correct: " + deviceConfiguration.getClockFrequency());
                    } else {
                        result = printFailedStatus("Not expected Device clock frequency: " + deviceConfiguration.getClockFrequency());
                    }
                    if (deviceConfiguration.getClockMode() == deviceConfig.getClockMode()) {
                        System.out.println("Clock Mode is correct: " + printClockMode(deviceConfiguration.getClockMode()));
                    } else {
                        result = printFailedStatus("Not expected Clock Mode: " + printClockMode(deviceConfiguration.getClockMode()));
                    }
                    if (deviceConfiguration.getCSActiveLevel() == deviceConfig.getCSActiveLevel()) {
                        System.out.println("CS Active Level is correct: " + printCSActiveLevel(deviceConfiguration.getCSActiveLevel()));
                    } else {
                        result = printFailedStatus("Not expected CS Active Level: " + printCSActiveLevel(deviceConfiguration.getCSActiveLevel()));
                    }
                    if (deviceConfiguration.getBitOrdering() == deviceConfig.getBitOrdering()) {
                        System.out.println("Device bit ordering is correct: " + deviceConfiguration.getBitOrdering());
                    } else {
                        result = printFailedStatus("Not expected Device bit ordering: " + deviceConfiguration.getBitOrdering());
                    }
                    if (deviceConfiguration.getWordLength() == deviceConfig.getWordLength()) {
                        System.out.println("Device data word length is correct: " + deviceConfiguration.getWordLength());
                    } else {
                        result = printFailedStatus("Not expected Device data word length: " + deviceConfiguration.getWordLength());
                    }
                }
                System.out.println("SPI device was checked OK");
            } catch (IOException e) {
                result = printFailedStatus("Some I/O error occured " + e.getClass().getName() + ":" + e.getMessage());
            }
        }

        stop();
        return result;
    }

    private Status testSPIDefault() {
        Status result = Status.passed("OK");
        start("Creating SPI device configs using DEFAULT parameters where it is possible");
        SPIDevice spiDevice;
        SPIConfig spiConfig;
        SPIDeviceConfig deviceConfig;
        SPIDeviceConfig defaultConfig;

        Iterator<SPIConfig> devicesList = SPIDevices.iterator();
        while (devicesList.hasNext()) {
            try {
                spiConfig = devicesList.next();

                deviceConfig = spiConfig.getDevCfg();
                defaultConfig = new SPIDeviceConfig(DeviceConfig.DEFAULT, deviceConfig.getAddress(), DeviceConfig.DEFAULT, deviceConfig.getClockMode(), DeviceConfig.DEFAULT, DeviceConfig.DEFAULT);
                spiDevice = DeviceManager.open(defaultConfig);
                spiDevice.close();
                System.out.println("New SPI devices was open/closed OK");
            } catch (IOException e) {
                result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
            }
        }

        stop();
        return result;
    }
}
