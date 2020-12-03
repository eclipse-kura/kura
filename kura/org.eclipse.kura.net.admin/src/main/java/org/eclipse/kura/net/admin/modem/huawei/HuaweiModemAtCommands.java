/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.net.admin.modem.huawei;

public enum HuaweiModemAtCommands {

    GET_ICCID("at^iccid?\r\n"),
    GET_SIM_TYPE("at^cardmode\r\n"),
    DISABLE_URC("at^curc=0\r\n");

    private String command;

    HuaweiModemAtCommands(String command) {
        this.command = command;
    }

    public String getCommand() {
        return this.command;
    }

}
