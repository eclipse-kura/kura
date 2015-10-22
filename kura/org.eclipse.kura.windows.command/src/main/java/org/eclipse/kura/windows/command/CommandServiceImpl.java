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
package org.eclipse.kura.windows.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.command.CommandService;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandServiceImpl  implements CommandService{
	
	private static final Logger s_logger = LoggerFactory.getLogger(CommandServiceImpl.class);
	
	private static final String SCRIPT_FILE = System.getProperty("java.io.tmpdir") + File.separator + "runCmd.bat";
	
	private File	m_scriptFile;

	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	protected void activate() {
		s_logger.debug("Activating...");
	}
	
	protected void deactivate() {
		s_logger.debug("Deactivating...");
	}
	
	// ----------------------------------------------------------------
	//
	//   Service APIs
	//
	// ----------------------------------------------------------------
	
	public String execute(String cmd) throws KuraException {
		if (cmd == null) {
			s_logger.debug("null command");
			return "null command";
		}
		// Delete script file if it exists
		m_scriptFile = new File(SCRIPT_FILE);
		if (m_scriptFile.exists()) {
			try {
				m_scriptFile.delete();
			} catch (SecurityException se) {
				s_logger.error("File " + m_scriptFile + " cannot be deleted");
			}
		}
		
		// Create script file and set appropriate permissions
		createScript(cmd);
		setPermissions();

		// Run script
		String output = runScript();
		
		return output;

	}
	
	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------
	private void createScript(String cmd) throws KuraException {
		try {
			cmd = "cd " + System.getProperty("java.io.tmpdir") + "\n" +
					cmd;
			
			FileOutputStream fos = new FileOutputStream(m_scriptFile);
			PrintWriter pw = new PrintWriter(fos);
			pw.write(cmd);
			pw.write("\n");
			pw.flush();
			fos.getFD().sync();
			pw.close();
			fos.close();
		} catch (IOException e) {
			throw KuraException.internalError(e);
		}
	}
	
	private void setPermissions() throws KuraException {
	}
	
	private String runScript() throws KuraException{
		SafeProcess procUserScript = null;
		InputStream is = null;
		InputStream es = null;
		StreamGobbler isg = null;
		StreamGobbler esg = null;

		try {
			procUserScript = ProcessUtil.exec("cmd /c " + m_scriptFile.toString());
			
			is = procUserScript.getInputStream();
			es = procUserScript.getErrorStream();
			
			isg = new StreamGobbler(is, "stdout");
			esg = new StreamGobbler(es, "stderr");
			isg.start();
			esg.start();
			
			procUserScript.waitFor();
			isg.join(1000);
			esg.join(1000);
			
			if (procUserScript.exitValue() == 0) {
				return isg.getStreamAsString();
			}
			else {
				return esg.getStreamAsString();
			}
		} catch (Exception e) {
			throw KuraException.internalError(e);
		} finally {
			if (procUserScript != null) ProcessUtil.destroy(procUserScript);
		}
	}
	
}
