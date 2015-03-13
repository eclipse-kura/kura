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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessUtil 
{
	private static final Logger s_logger = LoggerFactory.getLogger(ProcessUtil.class);

	private static final String BASH      = "/bin/bash";
    private static final String BASH_FLAG = "-c";
    
	public static SafeProcess exec(String command)
		throws IOException
	{
		s_logger.debug("Executing: {}", command);
		Runtime runtime = Runtime.getRuntime();
		//return new SafeProcess(runtime.exec(command));
		String[] cmdarray = new String[] {BASH, BASH_FLAG, command};
		return new SafeProcess(runtime.exec(cmdarray));
	}

	
//	public static SafeProcess exec(String command, String[] envp)
//		throws IOException
//	{
//		s_logger.debug("Executing: {}", command);
//		Runtime runtime = Runtime.getRuntime();
//		return new SafeProcess(runtime.exec(command, envp));
//	}
//
//	
	public static SafeProcess exec(String[] cmdarray)
		throws IOException
	{
		s_logger.debug("Executing: {}", cmdarray[0]);
		Runtime runtime = Runtime.getRuntime();
		//return new SafeProcess(runtime.exec(cmdarray));
//		StringBuilder sb = new StringBuilder();
//		for (String s : cmdarray) {
//			sb.append(s);
//			sb.append(" ");
//		}
		String[] newCmdArray = new String[cmdarray.length + 2];
		newCmdArray[0] = BASH;
		newCmdArray[1] = BASH_FLAG;
		System.arraycopy(cmdarray, 0, newCmdArray, 2, cmdarray.length);
		return new SafeProcess(runtime.exec(newCmdArray));
	}
//
//	
//	public static SafeProcess exec(String[] cmdarray, String[] envp)
//		throws IOException
//	{
//		s_logger.debug("Executing: {}", cmdarray[0]);
//		Runtime runtime = Runtime.getRuntime();
//		return new SafeProcess(runtime.exec(cmdarray, envp));
//	}

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
