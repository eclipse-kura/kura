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

import java.util.Objects;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This class contains all relevant properties to describe a SIM (Subscriber
 * Identity Module).
 *
 */
@ProviderType
public class Sim {

    private final boolean active;
    private final boolean primary;
    private final String iccid;
    private final String imsi;
    private final String eid;
    private final String operatorName;
    private final SimType simType;
    private final ESimStatus eSimStatus;

    public Sim(SimBuilder builder) {
        this.active = builder.active;
        this.primary = builder.primary;
        this.iccid = builder.iccid;
        this.imsi = builder.imsi;
        this.eid = builder.eid;
        this.operatorName = builder.operatorName;
        this.simType = builder.simType;
        this.eSimStatus = builder.eSimStatus;
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isPrimary() {
        return this.primary;
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

    public static SimBuilder builder() {
        return new SimBuilder();
    }

    public static final class SimBuilder {

        private boolean active;
        private boolean primary;
        private String iccid = "NA";
        private String imsi = "NA";
        private String eid = "NA";
        private String operatorName = "NA";
        private SimType simType = SimType.UNKNOWN;
        private ESimStatus eSimStatus = ESimStatus.UNKNOWN;

        private SimBuilder() {
        }

        public SimBuilder withActive(boolean active) {
            this.active = active;
            return this;
        }

        public SimBuilder withPrimary(boolean primary) {
            this.primary = primary;
            return this;
        }

        public SimBuilder withIccid(String iccid) {
            this.iccid = iccid;
            return this;
        }

        public SimBuilder withImsi(String imsi) {
            this.imsi = imsi;
            return this;
        }

        public SimBuilder withEid(String eid) {
            this.eid = eid;
            return this;
        }

        public SimBuilder withOperatorName(String operatorName) {
            this.operatorName = operatorName;
            return this;
        }

        public SimBuilder withSimType(SimType simType) {
            this.simType = simType;
            return this;
        }

        public SimBuilder withESimStatus(ESimStatus eSimStatus) {
            this.eSimStatus = eSimStatus;
            return this;
        }

        public Sim build() {
            return new Sim(this);
        }

    }

    @Override
    public int hashCode() {
        return Objects.hash(this.active, this.primary, this.eSimStatus, this.eid, this.iccid, this.imsi,
                this.operatorName, this.simType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Sim other = (Sim) obj;
        return this.active == other.active && this.primary == other.primary && this.eSimStatus == other.eSimStatus
                && Objects.equals(this.eid, other.eid) && Objects.equals(this.iccid, other.iccid)
                && Objects.equals(this.imsi, other.imsi) && Objects.equals(this.operatorName, other.operatorName)
                && this.simType == other.simType;
    }

}
