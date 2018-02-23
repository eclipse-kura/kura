/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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
