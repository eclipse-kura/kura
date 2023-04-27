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
 *******************************************************************************/
package org.eclipse.kura.network.status.provider.api;

import org.eclipse.kura.net.status.modem.ESimStatus;
import org.eclipse.kura.net.status.modem.Sim;
import org.eclipse.kura.net.status.modem.SimType;

@SuppressWarnings("unused")
public class SimDTO {

    private final boolean active;
    private final boolean primary;
    private final String iccid;
    private final String imsi;
    private final String eid;
    private final String operatorName;
    private final SimType simType;
    private final ESimStatus eSimStatus;

    public SimDTO(final Sim sim) {
        this.active = sim.isActive();
        this.primary = sim.isPrimary();
        this.iccid = sim.getIccid();
        this.imsi = sim.getImsi();
        this.eid = sim.getEid();
        this.operatorName = sim.getOperatorName();
        this.simType = sim.getSimType();
        this.eSimStatus = sim.geteSimStatus();
    }
}
