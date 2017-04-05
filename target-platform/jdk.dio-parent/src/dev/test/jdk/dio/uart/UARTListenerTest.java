/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package dio.uart;

import com.sun.javatest.Status;
import com.sun.javatest.Test;
import static dio.shared.TestBase.STATUS_OK;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.Policy;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jdk.dio.DeviceMgmtPermission;
import jdk.dio.uart.ModemUART;
import jdk.dio.uart.UART;
import jdk.dio.uart.UARTEvent;
import jdk.dio.uart.UARTEventListener;
import jdk.dio.uart.UARTPermission;

/**
 *
 * @test
 * @sources UARTListenerTest.java
 * @executeClass dio.uart.UARTListenerTest
 *
 * @title UART listener verification
 *
 * @author stanislav.smirnov@oracle.com
 */
public class UARTListenerTest extends UARTTestBase implements Test, UARTEventListener {

    volatile boolean result = true;

    UART uart = null;
    volatile int targetEventID;
    volatile boolean targetEventCaptured = false;

    /**
     * Standard command-line entry point.
     *
     * @param args command line args (ignored)
     */
    public static void main(String[] args) {
        PrintWriter err = new PrintWriter(System.err, true);
        Test t = new UARTListenerTest();
        Status s = t.run(args, null, err);
        s.exit();
    }

    /**
     * Main test method. The test consists of a series of test cases; the test
     * passes only if all the individual test cases pass.
     *
     * @param args ignored
     * @param out ignored
     * @param err a stream to which to write details about test failures
     * @return a Status object indicating if the test passed or failed
     */
    @Override
    public Status run(String[] args, PrintWriter out, PrintWriter err) {
        if(!decodeConfig(args)){
            return printFailedStatus("Error occured while decoding input arguments");
        }


        Policy.setPolicy(new UARTPolicy(
                new DeviceMgmtPermission("*:*", "open"),
                new UARTPermission("*:*", "open")));

        List<Integer> testResult = new LinkedList<>();

        testResult.add(testUARTListenerBufferEmpty().getType());

        testResult.add(testUARTListenerDataAvailableByTimeout().getType());

        testResult.add(testUARTListenerDataAvailableByTrigger().getType());

        testResult.add(testUARTListenerBufferOverrun().getType());

        testResult.add(testUARTListenerBreak().getType());

        return (testResult.contains(Status.FAILED)
                ? printFailedStatus("some test cases failed") : Status.passed("OK"));
    }

    /**
     * Testcase to verify uart OUTPUT_BUFFER_EMPTY event
     * @return Status passed/failed
     */
    private Status testUARTListenerBufferEmpty() {
        Status testResult = Status.passed(STATUS_OK);

        start("Verify UART listener OUTPUT_BUFFER_EMPTY event");
        Config initialConfig;

        result = true;
        this.targetEventID = UARTEvent.OUTPUT_BUFFER_EMPTY;
        targetEventCaptured = false;

        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            try {
                initialConfig = devicesList.next();
                uart = (initialConfig.isModemUART() ? (ModemUART) initialConfig.open() : (UART) initialConfig.open());
                uart.setEventListener(targetEventID, this);

                ByteBuffer bb = ByteBuffer.wrap(new byte[8]);
                bb.position(bb.limit());

                try {
                    uart.write(bb);
                    System.out.println("Have written an \"empty\" ByteBuffer");
                    try {
                        Thread.sleep(1000 * 2);
                    } catch (InterruptedException ex) {
                    }
                } catch (IOException ex) {
                    result = false;
                    testResult = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
                }

                if (!targetEventCaptured) {
                    result = false;
                    testResult = printFailedStatus("Expected " + printEventType(targetEventID) + " was NOT captured");
                } else {
                    System.out.println("Expected " + printEventType(targetEventID) + " WAS captured");
                }
            } catch (UnsupportedOperationException e) {
                testResult = printFailedStatus("Device does not support asynchronous event notification.");
                try {
                    uart.close();
                } catch (IOException ex) {
                    System.out.println("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
                    result = false;
                }
                uart = null;
            } catch (IOException ex) {
                testResult = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
                result = false;
            } finally {
                flush();
            }
        }
        stop();
        return testResult;
    }

    /**
     * Testcase to verify uart INPUT_DATA_AVAILABLE event
     * @return Status passed/failed
     */
    private Status testUARTListenerDataAvailableByTimeout() {
        Status testResult = Status.passed(STATUS_OK);

        start("Verify UART listener INPUT_DATA_AVAILABLE event");
        Config initialConfig;

        result = true;
        this.targetEventID = UARTEvent.INPUT_DATA_AVAILABLE;
        targetEventCaptured = false;

        int timeout = 0;

        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            try {
                initialConfig = devicesList.next();
                uart = (initialConfig.isModemUART() ? (ModemUART) initialConfig.open() : (UART) initialConfig.open());
                uart.setEventListener(targetEventID, this);

                try {
                    timeout = uart.getReceiveTimeout();
                    System.out.println("Successfully got the Receive Timout of the UART: " + timeout);
                } catch (IOException e) {
                    testResult = printFailedStatus(e.getClass().getName() + " happened when trying to get the Receive Timout of the UART");
                    result = false;
                }

                if (timeout == 0) {
                    System.out.println("Receive Timeout is NOT supported by this UART");
                }

                int TIMEOUT = 1000 * 4;
                try {
                    uart.setReceiveTimeout(TIMEOUT);
                    if (timeout == 0) {
                        result = false;
                        testResult = printFailedStatus("The UART does NOT support Receive Timeout, but expected UnsupportedOperationException was NOT thrown when trying to set its Receive Timeout");
                    } else {
                        System.out.println("Successfully set the UART's Receive Timeout to " + TIMEOUT);
                    }
                } catch (UnsupportedOperationException e) {
                    if (timeout == 0) {
                        System.out.println("The UART does NOT support Receive Timeout, AND expected UnsupportedOperationException was thrown when trying to set its Receive Timeout");
                    } else {
                        result = false;
                        testResult = printFailedStatus("The UART supports Receive Timeout, but unexpected UnsupportedOperationException was thrown when trying to set the UART's Receive Timeout to " + TIMEOUT);
                    }
                }

                new MyThread().start();

                try {
                    uart.write(ByteBuffer.wrap(new byte[timeout == 0 ? 8 : 4]));
                    System.out.println("Successfully written out " + (timeout == 0 ? 8 : 4) + " bytes to the UART");
                } catch (IOException e) {
                    testResult = printFailedStatus(e.getClass().getName() + " happened when trying to write out " + (timeout == 0 ? 8 : 4) + " bytes to the UART");
                    result = false;
                }

                try {
                    Thread.sleep(TIMEOUT + 1000 * 1);
                } catch (InterruptedException e) {
                }

                if (!targetEventCaptured) {
                    result = false;
                    testResult = printFailedStatus("Expected " + printEventType(targetEventID) + " was NOT captured");
                } else {
                    System.out.println("Expected " + printEventType(targetEventID) + " WAS captured");
                }
            } catch (IOException ex) {
                testResult = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
                result = false;
            } finally {
                flush();
            }
        }
        stop();
        return testResult;
    }

    /**
     * Testcase to verify uart INPUT_DATA_AVAILABLE event
     * @return Status passed/failed
     */
    private Status testUARTListenerDataAvailableByTrigger() {
        Status testResult = Status.passed(STATUS_OK);

        start("Verify UART listener INPUT_DATA_AVAILABLE event");
        Config initialConfig;

        result = true;
        this.targetEventID = UARTEvent.INPUT_DATA_AVAILABLE;
        targetEventCaptured = false;

        int trigger = 0;

        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            try {
                initialConfig = devicesList.next();
                uart = (initialConfig.isModemUART() ? (ModemUART) initialConfig.open() : (UART) initialConfig.open());
                uart.setEventListener(targetEventID, this);

                try {
                    trigger = uart.getReceiveTriggerLevel();
                    System.out.println("Successfully got the Receive Trigger Level of the UART: " + trigger);
                } catch (IOException e) {
                    testResult = printFailedStatus(e.getClass().getName() + " happened when trying to get the Receive Trigger Level of the UART");
                    result = false;
                }

                if (trigger == 0) {
                    System.out.println("Receive Trigger Level is NOT supported by this UART");
                }

                int TRIGGER = 4;
                try {
                    uart.setReceiveTriggerLevel(TRIGGER);
                    if (trigger == 0) {
                        result = false;
                        testResult = printFailedStatus(""
                                + "The UART does NOT support Receive Trigger Level, "
                                + "but expected UnsupportedOperationException was NOT thrown when trying to set its Receive Trigger Level");
                    } else {
                        System.out.println("Successfully set the UART's Receive Trigger Level to " + TRIGGER);
                    }
                } catch (UnsupportedOperationException e) {
                    if (trigger == 0) {
                        System.out.println(""
                                + "The UART does NOT support Receive Trigger Level, "
                                + "AND expected UnsupportedOperationException was thrown when trying to set its Receive Trigger Level");
                    } else {
                        result = false;
                        testResult = printFailedStatus(""
                                + "The UART supports Receive Timeout, but "
                                + "unexpected UnsupportedOperationException was thrown when trying to set the UART's Receive Trigger Level to " + TRIGGER);
                    }
                }

                new MyThread().start();

                try {
                    uart.write(ByteBuffer.wrap(new byte[trigger == 0 ? 8 : 4]));
                    System.out.println("Successfully written out " + (trigger == 0 ? 8 : 4) + " bytes to the UART");
                } catch (IOException e) {
                    testResult = printFailedStatus(e.getClass().getName() + " happened when trying to write out " + (trigger == 0 ? 8 : 4) + " bytes to the UART");
                    result = false;
                }

                try {
                    Thread.sleep(5000 * 2);
                } catch (InterruptedException e) {
                }

                if (!targetEventCaptured) {
                    result = false;
                    testResult = printFailedStatus("Expected " + printEventType(targetEventID) + " was NOT captured");
                } else {
                    System.out.println("Expected " + printEventType(targetEventID) + " WAS captured");
                }
            } catch (IOException ex) {
                testResult = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
                result = false;
            } finally {
                flush();
            }
        }
        stop();
        return testResult;
    }

    /**
     * Testcase to verify uart INPUT_BUFFER_OVERRUN event
     * @return Status passed/failed
     */
    private Status testUARTListenerBufferOverrun() {
        Status testResult = Status.passed(STATUS_OK);

        start("Verify UART listener INPUT_BUFFER_OVERRUN event");
        Config initialConfig;

        result = true;
        this.targetEventID = UARTEvent.INPUT_BUFFER_OVERRUN;
        targetEventCaptured = false;

        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            try {
                initialConfig = devicesList.next();
                uart = (initialConfig.isModemUART() ? (ModemUART) initialConfig.open() : (UART) initialConfig.open());
                uart.setEventListener(targetEventID, this);

                int i = 0;
                try {
                    // Going to write at most 1024 * 1024 KB = 1 GB to generate UARTEvent.INPUT_BUFFER_OVERRUN
                    for (i = 0; !targetEventCaptured && i < 1024 * 1024; i++) {
                        if (i % 1024 == 0) {
                            System.out.println("Have written out " + i + " KB ...");
                        }
                        uart.write(ByteBuffer.wrap(new byte[1024]));
                    }
                } catch (IOException e) {
                    result = false;
                    testResult = printFailedStatus(e.getClass().getName() + " happened after having written out " + i + " KB");
                }
            } catch (IOException ex) {
                testResult = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
                result = false;
            } finally {
                flush();
            }
        }
        stop();
        return testResult;
    }

    /**
     * Testcase to verify uart BREAK_INTERRUPT event
     * @return Status passed/failed
     */
    private Status testUARTListenerBreak() {
        Status testResult = Status.passed(STATUS_OK);

        start("Verify UART listener BREAK_INTERRUPT event");
        Config initialConfig;

        result = true;
        this.targetEventID = UARTEvent.BREAK_INTERRUPT;
        targetEventCaptured = false;

        int DURATION = 1000;
        boolean notSupported = false;

        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            try {
                initialConfig = devicesList.next();
                uart = (initialConfig.isModemUART() ? (ModemUART) initialConfig.open() : (UART) initialConfig.open());
                uart.setEventListener(targetEventID, this);

                try {
                    uart.generateBreak(DURATION);
                    System.out.println("Successfully generated BREAK_INTERRUPT for: " + DURATION + "ms");
                } catch (UnsupportedOperationException e) {
                    notSupported = true;
                    System.out.println("The UART does NOT support BREAK");
                } catch (IOException e) {
                    testResult = printFailedStatus(e.getClass().getName() + " happened when trying to generated BREAK_INTERRUPT for:" + DURATION + "ms");
                    result = false;
                }

                if (result && !notSupported) {
                    try {
                        Thread.sleep(DURATION + 1000 * 2);
                    } catch (InterruptedException e) {
                    }

                    if (!targetEventCaptured) {
                        result = false;
                        testResult = printFailedStatus("Expected " + printEventType(targetEventID) + " was NOT captured");
                    } else {
                        System.out.println("Expected " + printEventType(targetEventID) + " WAS captured");
                    }
                }
            } catch (IOException ex) {
                testResult = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
                result = false;
            } finally {
                flush();
            }
        }
        stop();
        return testResult;
    }

    @Override
    public void eventDispatched(UARTEvent uarte) {
        if(targetEventID == uarte.getID()) {
            targetEventCaptured = true;
            System.out.println("EventListener captured event " + printEventType(targetEventID));
        }
    }

    public void flush() {
        if(uart != null) {
            try {
                uart.setEventListener(targetEventID, null);
                System.out.println("Successfully removed the event listener for " + printEventType(targetEventID) + " from the UART");
            }  catch(UnsupportedOperationException ex){
                result = false;
                System.out.println("Operation is not supported");
            }  catch(IOException ex) {
                result = false;
                System.out.println("Unexcpected " + ex.getClass().getName() + " was thrown when trying to remove the event listener for " + printEventType(targetEventID) + " from the UART");
            }

            try {
                uart.close();
            }
            catch(IOException ex) {
                System.out.println("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
            uart = null;
        }
    }

    class MyThread extends Thread {
        @Override
        public void run() {
            while(!targetEventCaptured) {
                try {
                    uart.read(ByteBuffer.wrap(new byte[8]));
                }
                catch(IOException ex) {
                    System.out.println("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
                }
            }
        }
    }

}
