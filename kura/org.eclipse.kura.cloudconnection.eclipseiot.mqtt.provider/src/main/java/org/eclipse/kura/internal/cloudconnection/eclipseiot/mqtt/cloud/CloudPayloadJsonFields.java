/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud;

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
