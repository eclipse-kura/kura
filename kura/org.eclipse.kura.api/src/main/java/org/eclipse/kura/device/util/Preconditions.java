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
package org.eclipse.kura.device.util;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;

/**
 * The Class Preconditions is responsible to provide utility methods to check
 * for conditions or predicates and it throws {@link KuraRuntimeException} with
 * {@link KuraErrorCode#INTERNAL_ERROR} if the provided flag is satisfied
 */
public final class Preconditions {

	/**
	 * Checks condition and throws exception if satisfied
	 *
	 * @param flag
	 *            the flag condition to check, and if it's satisfied, then throw
	 *            the exception
	 * @throws KuraRuntimeException
	 *             if the provided flag or condition is true
	 */
	public static void checkCondition(final boolean flag) {
		checkCondition(flag, KuraErrorCode.INTERNAL_ERROR, "");
	}

	/**
	 * Checks condition and throws exception if satisfied
	 *
	 * @param flag
	 *            the flag condition to check, and if it's satisfied, then throw
	 *            the exception
	 * @param errorCode
	 *            the error code to set
	 * @throws KuraRuntimeException
	 *             if the provided flag or condition is true
	 */
	public static void checkCondition(final boolean flag, final KuraErrorCode errorCode) {
		checkCondition(flag, errorCode, errorCode.toString());
	}

	/**
	 * Checks condition and throws exception if satisfied
	 *
	 * @param flag
	 *            the flag condition to check, and if it's satisfied, then throw
	 *            the exception
	 * @param errorCode
	 *            the error code to set
	 * @param message
	 *            the exception message
	 * @throws KuraRuntimeException
	 *             if the provided flag or condition is true
	 */
	public static void checkCondition(final boolean flag, final KuraErrorCode errorCode, final String message) {
		if (flag) {
			throw new KuraRuntimeException(errorCode, message);
		}
	}

	/**
	 * Checks condition and throws exception if satisfied
	 *
	 * @param flag
	 *            the flag condition to check, and if it's satisfied, then throw
	 *            the exception
	 * @param message
	 *            the exception message
	 * @throws KuraRuntimeException
	 *             if the provided flag or condition is true
	 */
	public static void checkCondition(final boolean flag, final String message) {
		checkCondition(flag, KuraErrorCode.INTERNAL_ERROR, message);
	}

	/**
	 * Instantiates a new device preconditions.
	 */
	private Preconditions() {
		// Static Factory Methods container. No need to instantiate.
	}

}
