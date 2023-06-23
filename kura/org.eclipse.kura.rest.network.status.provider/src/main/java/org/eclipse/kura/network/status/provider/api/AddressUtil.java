/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.network.status.provider.api;

public class AddressUtil {

    private AddressUtil() {
    }

    public static String formatHardwareAddress(final byte[] value) {
        final StringBuilder result = new StringBuilder();

        for (int i = 0; i < value.length; i++) {
            result.append(String.format("%02X", value[i] & 0xff));

            if (i < value.length - 1) {
                result.append(':');
            }
        }

        return result.toString();
    }
}