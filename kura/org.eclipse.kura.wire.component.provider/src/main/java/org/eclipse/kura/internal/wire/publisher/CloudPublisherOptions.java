/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.internal.wire.publisher;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Class CloudPublisherOptions is responsible to provide all the required
 * options for the Cloud Publisher Wire Component
 */
final class CloudPublisherOptions {

    private static final Logger logger = LogManager.getLogger(CloudPublisherOptions.class);

    private static final String CONF_POSITION = "publish.position";
    private static final String CONF_BODY_PROPERTY = "set.body.from.property";

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

    Optional<String> getBodyProperty() {
        final Object propertyRaw = this.properties.get(CONF_BODY_PROPERTY);

        if (!(propertyRaw instanceof String)) {
            return Optional.empty();
        }

        final String property = (String) propertyRaw;

        if (property.trim().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(property);
    }
}