/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.rest.system.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

public class KuraPropertiesDTO {

    private Map<String, String> kuraProperties;

    public KuraPropertiesDTO(Properties kuraProperties) {
        this.kuraProperties = new HashMap<>();

        populateKuraProperties(kuraProperties, s -> true);
    }

    public KuraPropertiesDTO(Properties kuraProperties, List<String> names) {
        this.kuraProperties = new HashMap<>();

        populateKuraProperties(kuraProperties, names::contains);
    }

    private void populateKuraProperties(Properties properties, Predicate<String> condition) {
        for (String key : properties.stringPropertyNames()) {
            putIf(key, properties.getProperty(key), condition.test(key));
        }
    }

    private void putIf(String key, String value, boolean condition) {
        if (condition) {
            this.kuraProperties.put(key, value);
        }
    }

}
