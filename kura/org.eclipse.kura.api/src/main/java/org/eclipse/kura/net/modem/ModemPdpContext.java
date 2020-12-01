/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.modem;

/**
 * @since 1.4
 */
public class ModemPdpContext {

    private final int number;
    private final ModemPdpContextType type;
    private final String apn;

    public ModemPdpContext(int number, ModemPdpContextType type, String apn) {
        this.number = number;
        this.type = type;
        this.apn = apn;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getName());
        sb.append(super.toString());
        sb.append("; ").append("Context Number: ").append(this.number);
        sb.append("; ").append("PDP Type: ").append(this.type);
        sb.append("; ").append("APN: ").append(this.apn);
        return sb.toString();
    }

    public int getNumber() {
        return this.number;
    }

    public ModemPdpContextType getType() {
        return this.type;
    }

    public String getApn() {
        return this.apn;
    }
}
