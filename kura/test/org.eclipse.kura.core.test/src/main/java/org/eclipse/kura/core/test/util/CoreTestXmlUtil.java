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
package org.eclipse.kura.core.test.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.eclipse.kura.core.configuration.XmlSnapshotIdResult;
import org.eclipse.kura.core.inventory.resources.SystemBundle;
import org.eclipse.kura.core.inventory.resources.SystemBundles;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackage;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CoreTestXmlUtil {

    /*
     * The following methods are for Java XML objects that don't need an
     * unmarshal method during normal operation. The below methods are only
     * needed for testing purposes.
     *
     */
    public static <T> T unmarshal(String s, Class<T> clazz) throws XMLStreamException, FactoryConfigurationError {
        StringReader sr = new StringReader(s);
        System.out.println("CoreTestXmlUtil: 30");
        return unmarshal(sr, clazz);
    }

    public static <T> T unmarshal(Reader r, Class<T> clazz) throws XMLStreamException, FactoryConfigurationError {
        DocumentBuilderFactory factory = null;
        DocumentBuilder parser = null;

        try {
            factory = DocumentBuilderFactory.newInstance();
            parser = factory.newDocumentBuilder();
        } catch (FactoryConfigurationError fce) {
            // The implementation is not available or cannot be instantiated
            System.out.println("Parser Factory configuration Error");
            throw fce;
        } catch (ParserConfigurationException pce) {
            // the parser cannot be created with the specified configuration
            System.out.println("Parser configuration exception");
            throw new FactoryConfigurationError(pce);
        }

        // parse the document
        Document doc = null;
        try {
            InputSource is = new InputSource(r);
            doc = parser.parse(is);
            doc.getDocumentElement().normalize();

            // Select the correct unmarshal method
            if (clazz.equals(SystemDeploymentPackages.class)) {
                return unmarshalSystemDeploymentPackages(doc);
            } else if (clazz.equals(SystemBundles.class)) {
                return unmarshalXmlBundles(doc);
            } else if (clazz.equals(XmlSnapshotIdResult.class)) {
                return unmarshalXmlSnapshotIdResult(doc);
            }
        } catch (SAXException se) {
            throw new XMLStreamException(se.getMessage());
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe.getMessage());
        } catch (IllegalArgumentException iae) {
            throw new XMLStreamException(iae.getMessage());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    // unmarshal SystemDeploymentPackages
    @SuppressWarnings("unchecked")
    public static <T> T unmarshalSystemDeploymentPackages(Document doc) throws Exception {
        // Java objects for unmarshal
        SystemDeploymentPackages systemDeploymentPackages = new SystemDeploymentPackages();
        List<SystemDeploymentPackage> systemDeploymentPackageList = new ArrayList<>();

        // XML elements
        Element packages = doc.getDocumentElement();
        NodeList packagesChildren = packages.getChildNodes();

        // Collect all packages
        for (int propIndex = 0; propIndex < packagesChildren.getLength(); propIndex++) {
            Node currentNode = packagesChildren.item(propIndex);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) currentNode;
                if (el.getNodeName().equals("package")) {
                    String name = "";
                    String version = "";
                    SystemBundle[] bundleInfos = new SystemBundle[0];

                    NodeList nodeList = el.getChildNodes();
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node tmpNode = nodeList.item(i);

                        // Set package name
                        if (tmpNode.getNodeName().equals("name")) {
                            name = tmpNode.getTextContent();
                        } else if (tmpNode.getNodeName().equals("version")) {
                            version = tmpNode.getTextContent();
                        } else if (tmpNode.getNodeName().equals("bundles")) {
                            bundleInfos = parseBundles(tmpNode);
                        }
                    }
                    SystemDeploymentPackage xdp = new SystemDeploymentPackage(name, version);
                    xdp.setBundleInfos(bundleInfos);

                    systemDeploymentPackageList.add(xdp);
                }
            }
        }

        // Add packages
        systemDeploymentPackages.setDeploymentPackages(
                systemDeploymentPackageList.toArray(new SystemDeploymentPackage[systemDeploymentPackageList.size()]));

        return (T) systemDeploymentPackages;
    }

    // unmarshal SystemBundles
    @SuppressWarnings("unchecked")
    public static <T> T unmarshalXmlBundles(Document doc) throws Exception {
        SystemBundles systemBundles = new SystemBundles();
        List<SystemBundle> systemBundleList = new ArrayList<>();

        // XML elements
        Element bundles = doc.getDocumentElement();
        NodeList bundlesChildren = bundles.getChildNodes();

        // Collect all bundles
        for (int propIndex = 0; propIndex < bundlesChildren.getLength(); propIndex++) {
            Node currentNode = bundlesChildren.item(propIndex);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                String name = "";
                String version = "";
                long id = -1;
                String state = "";

                Element el = (Element) currentNode;
                if (el.getNodeName().equals("bundle")) {
                    NodeList nodeList = el.getChildNodes();
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node tmpNode = nodeList.item(i);

                        // Set bundle name
                        if (tmpNode.getNodeName().equals("name")) {
                            name = tmpNode.getTextContent();
                        }

                        // Set bundle version
                        if (tmpNode.getNodeName().equals("version")) {
                            version = tmpNode.getTextContent();
                        }

                        // Set bundle ID
                        if (tmpNode.getNodeName().equals("id")) {
                            id = Long.parseLong(tmpNode.getTextContent());
                        }

                        // Set bundle state
                        if (tmpNode.getNodeName().equals("state")) {
                            state = tmpNode.getTextContent();
                        }
                    }
                    SystemBundle xb = new SystemBundle(name, version);
                    xb.setId(id);
                    xb.setState(state);

                    // Add bundle to list
                    systemBundleList.add(xb);
                }
            }
        }

        // Add bundles
        systemBundles.setBundles(systemBundleList.toArray(new SystemBundle[systemBundleList.size()]));

        return (T) systemBundles;
    }

    // unmarshal XmlSnapshotIdResult
    @SuppressWarnings("unchecked")
    public static <T> T unmarshalXmlSnapshotIdResult(Document doc) throws Exception {
        XmlSnapshotIdResult xmlSnapshotIdResult = new XmlSnapshotIdResult();
        List<Long> idList = new ArrayList<>();

        // XML elements
        Element snapshotIds = doc.getDocumentElement();
        NodeList snapshotIdsChildren = snapshotIds.getChildNodes();

        // Collect all bundles
        for (int propIndex = 0; propIndex < snapshotIdsChildren.getLength(); propIndex++) {
            Node currentNode = snapshotIdsChildren.item(propIndex);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) currentNode;
                if (el.getNodeName().equals("esf:snapshotIds")) {
                    // Add ID to list
                    idList.add(Long.parseLong(el.getTextContent()));
                }
            }
        }

        // Add IDs
        xmlSnapshotIdResult.setSnapshotIds(idList);

        return (T) xmlSnapshotIdResult;
    }

    private static SystemBundle[] parseBundles(Node node) {
        List<SystemBundle> bundleInfos = new ArrayList<>();
        NodeList nodeList = node.getChildNodes();

        // Get information for each bundle
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node bundleNode = nodeList.item(i);
            if (bundleNode.getNodeType() == Node.ELEMENT_NODE) {
                String name = "";
                String version = "";

                NodeList infoList = bundleNode.getChildNodes();
                for (int j = 0; j < infoList.getLength(); j++) {
                    Node tmpNode = infoList.item(j);

                    // Set Bundle Name
                    if (tmpNode.getNodeName().equals("name")) {
                        name = tmpNode.getTextContent();
                    } else if (tmpNode.getNodeName().equals("version")) {
                        version = tmpNode.getTextContent();
                    }
                }

                SystemBundle xbi = new SystemBundle(name, version);
                // Add bundle to array
                bundleInfos.add(xbi);
            }
        }

        return bundleInfos.toArray(new SystemBundle[bundleInfos.size()]);
    }
}
