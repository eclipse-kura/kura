/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.camel.router;

import org.apache.camel.component.properties.PropertiesFunction;
import org.osgi.service.cm.Configuration;

import java.util.Dictionary;
import java.util.Map;

public class KuraMetatypePropertiesFunction implements PropertiesFunction {
    Map<String, Object> m_properties;
    Dictionary<String, Object> m_dictonnary;

    public KuraMetatypePropertiesFunction(Map<String, Object> properties) {
        m_properties = properties;
    }

    public KuraMetatypePropertiesFunction(Configuration config) {
        m_dictonnary = config.getProperties();
    }

    @Override
    public String getName() {
        return org.eclipse.kura.camel.KuraConstants.METATYPE_NAME;
    }

    @Override
    public String apply(String remainder) {
        if (m_properties != null) {
            return (m_properties.get(remainder) != null) ? m_properties.get(remainder).toString() : null;
        } else if (m_dictonnary != null) {
            return (m_dictonnary.get(remainder) != null) ? m_dictonnary.get(remainder).toString() : null;
        }
        return null;
    }
}
