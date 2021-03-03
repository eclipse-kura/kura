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

import org.eclipse.kura.core.inventory.resources.SystemPackage;
import org.eclipse.kura.core.inventory.resources.SystemPackages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlJavaSystemPackagesMapper implements XmlJavaDataMapper {

    private static final String SYSTEM_PACKAGES = "systemPackages";
    private static final String SYSTEM_PACKAGES_PACKAGE = "systemPackage";
    private static final String SYSTEM_PACKAGES_PACKAGE_NAME = "name";
    private static final String SYSTEM_PACKAGES_PACKAGE_VERSION = "version";
    private static final String SYSTEM_PACKAGES_PACKAGE_TYPE = "type";

    @Override
    public Element marshal(Document doc, Object object) throws Exception {
        Element packages = doc.createElement(SYSTEM_PACKAGES);
        doc.appendChild(packages);

        SystemPackages xmlPackages = (SystemPackages) object;
        List<SystemPackage> xmlPackagesList = xmlPackages.getSystemPackages();

        xmlPackagesList.stream().forEach(xmlPackage -> {
            Element p = doc.createElement(SYSTEM_PACKAGES_PACKAGE);
            marshallPackage(doc, xmlPackage, p);
            packages.appendChild(p);
        });
        return packages;
    }

    @Override
    public <T> T unmarshal(Document doc) throws Exception {
        return null;
    }

    //
    // Marshaller's private methods
    //
    private static void marshallPackage(Document doc, SystemPackage systemPackage, Element p) {
        // Extract data from XmlSystemPackage
        String packageName = systemPackage.getName();
        String packageVersion = systemPackage.getVersion();
        String packageType = systemPackage.getTypeString();

        // Create xml elements
        if (packageName != null && !packageName.trim().isEmpty()) {
            Element name = doc.createElement(SYSTEM_PACKAGES_PACKAGE_NAME);
            name.setTextContent(packageName);
            p.appendChild(name);
        }

        if (packageVersion != null && !packageVersion.trim().isEmpty()) {
            Element version = doc.createElement(SYSTEM_PACKAGES_PACKAGE_VERSION);
            version.setTextContent(packageVersion);
            p.appendChild(version);
        }

        if (packageType != null && !packageType.trim().isEmpty()) {
            Element type = doc.createElement(SYSTEM_PACKAGES_PACKAGE_TYPE);
            type.setTextContent(packageType);
            p.appendChild(type);
        }
    }
}
