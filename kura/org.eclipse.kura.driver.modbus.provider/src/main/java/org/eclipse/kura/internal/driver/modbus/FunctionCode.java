/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.driver.modbus;

/**
 * Supported ModBus Function Codes.
 */
public enum FunctionCode {

	/** Read Coil Request. */
	FC_01_READ_COILS(1),

	/** Read Input Discrete Request. */
	FC_02_READ_DISCRETE_INPUTS(2),

	/** Read Multiple Registers Request. */
	FC_03_READ_HOLDING_REGISTERS(3),

	/** Read Input Registers Request. */
	FC_04_READ_INPUT_REGISTERS(4),

	/** Write Coil Request. */
	FC_05_WRITE_SINGLE_COIL(5),

	/** Write Coil Request. */
	FC_06_WRITE_SINGLE_REGISTER(6),

	/** Write Multiple Coils Request. */
	FC_15_WRITE_MULITPLE_COILS(15),

	/** Write Multiple Registers Request. */
	FC_16_WRITE_MULTIPLE_REGISTERS(16);

	/** The function code as associated. */
	private int code;

	/**
	 * Constructor.
	 *
	 * @param code
	 *            the function code
	 */
	private FunctionCode(final int code) {
		this.code = code;
	}

	/**
	 * Returns the associated Function Code.
	 *
	 * @return the function code
	 */
	public int code() {
		return this.code;
	}
}
