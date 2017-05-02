/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.

 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dio.gpio;

import java.util.*;
import jdk.dio.*;
import jdk.dio.gpio.*;

/*
    GPIOInputSample

    Demonstrates how to configure and use a GPIOPin for input.

*/
public class GPIOInputSample {

    private static class InputThread extends Thread {
        private int pinNumber;
        private volatile boolean running = true;

        public InputThread(int pinNumber) {
            this.pinNumber = pinNumber;
        }

        public void run() {
            // create the listener class to handle GPIO pin events
            PinListener listener = new PinListener() {
                private int eventCount = 0;
                public void valueChanged(PinEvent event) {
                    System.out.println("Pin event received. Value = " + event.getValue());
                    if (eventCount++ >= 5) {
                        running = false;
                        System.out.println("Event count reached. Stopping");
                    }
                }
            };
            GPIOPin pin = null;

            try {
                System.out.println("Listening to GPIO" + pinNumber);

                // configure and open given GPIO pin for input
                GPIOPinConfig pinConfig = new GPIOPinConfig(0,
                                                            pinNumber,
                                                            GPIOPinConfig.DIR_INPUT_ONLY,
                                                            GPIOPinConfig.DEFAULT,
                                                            GPIOPinConfig.TRIGGER_RISING_EDGE | GPIOPinConfig.TRIGGER_FALLING_EDGE,
                                                            true);
                pin = (GPIOPin)DeviceManager.open(GPIOPin.class, pinConfig);

                // set the listener for the pin
                pin.setInputListener(listener);

                // loop while waiting for events
                while (running) {
                    Thread.sleep(500);
                }
            } catch (InterruptedException ie) {
                // ignore
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    System.out.println("Closing GPIO" + pinNumber);
                    if (pin != null) {
                        pin.close();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void main(String[] args) {
        int pinNumber = 0;

        // get pin number argument
        if (args.length > 0) {
            pinNumber = Integer.parseInt(args[0]);
        } else {
            System.err.println("GPIO pin number required. Exiting");
            System.exit(2);
        }

        InputThread thread = new InputThread(pinNumber);
        thread.start();
    }
}

