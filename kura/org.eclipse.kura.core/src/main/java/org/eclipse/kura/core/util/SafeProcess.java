package org.eclipse.kura.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeProcess 
{
	private static final Logger s_logger = LoggerFactory.getLogger(SafeProcess.class);

	private static final ExecutorService s_streamGobblers = Executors.newFixedThreadPool(2);
	
	private Process m_process;
	private Future<byte[]> m_futureInputGobbler;
	private Future<byte[]> m_futureErrorGobbler;
	private byte[]  m_inBytes;
	private byte[]  m_errBytes;
	private boolean m_waited;
	private int     m_exitValue;
	
    SafeProcess() {
		super();
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

	void exec(String[] cmdarray)
	    throws IOException
	{
        s_logger.debug("Executing: {}", Arrays.toString(cmdarray));
        ProcessBuilder pb = new ProcessBuilder(cmdarray);
        m_process = pb.start();

        // process the input stream
        m_futureInputGobbler = s_streamGobblers.submit( new Callable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                Thread.currentThread().setName("SafeProcess InputStream Gobbler");
                return readStreamFully(m_process.getInputStream());                    
            }
        });

        // process the error stream
        m_futureErrorGobbler = s_streamGobblers.submit( new Callable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                Thread.currentThread().setName("SafeProcess ErrorStream Gobbler");
                return readStreamFully(m_process.getErrorStream());                    
            }
        });      
        
        // wait for the process execution
        try {
            m_inBytes   = m_futureInputGobbler.get();
            m_errBytes  = m_futureErrorGobbler.get();
            m_exitValue = m_process.waitFor();
        }
        catch (InterruptedException e) {
            throw new IOException(e);
        }
        catch (ExecutionException e) {
            throw new IOException(e);
        }
        finally {
            closeQuietly(m_process.getInputStream());
            closeQuietly(m_process.getErrorStream());
            closeQuietly(m_process.getOutputStream());
            m_process.destroy();
            m_process = null;
            m_waited  = true;
        }
	}
	
	public int waitFor() throws InterruptedException 
	{
		return m_exitValue;
	}

	public int exitValue() {
		return m_exitValue;
	}

	public void destroy() {
		if (!m_waited) {
			s_logger.warn("Calling destroy() before waitFor() might lead to resource leaks");
			Thread.dumpStack();
			if (m_process != null) {
			    m_process.destroy();
			}
		}
		m_inBytes  = null; // just in case...
		m_errBytes = null;
		m_process  = null;
	}
	
	
    private byte[] readStreamFully(InputStream is) 
        throws IOException 
    {
        int len;
        byte[] buf = new byte[1024];
        ByteArrayOutputStream inBaos = new ByteArrayOutputStream(1024);
        while ((len = is.read(buf)) != -1) {
            inBaos.write(buf, 0, len);
        }
        return inBaos.toByteArray();
    }
    
    private void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
                is = null;
            } catch (IOException e) {
                s_logger.warn("Failed to close process input stream", e);
            }
        }
    }

    private void closeQuietly(OutputStream os) {
        if (os != null) {
            try {
                os.close();
                os = null;
            } catch (IOException e) {
                s_logger.warn("Failed to close process output stream", e);
            }
        }
    }
}
