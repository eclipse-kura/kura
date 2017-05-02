/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.dio.impl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Event queue implementation.
 */
public class EventQueue {
    private final ByteBuffer        buffer;
    private final LinkedList<Event> queue = new LinkedList<>();
    private final ArrayList<Object> listeners = new ArrayList<>();

    private static final int        SHARED_QUEUE_BUFFER_SIZE = 4096;

    private Thread                  nativeEventThread;
    private Thread                  eventDispatchThread;

    private static class QueueInstanceHolder {
        private static EventQueue sharedEventQueue = new EventQueue(SHARED_QUEUE_BUFFER_SIZE);
    }

    private EventQueue(int bufferSize) {
        buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.position(0).limit(0);
        startQueue();
    }

    /**
     * This method creates new event queue.
     * @param bufferSize size of the native buffer in bytes
     * @return event queue
     */
    public static EventQueue createEventQueue(int bufferSize) {
        return new EventQueue(bufferSize);
    }

    /**
     * This methods returns the reference to the shared event queue. Actual queue
     * is lazily created and started upon first call of this method.
     * @return event queue
     */
    public static EventQueue getSharedEventQueue() {
        return QueueInstanceHolder.sharedEventQueue;
    }

    private class NativeMethodThread implements Runnable {
        @Override
        public void run() {
            synchronized (buffer) {
                try {
                    while (true) {
                        while (buffer.hasRemaining()) {
                            Class<? extends Event> eventClass = getEventClass(buffer, buffer.position());
                            byte[] payload = null;
                            byte hibyte = buffer.get();
                            byte lobyte = buffer.get();
                            short len = (short)((0xff00 & (hibyte << 8)) | (lobyte & 0xff));
                            if (len > 0) {
                                payload = new byte[len];
                                buffer.get(payload);
                            }
                            try {
                                Event e = eventClass.newInstance();
                                e.setPayload(payload);
                                postEvent(e);
                            } catch (InstantiationException | IllegalAccessException ex) {
                                // do nothing, just skip
                            }
                        }
                        buffer.position(0).limit(0);
                        buffer.wait();
                    }
                } catch (InterruptedException ex) {
                    // do nothing on interrupt
                }
            }
        }
    }

    private class EventDispatchThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                synchronized (queue) {
                    try {
                        if (queue.isEmpty()) {
                            queue.wait();
                        }

                        while (!queue.isEmpty()) {
                            Event evt = queue.poll();
                            if (evt != null) {
                                dispatch(evt);
                            }
                        }
                    } catch (InterruptedException ex) {
                        // do something
                    }
                }
            }
        }
    }


    /**
     * This method posts event to be asynchronously dispatched. Can be used by
     * Java components.
     * @param evt event
     */
    public void postEvent(Event evt) {
        if (evt == null) {
            throw new IllegalArgumentException();
        }

        synchronized (queue) {
            queue.add(evt);
            queue.notify();
        }
    }

    /**
     * Registers listener for event with specified class.
     * @param <T> event class
     * @param evtClass class object of event class
     * @param handler listener
     */
    public <T extends Event> void registerForEvent(Class<T> evtClass, EventHandler handler) {
        if (evtClass == null || handler == null) {
            throw new IllegalArgumentException();
        }

        synchronized (listeners) {
            listeners.add(evtClass);
            listeners.add(handler);
        }
    }

    private void dispatch(final Event evt) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i += 2) {
                if (listeners.get(i).equals(evt.getClass())) {
                    final EventHandler h = (EventHandler)listeners.get(i+1);
                    new Thread() {
                        @Override
                        public void run() {
                            h.handleEvent(evt);
                        }
                    }.start();
                }
            }
        }
    }

    private void startQueue() {
        nativeEventThread = new Thread(new NativeMethodThread());
        nativeEventThread.setDaemon(true);
        nativeEventThread.start();
        eventDispatchThread = new Thread(new EventDispatchThread());
        eventDispatchThread.setDaemon(true);
        eventDispatchThread.start();
    }

    /**
     * Returns native buffer. This is intended solely for components with native
     * event generating. Java components must not call any methods of the
     * returned buffer object.
     * @return native buffer
     */
    public Buffer getNativeBuffer() {
        return buffer;
    }

    private static native Class<? extends Event> getEventClass(ByteBuffer buffer, int position);
}
