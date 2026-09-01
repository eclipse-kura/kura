
package com.codeminders.hidapi;

import java.io.IOException;

/**
 * Container class which contains HID device properties.
 *
 * @author lord
 */
public class HIDDeviceInfo
{
    private String path;
    private int    vendor_id;
    private int    product_id;
    private String serial_number;
    private int    release_number;
    private String manufacturer_string;
    private String product_string;
    private int    usage_page;
    private int    usage;
    private int    interface_number;

    /**
     * Protected constructor, used from JNI Allocates a new
     * <code>HIDDeviceInfo<code> object.
     */
    HIDDeviceInfo()
    {
    }
    
    /** 
     * Get the platform-specific device path. 
     * @return the string value
     */
    public String getPath()
    {
        return path;
    }
    
    /** 
     * Get the device USB vendor ID. 
     * @return integer value
     */
    public int getVendor_id()
    {
        return vendor_id;
    }
    
    /** 
     * Get the device USB product ID.
     * @return the integer value
     */
    public int getProduct_id()
    {
        return product_id;
    }
    
    /** 
     * Get the device serial number.
     * @return the string value
     */
    public String getSerial_number()
    {
        return serial_number;
    }
    
    /** 
     * Get the device release number in binary-coded decimal,
     * also known as device version number. 
     * @return the integer value
     */
    public int getRelease_number()
    {
        return release_number;
    }
    
    /** 
     * Get the device manufacturer string. 
     * @return the string value
     */
    public String getManufacturer_string()
    {
        return manufacturer_string;
    }
    
    /** 
     * Get the device product string
     * @return the integer value
     */
    public String getProduct_string()
    {
        return product_string;
    }
    
    /** 
     * Get the device usage page (Windows/Mac only).
     * @return the integer value
     */
    public int getUsage_page()
    {
        return usage_page;
    }
    
    /** 
     * Get the device usage (Windows/Mac only).
     * @return the integer value
     */
    public int getUsage()
    {
        return usage;
    }
    
    /**
     * Get the USB interface which this logical device
     * represents. Valid on both Linux implementations in all cases,
     * and valid on the Windows implementation only if the device
     * contains more than one interface.
     * @return the integer value
     */
    public int getInterface_number()
    {
        return interface_number;
    }
    
    /**
     *  Open a HID device using a path name from this class.  
     *  Used from JNI.
     *
     * @return return a reference to the <code>HIDDevice<code> object
     * @throws IOException
     */
    public native HIDDevice open() throws IOException;
    
    /**
     *  Override method for conversion this object to <code>String<code> object.
     *
     * @return return a reference to the <code>String<code> object
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("HIDDeviceInfo [path=");
        builder.append(path);
        builder.append(", vendor_id=");
        builder.append(vendor_id);
        builder.append(", product_id=");
        builder.append(product_id);
        builder.append(", serial_number=");
        builder.append(serial_number);
        builder.append(", release_number=");
        builder.append(release_number);
        builder.append(", manufacturer_string=");
        builder.append(manufacturer_string);
        builder.append(", product_string=");
        builder.append(product_string);
        builder.append(", usage_page=");
        builder.append(usage_page);
        builder.append(", usage=");
        builder.append(usage);
        builder.append(", interface_number=");
        builder.append(interface_number);
        builder.append("]");
        return builder.toString();
    }
    
}
