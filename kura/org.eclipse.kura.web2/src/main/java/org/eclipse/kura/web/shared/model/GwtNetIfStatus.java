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
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum GwtNetIfStatus implements Serializable, IsSerializable {
    netIPv4StatusDisabled("Disabled"),
    netIPv4StatusUnmanaged("Unmanaged"),
    netIPv4StatusL2Only("L2Only"),
    netIPv4StatusEnabledLAN("LAN"),
    netIPv4StatusEnabledWAN("WAN");

    private String status;

    private GwtNetIfStatus(String status) {
        this.status = status;
    }

    public String getValue() {
        return this.status;
    }
}
