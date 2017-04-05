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

package jdk.dio.counter;

import jdk.dio.AsyncErrorHandler;
import jdk.dio.DeviceEventListener;

/**
 * The {@code CountingListener} interface defines methods for getting notified of pulse counter
 * counting conditions such as counter terminal value reached or counting session time interval
 * expired as well of device errors. A {@code CountingListener} can be registered using the
 * {@link PulseCounter#startCounting(int, long, jdk.dio.counter.CountingListener)}
 * method.
 *
 * @see PulseCounter
 * @since 1.0
 */
public interface CountingListener extends DeviceEventListener, AsyncErrorHandler<PulseCounter> {

    /**
     * Invoked when the pulse count has reached the specified terminal value or the specified
     * counting time interval has expired - whichever happens first.
     *
     * @param event
     *            the event that occurred.
     */
    void countValueAvailable(CountingEvent event);

    /**
     * Invoked when an I/O operation fails.
     *
     * @param exception
     *            The exception to indicate why the I/O operation failed
     * @param source
     *            The {@code PulseCounter} instance that generated the error.
     */
    @Override
    void failed(Throwable exception, PulseCounter source);
}