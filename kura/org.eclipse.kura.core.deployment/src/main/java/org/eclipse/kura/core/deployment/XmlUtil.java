/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
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

import org.eclipse.kura.core.deployment.util.XmlJavaBundlesMapper;
import org.eclipse.kura.core.deployment.util.XmlJavaPackagesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class XmlUtil 
{
	@SuppressWarnings("unused")
	private static final Logger s_logger = LoggerFactory.getLogger(XmlUtil.class);


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

				new XmlJavaPackagesMapper().marshal(doc, object);

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

				new XmlJavaBundlesMapper().marshal(doc, object);

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
}