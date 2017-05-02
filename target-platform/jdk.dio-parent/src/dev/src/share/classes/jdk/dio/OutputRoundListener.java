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
 * The {@code OutputRoundListener} interface defines methods for getting notified of the completion
 * of an output round. An {@code OutputRoundListener} gets invoked for each of the successive rounds
 * of an output operation when the data has been output and the output buffer is available for
 * copying more data to output.
 * <p />
 * This interface also extends the {@link AsyncErrorHandler} interface for getting notified of
 * asynchronous I/O errors.
 *
 * @param <P>
 *            the device type that generates the event to listener for.
 * @param <B>
 *            the output buffer type.
 * @since 1.0
 */
public interface OutputRoundListener<P extends Device<? super P>, B extends Buffer> extends
        DeviceEventListener, AsyncErrorHandler<P> {

    /**
     * Invoked when the data has been output and the output buffer is available for copying more
     * data to output or when an output underrun error occurred.
     *
     * @param event
     *            the event that occurred.
     */
    void outputRoundCompleted(RoundCompletionEvent<P, B> event);
}
