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
package org.eclipse.kura.net.admin.util;

import static java.util.Objects.isNull;

import org.apache.commons.lang3.math.NumberUtils;

public class PppPdpUtil {
    
    private PppPdpUtil() {
    }

    public static int getPdpContextNumber(String dialString) {
        String pdpNum = "1";
        if (!isNull(dialString) && !dialString.isEmpty() && dialString.toLowerCase().startsWith("atd*99***")) {
            pdpNum = dialString.substring("atd*99***".length(), dialString.length() - 1);
        }

        return NumberUtils.toInt(pdpNum, 1);
    }
}