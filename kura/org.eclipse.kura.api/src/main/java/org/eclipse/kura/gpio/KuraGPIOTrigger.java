/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.gpio;

public enum KuraGPIOTrigger {
	/**
	 * @deprecated Use RISING_EDGE instead 
	 */
	@Deprecated
	RAISING_EDGE,
    RISING_EDGE,
    FALLING_EDGE,
    BOTH_EDGES,
    HIGH_LEVEL,
    LOW_LEVEL,
    BOTH_LEVELS,
    NONE
}
