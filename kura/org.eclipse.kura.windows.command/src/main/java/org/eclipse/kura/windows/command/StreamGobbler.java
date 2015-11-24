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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamGobbler extends Thread {
	
	private static final Logger s_logger = LoggerFactory.getLogger(StreamGobbler.class);
	
	private static final int BUF_LEN = 1024;
	private static final int MAX_BYTES = 100 * BUF_LEN;
	
	private InputStream 	m_is;
	private String			m_streamType;
	private StringBuilder	m_sb;
	
	public StreamGobbler(InputStream is, String streamType) {
		m_is = is;
		m_streamType = streamType;
		m_sb = new StringBuilder();
	}
	
	public String getStreamAsString() {
		return m_sb.toString();
	}
	
	public void run() {
		InputStreamReader isr = new InputStreamReader(m_is);
		BufferedReader br = new BufferedReader(isr);
		
		try {
			int count = 0;
			char[] cbuf = new char[BUF_LEN];
			int read = -1;
			
			while ((read = br.read(cbuf)) != -1) {
				if (count < MAX_BYTES) {
					count += read;
					m_sb.append(cbuf, 0, read);
				}
			}
		} catch (IOException ioe) {
			s_logger.error(ioe.getMessage());
		} finally {
			try {
				br.close();
				m_is.close();
			} catch (IOException ioe) {
				s_logger.error(ioe.getMessage());
			}
		}
	}

}
