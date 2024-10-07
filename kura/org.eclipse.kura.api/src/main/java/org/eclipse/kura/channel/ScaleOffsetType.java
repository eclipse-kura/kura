/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.channel;

/**
 * This contains all the required type constants for representing
 * Scale and Offset.
 *
 * @since 2.8
 */
public enum ScaleOffsetType {

    DEFINED_BY_VALUE_TYPE,

    DOUBLE;

    /**
     * Converts {@code stringScaleOffsetType}, if possible, to the related {@link ScaleOffsetType}.
     *
     * @param stringDataType
     *            String that we want to use to get the respective {@link ScaleOffsetType}.
     * @return a ScaleOffsetType that corresponds to the String passed as argument.
     * @throws IllegalArgumentException
     *             if the passed string does not correspond to an existing {@link ScaleOffsetType}.
     */
    public static ScaleOffsetType getScaleOffsetType(String stringScaleOffsetType) {

        if (DEFINED_BY_VALUE_TYPE.name().equalsIgnoreCase(stringScaleOffsetType)) {
            return DEFINED_BY_VALUE_TYPE;
        }

        if (DOUBLE.name().equalsIgnoreCase(stringScaleOffsetType)) {
            return DOUBLE;
        }

        throw new IllegalArgumentException("Cannot convert to DataType");
    }

}
