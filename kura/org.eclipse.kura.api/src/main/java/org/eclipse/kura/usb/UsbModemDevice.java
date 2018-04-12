/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.usb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.kura.net.modem.ModemDevice;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Representation of USB modem devices
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class UsbModemDevice extends AbstractUsbDevice implements ModemDevice {

    /** The TTY devices associated with modem **/
    private final ArrayList<TtyDev> ttyDevs;

    /** The block devices associated with the modem **/
    private final ArrayList<String> blockDevs;

    public UsbModemDevice(String vendorId, String productId, String manufacturerName, String productName,
            String usbBusNumber, String usbDevicePath) {
        super(vendorId, productId, manufacturerName, productName, usbBusNumber, usbDevicePath);

        this.ttyDevs = new ArrayList<>();
        this.blockDevs = new ArrayList<>();
    }

    public UsbModemDevice(AbstractUsbDevice usbDevice) {
        super(usbDevice);

        this.ttyDevs = new ArrayList<>();
        this.blockDevs = new ArrayList<>();
    }

    @Override
    public List<String> getSerialPorts() {
        List<String> serialPorts = new ArrayList<>();
        for (TtyDev dev : this.ttyDevs) {
            serialPorts.add(dev.getPortName());
        }
        return serialPorts;
    }

    /**
     * Return a list of tty devices, sorted in a way to facilitate the client code identifying dedicated devices,
     * e.g. for AT commands, PPP link or NMEA sentences, based on their position in the list.
     * Originally, only the tty name was used for the comparison. This proved to be wrong as a tty name does reliably
     * identify the USB interface number (bInterfaceNumber) of the tty device.
     * To preserve the API contract, the tty devices can be added specifying the USB interface number and this will be
     * used to sort the list.
     *
     * @return sorted list of tty devices
     */
    public List<String> getTtyDevs() {
        return getSerialPorts();
    }

    /**
     * @return sorted list of block devices
     */
    public List<String> getBlockDevs() {
        return Collections.unmodifiableList(this.blockDevs);
    }

    /**
     * Adds a tty device identified by its name and USB interface number (bInterfaceNumber).
     * The devices will be sorted by the interface number. If this is missing, the name will be used.
     *
     * @since 1.4
     *
     * @param ttyDev
     *            the name of the tty device
     * @param interfaceNumber
     *            the number of the interface as described by the bInterfaceNumber property
     */
    public void addTtyDev(String ttyDev, Integer interfaceNumber) {
        TtyDev dev = new TtyDev(ttyDev, interfaceNumber);
        if (!this.ttyDevs.contains(dev)) {
            this.ttyDevs.add(new TtyDev(ttyDev, interfaceNumber));
            Collections.sort(this.ttyDevs, new TtyDevComparator());
        }
    }

    /**
     * @deprecated this method is deprecated in favor of addTtyDev(String ttyDev, Integer interfaceNumber)
     */
    @Deprecated
    public void addTtyDev(String ttyDev) {
        addTtyDev(ttyDev, null);
    }

    /**
     * Adds a block device identified by its name. The block devices will be sorted by the name.
     *
     * @param blockDev
     *            the name of the block device
     */
    public void addBlockDev(String blockDev) {
        if (!this.blockDevs.contains(blockDev)) {
            this.blockDevs.add(blockDev);
            Collections.sort(this.blockDevs, new DevNameComparator());
        }
    }

    /**
     * Remove a tty device form the list.
     *
     * @param ttyDev
     *            the name of the tty device
     * @return true if the list contained the specified device
     */
    public boolean removeTtyDev(String ttyDev) {
        return this.ttyDevs.remove(new TtyDev(ttyDev));
    }

    /**
     * Remove a block device form the list.
     *
     * @param blockDev
     *            the name of the block device
     * @return true if the list contained the specified device
     */
    public boolean removeBlockDev(String blockDev) {
        return this.blockDevs.remove(blockDev);
    }

    @Override
    public int hashCode() {
        final int prime = 37;
        int result = super.hashCode();
        result = prime * result + (this.ttyDevs == null ? 0 : this.ttyDevs.hashCode());
        result = prime * result + (this.blockDevs == null ? 0 : this.blockDevs.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UsbModemDevice other = (UsbModemDevice) obj;
        if (this.ttyDevs == null) {
            if (other.ttyDevs != null) {
                return false;
            }
        } else if (!this.ttyDevs.equals(other.ttyDevs)) {
            return false;
        }
        if (this.blockDevs == null) {
            if (other.blockDevs != null) {
                return false;
            }
        } else if (!this.blockDevs.equals(other.blockDevs)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UsbModem [");
        sb.append("vendorId=").append(getVendorId());
        sb.append(", productId=").append(getProductId());
        sb.append(", manufName=").append(getManufacturerName());
        sb.append(", productName=").append(getProductName());
        sb.append(", usbPort=").append(getUsbPort());
        sb.append(", ttyDevs=").append(this.ttyDevs.toString());
        sb.append(", blockDevs=").append(this.blockDevs.toString());
        sb.append("]");

        return sb.toString();
    }

    private class DevNameComparator implements Comparator<String> {

        @Override
        /**
         * Split the device name into the digit and non-digit portions and compare separately
         * i.e. for "/dev/ttyUSB9" and "/dev/ttyUSB10", the "/dev/ttyUSB" parts are first compared
         * then the "9" and "10" are compared numerically.
         */
        public int compare(String dev1, String dev2) {
            int digitPos1 = getDigitPosition(dev1);
            int digitPos2 = getDigitPosition(dev2);

            String text1 = dev1.substring(0, digitPos1);
            String text2 = dev2.substring(0, digitPos2);

            String num1 = dev1.substring(digitPos1, dev1.length());
            String num2 = dev2.substring(digitPos2, dev2.length());

            // Compare text portion
            int textCompare = text1.compareTo(text2);
            if (textCompare != 0) {
                return textCompare;
            }

            // Compare numerical portion
            if (num1 == null || num1.isEmpty()) {
                if (num2 == null || num2.isEmpty()) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (num2 == null || num2.isEmpty()) {
                return 1;
            }

            Integer int1 = Integer.parseInt(num1);
            Integer int2 = Integer.parseInt(num2);
            return int1.compareTo(int2);
        }

        private int getDigitPosition(String devName) {
            int pos = devName.length();

            for (int i = devName.length() - 1; i >= 0; i--) {
                if (Character.isDigit(devName.charAt(i))) {
                    pos = i;
                } else {
                    break;
                }
            }

            return pos;
        }
    }

    private static class TtyDev {

        private final String portName;
        private Integer interfaceNumber;

        public TtyDev(String portName) {
            this.portName = portName;
        }

        public TtyDev(String portName, Integer interfaceNumber) {
            this.portName = portName;
            this.interfaceNumber = interfaceNumber;
        }

        public String getPortName() {
            return this.portName;
        }

        public Integer getInterfaceNumber() {
            return this.interfaceNumber;
        }

        @Override
        public String toString() {
            String number = this.interfaceNumber != null ? this.interfaceNumber.toString() : "null";
            return "TtyDev [portName=" + this.portName + ", interfaceNumber=" + number + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (this.portName == null ? 0 : this.portName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TtyDev other = (TtyDev) obj;
            if (this.portName == null) {
                if (other.portName != null) {
                    return false;
                }
            } else if (!this.portName.equals(other.portName)) {
                return false;
            }
            return true;
        }

    }

    private class TtyDevComparator implements Comparator<TtyDev> {

        @Override
        /**
         * If the devices have an interface number, use it for comparing.
         * Otherwise use the port names.
         * Note: this comparator imposes orderings that are inconsistent with equals. The comparison will be performed
         * on the interface numbers if present, while the equals method is based only on the port names.
         */
        public int compare(TtyDev dev1, TtyDev dev2) {
            if (dev1.getInterfaceNumber() != null && dev2.getInterfaceNumber() != null) {
                return dev1.getInterfaceNumber().compareTo(dev2.getInterfaceNumber());
            } else {
                return new DevNameComparator().compare(dev1.getPortName(), dev2.getPortName());
            }
        }
    }
}
