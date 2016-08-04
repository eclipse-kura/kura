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
package org.eclipse.kura.wire;

import org.eclipse.kura.type.TypedValues;

/**
 * The TimerWireField represents an ADT (abstract data type) for a Timer
 * specific field to be used in {@link WireRecord}.
 */
public final class TimerWireField extends WireField {

	/** Timer Field Constant */
	private static final String PROP = "TIMER";

	/**
	 * Instantiates a new timer wire field.
	 */
	public TimerWireField() {
		super(PROP, TypedValues.newStringValue(PROP));
	}

}
