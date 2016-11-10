/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura;

import org.eclipse.kura.annotation.Nullable;

/**
 * The Class Preconditions is responsible to provide utility static factory
 * methods to check for conditions or predicates and it throws
 * {@link KuraRuntimeException} with {@link KuraErrorCode#INTERNAL_ERROR} if the
 * condition is satisfied
 *
 * @since 1.0.10
 */
public final class Preconditions {

    /** Constructor */
    private Preconditions() {
        // Static Factory Methods container. No need to instantiate.
    }

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
            throw new KuraRuntimeException(errorCode, message == null ? "" : message);
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
     *            the instance to check for compliance
     * @param message
     *            the exception message
     * @throws KuraRuntimeException
     *             if the check is successful
     * @return the non null instance
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
     *            the instance to check for compliance
     * @param message
     *            the exception message
     * @throws KuraRuntimeException
     *             if the condition is successful
     * @return the non null instance
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

}
