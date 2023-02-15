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
 ******************************************************************************/
package org.eclipse.kura.net.status.modem;

public class Sim {

    private final boolean active;
    private final String iccid;
    private final String imsi;
    private final String eid;
    private final String operatorName;
    private final SimType simType;
    private final ESimStatus eSimStatus;

    public Sim(boolean active, String iccid, String imsi, String eid, String operatorName, SimType simType,
            ESimStatus eSimStatus) {
        this.active = active;
        this.iccid = iccid;
        this.imsi = imsi;
        this.eid = eid;
        this.operatorName = operatorName;
        this.simType = simType;
        this.eSimStatus = eSimStatus;
    }

    public boolean isActive() {
        return this.active;
    }

    public String getIccid() {
        return this.iccid;
    }

    public String getImsi() {
        return this.imsi;
    }

    public String getEid() {
        return this.eid;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public SimType getSimType() {
        return this.simType;
    }

    public ESimStatus geteSimStatus() {
        return this.eSimStatus;
    }
}
