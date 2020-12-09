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
 ******************************************************************************/
package org.eclipse.kura.net.modem;

public enum ModemCdmaServiceProvider {

    UNKNOWN(0),
    SPRINT(1),
    AERIS(2),
    VERIZON(3);

    private int provider;

    private ModemCdmaServiceProvider(int provider) {
        this.provider = provider;
    }

    public int getProvider() {
        return this.provider;
    }
}
