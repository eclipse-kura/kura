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
import java.util.Map;
import java.util.Properties;

public class KuraPropertiesDTO {

    private Map<String, String> kuraProperties;

    public KuraPropertiesDTO(Properties kuraProperties) {
        this.kuraProperties = new HashMap<>();

        for (String key : kuraProperties.stringPropertyNames()) {
            this.kuraProperties.put(key, kuraProperties.getProperty(key));
        }
    }

}
