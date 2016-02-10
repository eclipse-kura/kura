/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Date;

import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommConnectionImpl implements CommConnection 
{
	private static final Logger s_logger = LoggerFactory.getLogger(CommConnectionImpl.class);

	//set up the appropriate ext dir for RXTX extra device nodes
	static {
		String kuraExtDir = System.getProperty("kura.ext.dir");
		if(kuraExtDir != null) {
			StringBuffer sb = new StringBuffer();
			String existingDirs = System.getProperty("java.ext.dirs");
			if(existingDirs != null) {
				if(!existingDirs.contains(kuraExtDir)) {
					sb.append(existingDirs)
					.append(":")
					.append(kuraExtDir);
					System.setProperty("java.ext.dirs", sb.toString());
				}
			} else {
				sb.append(kuraExtDir);
				System.setProperty("java.ext.dirs", sb.toString());
			}
		}
	}
	
	private String	   m_port;
	private int		   m_baudRate;
	private int 	   m_dataBits;
	private int		   m_stopBits;
	private int	 	   m_parity;
	private int        m_flowControl;
	private int		   m_timeout;
	
	private CommURI      m_commUri;
	private SerialPort   m_serialPort;
	private InputStream  m_inputStream;
	private OutputStream m_outputStream;

	public CommConnectionImpl(CommURI commUri, int mode, boolean timeouts)
		throws IOException, NoSuchPortException, PortInUseException
	{
		m_commUri 	  = commUri;
		m_serialPort  = null;
		m_port        = m_commUri.getPort();
		m_baudRate    = m_commUri.getBaudRate();
		m_dataBits    = m_commUri.getDataBits();
		m_stopBits    = m_commUri.getStopBits();
		m_parity      = m_commUri.getParity();
		m_flowControl = m_commUri.getFlowControl();
		m_timeout	  = m_commUri.getTimeout();

		CommPortIdentifier commPortIdentifier = CommPortIdentifier.getPortIdentifier(m_port);
		
		CommPort commPort = commPortIdentifier.open(this.getClass().getName(), m_timeout);

		if (commPort instanceof SerialPort) {
			m_serialPort = (SerialPort) commPort;
			try {
				m_serialPort.setSerialPortParams(m_baudRate, m_dataBits, m_stopBits, m_parity);
				m_serialPort.setFlowControlMode(m_flowControl);
			} catch (UnsupportedCommOperationException e) {
				e.printStackTrace();
			}
		} else {
			throw new IOException("Unsupported Port Type");
		}
	}

	@Override
	public CommURI getURI() {
		return m_commUri;
	}

	@Override
	public DataInputStream openDataInputStream() throws IOException {	
		return new DataInputStream(openInputStream());
	}

	@Override
	public synchronized InputStream openInputStream() throws IOException {
		 if (m_inputStream == null) {
			 m_inputStream = m_serialPort.getInputStream();
		 }
		 return m_inputStream;
	}

	@Override
	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	@Override
	public synchronized OutputStream openOutputStream() throws IOException {
		if (m_outputStream == null) {
			m_outputStream = m_serialPort.getOutputStream();
		 }
		 return m_outputStream;
	}

	@Override
	public synchronized void close() throws IOException {
	    if(m_serialPort != null) {
    		m_serialPort.notifyOnDataAvailable(false);
    		m_serialPort.removeEventListener();
    		if (m_inputStream != null) {
    			m_inputStream.close();
    			m_inputStream = null;
    		}
    		if (m_outputStream != null) {
    			m_outputStream.close();
    			m_outputStream = null;
    		}
    
    		m_serialPort.close();
    		m_serialPort = null;
	    }
	}

	@Override
	public synchronized void sendMessage(byte[] message) throws KuraException, IOException {
		if (message != null) {
			s_logger.debug("sendMessage() - " + getBytesAsString(message));
			
			if(m_outputStream == null) {
				openOutputStream();
			}
			
			m_outputStream.write(message, 0, message.length);
			m_outputStream.flush();
		} else {
			throw new NullPointerException("Serial message is null");
		}
	}

	@Override
	public synchronized byte[] sendCommand(byte[] command, int timeout) throws KuraException, IOException {
		if (command != null) {
			s_logger.debug("sendMessage() - " + getBytesAsString(command));

			if(m_outputStream == null) {
				openOutputStream();
			}
			if(m_inputStream == null) {
				openInputStream();
			}

			byte[] dataInBuffer = flushSerialBuffer();
			if(dataInBuffer != null && dataInBuffer.length > 0) {
				s_logger.warn("eating bytes in the serial buffer input stream before sending command: " + getBytesAsString(dataInBuffer));
			}
			m_outputStream.write(command, 0, command.length);
			m_outputStream.flush();

			ByteBuffer buffer = getResponse(timeout);
			if(buffer != null) {
				byte[] response = new byte[buffer.limit()];
				buffer.get(response, 0, response.length);
				return response;
			} else {
				return null;
			}
		} else {
			throw new NullPointerException("Serial command is null");
		}
	}
	
	@Override
	public synchronized byte[] sendCommand(byte[] command, int timeout, int demark) throws KuraException, IOException {
		if (command != null) {
			s_logger.debug("sendMessage() - " + getBytesAsString(command));

			if(m_outputStream == null) {
				openOutputStream();
			}
			if(m_inputStream == null) {
				openInputStream();
			}

			byte[] dataInBuffer = flushSerialBuffer();
			if(dataInBuffer != null && dataInBuffer.length > 0) {
				s_logger.warn("eating bytes in the serial buffer input stream before sending command: " + getBytesAsString(dataInBuffer));
			}
			m_outputStream.write(command, 0, command.length);
			m_outputStream.flush();

			ByteBuffer buffer = getResponse(timeout, demark);
			if(buffer != null) {
				byte[] response = new byte[buffer.limit()];
				buffer.get(response, 0, response.length);
				return response;
			} else {
				return null;
			}
		} else {
			throw new NullPointerException("Serial command is null");
		}
	}


	@Override
	public synchronized byte[] flushSerialBuffer() throws KuraException, IOException {
		ByteBuffer buffer = getResponse(50);
		if(buffer != null) {
			byte[] response = new byte[buffer.limit()];
			buffer.get(response, 0, response.length);
			return response;
		} else {
			return null;
		}
	}
	
	private synchronized ByteBuffer getResponse(int timeout) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(4096);
		Date start = new Date();
		
        while(m_inputStream.available() < 1 && ((new Date()).getTime() - start.getTime()) < timeout) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // ignore
            }
        }

		while (m_inputStream.available() >= 1) {
            int c = m_inputStream.read();
            buffer.put((byte) c);
		}

		buffer.flip();
		
		return (buffer.limit() > 0) ? buffer : null;
	}
	
	private synchronized ByteBuffer getResponse(int timeout, int demark) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(4096);
		long start = System.currentTimeMillis();
		
		while ((m_inputStream.available() < 1)
				&& ((System.currentTimeMillis() - start) < timeout)) {
			try {Thread.sleep(10);}catch (InterruptedException e) {}
		}

		start = System.currentTimeMillis();
		do {
			if (m_inputStream.available() > 0) {
				start = System.currentTimeMillis();
				int c = m_inputStream.read();
	            buffer.put((byte) c);
			} 
		} while ((System.currentTimeMillis() - start) < demark);

		buffer.flip();
		
		return (buffer.limit() > 0) ? buffer : null;
	}

	private String getBytesAsString(byte[] bytes) {
		if(bytes == null) {
			return null;
		} else {
			StringBuffer sb = new StringBuffer();
			for(byte b : bytes) {
				sb.append("0x")
				.append(Integer.toHexString(b))
				.append(" ");
			}
			
			return sb.toString();
		}
	}
}
