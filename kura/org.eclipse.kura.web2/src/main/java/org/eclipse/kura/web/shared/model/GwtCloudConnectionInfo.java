/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.kura.web.client.util.KuraBaseModel;

public class GwtCloudConnectionInfo extends KuraBaseModel implements Serializable {

    private static final String CONNECTION_NAME_KEY = "Connection Name";
    private static final long serialVersionUID = -8403881748695073659L;

    public GwtCloudConnectionInfo() {
    }

    public String getCloudServicePid() {
        return get(CONNECTION_NAME_KEY);
    }

    public void setCloudServicePid(String cloudServicePid) {
        set(CONNECTION_NAME_KEY, cloudServicePid);
    }

    public Map<String, Object> getConnectionProperties() {
        Map<String, Object> properties = getProperties();

        Map<String, Object> result = new HashMap<>(properties);
        result.remove(CONNECTION_NAME_KEY);

        return result.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey,
                Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public void setConnectionProperties(Map<String, Object> connectionProperties) {
        setProperties(connectionProperties);
    }

    public void addConnectionProperty(String key, String property) {
        set(key, property);
    }

}
