package org.eclipse.kura.linux.bluetooth.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.bluetooth.BluetoothGatt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothProcess {

	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothProcess.class);
	private static final ExecutorService s_streamGobblers = Executors.newFixedThreadPool(2);

	private Process m_process;
	private Future<Void> m_futureInputGobbler;
	private Future<Void> m_futureErrorGobbler;
	private BufferedWriter m_bufferedWriter;
	
	public BufferedWriter getWriter() {
		return new BufferedWriter(new OutputStreamWriter(m_process.getOutputStream()));
	}
	
	void exec(String[] cmdArray, final BluetoothProcessListener listener) throws IOException{
		s_logger.debug("Executing: {}", Arrays.toString(cmdArray));
		ProcessBuilder pb = new ProcessBuilder(cmdArray);
		m_process = pb.start();
		
		// process the input stream
		m_futureInputGobbler = s_streamGobblers.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				Thread.currentThread().setName("BluetoothProcess Input Stream Gobbler");
				return readInputStreamFully(m_process.getInputStream(), listener);
			}
			
		});
		
		// process the error stream
        m_futureErrorGobbler = s_streamGobblers.submit( new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Thread.currentThread().setName("BluetoothProcess ErrorStream Gobbler");
                return readErrorStreamFully(m_process.getErrorStream(), listener);                    
            }
        });
        
	}
	
	public void destroy() {
		if (m_process != null) {
			s_logger.info("Closing streams and killing..." );
            closeQuietly(m_process.getInputStream());
            closeQuietly(m_process.getErrorStream());
            closeQuietly(m_process.getOutputStream());
            m_process.destroy();
            m_process = null;
		}
		m_process  = null;
	}
	
	private Void readInputStreamFully(InputStream is, BluetoothProcessListener listener) throws IOException {
		int ch;
		
		if (listener instanceof BluetoothGatt) {
			BufferedReader br = null;
			br = new BufferedReader(new InputStreamReader(is));
			while ((ch = br.read()) != -1) {
				listener.processInputStream((char) ch);
			}
			s_logger.debug("End of stream!");
			return null;
		} else {
			StringBuilder stringBuilder = new StringBuilder();

			BufferedReader br = null;
			br = new BufferedReader(new InputStreamReader(is));
			while ((ch = br.read()) != -1) {
				stringBuilder.append((char) ch);
			}
			listener.processInputStream(stringBuilder.toString());
			s_logger.debug("End of stream!");
			return null;
		}
	}
	
	private Void readErrorStreamFully(InputStream is, BluetoothProcessListener listener) throws IOException {
		int ch;

		StringBuilder stringBuilder = new StringBuilder();

		BufferedReader br = null;
		br = new BufferedReader(new InputStreamReader(is));
		while ((ch = br.read()) != -1) {
			stringBuilder.append((char) ch);
		}
		listener.processErrorStream(stringBuilder.toString());
		s_logger.debug("End of stream!");
		return null;
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
