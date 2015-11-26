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
package org.eclipse.kura.core.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.system.SystemAdminService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.kura.windows.KuraNativeWin;

public class SystemAdminServiceImpl implements SystemAdminService 
{
	private static final Logger s_logger = LoggerFactory.getLogger(SystemAdminServiceImpl.class);

	private static final String OS_LINUX    = "Linux";
	private static final String OS_MAC_OSX  = "Mac OS X";
	private static final String OS_WINDOWS  = "windows";
	private static final String UNKNOWN     = "UNKNOWN";
	
	@SuppressWarnings("unused")
	private ComponentContext      m_ctx;

	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	
	protected void activate(ComponentContext componentContext) 
	{			
		//
		// save the bundle context
		m_ctx = componentContext;
	}
	
	
	protected void deactivate(ComponentContext componentContext) 
	{
		m_ctx = null;
	}

	
	
	// ----------------------------------------------------------------
	//
	//   Service APIs
	//
	// ----------------------------------------------------------------
		
	public String getUptime() {
		
		String uptimeStr = UNKNOWN;
		long uptime = 0;

		if(this.getOsName().toLowerCase().contains(OS_WINDOWS))
		{
			try {
				KuraNativeWin nativeWin = new KuraNativeWin();
				uptime = nativeWin.getTickCount();
				uptimeStr = Long.toString(uptime);
			}

			catch(Exception e) {
				uptimeStr = "0";
				s_logger.error("Could not read uptime", e);
			}
		}
		else if(OS_LINUX.equals(this.getOsName())) {
			try {
				File file = new File("/proc/uptime");
				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
	
				String line = br.readLine();
				br.close();
				
				uptime = (long) (Double.parseDouble(line.substring(0, line.indexOf(" "))) * 1000);
				uptimeStr = Long.toString(uptime);
			} catch (Exception e) {
				s_logger.error("Could not read uptime", e);
			}
		} else if(OS_MAC_OSX.equals(this.getOsName())) {
			try {
				String systemUptime = runSystemCommand("uptime");
				if(!systemUptime.isEmpty()) {
					String[] uptimeParts = systemUptime.split("up\\s+")[1].split("\\s*,\\s*");
					int days = 0, hours = 0, mins = 0;

					String uptimePart = uptimeParts[0];
					
					// If up less than a day, it will only show the number of mins, hr, or HH:MM
					if(uptimePart.contains("days")) {
						days = Integer.parseInt(uptimePart.split("\\s+days")[0]);
						uptimePart = uptimeParts[1];
					}
					else if(uptimePart.contains("day")) {
						days = Integer.parseInt(uptimePart.split("\\s+day")[0]);
						uptimePart = uptimeParts[1];
					}
					
					if(uptimePart.contains(":")) {
						// Showing HH:MM
						hours = Integer.parseInt(uptimePart.split(":")[0]);
						mins = Integer.parseInt(uptimePart.split(":")[1]);
					} else if(uptimePart.contains("hr")) {
						// Only showing hr
						hours = Integer.parseInt(uptimePart.split("\\s*hr")[0]);
					} else if(uptimePart.contains("mins")) {
						// Only showing mins
						mins = Integer.parseInt(uptimePart.split("\\s*mins")[0]);
					} else {
						s_logger.error("uptime could not be parsed correctly: " + uptimeParts[0]);
					}

					uptime = (((days * 24) + hours) * 60 + mins) * 60;
					uptimeStr = Long.toString(uptime * 1000);
				}
			} catch (Exception e) {
				s_logger.error("Could not parse uptime", e);
			}
		}
		return uptimeStr;
	}
	
	public void reboot() {
		String cmd = "";
		if(OS_LINUX.equals(this.getOsName()) || OS_MAC_OSX.equals(this.getOsName())) {		
			cmd = "reboot";
		} else if(this.getOsName().startsWith(OS_WINDOWS)) {
			cmd = "shutdown -r";
		} else {
			s_logger.error("Unsupported OS for reboot()");
			return;
		}
		SafeProcess proc = null;
		try {
			proc = ProcessUtil.exec(cmd);
			proc.waitFor();
		} catch(Exception e) {
			s_logger.error("failed to issue reboot", e);
		}
		finally {
			if (proc != null) ProcessUtil.destroy(proc);
		}
	}
	
	public void sync() {
		String cmd = "";
		if(OS_LINUX.equals(this.getOsName()) || OS_MAC_OSX.equals(this.getOsName())) {		
			cmd = "sync";
		} else {
			s_logger.error("Unsupported OS for sync()");
			return;
		}
		SafeProcess proc = null;		
		try {
			proc = ProcessUtil.exec(cmd);
			int status = proc.waitFor();
			if (status != 0) {
				s_logger.error("sync command failed with exit code of " + status);
			}
		} catch(Exception e) {
			s_logger.error("failed to issue sync command", e);
		}
		finally {
			if (proc != null) ProcessUtil.destroy(proc);
		}
	}

	
	private String getOsName() {
		return System.getProperty("os.name");
	}
	
	private String runSystemCommand(String command) {
		return this.runSystemCommand(command.split("\\s+"));
	}
	
	private String runSystemCommand(String[] commands) {
		SafeProcess proc = null;
		StringBuffer response = new StringBuffer(); 
		BufferedReader br = null;
		try {
			proc = ProcessUtil.exec(commands);
			proc.waitFor();
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			String newLine = "";
			while ((line = br.readLine()) != null) {
				response.append(newLine);
				response.append(line);
				newLine = "\n";
			}
		} catch(Exception e) {
			String command = "";
			String delim = "";
			for(int i=0; i<commands.length; i++) {
				command += delim + commands[i];
				delim = " ";
			}
			s_logger.error("failed to run commands " + command, e);
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
		
		return response.toString();
	}
}
