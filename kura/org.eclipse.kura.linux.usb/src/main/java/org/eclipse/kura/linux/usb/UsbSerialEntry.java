/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.usb;

public class UsbSerialEntry {

    private int ttyUsbPortNo = 0;
    private String vendorID = null;
    private String productID = null;
    private int numberOfPorts = 0;
    private int portEnumeration = 0;
    private String path2usbDevice = null;

    /**
     * UsbSerialEntry constructor
     *
     * @param ttyUsbPortNo
     *            - ttyUSB port number
     * @param vendor
     *            - vendor ID
     * @param product
     *            - product ID
     * @param portEnum
     *            - port enumeration
     * @param path
     *            - path to USB device
     */
    public UsbSerialEntry(int ttyUsbPortNo, String vendor, String product, int numPorts, int portEnum, String path) {

        this.ttyUsbPortNo = ttyUsbPortNo;
        this.vendorID = vendor;
        this.productID = product;
        this.numberOfPorts = numPorts;
        this.portEnumeration = portEnum;
        this.path2usbDevice = path;
    }

    /**
     * Reports ttyUSB port number
     *
     * @return ttyUSB port number as <code>int</code>
     */
    public int getTtyUsbPortNo() {
        return this.ttyUsbPortNo;
    }

    /**
     * Reports vendor ID
     *
     * @return vendor ID as <code>String</code>
     */
    public String getVendorID() {
        return this.vendorID;
    }

    /**
     * Reports product ID
     *
     * @return product ID as <code>String</code>
     */
    public String getProductID() {
        return this.productID;
    }

    /**
     * Reports total number of ports.
     *
     * @return number of ports as <code>int</code>
     */
    public int getNumberOfPorts() {
        return this.numberOfPorts;
    }

    /**
     * Reports port enumeration
     *
     * @return port enumeration as <code>int</code>
     */
    public int getPortEnumeration() {
        return this.portEnumeration;
    }

    /**
     * Reports path to USB device
     *
     * @return path to USB device as <code>String</code>
     */
    public String getPath2usbDevice() {
        return this.path2usbDevice;
    }
}
