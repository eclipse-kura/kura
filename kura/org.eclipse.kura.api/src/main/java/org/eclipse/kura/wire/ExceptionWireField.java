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
 * The ExceptionWireField represents an ADT (abstract data type) for
 * representing an exception flag in {@link WireRecord}.
 */
public final class ExceptionWireField extends WireField {

	/** Exception Constant */
	private static final String PROP = "EXCEPTION";

	/**
	 * Instantiates a new exception wire field.
	 */
	public ExceptionWireField() {
		super(PROP, TypedValues.newStringValue(PROP));
	}

}
