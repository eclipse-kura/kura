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

package jdk.dio.adc;

import jdk.dio.AsyncErrorHandler;
import jdk.dio.DeviceEventListener;

/**
 * The {@code MonitoringListener} interface defines methods for getting notified of ADC channel under- and
 * over-threshold input value conditions as well as device errors. <br />
 * A {@code MonitoringListener} can be registered using the
 * {@link ADCChannel#startMonitoring ADCChannel.startMonitoring} method.
 *
 * @see ADCChannel
 * @since 1.0
 */
public interface MonitoringListener extends DeviceEventListener, AsyncErrorHandler<ADCChannel> {

    /**
     * Invoked when the input value has reached the {@code low} or {@code high} threshold.
     *
     * @param event
     *            the event that occurred.
     */
    void thresholdReached(MonitoringEvent event);

    /**
     * Invoked when an I/O operation fails.
     *
     * @param exception The exception to indicate why the I/O operation failed
     * @param source The {@link ADCChannel} instance that generated the error.
     */
    @Override
    void failed(Throwable exception, ADCChannel source);
}