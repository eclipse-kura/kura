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
    };

    public static final String RTU = "RTU";
    public static final String ASCII = "ASCII";
    public static final int RTU_MODE = 0;
    public static final int ASCII_MODE = 1;
}
