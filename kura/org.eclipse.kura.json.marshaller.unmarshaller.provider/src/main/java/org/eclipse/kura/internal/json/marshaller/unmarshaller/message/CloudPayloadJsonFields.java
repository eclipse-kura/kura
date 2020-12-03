/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.json.marshaller.unmarshaller.message;

/**
 * Represents the encoded fields of a Json payload.
 *
 */
public enum CloudPayloadJsonFields {
    SENTON("sentOn"),
    POSITION("position"),
    METRICS("metrics"),
    BODY("body");

    public enum CloudPayloadJsonPositionFields {
        LATITUDE("latitude"),
        LONGITUDE("longitude"),
        ALTITUDE("altitude"),
        HEADING("heading"),
        PRECISION("precision"),
        SATELLITES("satellites"),
        SPEED("speed"),
        TIMESTAMP("timestamp"),
        STATUS("status");

        private String value;

        private CloudPayloadJsonPositionFields(final String value) {
            this.value = value;
        }

        /**
         * Returns the string representation of the constant
         *
         * @return the string value
         */
        public String value() {
            return this.value;
        }
    }

    private String value;

    private CloudPayloadJsonFields(final String value) {
        this.value = value;
    }

    /**
     * Returns the string representation of the constant
     *
     * @return the string value
     */
    public String value() {
        return this.value;
    }
}
