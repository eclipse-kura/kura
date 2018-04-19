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
package com.oracle.dio.uart.impl;

import com.oracle.dio.impl.EventQueue;
import com.oracle.dio.impl.EventHandler;
import com.oracle.dio.impl.Event;
import java.util.Hashtable;
import java.nio.Buffer;

class UARTEventHandler implements EventHandler {

    private static class UARTHash {
        final long port;
        final int eventType;
        final int hash;

        UARTHash(long port, int eventType) {
            this.port = port;
            this.eventType = eventType;
            long lHash = 17 + port;
            lHash = 17 * lHash + eventType;
            hash = (int)lHash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof UARTHash) {
                return (((UARTHash)obj).port == port &&
                        ((UARTHash)obj).eventType == eventType);
            } else {
                return super.equals(obj);
            }
        }
    }

    private final Hashtable<UARTHash, UARTImpl> listenerRegistry = new Hashtable();
    private static final UARTEventHandler instance = new UARTEventHandler();
    private static final int QUEUE_BUFFER_SIZE = 4096;
    private final EventQueue queue =
                             EventQueue.createEventQueue(QUEUE_BUFFER_SIZE);

    static UARTEventHandler getInstance() {
        return instance;
    }

    private UARTEventHandler() {
        queue.registerForEvent(UARTEvent.class, this);
    }

    void addEventListener(int eventType, UARTImpl uart) {
        if (null == uart) {
            throw new NullPointerException("uart == null");
        }
        long port = uart.getHandle().getNativeHandle();
        listenerRegistry.put(new UARTHash(port, eventType), uart);
    }

    UARTImpl getEventListener(int port, int eventType) {
        return listenerRegistry.get(new UARTHash(port, eventType));
    }

    void removeEventListener(int eventType, UARTImpl uart) {
        long port = uart.getHandle().getNativeHandle();
        listenerRegistry.remove(new UARTHash(port, eventType));
    }

    protected static class UARTEvent extends Event {
        UARTEvent(byte[] payload) {
            super(payload);
        }

        public UARTEvent() {
            super();
        }

        int getPort() {
            byte[] payload = getPayload();
            int port = (((int)(0x00ff & payload[0])) << 24) | (((int)(0x00ff & payload[1])) << 16) |
                       (((int)(0x00ff & payload[2])) << 8 ) | (((int)(0x00ff & payload[3])));
            return port;
        }
        int getEventType() {
            byte[] payload = getPayload();
            int type = (((int)(0x00ff & payload[4])) << 24) | (((int)(0x00ff & payload[5])) << 16) |
                       (((int)(0x00ff & payload[6]) << 8 ) | (((int)(0x00ff & payload[7]))));
            return type;
        }
        int getBytesProcessed() {
            byte[] payload = getPayload();
            int bytes = (((int)(0x00ff & payload[8]))  << 24) | (((int)(0x00ff & payload[9]) << 16)) |
                        (((int)(0x00ff & payload[10])) << 8 ) | (((int)(0x00ff & payload[11])));
            return bytes;
        }
    }

    public void sendTimeoutEvent(long handle) {
        int type = jdk.dio.uart.UARTEvent.INPUT_DATA_AVAILABLE;
        int bytes = -1;
        byte[] payload = new byte[] {
                (byte)(handle >> 56), (byte)(handle >> 48), (byte)(handle >> 40), (byte)(handle >> 32),
                (byte)(handle >> 24), (byte)(handle >> 16), (byte)(handle >> 8), (byte)(handle),
                (byte)(type >> 24), (byte)(type >> 16), (byte)(type >> 8), (byte)(type),
                (byte)(bytes >> 24), (byte)(bytes >> 16), (byte)(bytes >> 8), (byte)(bytes),
            };
        UARTEvent event = new UARTEvent(payload);
        queue.postEvent(event);
    }

    /**
     * This method is called by EventQueue.dispatch(). Each call is made on a
     * separate thread.
     * @param event a previously queued event to handle
     */
    public boolean handleEvent(Event event) {
        UARTEvent e = (UARTEvent)event;
        int port = e.getPort();
        int type = e.getEventType();
        int bytesProcessed = e.getBytesProcessed();
        UARTImpl uart = listenerRegistry.get(new UARTHash(port, type));
        if (uart != null) {
            uart.processEvent(type, bytesProcessed);
        }
        return true;
    }

    static {
        setNativeEntries(instance.queue.getNativeBuffer(), UARTEvent.class);
    }

    private static native void setNativeEntries(Buffer buffer,
                                                Class<UARTEvent> eventClass);
}
