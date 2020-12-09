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
package org.eclipse.kura.protocol.modbus;

/**
 * supported modbus commands
 *
 * @author matt.demaree
 *
 */
public class ModbusFunctionCodes {

    /**
     *
     */
    private ModbusFunctionCodes() {
    }

    public static final int READ_COIL_STATUS = 1;
    public static final int READ_INPUT_STATUS = 2;
    public static final int READ_HOLDING_REGS = 3;
    public static final int READ_INPUT_REGS = 4;
    public static final int FORCE_SINGLE_COIL = 5;
    public static final int PRESET_SINGLE_REG = 6;
    public static final int READ_EXCEPTION_STATUS = 7;
    public static final int GET_COMM_EVENT_COUNTER = 11;
    public static final int GET_COMM_EVENT_LOG = 12;
    public static final int FORCE_MULTIPLE_COILS = 15;
    public static final int PRESET_MULTIPLE_REGS = 16;
}
