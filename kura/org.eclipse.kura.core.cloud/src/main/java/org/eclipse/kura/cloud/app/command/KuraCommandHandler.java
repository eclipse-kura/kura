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
package org.eclipse.kura.cloud.app.command;

import java.io.File;
import java.io.IOException;

import org.eclipse.kura.message.KuraResponsePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KuraCommandHandler
{
	private static final Logger s_logger = LoggerFactory.getLogger(KuraCommandHandler.class);


	public static void handleRequest(KuraCommandRequestPayload req, KuraCommandResponsePayload resp)
	{
		String command = req.getCommand();
		if (command == null) {
			s_logger.error("null command");
			resp.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}

		String[] args = req.getArguments();
		int argsCount = args != null ? args.length : 0;
		String[] cmdarray = new String[1 + argsCount];

		cmdarray[0] = command;
		for (int i = 0; i < argsCount; i++) {
			cmdarray[1 + i] = args[i];
		}

		for (int i = 0; i < cmdarray.length; i++) {
			s_logger.debug("cmdarray: {}", cmdarray[i]);
		}

		String[] envp = req.getEnvironmentPairs();
		String    dir = req.getWorkingDir();
		if (dir != null && dir.isEmpty()) {
			dir = null;
		}
		byte[] zipBytes = req.getZipBytes();
		if (zipBytes != null) {
			try {				
				UnZip.unZipBytes(zipBytes, dir);
			} 
			catch (IOException e) {
				s_logger.error("Error unzipping command zip bytes", e);

				resp.setException(e);
				return;
			}
		}
		Process proc= null;
		try {
			//proc= CommandCloudApp.executeCommand(dir, cmdarray, envp);
		} catch (Throwable t) {
			s_logger.error("Error executing command {}", t);
			resp.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			resp.setException(t);
			return;
		}

//		Runtime rt = Runtime.getRuntime();
//		Process  proc = null;
//		try {
//
//			File fileDir = dir == null ? null : new File(dir);
//
//			proc = rt.exec(cmdarray, envp, fileDir);
//		} catch (Throwable t) {
//			s_logger.error("Error executing command {}", t);
//			resp.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
//			resp.setException(t);
//			return;
//		}
		
		ProcessMonitorThread pmt = null;
		boolean runAsync = req.isRunAsync() != null ? req.isRunAsync() : false;
		// Whether to timeout process termination
		// 0: wait until process terminates
		// > 0: timed out wait
		int timeout = req.getTimeout() != null ? req.getTimeout() : 0;

		// TODO: rewrite this using ThreadPoolExecutor and Future
		pmt = new ProcessMonitorThread(proc, req.getStdin(), timeout);
		pmt.start();
		
		if (!runAsync) {
			try {
				pmt.join();
				if (pmt.getException() != null) {
					resp.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
					resp.setException(pmt.getException());
					resp.setStderr(pmt.getStderr());
					resp.setStdout(pmt.getStdout());
				} else {
					resp.setStderr(pmt.getStderr());
					resp.setStdout(pmt.getStdout());
					resp.setTimedout(pmt.isTimedOut());

					if (!pmt.isTimedOut()) {
						resp.setExitCode(pmt.getExitValue());
					}
				}
			} catch (InterruptedException e) {
				Thread.interrupted();
				pmt.interrupt();

				resp.setStderr(pmt.getStderr());
				resp.setStdout(pmt.getStdout());
				resp.setTimedout(true);
			}
		}
	}
}