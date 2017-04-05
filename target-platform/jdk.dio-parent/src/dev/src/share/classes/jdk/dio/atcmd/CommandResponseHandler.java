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

package jdk.dio.atcmd;

/**
 * The {@code CommandResponseHandler} interface defines methods for handling responses to AT commands.
 * <p />
 * When commands are submitted asynchronously using the
 * {@link ATDevice#sendCommand(java.lang.String, jdk.dio.atcmd.CommandResponseHandler)
 * sendCommand(String, CommandResponseHandler)} a {@code CommandResponseHandler} instance must be provided to handle the
 * response when available.
 * <p />
 * Only information text, final result code and intermediate result code responses can be handled by a
 * {@code CommandResponseHandler} instance. Unsolicited result code responses can be handled by a
 * {@link UnsolicitedResponseHandler} instance.
 * <p />
 * A {@code CommandResponseHandler} should not throw any unchecked exception. A compliant implementation of this
 * specification MUST catch unchecked exceptions that may be thrown by a {@code CommandResponseHandler}.
 *
 * @see ATDevice
 * @since 1.0
 */
public interface CommandResponseHandler {

    /**
     * Invoked to process an information text, final result code or intermediate result code response.
     *
     * @param atDevice
     *            the {@code ATDevice} instance issuing the response.
     * @param response
     *            the response to handle or {@code null} if the response was too long or in case of an un-handled error.
     * @return complementary input if an intermediate result code requiring input (such as a prompt) was provided; or
     *         {@code null} if no additional input is required (such as final result code was received).
     */
    String processResponse(ATDevice atDevice, String response);
}
