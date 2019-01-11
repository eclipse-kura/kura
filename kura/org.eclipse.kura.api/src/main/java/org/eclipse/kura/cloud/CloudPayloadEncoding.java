/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud;

/**
 * This enum specifies the supported payload encodings.
 * 
 * @since 1.2
 */
public enum CloudPayloadEncoding {
    KURA_PROTOBUF("kura-protobuf"),
    SIMPLE_JSON("simple-json");

    private final String encodingText;

    private CloudPayloadEncoding(String encoding) {
        this.encodingText = encoding;
    }

    /**
     * Allows to map a provided string with the corresponding {@link CloudPayloadEncoding}
     *
     * @param proposedEncoding
     *            the String that has to be mapped to the corresponding {@link CloudPayloadEncoding}
     * @return {@link CloudPayloadEncoding} if the matching between passed string and enum values succeeds
     * @throws IllegalArgumentException
     *             if the argument cannot be matched to a corresponding {@link CloudPayloadEncoding} object.
     */
    public static CloudPayloadEncoding getEncoding(String proposedEncoding) {
        for (CloudPayloadEncoding encoding : CloudPayloadEncoding.values()) {
            if (encoding.encodingText.equalsIgnoreCase(proposedEncoding)) {
                return encoding;
            }
        }
        throw new IllegalArgumentException("Unsupported Encoding!");
    }
}
