/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.protocol.can;

public class CanMessage {

    private int m_canId;
    private byte[] m_data;

    public byte[] getData() {
        return this.m_data;
    }

    public void setData(byte[] data) {
        this.m_data = data;
    }

    public int getCanId() {
        return this.m_canId;
    }

    public void setCanId(int canId) {
        this.m_canId = canId;
    }

}
