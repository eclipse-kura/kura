/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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

import org.eclipse.kura.core.deployment.xml.XmlBundle;
import org.eclipse.kura.core.deployment.xml.XmlBundles;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlJavaBundlesMapper implements XmlJavaDataMapper {

    private static final String BUNDLES = "bundles";
    private static final String BUNDLES_BUNDLE = "bundle";
    private static final String BUNDLES_BUNDLE_NAME = "name";
    private static final String BUNDLES_BUNDLE_VERSION = "version";
    private static final String BUNDLES_BUNDLE_ID = "id";
    private static final String BUNDLES_BUNDLE_STATE = "state";

    @Override
    public Element marshal(Document doc, Object object) throws Exception {
        Element bundles = doc.createElement(BUNDLES);
        doc.appendChild(bundles);

        XmlBundles xmlBundles = (XmlBundles) object;
        XmlBundle[] xmlBundleArray = xmlBundles.getBundles();

        for (XmlBundle xmlBundle : xmlBundleArray) {
            Element bundle = doc.createElement(BUNDLES_BUNDLE);
            marshallBundle(doc, xmlBundle, bundle);
            bundles.appendChild(bundle);
        }
        return bundles;
    }

    @Override
    public <T> T unmarshal(Document doc) throws Exception {
        return null;
    }

    //
    // Marshaller's private methods
    //
    private static void marshallBundle(Document doc, XmlBundle xmlBundle, Element bundle) {
        // Extract data from XmlBundle
        String bundleName = xmlBundle.getName();
        String bundleVersion = xmlBundle.getVersion();
        String bundleId = Long.toString(xmlBundle.getId());
        String bundleState = xmlBundle.getState();

        // Create xml elements
        if (bundleName != null && !bundleName.trim().isEmpty()) {
            Element name = doc.createElement(BUNDLES_BUNDLE_NAME);
            name.setTextContent(bundleName);
            bundle.appendChild(name);
        }

        if (bundleVersion != null && !bundleVersion.trim().isEmpty()) {
            Element version = doc.createElement(BUNDLES_BUNDLE_VERSION);
            version.setTextContent(bundleVersion);
            bundle.appendChild(version);
        }

        if (bundleId != null && !bundleId.trim().isEmpty()) {
            Element id = doc.createElement(BUNDLES_BUNDLE_ID);
            id.setTextContent(bundleId);
            bundle.appendChild(id);
        }

        if (bundleState != null && !bundleState.trim().isEmpty()) {
            Element state = doc.createElement(BUNDLES_BUNDLE_STATE);
            state.setTextContent(bundleState);
            bundle.appendChild(state);
        }
    }
}
