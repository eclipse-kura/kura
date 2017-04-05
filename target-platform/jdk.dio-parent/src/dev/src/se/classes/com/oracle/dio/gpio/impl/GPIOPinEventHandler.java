/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.dio.gpio.impl;

import jdk.dio.gpio.PinEvent;
import jdk.dio.gpio.PinListener;
import java.nio.Buffer;

final class GPIOPinEventHandler extends GPIOEventHandler<GPIOPinImpl> {

    private static final GPIOPinEventHandler instance = new GPIOPinEventHandler();

    private GPIOPinEventHandler() {
        super(InternalEvent.class);
    }

    static GPIOPinEventHandler getInstance() {
        return instance;
    }

    static {
        setNativeEntries(instance.queue.getNativeBuffer(), InternalEvent.class);
    }

    protected void handleGPIOEvent(Object listener, GPIOPinImpl pin, GPIOEvent event) {
        boolean value = ((InternalEvent)event).getValue();
        PinListener pinListener = (PinListener)listener;
        pinListener.valueChanged(new PinEvent(pin, value));
    }

    public static class InternalEvent extends GPIOEvent {
        boolean getValue() {
            byte[] payload = getPayload();
            int value = ((int)(0x00ff & payload[4]) << 24) |
                        ((int)(0x00ff & payload[5]) << 16) |
                        ((int)(0x00ff & payload[6]) << 8)  |
                        ((int)(0x00ff & payload[7]));
            return (value != 0);
        }
    }

    private static native void setNativeEntries(Buffer buffer, Class<InternalEvent> eventClass);
}
