/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.core.inventory.resources.SystemBundle;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackage;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlJavaPackagesMapper implements XmlJavaDataMapper {

    private static final String PACKAGES = "packages";
    private static final String PACKAGES_PACKAGE = "package";
    private static final String PACKAGES_PACKAGE_NAME = "name";
    private static final String PACKAGES_PACKAGE_VERSION = "version";
    private static final String PACKAGES_PACKAGE_BUNDLES = "bundles";
    private static final String PACKAGES_PACKAGE_BUNDLES_BUNDLE = "bundle";
    private static final String PACKAGES_PACKAGE_BUNDLES_BUNDLE_NAME = "name";
    private static final String PACKAGES_PACKAGE_BUNDLES_BUNDLE_VERSION = "version";

    @Override
    public Element marshal(Document doc, Object object) throws Exception {
        Element packages = doc.createElement(PACKAGES);
        doc.appendChild(packages);

        SystemDeploymentPackages xdps = (SystemDeploymentPackages) object;
        SystemDeploymentPackage[] xdpArray = xdps.getDeploymentPackages();

        for (SystemDeploymentPackage xdp : xdpArray) {
            Element packageInstalled = doc.createElement(PACKAGES_PACKAGE);
            marshalDeploymentPackage(doc, xdp, packageInstalled);
            packages.appendChild(packageInstalled);
        }
        return packages;
    }

    @Override
    public <T> T unmarshal(Document doc) throws Exception {
        return null;
    }

    //
    // Marshaller's private methods
    //
    private static void marshalDeploymentPackage(Document doc, SystemDeploymentPackage xdp, Element packageInstalled) {
        // Extract data from XmlDeploymentPackage
        String packageName = xdp.getName();
        String packageVersion = xdp.getVersion();
        SystemBundle[] xbiArray = xdp.getBundleInfos();

        // Create xml elements
        if (packageName != null && !packageName.trim().isEmpty()) {
            Element name = doc.createElement(PACKAGES_PACKAGE_NAME);
            name.setTextContent(packageName);
            packageInstalled.appendChild(name);
        }

        if (packageVersion != null && !packageVersion.trim().isEmpty()) {
            Element version = doc.createElement(PACKAGES_PACKAGE_VERSION);
            version.setTextContent(packageVersion);
            packageInstalled.appendChild(version);
        }

        Element bundles = doc.createElement(PACKAGES_PACKAGE_BUNDLES);
        packageInstalled.appendChild(bundles);

        if (xbiArray != null) {
            for (SystemBundle xbi : xbiArray) {
                Element bundle = doc.createElement(PACKAGES_PACKAGE_BUNDLES_BUNDLE);
                marshalBundleInfo(doc, xbi, bundle);
                bundles.appendChild(bundle);
            }
        }
    }

    private static void marshalBundleInfo(Document doc, SystemBundle xbi, Element bundle) {
        // Extract data from XmlBundleInfo
        String bundleName = xbi.getName();
        String bundleVersion = xbi.getVersion();

        // Create xml elements
        if (bundleName != null && !bundleName.trim().isEmpty()) {
            Element name = doc.createElement(PACKAGES_PACKAGE_BUNDLES_BUNDLE_NAME);
            name.setTextContent(bundleName);
            bundle.appendChild(name);
        }

        if (bundleVersion != null && !bundleVersion.trim().isEmpty()) {
            Element version = doc.createElement(PACKAGES_PACKAGE_BUNDLES_BUNDLE_VERSION);
            version.setTextContent(bundleVersion);
            bundle.appendChild(version);
        }
    }
}
