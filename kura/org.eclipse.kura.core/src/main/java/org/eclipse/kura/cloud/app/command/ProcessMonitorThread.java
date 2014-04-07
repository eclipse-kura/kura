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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Timer;

public class ProcessMonitorThread extends Thread 
{
	private Process proc;
	private String stdin;
	private int procTout;
	private String stdout;
	private String stderr;
	private boolean timedOut;
	private Exception exception;
	private Integer exitValue;

	
	public ProcessMonitorThread(Process proc, String stdin, int procTout) {
		this.proc = proc;
		this.stdin = stdin;
		this.procTout = procTout;
		this.timedOut = false;
	}

	
	public String getStdout() {
		return stdout;
	}


	public String getStderr() {
		return stderr;
	}


	public Integer getExitValue() {
		return exitValue;
	}

	
	public boolean isTimedOut() {
		return timedOut;
	}
	
	
	public Exception getException() {
		return exception;
	}


	public void setException(Exception exception) {
		this.exception = exception;
	}

	
	public void run() {

		// Resources that should be cleaned up
		InputStream is = null;
		InputStream es = null;
		OutputStream os = null;
		BufferedWriter obw = null;
		
		StreamGobbler isg = null;
		StreamGobbler esg = null;
		Timer timer = null;

		try {
			if (procTout > 0) {
				timer = new Timer(true);
				InterruptTimerTask interrupter = new InterruptTimerTask(Thread.currentThread());
				timer.schedule(interrupter, procTout /*seconds*/ * 1000 /*milliseconds per second*/);
			}

			is = proc.getInputStream();
			es = proc.getErrorStream();

			os = proc.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			obw = new BufferedWriter(osw);

			// Spawn a couple of StreamGobbler to consume output streams
			isg = new StreamGobbler(is, "stdout");
			esg = new StreamGobbler(es, "stderr");
			isg.start();
			esg.start();

			if (stdin != null) {
				try {
					obw.write(stdin);
					obw.newLine();
					obw.flush();
				} catch (IOException e) {
					throw e;
				}
			}

			proc.waitFor();
			isg.join(1000);
			esg.join(1000);
			exitValue = proc.exitValue();
		} catch (IOException e) {
			exception = e;
		} catch (InterruptedException e) {
			timedOut  = true;
		} finally {
			if (timer != null) {
				timer.cancel();
			}

			if (isg != null) {
				isg.interrupt();
				try {
					isg.join(1000);
				} catch (InterruptedException e) {
					// Ignore
				}
				stdout = isg.getStreamAsString();
			}

			if (esg != null) {
				esg.interrupt();
				try {
					esg.join(1000);
				} catch (InterruptedException e) {
					// Ignore
				}
				stderr = esg.getStreamAsString();
			}

			// See http://kylecartmell.com/?p=9
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// Ignore
				}
			}
			if (es != null) {
				try {
					es.close();
				} catch (IOException e) {
					// Ignore
				}
			}
			if (obw != null) {
				try {
					obw.close();
				} catch (IOException e) {
					// Ignore
				}				
			}
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					// Ignore
				}				
			}			
			if (proc != null) {
				proc.destroy();
			}

			Thread.interrupted(); // See http://kylecartmell.com/?p=9
		}
	}
}