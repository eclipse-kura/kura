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
package org.eclipse.kura.internal.wire.subscriber;

import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.type.DataType;

final class CloudSubscriberOptions {

    private static final String CONF_BODY_PROPERTY = "set.property.from.body";
    private static final String CONF_BODY_PROPERTY_TYPE = "body.property.type";

    private final Map<String, Object> properties;

    CloudSubscriberOptions(final Map<String, Object> properties) {
        this.properties = properties;
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

    DataType getBodyPropertyType() {
        try {
            return DataType.valueOf((String) this.properties.get(CONF_BODY_PROPERTY_TYPE));
        } catch (final Exception e) {
            return DataType.BYTE_ARRAY;
        }
    }
}