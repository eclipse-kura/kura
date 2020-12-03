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
 * This static class is provided only for completeness. Currently the
 * Modbus protocol only supports RTU mode communications.
 * <p>
 * The Field values, not the field names should be used in configuration files.
 *
 *
 */
public class ModbusTransmissionMode {

    /**
     *
     */
    private ModbusTransmissionMode() {
    }

    public static final String RTU = "RTU";
    public static final String ASCII = "ASCII";
    public static final int RTU_MODE = 0;
    public static final int ASCII_MODE = 1;
}
