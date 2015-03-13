package org.eclipse.kura.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeProcess {
	private static final Logger s_logger = LoggerFactory.getLogger(SafeProcess.class);
	
	private Process m_process;
	private byte[]  m_inBytes;
	private byte[]  m_errBytes;
	private boolean m_waited;
	
//	private static List<SafeProcess> s_trackList = new ArrayList<SafeProcess>();

    SafeProcess(Process process) {
		super();
		m_process = process;
//		s_trackList.add(this);
	}

	public OutputStream getOutputStream() {
		s_logger.warn("getOutputStream() is unsupported");
		return null;
	}

	public InputStream getInputStream() {
		if (!m_waited) {
			s_logger.warn("getInputStream() must be called after waitFor()");
			//Thread.dumpStack();
		}
 		return new ByteArrayInputStream(m_inBytes);
	}

	public InputStream getErrorStream() {
		if (!m_waited) {
			s_logger.warn("getErrorStream() must be called after waitFor()");
			//Thread.dumpStack();
		}
		return new ByteArrayInputStream(m_errBytes);
	}

	public int waitFor() throws InterruptedException {
	    ByteArrayOutputStream inBaos = new ByteArrayOutputStream(1024);
	    ByteArrayOutputStream errBaos = new ByteArrayOutputStream(1024);
		InputStream is = null;
		InputStream es = null;
		int exitValue = -1;
		try {
			exitValue = m_process.waitFor();
			
			byte[] buf = new byte[1024];
			int len;
			is = m_process.getInputStream();
			while ((len = is.read(buf)) != -1) {
				inBaos.write(buf, 0, len);
			}
			
			es = m_process.getErrorStream();
			while ((len = es.read(buf)) != -1) {
				errBaos.write(buf, 0, len);
			}
			
	        m_inBytes = inBaos.toByteArray();
	        m_errBytes = errBaos.toByteArray();			
		} 
		catch (IOException e) {
			s_logger.warn("Failed to consume process input/error stream", e);
		} 
		finally {
			if (is != null) {
				try {
					is.close();
					is = null;
				} catch (IOException e) {
					s_logger.warn("Failed to close process input stream", e);
				}
			}
			if (es != null) {
				try {
					es.close();
                    es = null;
				} catch (IOException e) {
					s_logger.warn("Failed to close process error stream", e);
				}
			}
            if (inBaos != null) {
                try {
                    inBaos.close();
                    inBaos = null;
                } catch (IOException e) {
                    s_logger.warn("Failed to close inBaos stream", e);
                }
            }
            if (errBaos != null) {
                try {
                    errBaos.close();
                    errBaos = null;
                } catch (IOException e) {
                    s_logger.warn("Failed to close errBaos stream", e);
                }
            }
			m_process.destroy();
			m_waited = true;
//			s_trackList.remove(this);
//			s_logger.debug("Number of running processes: {}", s_trackList.size());
		}
		
		return exitValue;
	}

	public int exitValue() {
		return m_process.exitValue();
	}

	public void destroy() {
		if (!m_waited) {
			s_logger.warn("Calling destroy() before waitFor() might lead to resource leaks");
			Thread.dumpStack();
			m_process.destroy();
//			s_trackList.remove(this);
//			s_logger.debug("Number of running processes: {}", s_trackList.size());
		}
		m_inBytes  = null; // just in case...
		m_errBytes = null;
		m_process  = null;
	}
}
