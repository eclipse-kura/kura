package org.eclipse.kura.linux.bluetooth.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
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
	private Future<?> m_futureInputGobbler;
	private Future<?> m_futureErrorGobbler;
	private BufferedWriter m_bufferedWriter;
	
	public BufferedWriter getWriter() {
		return m_bufferedWriter;
	}
	
	void exec(String[] cmdArray, final BluetoothProcessListener listener) throws IOException{
		s_logger.debug("Executing: {}", Arrays.toString(cmdArray));
		ProcessBuilder pb = new ProcessBuilder(cmdArray);
		m_process = pb.start();
		m_bufferedWriter = new BufferedWriter(new OutputStreamWriter(m_process.getOutputStream()));
		
		// process the input stream
		m_futureInputGobbler = s_streamGobblers.submit(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("BluetoothProcess Input Stream Gobbler");
				try {
					readInputStreamFully(m_process.getInputStream(), listener);
				} catch (IOException e) {
					s_logger.warn("Error in processing the input stream : ", e);
				}
			}
			
		});
		
		// process the error stream
        m_futureErrorGobbler = s_streamGobblers.submit(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("BluetoothProcess ErrorStream Gobbler");
                try {
					readErrorStreamFully(m_process.getErrorStream(), listener);
				} catch (IOException e) {
					s_logger.warn("Error in processing the error stream : ", e);
				}                    
            }
        });
        
	}
	
	public void destroy() {
		if (m_process != null) {
			s_logger.info("Closing streams and killing..." );
            closeQuietly(m_process.getInputStream());
            closeQuietly(m_process.getErrorStream());
            closeQuietly(m_process.getOutputStream());
            m_futureInputGobbler.cancel(true);
            m_futureErrorGobbler.cancel(true);
            m_process.destroy();
            m_process = null;
		}
		m_process  = null;
	}
	
	private void readInputStreamFully(InputStream is, BluetoothProcessListener listener) throws IOException {
		int ch;
		String line;
		
		if (listener instanceof BluetoothGatt) {
			BufferedReader br = null;
			br = new BufferedReader(new InputStreamReader(is));
			while ((ch = br.read()) != -1) {
				listener.processInputStream((char) ch);
			}
			s_logger.debug("End of stream!");
		} else {
			StringBuilder stringBuilder = new StringBuilder();

			BufferedReader br = null;
			br = new BufferedReader(new InputStreamReader(is));
			
			while ((line = br.readLine()) != null) {
				stringBuilder.append(line + "\n");
			}
			listener.processInputStream(stringBuilder.toString());
			s_logger.debug("End of stream!");
		}
	}
	
	private void readErrorStreamFully(InputStream is, BluetoothProcessListener listener) throws IOException {
		int ch;

		StringBuilder stringBuilder = new StringBuilder();

		BufferedReader br = null;
		br = new BufferedReader(new InputStreamReader(is));
		while ((ch = br.read()) != -1) {
			stringBuilder.append((char) ch);
		}
		listener.processErrorStream(stringBuilder.toString());
		s_logger.debug("End of stream!");
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
