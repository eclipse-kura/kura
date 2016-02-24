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
package org.eclipse.kura.core.configuration.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.XmlSnapshotIdResult;
import org.eclipse.kura.core.configuration.util.serializers.XmlJavaComponentConfigurationsMapper;
import org.eclipse.kura.core.configuration.util.serializers.XmlJavaMetadataMapper;
import org.eclipse.kura.core.configuration.util.serializers.XmlJavaSnapshotIdResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlUtil 
{
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

			if(object instanceof XmlSnapshotIdResult){
				// Resulting xml:
				// <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
				// <esf:snapshot-ids xmlns:ocd="http://www.osgi.org/xmlns/metatype/v1.2.0" xmlns:esf="http://eurotech.com/esf/2.0">
				//	 <esf:snapshotIds>1434122113492</esf:snapshotIds>
				//	 <esf:snapshotIds>1434122124387</esf:snapshotIds>
				// </esf:snapshot-ids>

				new XmlJavaSnapshotIdResultMapper().marshal(doc, object);

			}else if(object instanceof XmlComponentConfigurations){		
				new XmlJavaComponentConfigurationsMapper().marshal(doc, object);
			}

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);

			StreamResult result = new StreamResult(w); //System.out
			transformer.transform(source, result);
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}
	}


	//un-marshalling
	public static <T> T unmarshal(String s, Class<T> clazz) 
			throws XMLStreamException, FactoryConfigurationError
	{
		StringReader sr = new StringReader(s);
		T result=unmarshal(sr, clazz);
		return result;
	}

	public static <T> T unmarshal(Reader r, Class<T> clazz) 
			throws XMLStreamException, FactoryConfigurationError
	{
		DocumentBuilderFactory factory = null;
		DocumentBuilder parser = null;

		try {
			factory = DocumentBuilderFactory.newInstance();
			//factory.setValidating(true);
			parser = factory.newDocumentBuilder();
		} catch (FactoryConfigurationError fce) {
			// The implementation is not available or cannot be instantiated
			s_logger.error("Parser Factory configuration Error");
			throw fce;
		} catch (ParserConfigurationException pce) {
			// the parser cannot be created with the specified configuration
			s_logger.error("Parser configuration exception");
			throw new FactoryConfigurationError(pce);
		}

		// parse the document
		Document doc= null;
		try {
			InputSource is = new InputSource(r);
			doc = parser.parse(is);
			doc.getDocumentElement().normalize();
		} catch (SAXException se) {
			throw new XMLStreamException(se.getMessage());
		} catch (IOException ioe) {
			throw new XMLStreamException(ioe.getMessage());
		} catch (IllegalArgumentException iae) {
			throw new XMLStreamException(iae.getMessage());
		}

		//identify the correct parser that has to execute
		if(clazz.equals(XmlComponentConfigurations.class)){
			try {
				// Snapshot parser
				return new XmlJavaComponentConfigurationsMapper().unmarshal(doc);
			} catch (Exception e) {
				throw new XMLStreamException(e.getMessage());
			}
		}else {
			// MetaData parser
			return new XmlJavaMetadataMapper().unmarshal(doc);
		}
	}
}