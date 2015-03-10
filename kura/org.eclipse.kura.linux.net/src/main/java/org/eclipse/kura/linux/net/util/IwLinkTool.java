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

public class IwLinkTool implements LinkTool {

	private static final Logger s_logger = LoggerFactory.getLogger(IwLinkTool.class);
			
    private String m_tool = null;
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
    public IwLinkTool (String tool, String ifaceName) {
        this.m_tool = tool;
        this.m_interfaceName = ifaceName;
        
        this.m_duplex = "half";
    }

    @Override
    public boolean get() throws KuraException {
        SafeProcess proc = null;
        BufferedReader br = null;
        try {
            proc = ProcessUtil.exec(m_tool + " " + this.m_interfaceName + " link");
            proc.waitFor();
            br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            while((line = br.readLine()) != null) {
                line = line.trim();
                
                if(line.startsWith("Not connected")) {
                    return true;
                } else if (line.contains("signal:")) {
                    // e.g.: signal: -55 dBm
                    String parts[] = line.split("\\s");
                    try{
                        int signal = Integer.parseInt(parts[1]);
                        if(signal > -100) {     // TODO: adjust this threshold?
                        	m_signal = signal;
                            m_linkDetected = true;
                        }
                    } catch (NumberFormatException e) {
                    	s_logger.debug("Could not parse '" + parts[1] + "' as int in line: " + line);
                    	return false;
                    }
                } else if (line.contains("tx bitrate:")) {
                    // e.g.: tx bitrate: 1.0 MBit/s
                    String parts[] = line.split("\\s");
                    try{
                        double bitrate = Double.parseDouble(parts[2]);
                        if(parts[3].equals("MBit/s")) {
                            bitrate *= 1000000;
                        }
                        this.m_speed = (int) Math.round(bitrate);
                    } catch (NumberFormatException e) {
                    	s_logger.debug("Could not parse '" + parts[2] + "' as double in line: " + line);
                    	return false;
                    }                    
                }
            }
            
            int exitVal = proc.waitFor();
            if(exitVal != 0) {
            	s_logger.warn(m_tool + " returned with exit value " + exitVal);
            	return false;
            }
            
            return true;
        } catch(IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } catch (InterruptedException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        finally {
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

    public int getSignal() {
    	return m_signal;
    }
}
