/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.dhcp;

public enum DhcpServerTool {
    NONE("none"),
    DHCPD("dhcpd"),
    UDHCPD("udhcpd");

    private String toolName;

    private DhcpServerTool(String toolName) {
        this.toolName = toolName;
    }

    public String getValue() {
        return this.toolName;
    }
}
