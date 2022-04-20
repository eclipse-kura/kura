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
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.quectel.generic;

public enum QuectelGenericAtCommands {

    GET_SIM_STATUS("at+qsimstat?\r\n"),
    GET_SIM_PIN_STATUS("at+cpin?\r\n"),
    GET_MOBILESTATION_CLASS("at+cgclass?\r\n"),
    GET_REGISTRATION_STATUS("at+cgreg?\r\n"),
    GET_EXTENDED_REGISTRATION_STATUS("at+cgreg=2\r\n"),
    GET_REGISTERED_NETWORK("at+qspn\r\n"),
    GET_QUERY_NETWORK_INFORMATION("at+qnwinfo\r\n"),
    GET_GPRS_SESSION_DATA_VOLUME("at+qgdcnt?\r\n"),
    PDP_CONTEXT("at+cgdcont"),
    ENABLE_GPS("at+qgps=1\r\n"),
    DISABLE_GPS("at+qgpsend\r\n"),
    IS_GPS_ENABLED("at+qgps?\r\n"),
    ENABLE_NMEA_GPS("at+qgpscfg=\"nmeasrc\",1\r\n"),
    DISABLE_NMEA_GPS("at+qgpscfg=\"nmeasrc\",0\r\n");

    private String command;

    private QuectelGenericAtCommands(String atCommand) {
        this.command = atCommand;
    }

    public String getCommand() {
        return this.command;
    }
}