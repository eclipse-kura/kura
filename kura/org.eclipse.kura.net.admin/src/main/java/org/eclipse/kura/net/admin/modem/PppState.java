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
package org.eclipse.kura.net.admin.modem;

public enum PppState {

    NOT_CONNECTED(0),
    IN_PROGRESS(1),
    CONNECTED(2);

    private int state = 0;

    private PppState(int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }
}
