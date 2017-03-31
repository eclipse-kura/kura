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
package org.eclipse.kura.core.cloud;

/**
 * Represents the encoded fields of a Json payload.
 *
 */
public enum CloudPayloadJsonFields {
    TIMESTAMP,
    BODY,
    POSITION;

    public enum CloudPayloadJsonPositionFields {
        LATITUDE,
        LONGITUDE,
        ALTITUDE,
        HEADING,
        PRECISION,
        SATELLITES,
        SPEED,
        TIMESTAMP,
        STATUS;
    }
}
