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

package jdk.dio;

import java.nio.Buffer;

/**
 * The {@code InputRoundListener} interface defines methods for getting notified of the completion
 * of an input round. An {@code InputRoundListener} gets invoked for each of the successive rounds
 * of an input operation when the input buffer has been filled in and input data is available for
 * processing.
 * <p />
 * This interface also extends the {@link AsyncErrorHandler} interface for getting notified of
 * asynchronous I/O errors.
 *
 * @param <P>
 *            the device type that generates the event to listener for.
 * @param <B>
 *            the input buffer type.
 * @since 1.0
 */
public interface InputRoundListener<P extends Device<? super P>, B extends Buffer> extends DeviceEventListener,
        AsyncErrorHandler<P> {

    /**
     * Invoked when an input buffer has been filled and input data is available for processing or
     * when an input overrun error occurred.
     *
     * @param event
     *            the event that occurred.
     */
    void inputRoundCompleted(RoundCompletionEvent<P, B> event);
}