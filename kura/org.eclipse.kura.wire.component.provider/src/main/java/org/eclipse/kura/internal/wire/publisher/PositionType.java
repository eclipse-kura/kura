/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.wire.publisher;

/**
 * This enum specifies the supported position types managed by the cloud publisher.
 *
 */
public enum PositionType {
    NONE("none"),
    BASIC("basic"),
    FULL("full");

    private final String position;

    private PositionType(String positionType) {
        this.position = positionType;
    }

    /**
     * Allows to map a provided string with the corresponding {@link PositionType}
     *
     * @param proposedEncoding
     *            the String that has to be mapped to the corresponding {@link PositionType}
     * @return {@link PositionType} if the matching between passed string and enum values succeeds
     * @throws IllegalArgumentException
     *             if the argument cannot be matched to a corresponding {@link PositionType} object.
     */
    public static PositionType getEncoding(String proposedEncoding) {
        for (PositionType encoding : PositionType.values()) {
            if (encoding.position.equalsIgnoreCase(proposedEncoding)) {
                return encoding;
            }
        }
        throw new IllegalArgumentException("Unsupported Encoding!");
    }

}
