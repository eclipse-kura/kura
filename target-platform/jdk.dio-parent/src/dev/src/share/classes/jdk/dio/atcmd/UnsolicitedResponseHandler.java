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
 * The {@code UnsolicitedResponseHandler} interface defines methods for handling unsolicited result
 * code responses from an AT device. Unsolicited result codes (such as {@code RING}) indicate the
 * occurrence of an event not directly associated with the issuance of an AT command.
 * <p />
 * To receive unsolicited result codes an {@code UnsolicitedResponseHandler} instance must be
 * registered with the AT device using the
 * {@link ATDevice#setUnsolicitedResponseHandler ATDevice.setUnsolicitedResponseHandler} method.
 * <p />
 * A {@code UnsolicitedResponseHandler} should not throw any unchecked exception. A compliant
 * implementation of this specification MUST catch unchecked exceptions that may be thrown by a
 * {@code UnsolicitedResponseHandler}.
 *
 * @see ATDevice
 * @since 1.0
 */
public interface UnsolicitedResponseHandler {
    /**
     * Invoked to process an unsolicited result code response.
     *
     * @param atDevice
     *            the {@code ATDevice} instance issuing the unsolicited result code.
     * @param code
     *            the unsolicited result code to handle.
     */
    void processResponse(ATDevice atDevice, String code);
}
