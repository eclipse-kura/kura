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
 * The Enum ModbusType represents the access specifier for the Modbus
 * Communication
 */
public enum ModbusType {

	/** The Serial Access. */
	RTU,
	/** The TCP Mode. */
	TCP,
	/** The UDP Mode. */
	UDP

}
