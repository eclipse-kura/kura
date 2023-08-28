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
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.kura.system.ExtendedProperties;
import org.eclipse.kura.system.ExtendedPropertyGroup;
import org.eclipse.kura.system.SystemService;

@SuppressWarnings("unused")
public class ExtendedPropertiesDTO {

    private String version;
    private Map<String, Map<String, String>> extendedProperties;

    public ExtendedPropertiesDTO(SystemService systemService) {
        Optional<ExtendedProperties> properties = systemService.getExtendedProperties();

        if (properties.isPresent()) {
            populateExtendedProperties(properties.get(), s -> true);
        }
    }

    public ExtendedPropertiesDTO(SystemService systemService, List<String> groupNames) {
        Optional<ExtendedProperties> properties = systemService.getExtendedProperties();

        if (properties.isPresent()) {
            populateExtendedProperties(properties.get(), groupNames::contains);
        }
    }

    private void populateExtendedProperties(ExtendedProperties properties, Predicate<String> condition) {
        this.version = properties.getVersion();
        this.extendedProperties = new HashMap<>();

        for (ExtendedPropertyGroup group : properties.getPropertyGroups()) {
            putIf(group.getName(), group.getProperties(), condition.test(group.getName()));
        }
    }

    private void putIf(String key, Map<String, String> value, boolean condition) {
        if (condition) {
            this.extendedProperties.put(key, value);
        }
    }

}
