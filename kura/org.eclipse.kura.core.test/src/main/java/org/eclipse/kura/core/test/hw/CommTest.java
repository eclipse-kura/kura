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
package org.eclipse.kura.core.test.hw;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.service.io.ConnectionFactory;

public class CommTest {
	
	private static CountDownLatch s_dependencyLatch = new CountDownLatch(1);	// initialize with number of dependencies
	private static ConnectionFactory s_connectionFactory;
	//private static String SERIAL_PORT_NAME ="/dev/tty.PL2303-00001004";
	private static final String SERIAL_PORT_NAME ="/dev/ttyUSB0";
	
	@BeforeClass
	public static void setUp() {
		// Wait for OSGi dependencies
		try {
			if (!s_dependencyLatch.await(5, TimeUnit.SECONDS)) {
				fail("OSGi dependencies unfulfilled");
			}
		} catch (InterruptedException e) {
			fail("Interrupted waiting for OSGi dependencies");
		}
	}
	
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		CommTest.s_connectionFactory = connectionFactory;
		s_dependencyLatch.countDown();
	}
	
	public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
		CommTest.s_connectionFactory = null;
	}
	
	@Test
	public void testServiceExists() {
		assertNotNull(CommTest.s_connectionFactory);
	}
	
	//Dont run these hardware tests in CloudBees!!!
	@Test
	@Ignore
	public void testCommConnection()
		throws Exception
	{		
		String uri = new CommURI.Builder(SERIAL_PORT_NAME)
									.withBaudRate(19200)
									.withDataBits(8)
									.withStopBits(1)
									.withParity(0)
									.withTimeout(2000)
									.build().toString();
		
		CommConnection conn = (CommConnection) s_connectionFactory.createConnection(uri, 1, false);
		assertNotNull(conn);
		
		OutputStream os = conn.openOutputStream();
		os.write(10);
		os.write("hello".getBytes(), 0, "hello".getBytes().length);
		os.flush();
		
		os.close();
		os = null;
		conn.close();
		conn = null;
	}
	
	@Test
	@Ignore
	public void testOpenSerialPort() {
		try {
			String uri = new CommURI.Builder(SERIAL_PORT_NAME)
										.withBaudRate(19200)
										.withDataBits(8)
										.withStopBits(1)
										.withParity(0)
										.withTimeout(2000)
										.build().toString();
			CommConnection conn = (CommConnection) CommTest.s_connectionFactory.createConnection(uri, 1, false);
			assertNotNull(conn);

			InputStream is = conn.openInputStream();
			OutputStream os = conn.openOutputStream();
			
			assertNotNull(is);
			assertNotNull(os);
			
			is.close();
			os.close();
			is = null;
			os = null;
			conn.close();
			conn = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	@Ignore
	public void testWriteToPort() {
		try {
			String uri = new CommURI.Builder(SERIAL_PORT_NAME)
										.withBaudRate(19200)
										.withDataBits(8)
										.withStopBits(1)
										.withParity(0)
										.withTimeout(2000)
										.build().toString();
			CommConnection conn = (CommConnection) CommTest.s_connectionFactory.createConnection(uri, 1, false);
			assertNotNull(conn);

			OutputStream os = conn.openOutputStream();
			
			assertNotNull(os);
			
			os.write("this is a set of test data coming from a junit test in Kura\r\n".getBytes());
			os.flush();
			
			os.close();
			os = null;
			conn.close();
			conn = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//  CAN'T READ IN AUTOMATED TEST
	@Test
	@Ignore
	public void testReadFromPort() {
		try {
			String uri = new CommURI.Builder(SERIAL_PORT_NAME)
										.withBaudRate(19200)
										.withDataBits(8)
										.withStopBits(1)
										.withParity(0)
										.withTimeout(2000)
										.build().toString();
			CommConnection conn = (CommConnection) CommTest.s_connectionFactory.createConnection(uri, 1, false);
			assertNotNull(conn);
			
			InputStream is = conn.openInputStream();
			
			assertNotNull(is);
			
			System.out.println("waiting for serial port input - make sure to end this with a <ENTER>...");			
			StringBuffer sb = new StringBuffer();
			int c;
			while((c = is.read()) != 0xd) {
				System.out.println("read byte: 0x" + Integer.toHexString(c) + " -> " + (char)c);
				sb.append((char)c);
			}
			
			System.out.println("Read from serial port: " + sb.toString());
			
			is.close();
			is = null;
			conn.close();
			conn = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	@Ignore
	public void testTwoPorts() {
		try {
			String uri = new CommURI.Builder(SERIAL_PORT_NAME)
										.withBaudRate(19200)
										.withDataBits(8)
										.withStopBits(1)
										.withParity(0)
										.withTimeout(2000)
										.build().toString();
			CommConnection connOne = (CommConnection) CommTest.s_connectionFactory.createConnection(uri, 1, false);
			assertNotNull(connOne);
			uri = new CommURI.Builder(SERIAL_PORT_NAME)
										.withBaudRate(19200)
										.withDataBits(8)
										.withStopBits(1)
										.withParity(0)
										.withTimeout(2000)
										.build().toString();
			CommConnection connTwo = (CommConnection) CommTest.s_connectionFactory.createConnection(uri, 1, false);
			assertNotNull(connTwo);
			
			InputStream isOne = connOne.openInputStream();
			OutputStream osOne = connOne.openOutputStream();
			InputStream isTwo = connTwo.openInputStream();
			OutputStream osTwo = connTwo.openOutputStream();
			
			assertNotNull(isOne);
			assertNotNull(osOne);
			assertNotNull(isTwo);
			assertNotNull(osTwo);
			
			//write from one to two
			byte[] array = "this is a message from one to two\n".getBytes();
			osOne.write(array);
			StringBuffer sb = new StringBuffer();
			int c;
			while((c = isTwo.read()) != 0xa) {
				System.out.println("port 2: read byte: 0x" + Integer.toHexString(c) + " -> " + (char)c);
				sb.append((char)c);
			}
			System.out.println("Port 2: Read from serial port two: " + sb.toString());
			
			array = "this is a message from two to one\n".getBytes();
			osTwo.write(array);
			sb = new StringBuffer();
			while((c = isOne.read()) != 0xa) {
				System.out.println("port 1: read byte: 0x" + Integer.toHexString(c) + " -> " + (char)c);
				sb.append((char)c);
			}
			System.out.println("Port 1: Read from serial port: " + sb.toString());
			
			isOne.close();
			osOne.close();
			isOne = null;
			osOne = null;
			isTwo.close();
			osTwo.close();
			isTwo = null;
			osTwo = null;
			connOne.close();
			connOne = null;
			connTwo.close();
			connTwo = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
