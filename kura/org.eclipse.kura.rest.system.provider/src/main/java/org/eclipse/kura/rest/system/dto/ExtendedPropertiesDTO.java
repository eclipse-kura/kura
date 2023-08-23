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

import org.eclipse.kura.system.ExtendedProperties;
import org.eclipse.kura.system.ExtendedPropertyGroup;

@SuppressWarnings("unused")
public class ExtendedPropertiesDTO {

    private String version;
    private Map<String, Map<String, String>> extendedProperties;

    public ExtendedPropertiesDTO(ExtendedProperties properties) {
        this.version = properties.getVersion();
        this.extendedProperties = new HashMap<>();

        List<ExtendedPropertyGroup> groups = properties.getPropertyGroups();
        for (ExtendedPropertyGroup group : groups) {
            this.extendedProperties.put(group.getName(), group.getProperties());
        }
    }

}
