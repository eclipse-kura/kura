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
package org.eclipse.kura;

import org.eclipse.kura.annotation.Nullable;

import com.google.common.base.Strings;

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
		checkCondition(flag, KuraErrorCode.INTERNAL_ERROR, null);
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
	public static void checkCondition(final boolean flag, final KuraErrorCode errorCode,
			@Nullable final String message) {
		if (flag) {
			throw new KuraRuntimeException(errorCode, Strings.nullToEmpty(message));
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
	public static void checkCondition(final boolean flag, @Nullable final String message) {
		checkCondition(flag, KuraErrorCode.INTERNAL_ERROR, message);
	}

	/**
	 * Checks if the provided object is an instance of the provided class
	 *
	 * @param object
	 *            the object to verify
	 * @param instanceClass
	 *            the instance to check
	 * @param message
	 *            the exception message
	 * @throws KuraRuntimeException
	 *             if the check is successful
	 */
	public static <T> T checkInstance(final T object, final Class<?> instanceClass, @Nullable final String message) {
		checkCondition(object.getClass().isAssignableFrom(instanceClass), KuraErrorCode.INTERNAL_ERROR, message);
		return object;
	}

	/**
	 * Checks if the provided object is an instance of the provided class
	 *
	 * @param object
	 *            the object to verify
	 * @param instanceClass
	 *            the instance to check
	 * @param message
	 *            the exception message
	 * @throws KuraRuntimeException
	 *             if the check is successful
	 */
	public static <T> T checkNonInstance(final T object, final Class<?> instanceClass, @Nullable final String message) {
		checkCondition(!object.getClass().isAssignableFrom(instanceClass), KuraErrorCode.INTERNAL_ERROR, message);
		return object;
	}

	/**
	 * Checks if the provided object is null
	 *
	 * @param object
	 *            the object to check if it's null
	 * @param message
	 *            the exception message
	 * @return the non null instance
	 * @throws KuraRuntimeException
	 *             if the provided flag or condition is true
	 */
	public static <T> T checkNull(final T object, @Nullable final String message) {
		checkCondition(object == null, message);
		return object;
	}

	/** Constructor */
	private Preconditions() {
		// Static Factory Methods container. No need to instantiate.
	}

}
