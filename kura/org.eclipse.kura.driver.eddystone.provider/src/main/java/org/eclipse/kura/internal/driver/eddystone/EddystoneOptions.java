/**
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 */
package org.eclipse.kura.internal.driver.eddystone;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

/**
 * The Class {@link EddystoneOptions} is responsible to provide all the required
 * configurable options for the Eddystone Driver.<br/>
 * <br/>
 *
 * The property to configure an Eddystone Driver is as follows:
 * <ul>
 * <li>iname</li>
 * </ul>
 */
final class EddystoneOptions {

    private static final String INAME = "iname";
    private final Map<String, Object> properties;

    /**
     * Instantiates a new Eddystone options.
     *
     * @param properties
     *            the properties
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    EddystoneOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");

        this.properties = properties;
    }

    /**
     * Returns the Bluetooth Interface Name to be used
     *
     * @return the Bluetooth Adapter name (i.e. hci0)
     */
    String getBluetoothInterfaceName() {
        String interfaceName = null;
        final Object iname = this.properties.get(INAME);
        if (nonNull(iname) && iname instanceof String) {
            interfaceName = iname.toString();
        }
        return interfaceName;
    }

}
