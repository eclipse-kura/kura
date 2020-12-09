/*******************************************************************************
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
 *******************************************************************************/
package org.eclipse.kura.net.modem;

/**
 * @since 1.4
 */
public enum ModemPdpContextType {
    IP("IP"),
    PPP("PPP"),
    IPV6("IPV6"),
    IPV4IPV6("IPV4IPV6");

    private String contextType;

    private ModemPdpContextType(String contextType) {
        this.contextType = contextType;
    }

    public String getValue() {
        return this.contextType;
    }

    public static ModemPdpContextType getContextType(String str) {
        if ("IP".equals(str)) {
            return IP;
        } else if ("PPP".equals(str)) {
            return PPP;
        } else if ("IPV6".equals(str)) {
            return IPV6;
        } else {
            return IPV4IPV6;
        }
    }
}
