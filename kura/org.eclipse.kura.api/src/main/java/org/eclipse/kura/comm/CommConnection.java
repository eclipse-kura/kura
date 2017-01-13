/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.comm;

import java.io.IOException;

import javax.microedition.io.StreamConnection;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This is the primary control class for a Serial port. An instance of
 * this class may be operated on by more than one thread. Settings will be
 * those of the last thread to successfully change each particular setting.
 * <p>
 * Code written to use a javax.comm.SerialPort object in a shared mode should
 * make use of synchronization blocks where exclusive transactions are wanted.
 * In most instances, both the OutputStream and the InputStream should be
 * synchronized on, normally with the OutputStream being synchronized first.
 *
 * <pre>
 * Example Code:
 * 		try {
 *			String uri = new CommURI.Builder("/dev/tty.PL2303-00001004")
 *										.withBaudRate(19200)
 *										.withDataBits(8)
 *										.withStopBits(1)
 *										.withParity(0)
 *										.withTimeout(2000)
 *										.build().toString();
 *			CommConnection connOne = (CommConnection) CommTest.connectionFactory.createConnection(uri, 1, false);
 *			assertNotNull(connOne);
 *			uri = new CommURI.Builder("/dev/tty.PL2303-00002006")
 *										.withBaudRate(19200)
 *										.withDataBits(8)
 *										.withStopBits(1)
 *										.withParity(0)
 *										.withTimeout(2000)
 *										.build().toString();
 *			CommConnection connTwo = (CommConnection) CommTest.connectionFactory.createConnection(uri, 1, false);
 *			assertNotNull(connTwo);
 *
 *			InputStream isOne = connOne.openInputStream();
 *			OutputStream osOne = connOne.openOutputStream();
 *			InputStream isTwo = connTwo.openInputStream();
 *			OutputStream osTwo = connTwo.openOutputStream();
 *
 *			assertNotNull(isOne);
 *			assertNotNull(osOne);
 *			assertNotNull(isTwo);
 *			assertNotNull(osTwo);
 *
 *			//write from one to two
 *			byte[] array = "this is a message from one to two\n".getBytes();
 *			osOne.write(array);
 *			StringBuffer sb = new StringBuffer();
 *			int c;
 *			while((c = isTwo.read()) != 0xa) {
 *				sb.append((char)c);
 *			}
 *			System.out.println("Port 2: Read from serial port two: " + sb.toString());
 *
 *			array = "this is a message from two to one\n".getBytes();
 *			osTwo.write(array);
 *			sb = new StringBuffer();
 *			while((c = isOne.read()) != 0xa) {
 *				sb.append((char)c);
 *			}
 *			System.out.println("Port 1: Read from serial port: " + sb.toString());
 *
 *			isOne.close();
 *			osOne.close();
 *			isOne = null;
 *	 		osOne = null;
 *			isTwo.close();
 *			osTwo.close();
 *			isTwo = null;
 *			osTwo = null;
 *			connOne.close();
 *			connOne = null;
 *			connTwo.close();
 *			connTwo = null;
 *		} catch (Exception e) {
 *			e.printStackTrace();
 *		}
 * </pre>
 *
 * Note: avoid blocking read (InputStream.read()) if the InputStream can be closed on a different thread,
 * in this case, the read will never exit and the thread will be blocked forever.
 *
 * It is preferable to test InputStream.available before InputStream.read():
 *
 * <pre>
 *		if (isOne.available() != 0) {
 *			c = isOne.read();
 *		} else {
 *		try {
 *			Thread.sleep(100);
 *			continue;
 *		} catch (InterruptedException e) {
 *			return;
 *		}
 * </pre>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface CommConnection extends StreamConnection {

    /**
     * Returns the URI for this connection.
     *
     * @return this connection URI
     */
    public CommURI getURI();

    /**
     * Sends and array of bytes to a CommConnection
     *
     * @param message
     *            the array of bytes to send to the CommConnection
     * @throws KuraException
     * @throws IOException
     */
    public void sendMessage(byte[] message) throws KuraException, IOException;

    /**
     * Sends and array of bytes to a CommConnection and returns an array of bytes
     * that represents the 'response' to the command. If the timeout is exceeded
     * before any bytes are read on the InputStream null is returned. This is
     * meant to be used in common command/response type situations when communicating
     * with serial devices
     *
     * @param command
     *            the array of bytes to send to the CommConnection
     * @param timeout
     *            the maximum length of time to wait before returning a null
     *            response in the event no response is ever returned.
     * @return an array of bytes representing the response
     * @throws KuraException
     * @throws IOException
     */
    public byte[] sendCommand(byte[] command, int timeout) throws KuraException, IOException;

    public byte[] sendCommand(byte[] command, int timeout, int demark) throws KuraException, IOException;

    /**
     * Reads all bytes that are waiting in the serial port buffer and returns them in
     * an array. This can be used to read unsolicited messages from an attached
     * serial device.
     *
     * @return the array of bytes buffered on the InputStream if any
     * @throws KuraException
     * @throws IOException
     */
    public byte[] flushSerialBuffer() throws KuraException, IOException;

    @Override
    public void close() throws IOException;
}
