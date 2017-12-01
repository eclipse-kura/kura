/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.xml.marshaller.unmarshaller;

import org.eclipse.kura.core.deployment.xml.XmlBundleInfo;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackage;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackages;
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

        XmlDeploymentPackages xdps = (XmlDeploymentPackages) object;
        XmlDeploymentPackage[] xdpArray = xdps.getDeploymentPackages();

        for (XmlDeploymentPackage xdp : xdpArray) {
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
    private static void marshalDeploymentPackage(Document doc, XmlDeploymentPackage xdp, Element packageInstalled) {
        // Extract data from XmlDeploymentPackage
        String packageName = xdp.getName();
        String packageVersion = xdp.getVersion();
        XmlBundleInfo[] xbiArray = xdp.getBundleInfos();

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
            for (XmlBundleInfo xbi : xbiArray) {
                Element bundle = doc.createElement(PACKAGES_PACKAGE_BUNDLES_BUNDLE);
                marshalBundleInfo(doc, xbi, bundle);
                bundles.appendChild(bundle);
            }
        }
    }

    private static void marshalBundleInfo(Document doc, XmlBundleInfo xbi, Element bundle) {
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
