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
package dio.uart.io;

import com.sun.javatest.Status;
import com.sun.javatest.Test;
import static dio.shared.TestBase.STATUS_OK;
import dio.uart.Config;
import dio.uart.UARTPolicy;
import dio.uart.UARTTestBase;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.Policy;
import java.util.Iterator;
import jdk.dio.DeviceMgmtPermission;
import jdk.dio.uart.ModemUART;
import jdk.dio.uart.UART;
import jdk.dio.uart.UARTPermission;

/**
 *
 * @test
 * @sources UARTWriteReadTest.java
 * @executeClass dio.uart.io.UARTWriteReadTest
 *
 * @title UART write/read testing
 *
 * @author stanislav.smirnov@oracle.com
 */
public class UARTWriteReadTest extends UARTTestBase implements Test {

    private final byte[] arrayWrite = {32, 34, 36, 44, 46, 48, 50, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50};
    private final byte[] arrayRead = {32, 34, 36, 44, 46, 48, 50, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50};
    final private int LEN_LIMIT = 1024;

    /**
     * Standard command-line entry point.
     *
     * @param args command line args (ignored)
     */
    public static void main(String[] args) {
        PrintWriter err = new PrintWriter(System.err, true);
        Test t = new UARTWriteReadTest();
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

        return (testWriteRead().getType() == Status.FAILED
                ? printFailedStatus("some test cases failed") : Status.passed("OK"));
    }

    /**
     * Testcase to verify uart write/read operations
     * @return Status passed/failed
     */
    private Status testWriteRead(){
        Status result = Status.passed(STATUS_OK);
        Status resultFail = null;
        start("Verify UART write/read methods");
        Config initialConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            try (UART uart = (initialConfig.isModemUART() ? (ModemUART) initialConfig.open() : (UART) initialConfig.open())) {
                System.out.println("UART was open OK");
                byte[] actualArrayRead = new byte[128];
                ByteBuffer src = ByteBuffer.allocateDirect(LEN_LIMIT);
                int totalBytes = 0;
                try {
                    ByteBuffer dst = ByteBuffer.wrap(arrayWrite);
                    uart.write(dst);
                    System.out.println("Data was sent.");
                    int counter = 0;
                    System.out.println("Started reading data ...");
                    while (totalBytes < arrayRead.length) {
                        int readBytes = uart.read(src);
                        System.out.println("Data was read: " + readBytes + " bytes");
                        for (int i = 0; i < readBytes; i++) {
                            actualArrayRead[totalBytes + i] = src.get(i);
                            System.out.println("[" + (totalBytes + i) + "] = " + actualArrayRead[totalBytes + i]);
                        }
                        counter++;
                        if (counter > 100) {
                            break;
                        }
                        totalBytes = totalBytes + readBytes;
                    }
                    System.out.println("Data was read.");
                } catch (IOException ex) {
                    resultFail = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
                }
                System.out.println("Data exchange completed ...");
                if (arrayRead.length < totalBytes) {
                    resultFail = printFailedStatus("Too many bytes received: " + totalBytes);
                }
                if (arrayRead.length > totalBytes) {
                    resultFail = printFailedStatus("Too little bytes received:" + totalBytes);
                }
                for (int ii = 0; ii < totalBytes; ii++) {
                    System.out.println("Got data: [" + ii + "] =" + actualArrayRead[ii] + " expected = " + arrayRead[ii]);
                    if (ii < arrayRead.length) {
                        if (actualArrayRead[ii] != arrayRead[ii]) {
                            resultFail = printFailedStatus("Wrong data received");
                        }
                    }
                }
            } catch (IOException ex) {
                resultFail = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
        }
        stop();
        if (resultFail != null) {
            return resultFail;
        }
        return result;
    }
}
