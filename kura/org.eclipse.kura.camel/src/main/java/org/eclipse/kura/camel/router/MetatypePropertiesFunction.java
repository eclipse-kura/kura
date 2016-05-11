/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.camel.router;

import java.util.Dictionary;
import java.util.Map;

import org.apache.camel.component.properties.PropertiesFunction;
import org.eclipse.kura.camel.RouterConstants;
import org.osgi.service.cm.Configuration;

public class MetatypePropertiesFunction implements PropertiesFunction {
    private Map<String, Object> m_properties;
    private Dictionary<String, Object> m_dictionary;

    public MetatypePropertiesFunction(Map<String, Object> properties) {
        m_properties = properties;
    }

    public MetatypePropertiesFunction(Configuration config) {
        m_dictionary = config.getProperties();
    }

    @Override
    public String getName() {
        return RouterConstants.METATYPE_NAME;
    }

    @Override
    public String apply(String remainder) {
        if (m_properties != null) {
            return (m_properties.get(remainder) != null) ? m_properties.get(remainder).toString() : null;
        } else if (m_dictionary != null) {
            return (m_dictionary.get(remainder) != null) ? m_dictionary.get(remainder).toString() : null;
        }
        return null;
    }
}
