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

import org.eclipse.kura.core.deployment.xml.XmlSystemPackageInfos;
import org.eclipse.kura.system.SystemPackageInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlJavaSystemPackagesMapper implements XmlJavaDataMapper {

    private static final String SYSTEM_PACKAGES = "systemPackages";
    private static final String SYSTEM_PACKAGES_PACKAGE = "systemPackages";
    private static final String SYSTEM_PACKAGES_PACKAGE_NAME = "name";
    private static final String SYSTEM_PACKAGES_PACKAGE_VERSION = "version";
    private static final String SYSTEM_PACKAGES_PACKAGE_TYPE = "type";

    @Override
    public Element marshal(Document doc, Object object) throws Exception {
        Element packages = doc.createElement(SYSTEM_PACKAGES);
        doc.appendChild(packages);

        XmlSystemPackageInfos xmlPackages = (XmlSystemPackageInfos) object;
        List<SystemPackageInfo> xmlPackageList = xmlPackages.getSystemPackages();

        xmlPackageList.stream().forEach(xmlPackage -> {
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
    private static void marshallPackage(Document doc, SystemPackageInfo systemPackageInfo, Element p) {
        // Extract data from SystemPackageInfo
        String packageName = systemPackageInfo.getName();
        String packageVersion = systemPackageInfo.getVersion();
        String packageType = systemPackageInfo.getType();

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
