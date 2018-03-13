/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.modem;

/**
 * @since 1.4
 */
public class ModemPdpContext {

    private int number;
    private ModemPdpContextType type;
    private String apn;

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
        return number;
    }

    public ModemPdpContextType getType() {
        return type;
    }

    public String getApn() {
        return apn;
    }
}
