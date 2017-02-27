/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.protocol.modbus;

abstract class Communicate {
            
    abstract public void connect();

    abstract public void disconnect() throws ModbusProtocolException;

    abstract public int getConnectStatus();

    abstract public byte[] msgTransaction(byte[] msg) throws ModbusProtocolException;
}