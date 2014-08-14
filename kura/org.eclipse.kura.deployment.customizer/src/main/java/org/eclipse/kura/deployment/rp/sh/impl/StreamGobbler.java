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
package org.eclipse.kura.deployment.rp.sh.impl;

import java.io.*;

class StreamGobbler extends Thread {
	InputStream is;
	String type;
	StringBuilder sb;

	public StreamGobbler(InputStream is, String type) {
		this.is = is;
		this.type = type;
		this.sb = new StringBuilder();
	}

	public String getStreamAsString() {
		return sb.toString();
	}

	public void run() {
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		try	{
			final int BUF_LEN = 1024;
			final int MAX_BYTES = 100 * BUF_LEN;
			int count = 0;
			char[] cbuf = new char[BUF_LEN];
			int read = -1;

			while ((read = br.read(cbuf)) != -1) {
				//System.out.println(type + ">number of bytes read: " + read);
				
				if (count < MAX_BYTES) {
					count += read; // might slightly exceed MAX_BYTES
					sb.append(cbuf, 0, read);
				}
			}
		} catch (IOException ioe) {
			// ioe.printStackTrace();
		} finally {
			try {
				br.close();
				is.close(); // Just in case...
			} catch (IOException e) {
				// Ignore
			}
		}
	}
}