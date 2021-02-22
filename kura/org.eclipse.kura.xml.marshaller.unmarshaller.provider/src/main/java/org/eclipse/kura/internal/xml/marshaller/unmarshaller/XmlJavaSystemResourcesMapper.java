/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.xml.marshaller.unmarshaller;

import java.util.List;

import org.eclipse.kura.core.inventory.resources.SystemResourcesInfo;
import org.eclipse.kura.system.SystemResourceInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlJavaSystemResourcesMapper implements XmlJavaDataMapper {

    private static final String SYSTEM_INVENTORY = "inventory";
    private static final String SYSTEM_INVENTORY_RESOURCE = "resource";
    private static final String SYSTEM_INVENTORY_RESOURCE_NAME = "name";
    private static final String SYSTEM_INVENTORY_RESOURCE_VERSION = "version";
    private static final String SYSTEM_INVENTORY_RESOURCE_TYPE = "type";

    @Override
    public Element marshal(Document doc, Object object) throws Exception {
        Element inventory = doc.createElement(SYSTEM_INVENTORY);
        doc.appendChild(inventory);

        SystemResourcesInfo xmlInventory = (SystemResourcesInfo) object;
        List<SystemResourceInfo> xmlResourceList = xmlInventory.getSystemResources();

        xmlResourceList.stream().forEach(xmlResource -> {
            Element p = doc.createElement(SYSTEM_INVENTORY_RESOURCE);
            marshallResource(doc, xmlResource, p);
            inventory.appendChild(p);
        });
        return inventory;
    }

    @Override
    public <T> T unmarshal(Document doc) throws Exception {
        return null;
    }

    //
    // Marshaller's private methods
    //
    private static void marshallResource(Document doc, SystemResourceInfo systemResourceInfo, Element p) {
        // Extract data from SystemResourceInfo
        String resourceName = systemResourceInfo.getName();
        String resourceVersion = systemResourceInfo.getVersion();
        String resourceType = systemResourceInfo.getTypeString();

        // Create xml elements
        if (resourceName != null && !resourceName.trim().isEmpty()) {
            Element name = doc.createElement(SYSTEM_INVENTORY_RESOURCE_NAME);
            name.setTextContent(resourceName);
            p.appendChild(name);
        }

        if (resourceVersion != null && !resourceVersion.trim().isEmpty()) {
            Element version = doc.createElement(SYSTEM_INVENTORY_RESOURCE_VERSION);
            version.setTextContent(resourceVersion);
            p.appendChild(version);
        }

        if (resourceType != null && !resourceType.trim().isEmpty()) {
            Element type = doc.createElement(SYSTEM_INVENTORY_RESOURCE_TYPE);
            type.setTextContent(resourceType);
            p.appendChild(type);
        }
    }
}
