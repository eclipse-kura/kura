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
package org.eclipse.kura.driver;

/**
 * This represents all the Kura driver specific flag codes
 */
public enum DriverFlag {
	/**
	 * In case of communication failure for a known indistinguishable reason
	 */
	DRIVER_ERROR_UNSPECIFIED,
	/**
	 * In case of successful reading operation
	 */
	READ_SUCCESSFUL,
	/**
	 * In case of failure due to completely unknown reason
	 */
	UNKNOWN,
	/**
	 * In case of successful writing operation
	 */
	WRITE_SUCCESSFUL;
}
