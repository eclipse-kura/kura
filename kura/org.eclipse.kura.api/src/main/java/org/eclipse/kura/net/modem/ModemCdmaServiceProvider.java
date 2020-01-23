/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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

public enum ModemCdmaServiceProvider {

    UNKNOWN(0),
    SPRINT(1),
    AERIS(2),
    VERIZON(3);

    private int m_provider;

    private ModemCdmaServiceProvider(int provider) {
        this.m_provider = provider;
    }

    public int getProvider() {
        return this.m_provider;
    }
}
