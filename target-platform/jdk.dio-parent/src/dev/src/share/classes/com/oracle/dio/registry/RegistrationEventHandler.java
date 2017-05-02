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

package com.oracle.dio.registry;
import java.util.HashMap;
import java.util.Map;

import jdk.dio.Device;
import jdk.dio.DeviceDescriptor;
import jdk.dio.RegistrationEvent;
import jdk.dio.RegistrationListener;

public abstract class RegistrationEventHandler<T extends Device> {

    private static RegistrationEventHandler singleton;

    protected Map<Class<T>, RegistrationListener> listeners;

    protected RegistrationEventHandler() {
        listeners = new HashMap<Class<T>, RegistrationListener>();
    }

    public static synchronized <C extends Device> void addListener(RegistrationListener l, Class<C> type) {
        getHandler().startListening(type, l);
    }

    protected static RegistrationEventHandler getHandler() {
        // check null up front. Most invocations will never
        // need to synchronize
        if (null == singleton) {
            synchronized (RegistrationEventHandler.class) {
                // check again now that we've synched on the class
                if (null == singleton) {
                    singleton = new RegistrationEventHandlerImpl();
                }
            }
        }
        return singleton;
    }

    public static synchronized <C extends Device> void removeListener(RegistrationListener l, Class<C> type) {
        getHandler().stopListening(type, l);
    }

    protected void notifyRegistered(DeviceDescriptor dscr) {
        RegistrationEvent event = new RegistrationEvent(dscr);
        RegistrationListener l = listeners.get(dscr.getInterface());
        l.deviceRegistered(event);
    }

    protected void notifyRemoved(DeviceDescriptor dscr) {
        RegistrationEvent event = new RegistrationEvent(dscr);
        RegistrationListener l = listeners.get(dscr.getInterface());
        l.deviceUnregistered(event);
    }

    /**
     * Starts platfrom specific event processing thread
     *
     */
    protected void startListening(Class<T> type, RegistrationListener l) {
        listeners.put(type, l);
    }

    /**
     * Stops platfrom specific event processing thread
     *
     */
    protected void stopListening(Class<T> type, RegistrationListener l) {
        listeners.remove(type);
    }
}

