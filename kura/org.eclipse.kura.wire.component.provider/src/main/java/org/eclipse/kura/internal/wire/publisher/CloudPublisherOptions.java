/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.publisher;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Class CloudPublisherOptions is responsible to provide all the required
 * options for the Cloud Publisher Wire Component
 */
final class CloudPublisherOptions {

    private static final Logger logger = LogManager.getLogger();

    private static final String CONF_POSITION = "publish.position";

    private final Map<String, Object> properties;

    /**
     * Instantiates a new cloud publisher options.
     *
     * @param properties
     *            the properties
     */
    CloudPublisherOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = properties;
    }

    /**
     * Returns if the messages have to be enriched with gateway's position and which type of position is needed
     *
     * @return true if messages have to be enriched with gateway's position
     */
    PositionType getPositionType() {
        String positionTypeString = "";
        final Object configurationPositionType = this.properties.get(CONF_POSITION);
        if (nonNull(configurationPositionType) && configurationPositionType instanceof String) {
            positionTypeString = (String) configurationPositionType;
        }

        PositionType result = PositionType.NONE;
        try {
            result = PositionType.getEncoding(positionTypeString);
        } catch (IllegalArgumentException e) {
            logger.warn("Cannot parse the provided position type.", e);
        }
        return result;
    }
}