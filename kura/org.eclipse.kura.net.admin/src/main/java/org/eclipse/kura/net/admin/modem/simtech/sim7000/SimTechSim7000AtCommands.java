/*******************************************************************************
 * Copyright (c) 2020 3 PORT d.o.o. and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  3 PORT d.o.o.
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.simtech.sim7000;

public enum SimTechSim7000AtCommands {

    getSimPinStatus("at+cpin?\r\n"),
    getModuleStatus("at+cnsmod?\r\n"),
    getSignalStrength("at+csq\r\n"),
    pdpContext("AT+CGDCONT");

    private String atCommand;

    private SimTechSim7000AtCommands(String atCommand) {
        this.atCommand = atCommand;
    }

    public String getCommand() {
        return this.atCommand;
    }
}
