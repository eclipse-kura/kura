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
import java.io.IOException;

/* GPIOLEDSample
 * This class demonstrates how to use the use an output GPIO pin to control an LED.
 * For wiring instructions see Device I/O wiki page at https://wiki.openjdk.java.net/display/dio/Getting+Started.
 */
public class GPIOLEDSample {

    public static void blinkLEDByGPIONumber(int pinNumber) {
        System.out.println("Blinking LED on GPIO pin number " + pinNumber);
        GPIOPin pin = null;
        try {
            GPIOPinConfig pinConfig = new GPIOPinConfig(DeviceConfig.DEFAULT,
                                                        pinNumber,
                                                        GPIOPinConfig.DIR_OUTPUT_ONLY,
                                                        GPIOPinConfig.MODE_OUTPUT_PUSH_PULL,
                                                        GPIOPinConfig.TRIGGER_NONE,
                                                        false);
            pin = (GPIOPin)DeviceManager.open(GPIOPin.class, pinConfig);
            boolean pinOn = false;
            for (int i = 0; i < 20; i++) {
                Thread.sleep(2500);
                pinOn = !pinOn;
                pin.setValue(pinOn);
            }
        } catch (IOException ioe) {
            throw new LEDExampleException("IOException while opening device. Make sure you have the appropriate operating system permission to access GPIO devices.", ioe);
        } catch(InterruptedException ie) {
            // ignore
        } finally {
            try {
                System.out.println("Closing GPIO pin number " + pinNumber);
                if (pin != null) {
                    pin.close();
                }
            } catch (Exception e) {
                throw new LEDExampleException("Received exception while trying to close device.", e);
            }
        }
    }

    public static void blinkLEDByDeviceId(int deviceId) {
        System.out.println("Blinking LED on device ID " + deviceId);
        GPIOPin pin = null;
        try {
            pin = (GPIOPin)DeviceManager.open(deviceId);
            boolean pinOn = false;
            for (int i = 0; i < 20; i++) {
                Thread.sleep(2500);
                pinOn = !pinOn;
                pin.setValue(pinOn);
            }
        } catch (IOException ioe) {
            throw new LEDExampleException("IOException while opening device. Make sure you have the appropriate operating system permission to access GPIO devices.", ioe);
        } catch(InterruptedException ie) {
            // ignore
        } finally {
            try {
        System.out.println("Closing device ID " + deviceId);
                if (pin != null) {
                    pin.close();
                }
            } catch (Exception e) {
                throw new LEDExampleException("Received exception while trying to close device.", e);
            }
        }
    }

    public static void main(String[] args) {
        // First, blink the LED by specifying the GPIO pin number
        blinkLEDByGPIONumber(18);
        // Next, blink the LED by looking up the device associated with
        // the device id
        blinkLEDByDeviceId(18);
    }
}

class LEDExampleException extends RuntimeException {
    public LEDExampleException(String msg, Throwable t) {
        super(msg,t);
    }
}
