/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.huawei;

public enum HuaweiModemAtCommands {

    getICCID("at^iccid?\r\n"),
    getSimType("at^cardmode\r\n"),
    disableURC("at^curc=0\r\n");

    private String command;

    HuaweiModemAtCommands(String command) {
        this.command = command;
    }

    public String getCommand() {
        return this.command;
    }

}
