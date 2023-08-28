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
            this.version = properties.get().getVersion();
            this.extendedProperties = new HashMap<>();

            for (ExtendedPropertyGroup group : properties.get().getPropertyGroups()) {
                this.extendedProperties.put(group.getName(), group.getProperties());
            }
        }
    }

    public ExtendedPropertiesDTO(SystemService systemService, List<String> groupNames) {
        Optional<ExtendedProperties> properties = systemService.getExtendedProperties();

        if (properties.isPresent()) {
            this.version = properties.get().getVersion();
            this.extendedProperties = new HashMap<>();
            populateSearchedGroups(properties.get(), groupNames);
        }
    }

    private void populateSearchedGroups(ExtendedProperties properties, List<String> groupNames) {
        for (ExtendedPropertyGroup group : properties.getPropertyGroups()) {
            String groupName = group.getName();

            if (groupNames.contains(groupName)) {
                this.extendedProperties.put(groupName, group.getProperties());
            }
        }
    }

}
