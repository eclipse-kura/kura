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
package org.eclipse.kura.core.util;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessUtil 
{
	private static final Logger s_logger = LoggerFactory.getLogger(ProcessUtil.class);

	private static final ExecutorService s_processExecutor = Executors.newSingleThreadExecutor();

	private static final String BASH      = "/bin/bash";
    private static final String BASH_FLAG = "-c";
    
	public static SafeProcess exec(String command)
		throws IOException
	{
		return exec( new String[] {command});
	}

	public static SafeProcess exec(String[] cmdarray)
		throws IOException
	{
		final String[] newCmdArray = new String[cmdarray.length + 2];
		newCmdArray[0] = BASH;
		newCmdArray[1] = BASH_FLAG;
		System.arraycopy(cmdarray, 0, newCmdArray, 2, cmdarray.length);
		
		// Serialize process executions. One at a time so we can consume all streams.
        Future<SafeProcess> futureSafeProcess = s_processExecutor.submit( new Callable<SafeProcess>() {
            @Override
            public SafeProcess call() throws Exception {
                Thread.currentThread().setName("SafeProcessExecutor");
                SafeProcess safeProcess = new SafeProcess();
                safeProcess.exec(newCmdArray);
                return safeProcess;
            }           
        });
        
        try {
            return futureSafeProcess.get();
        } 
        catch (Exception e) {
            s_logger.error("Error waiting from SafeProcess ooutput", e);
            throw new IOException(e);
        }
	}

	/**
	 * @deprecated  The method does nothing
	 */
	@Deprecated
	public static void close(SafeProcess proc)
	{
	}
	
	public static void destroy(SafeProcess proc)
	{
		proc.destroy();	
	}
}



    