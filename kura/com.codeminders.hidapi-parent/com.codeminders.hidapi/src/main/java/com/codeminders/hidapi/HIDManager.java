
package com.codeminders.hidapi;

import java.io.IOException;

/**
 * HIDManager.java 
 * High-level interface to enumerate, find , open HID devices and 
 * get connect/disconnect notifications.
 *
 * @version 1.0 
 * @author lord
 * 
 */
public class HIDManager
{
	static {
		 System.loadLibrary("hidapi");
	}

	private static HIDManager instance = null;
	  
    protected long peer;

    /**
     * Get list of all the HID devices attached to the system.
     *
     * @return list of devices
     * @throws IOException
     */
    public native HIDDeviceInfo[] listDevices() throws IOException;

    /**
     * Initializing the underlying HID layer.
     *
     * @throws IOException
     */
    private native void init() throws IOException;

    /**
     * Release underlying HID layer. This method must be called when
     * <code>HIDManager<code> object is no longer needed. Failure to
     * do so could cause memory leaks or unterminated threads. It is
     * safe to call this method multiple times.
     *
     */
    public native void release();
    
    /**
     * Constructor to create HID object manager. It must be invoked
     * from subclass constructor to ensure proper initialization.
     *
     * @throws IOException
     */
    private HIDManager() throws IOException
    {
        init();
    }

    /**
     * Release HID manager. Will call release().
     *
     * @throws Throwable
     */
    protected void finalize() throws Throwable
    {
        // It is important to call release() if user forgot to do so,
        // since it frees pointer internal data structures and stops
        // thread under MacOS
        try
        {
           release();
        } finally
        {
           super.finalize();
        }
    }

    /**
     * Convenience method to find and open device by path
     * 
     * @param path USB device path
     * @return open device reference <code>HIDDevice<code> object
     * @throws IOException in case of internal error
     * @throws HIDDeviceNotFoundException if devive was not found
     */
    public HIDDevice openByPath(String path) throws IOException, HIDDeviceNotFoundException
    {
        HIDDeviceInfo[] devs = listDevices();
        for(HIDDeviceInfo d : devs)
        {
            if(d.getPath().equals(path))
                return d.open();
        }
        throw new HIDDeviceNotFoundException(); 
    }

    /**
     * Convenience method to open a HID device using a Vendor ID
     * (VID), Product ID (PID) and optionally a serial number.
     * 
     * @param vendor_id USB vendor ID
     * @param product_id USB product ID
     * @param serial_number USB device serial number (could be <code>null<code>)
     * @return open device
     * @throws IOException in case of internal error
     * @throws HIDDeviceNotFoundException if devive was not found
     */
    public HIDDevice openById(int vendor_id, int product_id, String serial_number) throws IOException, HIDDeviceNotFoundException
    {
        HIDDeviceInfo[] devs = listDevices();
        for(HIDDeviceInfo d : devs)
        {
            if(d.getVendor_id() == vendor_id && d.getProduct_id() == product_id
                    && (serial_number == null || d.getSerial_number().equals(serial_number)))
                return d.open();
        }
        throw new HIDDeviceNotFoundException(); 
    }

    public static HIDManager getInstance() throws IOException {
        if(instance == null) {
        	synchronized (HIDManager.class) {
        		if (null == instance) {
        			instance = new HIDManager();
        		}
			}
        }
        return instance;
     }
}
