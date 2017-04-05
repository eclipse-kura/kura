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

package jdk.dio.pwm;

import jdk.dio.AsyncErrorHandler;
import jdk.dio.DeviceEventListener;

/**
 * The {@code GenerationListener} interface defines methods for getting notified of pulse generation completion
 * conditions (i.e. maximum generated pulse count value reached) as well as device errors.
 * A {@code GenerationListener} can be registered using
 * one of the {@link PWMChannel#startGeneration} methods.
 *
 * @see PWMChannel#startGeneration(int, int, GenerationListener)
 * @since 1.0
 */
public interface GenerationListener extends DeviceEventListener, AsyncErrorHandler<PWMChannel> {

    /**
     * Invoked when the generated pulse count has reached the maximum value.
     * <p />
     * The corresponding pulse generation operation being completed another <em>asynchronous</em> pulse generation
     * operation may be started from within this method (see {@link PWMChannel#startGeneration}. This may be used to
     * generate a subsequent pulse train with different specifications: pulse period and/or width/duty cycle.
     *
     * @param event
     *            the event that occurred.
     */
    void pulseGenerationCompleted(GenerationEvent event);

    /**
     * Invoked when an I/O operation fails.
     *
     * @param exception The exception to indicate why the I/O operation failed
     * @param source The {@link PWMChannel} instance that generated the error.
     */
    @Override
    void failed(Throwable exception, PWMChannel source);
}