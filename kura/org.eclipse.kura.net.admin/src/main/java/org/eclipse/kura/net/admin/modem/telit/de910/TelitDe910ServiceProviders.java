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

public enum TelitDe910ServiceProviders {

    UNKNOWN(-1),
    SPRINT(0),
    AERIS(1),
    VERIZON(2);

    private int provider;

    private TelitDe910ServiceProviders(int provider) {
        this.provider = provider;
    }

    public int getProvider() {
        return this.provider;
    }
}
