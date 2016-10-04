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

public class RxTxTest {

    /**
     * For this to work on a MAC you must have enabled serial support:
     * Install this driver if using a PL2303 USB to Serial device: http://sourceforge.net/projects/osx-pl2303/
     * Run these commands to enable locking for RXTX:
     * sudo mkdir /var/lock
     * sudo chmod 777 /var/lock
     * Find your device node with 'ls -l /dev/*PL*' and replace it as needed below in the com port identifier call
     * This is commented out because it will not work on cloudbees (or anything without a physical comm port)
     */
    /*
     * @Test
     * public void serialTest() {
     *
     * try {
     * Enumeration ports = CommPortIdentifier.getPortIdentifiers();
     * while (ports.hasMoreElements()) {
     * CommPortIdentifier port = (CommPortIdentifier)ports.nextElement();
     * String type;
     * switch (port.getPortType()) {
     * case CommPortIdentifier.PORT_PARALLEL:
     * type = "Parallel";
     * break;
     * case CommPortIdentifier.PORT_SERIAL:
     * type = "Serial";
     * break;
     * default: /// Shouldn't happen
     * type = "Unknown";
     * break;
     * }
     * System.out.println(port.getName() + ": " + type);
     * }
     * } catch(Exception e) {
     * e.printStackTrace();
     * }
     *
     * CommPortIdentifier portIdentifier = null;
     * InputStream in = null;
     * OutputStream out = null;
     *
     * try {
     * portIdentifier = CommPortIdentifier.getPortIdentifier("/dev/ttyUSB0");
     * } catch (NoSuchPortException e) {
     * e.printStackTrace();
     * }
     * if ( portIdentifier.isCurrentlyOwned() ) {
     * System.out.println("Error: Port is currently in use");
     * }
     * else {
     * CommPort commPort = null;
     * try {
     * commPort = portIdentifier.open(this.getClass().getName(),2000);
     * } catch (PortInUseException e) {
     * e.printStackTrace();
     * }
     *
     * if ( commPort instanceof SerialPort ) {
     * SerialPort serialPort = (SerialPort) commPort;
     * try {
     * serialPort.setSerialPortParams(57600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
     * } catch (UnsupportedCommOperationException e) {
     * e.printStackTrace();
     * }
     *
     * try {
     * in = serialPort.getInputStream();
     * } catch (IOException e) {
     * e.printStackTrace();
     * }
     * try {
     * out = serialPort.getOutputStream();
     * } catch (IOException e) {
     * e.printStackTrace();
     * }
     *
     * Assert.assertNotNull(in);
     * Assert.assertNotNull(out);
     *
     * try {
     * in.close();
     * out.close();
     * in = null;
     * out = null;
     * serialPort.close();
     * serialPort = null;
     * } catch (IOException e) {
     * e.printStackTrace();
     * }
     * }
     * else {
     * System.out.println("Error: Only serial ports are handled by this example.");
     * }
     * }
     * }
     */
}
