/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.linux.udev;

import org.eclipse.kura.usb.UsbDevice;
import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface LinuxUdevListener {

    /**
     * Callback for notifications of new UsbDevice ATTACH events
     *
     * @param device
     *            The UsbDevice that was just attached
     */
    void attached(UsbDevice device);

    /**
     * Callback for notifications of new UsbDevice DETACH events
     *
     * @param device
     *            The UsbDevice that was just detached
     */
    void detached(UsbDevice device);
}
