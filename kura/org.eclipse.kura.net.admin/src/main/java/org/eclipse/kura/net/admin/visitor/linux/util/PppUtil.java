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
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux.util;

import java.util.HashMap;
import java.util.Map;

public class PppUtil {

    private PppUtil() {

    }

    /*
     * Parse ppp peer filename.
     */
    public static Map<String, String> parsePeerFilename(String filename) {
        Map<String, String> props = new HashMap<>();

        String[] parts = filename.split("_");
        switch (parts.length) {
        case 1:
            // Example: HE910 --> serial modem
            props.put("model", parts[0]);
            props.put("modemId", parts[0]);
            break;
        case 2:
            // Example: HE910_1-3.4 --> usb modem
            props.put("model", parts[0]);
            props.put("modemId", parts[1]);
            break;
        default:
        }
        return props;
    }
}
