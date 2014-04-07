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
/*
* Copyright (c) 2011 Eurotech Inc. All rights reserved.
*/

package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines ethtool utility 
 * 
 * @author ilya.binshtok
 *
 */
public class EthTool implements LinkTool {
	
	private static final Logger s_logger = LoggerFactory.getLogger(LinuxNetworkUtil.class);
	
	private static final String LINK_DETECTED = "Link detected:";
	private static final String DUPLEX = "Duplex:";
	private static final String SPEED = "Speed:";
	
	private String tool = null;
	private String ifaceName = null; 
	private boolean linkDetected = false;
	private int speed = 0; // in b/s
	private String duplex = null;

	/**
	 * ethtool constructor
	 * 
	 * @param ifaceName - interface name as {@link String}
	 */
	public EthTool (String tool, String ifaceName) {
		this.tool = tool;
		this.ifaceName = ifaceName;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.kura.util.net.service.ILinkTool#get()
	 */
	public boolean get () throws KuraException {
		Process proc = null;
		try {
			proc = ProcessUtil.exec(tool + " " + this.ifaceName);			
			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			int ind = -1;
		    while((line = br.readLine()) != null) {
		    	if ((ind = line.indexOf(LINK_DETECTED)) >= 0) {
		    		s_logger.trace("Link detected from: " + line);
		    		line = line.substring(ind + LINK_DETECTED.length()).trim();
		    		this.linkDetected = (line.compareTo("yes") == 0)? true : false;
		    	} else if ((ind = line.indexOf(DUPLEX)) >= 0) {
		    		this.duplex = line.substring(ind + DUPLEX.length()).trim();
		    	} else if ((ind = line.indexOf(SPEED)) >= 0) {
		    		line = line.substring(ind + SPEED.length()).trim();
		    		if (line.compareTo("10Mb/s") == 0) {
		    			this.speed = 10000000;
		    		} else if (line.compareTo("100Mb/s") == 0) {
		    			this.speed = 100000000;
		    		} else if (line.compareTo("1000Mb/s") == 0) {
		    			this.speed = 1000000000;
		    		}
		    	}
		    }
		    
		    return (proc.waitFor() == 0)? true : false;
		} catch(IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			ProcessUtil.destroy(proc);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.kura.util.net.service.ILinkTool#getIfaceName()
	 */
	public String getIfaceName() {
		return this.ifaceName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.kura.util.net.service.ILinkTool#isLinkDetected()
	 */
	public boolean isLinkDetected() {
		return this.linkDetected;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.kura.util.net.service.ILinkTool#getSpeed()
	 */
	public int getSpeed() {
		return this.speed;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.util.net.service.ILinkTool#getDuplex()
	 */
	public String getDuplex() {
		return this.duplex;
	}
}
