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
package org.eclipse.kura.core.configuration.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.XmlConfigPropertiesAdapted;
import org.eclipse.kura.core.configuration.XmlConfigPropertiesAdapter;
import org.eclipse.kura.core.configuration.XmlConfigPropertyAdapted;
import org.eclipse.kura.core.configuration.XmlConfigPropertyAdapted.ConfigPropertyType;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Ticon;
import org.eclipse.kura.core.configuration.metatype.Tmetadata;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlUtil 
{
	private static final Logger s_logger = LoggerFactory.getLogger(XmlUtil.class);
	private static final String nameSpace = "esf";
	private static final String configuration = "configuration";

	private final static String CONFIGURATION_PID = "pid";

	private final static String CONFIGURATION_PROPERTY_NAME = "name";
	private final static String CONFIGURATION_PROPERTY_ARRAY = "array";
	private final static String CONFIGURATION_PROPERTY_ENCRYPTED = "encrypted";
	private final static String CONFIGURATION_PROPERTY_TYPE = "type";
	
	
	private final static String METADATA_LOCALIZATION = "localization";
	
	private final static String METADATA_OCD_NAME = "nome";
	private final static String METADATA_OCD_ID = "id";
	private final static String METADATA_OCD_DESCRIPTION = "description";
	
	private final static String METADATA_IMAGE_RESOURCE = "resource";
	private final static String METADATA_IMAGE_SIZE = "size";
	
	private final static String METADATA_AD_ID = "id";
	private final static String METADATA_AD_NAME = "name";
	private final static String METADATA_AD_TYPE = "type";
	private final static String METADATA_AD_CARDINALITY = "cardinality";
	private final static String METADATA_AD_REQUIRED = "required";
	private final static String METADATA_AD_DEFAULT = "default";
	private final static String METADATA_AD_DESCRIPTION = "description";
	private final static String METADATA_AD_MIN = "min";
	private final static String METADATA_AD_MAX = "max";
	

	@SuppressWarnings("rawtypes")
	private static Map<Class,JAXBContext> s_contexts = new HashMap<Class,JAXBContext>();	


	public static String marshal(Object object) throws JAXBException 
	{
		StringWriter sw = new StringWriter();
		marshal(object, sw);
		return sw.toString();
	}

	@SuppressWarnings("rawtypes")
	public static void marshal(Object object, Writer w) throws JAXBException 
	{
		Class clazz = object.getClass();
		JAXBContext context = s_contexts.get(clazz);
		if (context == null) {			
			context = JAXBContext.newInstance(clazz);
			s_contexts.put(clazz, context);
		}

		ValidationEventCollector valEventHndlr = new ValidationEventCollector();
		Marshaller marshaller = context.createMarshaller();
		marshaller.setSchema(null);
		marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setEventHandler(valEventHndlr);

		try {
			marshaller.marshal(object, w);
		}
		catch (Exception e) {		
			if (e instanceof JAXBException) {
				throw (JAXBException) e;
			}
			else {
				throw new MarshalException(e.getMessage(), e);	
			}			
		}
		if (valEventHndlr.hasEvents()) {			
			for (ValidationEvent valEvent : valEventHndlr.getEvents()) {
				if (valEvent.getSeverity() != ValidationEvent.WARNING) {
					// throw a new Marshall Exception if there is a parsing error
					throw new MarshalException(valEvent.getMessage(), valEvent.getLinkedException());							
				}
			}
		}
	}


	public static <T> T unmarshal(String s, Class<T> clazz) 
			throws JAXBException, XMLStreamException, FactoryConfigurationError
	{
		StringReader sr = new StringReader(s);
		return unmarshal(sr, clazz);
	}


	public static <T> T unmarshal(StringReader r, Class<T> clazz) 
			throws JAXBException, XMLStreamException, FactoryConfigurationError
	{
		/*
		JAXBContext context = s_contexts.get(clazz);
		if (context == null) {			
			context = JAXBContext.newInstance(clazz);
			s_contexts.put(clazz, context);
		}

		ValidationEventCollector valEventHndlr = new ValidationEventCollector();
		XMLStreamReader xmlsr = XMLInputFactory.newFactory().createXMLStreamReader(r);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		unmarshaller.setSchema(null);
		unmarshaller.setEventHandler(valEventHndlr);

		JAXBElement<T> elem = null;
		try {
			elem = unmarshaller.unmarshal(xmlsr, clazz);
		}
		catch (Exception e) {		
			if (e instanceof JAXBException) {
				throw (JAXBException) e;
			}
			else {
				throw new UnmarshalException(e.getMessage(), e);	
			}			
		}

		if (valEventHndlr.hasEvents()) {			
			for (ValidationEvent valEvent : valEventHndlr.getEvents()) {
				if (valEvent.getSeverity() != ValidationEvent.WARNING) {
					// throw a new Unmarshall Exception if there is a parsing error
					String msg = MessageFormat.format("Line {0}, Col: {1}: {2}",
							valEvent.getLocator().getLineNumber(),
							valEvent.getLocator().getColumnNumber(),
							valEvent.getLinkedException().getMessage());
					throw new UnmarshalException(msg, valEvent.getLinkedException());
				}
			}
		}
		return elem.getValue();
		 */

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
			// a parser cannot be created with the requested configuration
			s_logger.error("Parser configuration exception");
			throw new FactoryConfigurationError(pce);
		}

		// parse the document
		Document document = null;
		try {
			InputSource is = new InputSource(r);
			document = parser.parse(is);
			document.getDocumentElement().normalize();
		} catch (SAXException se) {
			// Some general parse error occurred. Might be thrown because DocumentBuilder
			// class reuses several classes from the SAX API.
			throw new XMLStreamException(se.getMessage());
		} catch (IOException ioe) {
			// Some IO error occurred
			throw new XMLStreamException(ioe.getMessage());
		} catch (IllegalArgumentException iae) {
			// filename is null
			throw new XMLStreamException(iae.getMessage());
		}

		//identify the correct parser that has to execute
		if(clazz.equals(XmlComponentConfigurations.class)){
			try {
				//Snapshot parser
				return unmarshalXmlComponentConfig(document);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new XMLStreamException(e.getMessage());
			}
		}else {
			//MetaData parser
			return unmarshalMetaData(document);
		}

	}

	@SuppressWarnings("unchecked")
	private static <T> T unmarshalXmlComponentConfig(Document document) throws Exception {
		XmlComponentConfigurations xcc= new XmlComponentConfigurations();
		
		//Get all configurations
		NodeList configurationList = document.getElementsByTagName(nameSpace + ":" + configuration);

		List<ComponentConfigurationImpl> compConfList= new ArrayList<ComponentConfigurationImpl>();
		//Iterate through all the configuration elements inside configurations tag
		for(int configIndex= 0; configIndex < configurationList.getLength(); configIndex++){

			Element configuration = (Element) configurationList.item(configIndex);
			ComponentConfigurationImpl cci= parseConfiguration(configuration);
			compConfList.add(cci);
		}
		xcc.setConfigurations(compConfList);

		return (T) xcc;
	}

	private static ComponentConfigurationImpl parseConfiguration(Element configuration) throws Exception {
		XmlConfigPropertiesAdapter xmlPropAdapter= new XmlConfigPropertiesAdapter();

		//get configuration's properties
		NodeList propertiesList = configuration.getChildNodes();
		
		//get effective elements
		Element[] propertiesArray= getElementNodes(propertiesList);

		XmlConfigPropertiesAdapted xmlPropertiesAdapted= new XmlConfigPropertiesAdapted();
		for(int propIndex=0; propIndex < propertiesArray.length; propIndex++){
			//parse property elements
			NodeList propertyList= propertiesArray[propIndex].getChildNodes();
			Element[] propertyArray= getElementNodes(propertyList);
			
			XmlConfigPropertyAdapted[] xmlConfigProperties= new XmlConfigPropertyAdapted[propertyArray.length];
			for(int propertyIndex=0; propertyIndex < propertyArray.length; propertyIndex++){
				XmlConfigPropertyAdapted xmlProperty= parseProperty(propertyArray[propertyIndex]);
				xmlConfigProperties[propertyIndex] = xmlProperty;
			}
			xmlPropertiesAdapted.setProperties(xmlConfigProperties);
		}

		Map<String, Object> propertiesMap= xmlPropAdapter.unmarshal(xmlPropertiesAdapted);

		String pid= configuration.getAttribute(CONFIGURATION_PID);
		return new ComponentConfigurationImpl(pid, null, propertiesMap);
	}

	private static XmlConfigPropertyAdapted parseProperty(Element property) {
		NodeList valuesList= property.getChildNodes();
		Element[] valuesArray= getElementNodes(valuesList);
		String[] values= new String[valuesArray.length];

		//get values
		// TODO: check if works with arrays
		for(int valIndex = 0; valIndex < valuesArray.length; valIndex++){
			values[valIndex]= valuesArray[valIndex].getTextContent();
		}
		
		String name= property.getAttribute(CONFIGURATION_PROPERTY_NAME);
		String type= property.getAttribute(CONFIGURATION_PROPERTY_TYPE);
		String array= property.getAttribute(CONFIGURATION_PROPERTY_ARRAY);
		String encrypted= property.getAttribute(CONFIGURATION_PROPERTY_ENCRYPTED);

		ConfigPropertyType cct= getType(type);

		XmlConfigPropertyAdapted xmlProperty= new XmlConfigPropertyAdapted(name, cct, values);
		xmlProperty.setArray(Boolean.parseBoolean(array));
		xmlProperty.setEncrypted(Boolean.parseBoolean(encrypted));
		
		return xmlProperty;
	}

	private static Element[] getElementNodes(NodeList propertiesList) {
		List<Element> elementList= new ArrayList<Element>();
		for(int propIndex=0; propIndex < propertiesList.getLength(); propIndex++){
			Node currentNode= propertiesList.item(propIndex);
			if(currentNode.getNodeType() == Node.ELEMENT_NODE){
				Element el= (Element) currentNode;
				elementList.add(el);
			}
		}
		return elementList.toArray(new Element[0]);
	}


	private static ConfigPropertyType getType(String type) {
		if(type.equals("String")){
			return ConfigPropertyType.stringType;
		}else if(type.equals("Long")){
			return ConfigPropertyType.longType;
		}else if(type.equals("Double")){
			return ConfigPropertyType.doubleType;
		}else if(type.equals("Float")){
			return ConfigPropertyType.floatType;
		}else if(type.equals("Integer")){
			return ConfigPropertyType.integerType;
		}else if(type.equals("Byte")){
			return ConfigPropertyType.byteType;
		}else if(type.equals("Char")){
			return ConfigPropertyType.charType;
		}else if(type.equals("Boolean")){
			return ConfigPropertyType.booleanType;
		}else if(type.equals("Short")){
			return ConfigPropertyType.shortType;
		}else if(type.equals("Password")){
			return ConfigPropertyType.passwordType;
		}
		return null;
	}

	
	//metadata parsing
	@SuppressWarnings("unchecked")
	private static <T> T unmarshalMetaData(Document document) {
		Element metadata = document.getDocumentElement();
	    Tmetadata tMetadata= parseMetadataAttributes(metadata);
	    
	    NodeList ocdNodes= metadata.getElementsByTagName("OCD");
	    Tocd tocd= parseOCD(ocdNodes);
	    tMetadata.getOCD().add(tocd);
	    
	    return (T) tMetadata; 
	}

	private static Tocd parseOCD(NodeList ocdNodes) {
		//Extract ocds and get the first
		Element[] ocds= getElementNodes(ocdNodes);
		Element ocd= ocds[0];
		
		String ocdName= ocd.getAttribute(METADATA_OCD_NAME);
		String ocdID= ocd.getAttribute(METADATA_OCD_ID);
		String ocdDescription= ocd.getAttribute(METADATA_OCD_DESCRIPTION);
		Tocd tocd= new Tocd();
		tocd.setId(ocdID);
		tocd.setName(ocdName);
		tocd.setDescription(ocdDescription);
		
		//parse Icon
		NodeList iconNodes= ocd.getElementsByTagName("Icon");
		Ticon tIcon= parseIcon(iconNodes);
		tocd.getIcon().add(tIcon);
		
		//parse AD
		NodeList adNodes= ocd.getElementsByTagName("AD");
		Element[] adElements= getElementNodes(adNodes);
		for(int adIndex=0; adIndex < adElements.length; adIndex++ ){
			Element adElement= adElements[adIndex];
			Tad tad= parseAD(adElement);
			tocd.addAD(tad);
		}
		
		return tocd;
	}

	private static Ticon parseIcon(NodeList iconNodes) {
		Ticon result= new Ticon();
		Element[] iconss= getElementNodes(iconNodes);
		Element icon= iconss[0];
		
		String resource= icon.getAttribute(METADATA_IMAGE_RESOURCE);
		BigInteger size= new BigInteger(icon.getAttribute(METADATA_IMAGE_SIZE));
		if(size.signum() >= 0){
			result.setSize(size);
		}else{
			result.setSize(new BigInteger("0"));
		}
		result.setResource(resource);
		
		return result;
	}
	
	private static Tad parseAD(Element adElement) {
		Tad tad= new Tad();
		
		String id= adElement.getAttribute(METADATA_AD_ID);
		String name= adElement.getAttribute(METADATA_AD_NAME);
		Tscalar type= Tscalar.fromValue(adElement.getAttribute(METADATA_AD_TYPE));
		Integer cardinality= Integer.parseInt(adElement.getAttribute(METADATA_AD_CARDINALITY));
		Boolean required= Boolean.parseBoolean(adElement.getAttribute(METADATA_AD_REQUIRED));
		String defaultVal= adElement.getAttribute(METADATA_AD_DEFAULT);
		String description= adElement.getAttribute(METADATA_AD_DESCRIPTION);
		String min= adElement.getAttribute(METADATA_AD_MIN);
		String max= adElement.getAttribute(METADATA_AD_MAX);
		
		tad.setId(id);
		tad.setName(name);
		tad.setType(type);
		tad.setCardinality(cardinality);
		tad.setRequired(required);
		tad.setDefault(defaultVal);
		tad.setDescription(description);
		tad.setMin(min);
		tad.setMax(max);
		
		
		return tad;
	}

	private static Tmetadata parseMetadataAttributes(Element metadata) {
		Tmetadata tMetadata= new Tmetadata();
		String localization= metadata.getAttribute(METADATA_LOCALIZATION);
		
		tMetadata.setLocalization(localization);
		
		return tMetadata;
	}
}
