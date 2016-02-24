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

package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class iwconfigLinkTool implements LinkTool {
	
	private static final Logger s_logger = LoggerFactory.getLogger(iwconfigLinkTool.class);
	
	private String m_interfaceName = null; 
    private boolean m_linkDetected = false;
    private int m_speed = 0; // in b/s
    private String m_duplex = null;
    private int m_signal = 0;
    
    /**
     * constructor
     * 
     * @param ifaceName - interface name as {@link String}
     */
    public iwconfigLinkTool (String ifaceName) {
        m_interfaceName = ifaceName;
        m_duplex = "half";
    }
    
    @Override
    public boolean get() throws KuraException {
    	BufferedReader br = null;
    	boolean associated = false;
    	SafeProcess proc = null;
    	try {
    		proc = ProcessUtil.exec("iwconfig " + m_interfaceName);
	    	if (proc.waitFor() != 0) {
	    		s_logger.warn("The iwconfig returned with exit value {}", proc.exitValue());
	        	return false;
	        }
	    	
	    	 br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
	         String line = null;
	         while((line = br.readLine()) != null) {
	        	 line = line.trim();
	        	 if (line.contains("Mode:")) {
		        	 int modeInd = line.indexOf("Mode:");
		             if (modeInd >= 0) {
		            	 String mode = line.substring(modeInd + "Mode:".length());
		            	 mode = mode.substring(0, mode.indexOf(' '));
		            	 if (mode.equals("Managed")) {
		            		 int apInd = line.indexOf("Access Point:");
		    	             if (apInd  > 0) {
		    	            	 line = line.substring(apInd+"Access Point:".length()).trim();
		    	            	 if (line.startsWith("Not-Associated")) {
		    	            		 return true;
		    	            	 }
		    	            	 associated = true;
		    	             }
		            	 } else {
		            		 return true;
		            	 } 
		             }
	        	 } else if (line.contains("Bit Rate=")) {
	        		 int bitRateInd = line.indexOf("Bit Rate=");
	        		 line = line.substring(bitRateInd+"Bit Rate=".length());
	        		 line = line.substring(0, line.indexOf(' '));
	        		 double bitrate = Double.parseDouble(line) * 1000000;
                     m_speed = (int) Math.round(bitrate);
	        	 } else if (line.contains("Signal level=")) {
	        		 int sigLevelInd = line.indexOf("Signal level=");
	        		 line = line.substring(sigLevelInd+"Signal level=".length());
	        		 line = line.substring(0, line.indexOf(' '));
	        		 int signal = 0;
	        		 if (line.contains("/")) {
	        			// Could also be of format 39/100
							final String[] parts = line.split("/");
							signal = (int) Float.parseFloat(parts[0]);
							if(signal <= 0)
								signal = -100;
						    else if(signal >= 100)
						    	signal = -50;
						    else
						    	signal = (signal / 2) - 100;
	        		 } else {
		        		 signal = Integer.parseInt(line);
	        		 }
	        		 
					if (associated && (signal > -100)) { // TODO: adjust this threshold?
						s_logger.debug("get() :: !! Link Detected !!");
						m_signal = signal;
						m_linkDetected = true;
					}
	        	 }
	         }
	         
	         return true;
    	} catch (Exception e) {
    		throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
    	} finally {
    		if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}			

			if (proc != null) ProcessUtil.destroy(proc);
    	}
    }

    @Override
	public String getIfaceName() {
        return m_interfaceName;
    }

	 @Override
    public boolean isLinkDetected() {
        return m_linkDetected;
    }

    @Override
    public int getSpeed() {
        return m_speed;
    }

    @Override
    public String getDuplex() {
        return m_duplex;
    }

    @Override
    public int getSignal() {
    	return m_signal;
    }
}
