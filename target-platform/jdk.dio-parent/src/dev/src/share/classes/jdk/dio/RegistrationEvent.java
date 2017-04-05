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

import java.util.EventObject;

/**
 * The {@code RegistrationEvent} class encapsulates device registration and unregistration
 * conditions.
 *
 * @param <P>
 *            the device type the event is defined for.
 * @see DeviceManager
 * @see RegistrationListener
 * @since 1.0
 */
public class RegistrationEvent<P extends Device<? super P>> extends EventObject {
    /**
     * The identifying and descriptive information of the registered or unregistered device.
     */
    private DeviceDescriptor<P> descriptor;

    /**
     * Creates a new {@link RegistrationEvent} with the specified device descriptor and
     * initiator.
     *
     * @param initiator
     *            the free-formed name of the initiator of the registration or unregistration; or
     *            {@code null} if none is defined.
     * @param descriptor
     *            the identifying and descriptive information of the registered or unregistered
     *            device.
     * @throws NullPointerException
     *             if {@code descriptor} is {@code null}.
     */
    public RegistrationEvent(String initiator, DeviceDescriptor<P> descriptor) {
        super(initiator);
        this.descriptor = descriptor;
    }

    /**
     * Creates a new {@link RegistrationEvent} with the specified device descriptor and
     * initiator.
     *
     * @param descriptor
     *            the identifying and descriptive information of the registered or unregistered
     *            device.
     * @throws NullPointerException
     *             if {@code descriptor} is {@code null}.
     */
    public RegistrationEvent(DeviceDescriptor<P> descriptor) {
        this("Runtime", descriptor);
    }

    /**
     * Returns the identifying and descriptive information of the registered or unregistered
     * device.
     *
     * @return the device descriptor.
     */
    public DeviceDescriptor<P> getDescriptor() {
        return descriptor;
    }

    /**
     * Returns the free-formed name of the registration/unregistration initiator.
     *
     * @return a {@code String} name identifying the initiator; or {@code null} if none is defined.
     */
    public String getInitiator() {
        return (String) getSource();
    }
}