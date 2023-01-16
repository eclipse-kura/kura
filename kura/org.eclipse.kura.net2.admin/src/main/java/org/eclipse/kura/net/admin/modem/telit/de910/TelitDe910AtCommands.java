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
package org.eclipse.kura.net.admin.modem.telit.de910;

public enum TelitDe910AtCommands {

    getNetRegistrationStatus("at+creg?\r\n"),
    getMdn("at#modem=0?\r\n"),
    getMsid("at#modem=1?\r\n"),
    getServiceType("at+service?\r\n"),
    getSessionDataVolume("at#gdatavol=1\r\n"),
    provisionVerizon("atd*22899;\r\n");

    private String command;

    private TelitDe910AtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}
