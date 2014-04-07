/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.usb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.kura.net.modem.ModemDevice;

/**
 * Representation of USB modem devices
 */
public class UsbModemDevice extends AbstractUsbDevice implements ModemDevice {

	/** The TTY devices associated with modem **/
	private ArrayList<String> m_ttyDevs;
	
	/** The block devices associated with the modem **/
	private ArrayList<String> m_blockDevs;

	public UsbModemDevice(String vendorId, String productId, String manufacturerName, String productName, String usbBusNumber, String usbDevicePath) {
		super(vendorId, productId, manufacturerName, productName, usbBusNumber, usbDevicePath);
		
		m_ttyDevs = new ArrayList<String>();
		m_blockDevs = new ArrayList<String>();
	}
	
	public UsbModemDevice(AbstractUsbDevice usbDevice) {
	    super(usbDevice);
	    
        m_ttyDevs = new ArrayList<String>();
        m_blockDevs = new ArrayList<String>();	    
	}
	
	@Override
	public List<String> getSerialPorts() {
        return Collections.unmodifiableList(m_ttyDevs);
    }

	/**
	 * @return sorted list of tty devs
	 */
    public List<String> getTtyDevs() {
        return Collections.unmodifiableList(m_ttyDevs);
    }

    /**
     * @return sorted list of block devs
     */
	public List<String> getBlockDevs() {
		return Collections.unmodifiableList(m_blockDevs);
	}
    
    public void addTtyDev(String ttyDev) {
        if(!m_ttyDevs.contains(ttyDev)) {
            m_ttyDevs.add(ttyDev);
            Collections.sort(m_ttyDevs, new DevNameComparator());
        }
    }
	
	public void addBlockDev(String blockDev) {
	    if(!m_blockDevs.contains(blockDev)) {
    	    m_blockDevs.add(blockDev);
    	    Collections.sort(m_blockDevs, new DevNameComparator());
	    }
	}
    
    public boolean removeTtyDev(String ttyDev) {
        return m_ttyDevs.remove(ttyDev);
    }
	
	public boolean removeBlockDev(String blockDev) {
	    return m_blockDevs.remove(blockDev);
	}

	@Override
	public int hashCode() {
		final int prime = 37;
		int result = super.hashCode();
        result = prime * result
                + ((m_ttyDevs == null) ? 0 : m_ttyDevs.hashCode());
		result = prime * result
				+ ((m_blockDevs == null) ? 0 : m_blockDevs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		UsbModemDevice other = (UsbModemDevice) obj;
        if (m_ttyDevs == null) {
            if (other.m_ttyDevs != null)
                return false;
        } else if (!m_ttyDevs.equals(other.m_ttyDevs))
            return false;
		if (m_blockDevs == null) {
			if (other.m_blockDevs != null)
				return false;
		} else if (!m_blockDevs.equals(other.m_blockDevs))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
	    StringBuffer sb = new StringBuffer();
	    sb.append("UsbModem [");
	    sb.append("vendorId=").append(getVendorId());
	    sb.append(", productId=").append(getProductId());
	    sb.append(", manufName=").append(getManufacturerName());
	    sb.append(", productName=").append(getProductName());
	    sb.append(", usbPort=").append(getUsbPort());
	    sb.append(", ttyDevs=").append(m_ttyDevs.toString());
	    sb.append(", blockDevs=").append(m_blockDevs.toString());
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
	        if(textCompare != 0) {
	            return textCompare;
	        }
	        
	        // Compare numerical portion
	        if(num1 == null || num1.isEmpty()) {
	            if(num2 == null || num2.isEmpty()) {
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
	        
	        for(int i=devName.length()-1; i>=0; i--) {
	            if(Character.isDigit(devName.charAt(i))) {
	                pos = i;
	            } else {
	                break;
	            }
	        }
	        
	        return pos;
	    }
	}
}
