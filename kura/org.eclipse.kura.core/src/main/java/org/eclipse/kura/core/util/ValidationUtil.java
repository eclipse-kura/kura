/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.util;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class ValidationUtil {

    public static void notNull(Object value, String argumentName) throws KuraException {
        if (value == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, argumentName);
        }
    }

    /**
     * Throws an KuraException if the string value for the specified argument is empty, null, or whitespace.
     *
     * @param obj
     * @param argumentName
     * @throws KuraException
     */
    public static void notEmptyOrNull(String value, String argumentName) throws KuraException {
        if (value == null || value.trim().length() == 0) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, argumentName);
        }
    }

    /**
     * Throws an KuraException if the value for the specified argument is null.
     *
     * @param obj
     * @param argumentName
     * @throws KuraException
     */
    public static void notNegative(int value, String argumentName) throws KuraException {
        if (value < 0) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, argumentName);
        }
    }

    /**
     * Throws an KuraException if the value for the specified argument is null.
     *
     * @param obj
     * @param argumentName
     * @throws KuraException
     */
    public static void notNegative(short value, String argumentName) throws KuraException {
        if (value < 0) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, argumentName);
        }
    }

    /**
     * Throws an KuraException if the value for the specified argument is null.
     *
     * @param obj
     * @param argumentName
     * @throws KuraException
     */
    public static void notNegative(long value, String argumentName) throws KuraException {
        if (value < 0) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, argumentName);
        }
    }
}
