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

import com.oracle.dio.utils.Constants;
import com.oracle.dio.impl.EventQueue;
import com.oracle.dio.impl.EventHandler;
import com.oracle.dio.impl.Event;
import java.util.*;
import java.nio.Buffer;

/**
 * Serial signal proxy
 * @see SerialSignalListener
 */
class ModemSignalDispatcher implements EventHandler {

    /**
     * Serial signal event listener
     */
    static interface SerialSignalListener {

        /**
         * This method is called on any change of the corresponding signal line of the serial interface.
         *
         * @param signalLine signal line
         * @param state state of the signal
         */
        void signalChanged(int signalLine, boolean state);

    }

    synchronized static ModemSignalDispatcher getInstance() {
        if (instance == null) {
            instance = new ModemSignalDispatcher();
        }
        return instance;
    }

    private static ModemSignalDispatcher instance;
    private static final int QUEUE_BUFFER_SIZE = 4096;
    private final EventQueue queue =
                             EventQueue.createEventQueue(QUEUE_BUFFER_SIZE);

    private ModemSignalDispatcher() {
        queue.registerForEvent(SignalEvent.class, this);
    }

    private static class SerialContext {

        /** Serial signal context */
        final int context;

        /** Serial signal listeners */
        final List<SerialSignalListener> listeners = new ArrayList<SerialSignalListener>();

        SerialContext(int context) {
            this.context = context;
        }
    }

    private Map<Integer, SerialContext> contextMap = new HashMap<Integer, SerialContext>();

    private List<SerialSignalListener> getListeners(int serialHandler) {
        return contextMap.get(serialHandler).listeners;
    }

    private int getContext(int serialHandler) {
        return contextMap.get(serialHandler).context;
    }

    /**
     * Register a listener to receive state changes of a modem signal line,
     *
     * @param serialHandler serial port handler
     * @param listener serial signal listener
     */
    synchronized void addListener(int serialHandler, SerialSignalListener listener) {
        if (! contextMap.containsKey(serialHandler)) {
            int context = startListening(serialHandler);
            contextMap.put(serialHandler, new SerialContext(context));
        }

        List<SerialSignalListener> listeners = getListeners(serialHandler);
        if (! listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a listener,
     *
     * @param serialHandler serial port handler
     * @param listener serial signal listener
     */
    synchronized void removeListener(int serialHandler, SerialSignalListener listener) {
        if (contextMap.containsKey(serialHandler)) {
            List<SerialSignalListener> listeners = getListeners(serialHandler);
            listeners.remove(listener);

            if (listeners.isEmpty()) {
                int context = getContext(serialHandler);
                stopListening(context);
                contextMap.remove(serialHandler);
            }
        }
    }

    /**
     * Starts serial signal listening
     *
     * @param serialHandler serial port handler
     * @return handler of serial signal context
     */
    private int startListening(int serialHandler) {
        return startListening0(serialHandler);
    }

    /**
     * Stops serial signal listening
     *
     * @param context handler of serial signal context
     */
    private void stopListening(int context) {
        if (context != Constants.INVALID_HANDLE) {
            stopListening0(context);
        }
    }

    private static class SignalEvent extends Event {
        int getHandler() {
            byte[] payload = getPayload();
            int handler = (((int)(0x00ff & payload[0])) << 24) | (((int)(0x00ff & payload[1])) << 16) |
                          (((int)(0x00ff & payload[2])) << 8 ) | (((int)(0x00ff & payload[3])));
            return handler;
        }
        int getLine() {
            byte[] payload = getPayload();
            int line = (((int)(0x00ff & payload[4])) << 24) | (((int)(0x00ff & payload[5])) << 16) |
                       (((int)(0x00ff & payload[6])) << 8 ) | (((int)(0x00ff & payload[7])));
            return line;
        }
        boolean getState() {
            byte[] payload = getPayload();
            boolean state =  payload[8] != 0;
            return state;
        }
    }

    /**
     * This method is called by EventQueue.dispatch(). Each call is made on a
     * separate thread.
     * @param event a previously queued event to handle
     */
    public boolean handleEvent(Event event) {
        SignalEvent e = (SignalEvent)event;

        int serialHandler = e.getHandler();
        int signalLine = e.getLine();
        boolean signalState = e.getState();

        synchronized(this) {
            List<SerialSignalListener> listeners = getListeners(serialHandler);
            Iterator<SerialSignalListener> iter = listeners.iterator();
            while (iter.hasNext()) {
                iter.next().signalChanged(signalLine, signalState);
            }
        }
        return true;
    }

    static {
        setNativeEntries(instance.queue.getNativeBuffer(), SignalEvent.class);
    }

    private static native void setNativeEntries(Buffer buffer,
                                                Class<SignalEvent> eventClass);

    /**
     * Starts serial signal listening
     *
     * @param serialHandler serial port handler
     * @return handler of serial signal context
     */
    private native int startListening0(int serialHandler);

    /**
     * Stops serial signal listening
     *
     * @param context handler of serial signal context
     */
    private native void stopListening0(int context);

}
