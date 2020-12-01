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
package org.eclipse.kura.system;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Service to perform basic system tasks.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface SystemAdminService {

    /**
     * Gets the amount of time this device has been up in milliseconds.
     *
     * @return How long this device has been up in milliseconds.
     */
    public String getUptime();

    /**
     * Reboots the device.
     */
    public void reboot();

    /**
     * Synchronizes data on flash with memory.
     */
    public void sync();
}
