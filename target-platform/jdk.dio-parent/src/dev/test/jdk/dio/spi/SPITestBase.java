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

import dio.runner.gson.SPIDeviceCollection;
import dio.shared.TestBase;
import java.util.ArrayList;
import jdk.dio.DeviceConfig;
import jdk.dio.spibus.SPIDeviceConfig;

/**
 *
 * @author stanislav.smirnov@oracle.com
 */
public class SPITestBase implements TestBase {

    protected int validIntWrite;
    protected byte[] validArrayWrite;
    protected int validIntRead;
    protected byte[] validArrayRead;
    protected byte[] validArrayWriteRead;

    protected ArrayList<SPIConfig> SPIDevices;

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
            int index = getDataIndex(args, "-spi");
            if (index == -1) {
                result = false;
                System.err.println("Wrong input spi argument");
            } else {
                setupSpiConfig(args[index + 1]);
            }
        }
        return result;
    }

    /**
     * Method to setup configuration
     * @param config input configuration wrapped in Json
     */
    protected void setupSpiConfig(String config) {
        SPIDeviceCollection spiDevicesCollection = SPIDeviceCollection.deserialize(config, SPIDeviceCollection.class);
        spiDevicesCollection.spiDevices.stream().map((spiDevice) -> {
            if(SPIDevices == null){
                SPIDevices = new ArrayList<>();
            }
            return spiDevice;
        }).forEach((spiDevice) -> {
            SPIDevices.add(new SPIConfig(spiDevice.id, spiDevice.deviceName,
                    new SPIDeviceConfig(
                            spiDevice.deviceNumber,
                            spiDevice.address, spiDevice.chipSelectActive,
                            spiDevice.clockFrequency, spiDevice.clockMode,
                            spiDevice.wordLength, spiDevice.bitOrdering))
            );
        });

        validArrayWrite = new byte[]{33, 35, 52, 97, 78, 34, 39};
        validArrayRead = new byte[]{0, 0, 0, 0, 0, 0, 0};
        validArrayWriteRead = new byte[]{33, 35, 52, 97, 78, 34, 39};

        SPIDevices.trimToSize();
    }

    protected String printClockMode(int mode) {
        String text;
        switch (mode) {
            case 0:
                text = "0 = Active-high clocks selected / 0 = Sampling of data occurs at odd edges of the SCK clock";
                break;
            case 1:
                text = "0 = Active-high clocks selected / 1 = Sampling of data occurs at even edges of the SCK clock";
                break;
            case 2:
                text = "1 = Active-low clocks selected / 0 = Sampling of data occurs at odd edges of the SCK clock";
                break;
            case 3:
                text = "1 = Active-low clocks selected / 1 = Sampling of data occurs at even edges of the SCK clock";
                break;
            default:
                text = "! UNKNOWN !";
        }
        return text;
    }

    protected String printCSActiveLevel(int level) {
        String text;
        switch (level) {
            case DeviceConfig.DEFAULT:
                text = "DeviceConfig.DEFAULT";
                break;
            case SPIDeviceConfig.CS_ACTIVE_LOW:
                text = "CS_ACTIVE_LOW";
                break;
            case SPIDeviceConfig.CS_ACTIVE_HIGH:
                text = "CS_ACTIVE_HIGH";
                break;
            case SPIDeviceConfig.CS_NOT_CONTROLLED:
                text = "CS_NOT_CONTROLLED";
                break;
            default:
                text = "! UNKNOWN !";
        }
        return text;
    }
}
