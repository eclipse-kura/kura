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

package org.eclipse.kura.linux.util;

import java.io.InputStream;
import java.io.OutputStream;

public class ProcessStats {

	private Process m_process;
	
	public ProcessStats (Process proc) {
		m_process = proc;
	}
	
	public Process getProcess() {
		return m_process;
	}

	public OutputStream getOutputStream() {
		return m_process.getOutputStream();
	}
	public InputStream getInputStream() {
		return m_process.getInputStream();
	}
	public InputStream getErrorStream() {
		return m_process.getErrorStream();
	}
	public int getReturnValue() {
		return m_process.exitValue();
	}
}
