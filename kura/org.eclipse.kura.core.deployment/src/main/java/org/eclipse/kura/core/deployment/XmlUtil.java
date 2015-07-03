/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.deployment;

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlUtil 
{
	@SuppressWarnings("unused")
	private static final Logger s_logger = LoggerFactory.getLogger(XmlUtil.class);

	private static final String PACKAGES = "packages";
	private static final String PACKAGES_PACKAGE = "package";
	private static final String PACKAGES_PACKAGE_NAME = "name";
	private static final String PACKAGES_PACKAGE_VERSION = "version";
	private static final String PACKAGES_PACKAGE_BUNDLES = "bundles";
	private static final String PACKAGES_PACKAGE_BUNDLES_BUNDLE = "bundle";
	private static final String PACKAGES_PACKAGE_BUNDLES_BUNDLE_NAME = "name";
	private static final String PACKAGES_PACKAGE_BUNDLES_BUNDLE_VERSION = "version";

	private static final String BUNDLES = "bundles";
	private static final String BUNDLES_BUNDLE = "bundle";
	private static final String BUNDLES_BUNDLE_NAME = "name";
	private static final String BUNDLES_BUNDLE_VERSION = "version";
	private static final String BUNDLES_BUNDLE_ID = "id";
	private static final String BUNDLES_BUNDLE_STATE = "state";


	//
	// Public methods
	//

	//Marshalling
	public static String marshal(Object object) throws Exception 
	{
		StringWriter sw = new StringWriter();
		marshal(object, sw);
		return sw.toString();
	}

	public static void marshal(Object object, Writer w) throws Exception 
	{
		try{
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			Document doc = docBuilder.newDocument();
			doc.setXmlStandalone(true);

			if(object instanceof XmlDeploymentPackages){
				// Expected resulting xml:
				// <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
				// <packages>
				// 		<package>
				//			<name>org.eclipse.kura.demo.heater</name>
				//			<version>1.2.0.qualifier</version>
				//			<bundles>
				//				<bundle>
				//					<name>org.eclipse.kura.demo.heater</name>
				//				    <version>1.0.1</version>
				//				</bundle>
				//			</bundles>
				//		</package>
				// </packages>

				Element packages = doc.createElement(PACKAGES);
				doc.appendChild(packages);

				XmlDeploymentPackages xdps= (XmlDeploymentPackages) object;
				XmlDeploymentPackage[] xdpArray= xdps.getDeploymentPackages();

				for(XmlDeploymentPackage xdp:xdpArray){
					Element packageInstalled= doc.createElement(PACKAGES_PACKAGE);
					marshalDeploymentPackage(doc, xdp, packageInstalled);
					packages.appendChild(packageInstalled);
				}

			}else if (object instanceof XmlBundles){
				// Expected resulting xml:
				// <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
				// <bundles>
				// 		<bundle>
				// 			<name>org.eclipse.osgi</name>
				//			<version>3.8.1.v20120830-144521</version>
				//			<id>0</id>
				//			<state>ACTIVE</state>
				//		</bundle>
				// </bundles>

				Element bundles = doc.createElement(BUNDLES);
				doc.appendChild(bundles);

				XmlBundles xmlBundles= (XmlBundles) object;
				XmlBundle[] xmlBundleArray= xmlBundles.getBundles();

				for(XmlBundle xmlBundle:xmlBundleArray){
					Element bundle= doc.createElement(BUNDLES_BUNDLE);
					marshalBundle(doc, xmlBundle, bundle);
					bundles.appendChild(bundle);
				}

			} else {
				throw new Exception("Unsupported class");
			}
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);

			StreamResult result = new StreamResult(w); //System.out
			transformer.transform(source, result);
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}
	}

	//
	// Marshaller's private methods
	//
	private static void marshalDeploymentPackage(Document doc, XmlDeploymentPackage xdp, Element packageInstalled) {
		//Extract data from XmlDeploymentPackage
		String packageName= xdp.getName();
		String packageVersion= xdp.getVersion();
		XmlBundleInfo[] xbiArray= xdp.getBundleInfos();

		//Create xml elements
		if(packageName != null && !packageName.trim().isEmpty()){
			Element name= doc.createElement(PACKAGES_PACKAGE_NAME);
			name.setTextContent(packageName);
			packageInstalled.appendChild(name);
		}

		if(packageVersion != null && !packageVersion.trim().isEmpty()){
			Element version= doc.createElement(PACKAGES_PACKAGE_VERSION);
			version.setTextContent(packageVersion);
			packageInstalled.appendChild(version);
		}

		Element bundles= doc.createElement(PACKAGES_PACKAGE_BUNDLES);
		packageInstalled.appendChild(bundles);

		if(xbiArray != null){
			for(XmlBundleInfo xbi:xbiArray){
				Element bundle= doc.createElement(PACKAGES_PACKAGE_BUNDLES_BUNDLE);
				marshalBundleInfo(doc, xbi, bundle);
				bundles.appendChild(bundle);
			}
		}
	}

	private static void marshalBundleInfo(Document doc, XmlBundleInfo xbi, Element bundle) {
		//Extract data from XmlBundleInfo
		String bundleName= xbi.getName();
		String bundleVersion= xbi.getVersion();

		//Create xml elements
		if(bundleName != null && !bundleName.trim().isEmpty()){
			Element name= doc.createElement(PACKAGES_PACKAGE_BUNDLES_BUNDLE_NAME);
			name.setTextContent(bundleName);
			bundle.appendChild(name);
		}

		if(bundleVersion != null && !bundleVersion.trim().isEmpty()){
			Element version= doc.createElement(PACKAGES_PACKAGE_BUNDLES_BUNDLE_VERSION);
			version.setTextContent(bundleVersion);
			bundle.appendChild(version);
		}
	}

	private static void marshalBundle(Document doc, XmlBundle xmlBundle, Element bundle) {
		//Extract data from XmlBundle
		String bundleName= xmlBundle.getName();
		String bundleVersion= xmlBundle.getVersion();
		String bundleId= Long.toString(xmlBundle.getId());
		String bundleState= xmlBundle.getState();

		//Create xml elements
		if(bundleName != null && !bundleName.trim().isEmpty()){
			Element name= doc.createElement(BUNDLES_BUNDLE_NAME);
			name.setTextContent(bundleName);
			bundle.appendChild(name);
		}

		if(bundleVersion != null && !bundleVersion.trim().isEmpty()){
			Element version= doc.createElement(BUNDLES_BUNDLE_VERSION);
			version.setTextContent(bundleVersion);
			bundle.appendChild(version);
		}

		if(bundleId != null && !bundleId.trim().isEmpty()){
			Element id= doc.createElement(BUNDLES_BUNDLE_ID);
			id.setTextContent(bundleId);
			bundle.appendChild(id);
		}

		if(bundleState != null && !bundleState.trim().isEmpty()){
			Element state= doc.createElement(BUNDLES_BUNDLE_STATE);
			state.setTextContent(bundleState);
			bundle.appendChild(state);
		}
	}
}