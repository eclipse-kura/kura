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
package org.eclipse.kura.usb;

import java.util.List;

import javax.usb.UsbServices;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * OSGi interface for getting JSR-80 javax.usb.UsbServices currently available in the system.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface UsbService {

    /**
     * Gets the UsbServices currently available as specified by JSR-80 in the javax.usb
     *
     * @return The currently available javax.usb.UsbServices
     * @throws KuraException
     */
    public UsbServices getUsbServices() throws KuraException;

    /**
     * Gets the USB devices on the gateway
     *
     * @return The currently attached USB devices
     */
    public List<? extends UsbDevice> getUsbDevices();

    /**
     * Gets the USB block devices on the gateway
     *
     * @return The currently attached USB block devices
     */
    public List<UsbBlockDevice> getUsbBlockDevices();

    /**
     * Gets the USB network devices on the gateway
     *
     * @return The currently attached USB network devices
     */
    public List<UsbNetDevice> getUsbNetDevices();

    /**
     * Gets the USB TTY devices on the gateway
     *
     * @return The currently attached USB TTY devices
     */
    public List<UsbTtyDevice> getUsbTtyDevices();

}
