package com.codeminders.hidapplet;

import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;;

public class HidApplet extends JApplet
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 619732094067421147L;
	HIDManager hid_mgr = null;
    @Override
    public void init()
    {
        String os = System.getProperty("os.name", "win").toLowerCase();
        String arch = System.getProperty("os.arch", "x86");
        boolean x64 = arch.indexOf("_64") != -1;
        String library;
        if (os.indexOf("win") != -1)
        {
            library = "hidapi-windows.dll";
        } else if (os.indexOf("mac") != -1)
        {
            library = "hidapi-mac.so";
        } else
        {
            library = "hidapi-unix.so";
        }
        System.out.println("Using library: " + library);
        try
        {
            InputStream libSrc = getClass().getClassLoader().getResourceAsStream("native/" + library);
            if (libSrc == null) {
                System.err.println("No library found");
                return;
            }
                    
            File libFile = File.createTempFile("hdapi", ".lib");
            System.out.println("Copying library to: " + libFile.getAbsolutePath());
            
            byte buf[] = new byte[16384];
            OutputStream libDest = new FileOutputStream(libFile);
            int l;
            while ((l = libSrc.read(buf)) > 0)
                libDest.write(buf, 0, l);
            
            libSrc.close();
            libDest.close();
            System.out.println("Loading native library");
            System.load(libFile.getAbsolutePath());
            System.out.println("Native library loaded");
            System.out.println("Listing HID devices");
            
            JTextArea results = new JTextArea();
            results.setEditable(false);
            results.setEnabled(false);
            setLayout(new BorderLayout());
            add(new JScrollPane(results), BorderLayout.CENTER);
            StringBuilder b = new StringBuilder();
            hid_mgr = HIDManager.getInstance();
            for (HIDDeviceInfo info : hid_mgr.listDevices())
                 b.append(info).append('\n');
            results.setText(b.toString());
        
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        
    }
    public void destroy() {
        if(null!=hid_mgr)
            hid_mgr.release();
    }
} 

