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
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.XmlConfigPropertiesAdapted;
import org.eclipse.kura.core.configuration.XmlConfigPropertiesAdapter;
import org.eclipse.kura.core.configuration.XmlConfigPropertyAdapted;
import org.eclipse.kura.core.configuration.XmlConfigPropertyAdapted.ConfigPropertyType;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tdesignate;
import org.eclipse.kura.core.configuration.metatype.Ticon;
import org.eclipse.kura.core.configuration.metatype.Tmetadata;
import org.eclipse.kura.core.configuration.metatype.Tobject;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
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
	private static final String CONFIGURATIONS = "configurations";
	private static final String PROPERTIES = "properties";

	private final static String CONFIGURATION_PID = "pid";

	private static final String CONFIGURATION = "configuration";
	private final static String CONFIGURATION_PROPERTY= "property";
	private final static String CONFIGURATION_PROPERTY_NAME = "name";
	private final static String CONFIGURATION_PROPERTY_ARRAY = "array";
	private final static String CONFIGURATION_PROPERTY_ENCRYPTED = "encrypted";
	private final static String CONFIGURATION_PROPERTY_TYPE = "type";
	private final static String CONFIGURATION_PROPERTY_VALUE= "value";


	private final static String METADATA_LOCALIZATION = "localization";

	private final static String METADATA_OCD_NAME = "name";
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

	private final static String METADATA_AD_OPTION = "Option";
	private final static String METADATA_AD_OPTION_LABEL = "label";
	private final static String METADATA_AD_OPTION_VALUE = "value";

	private final static String METADATA_DESIGNATE_OBJECT = "Object";
	private final static String METADATA_DESIGNATE_PID = "pid";
	private final static String METADATA_DESIGNATE_FACTORY_PID = "factoryPid";
	private final static String METADATA_DESIGNATE_BUNDLE = "bundle";
	private final static String METADATA_DESIGNATE_OPTIONAL = "optional";
	private final static String METADATA_DESIGNATE_MERGE = "merge";

	private final static String METADATA_DESIGNATE_OBJECT_ATTRIBUTE = "Attribute";
	private final static String METADATA_DESIGNATE_OBJECT_OCDREF = "ocdref";
	


	public static String marshal(Object object) throws Exception 
	{
		StringWriter sw = new StringWriter();
		marshal(object, sw);
		return sw.toString();
	}

	public static void marshal(Object object, Writer w) throws Exception 
	{
		if(!(object instanceof XmlComponentConfigurations)){
			throw new Exception("Unsupported Object");
		}else{

			try {				
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

				// root elements
				Document doc = docBuilder.newDocument();
				doc.setXmlStandalone(true);


				Element configurations = doc.createElement(nameSpace + ":" + CONFIGURATIONS);
				configurations.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:esf","http://eurotech.com/esf/2.0");
				configurations.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ocd","http://www.osgi.org/xmlns/metatype/v1.2.0");
				doc.appendChild(configurations);

				XmlComponentConfigurations xmlCompConfig= (XmlComponentConfigurations) object;
				List<ComponentConfigurationImpl> configsList = xmlCompConfig.getConfigurations();

				for(ComponentConfigurationImpl config: configsList){
					Element configuration= marshallConfiguration(config, doc);
					configurations.appendChild(configuration);
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


	private static Element marshallConfiguration(ComponentConfigurationImpl config, Document doc) throws Exception {
		String pid= config.getPid();

		//create configuration element
		Element configurationElement= doc.createElement(nameSpace + ":" +CONFIGURATION);
		Attr propertiesAttribute= doc.createAttribute(CONFIGURATION_PID);
		propertiesAttribute.setNodeValue(pid);
		configurationElement.setAttributeNode(propertiesAttribute);

		Map<String, Object> property= config.getConfigurationProperties();
		//Add properties Node
		Element properties= doc.createElement(nameSpace + ":" +PROPERTIES);
		configurationElement.appendChild(properties);
		marshallProperties(property, doc, properties);

		return configurationElement;
	}

	private static void marshallProperties(Map<String, Object> propertyMap, Document doc, Element properties) throws Exception {
		XmlConfigPropertiesAdapter xmlPropAdapter = new XmlConfigPropertiesAdapter();
		XmlConfigPropertiesAdapted configPropAdapted= xmlPropAdapter.marshal(propertyMap);

		XmlConfigPropertyAdapted[] propArray = configPropAdapted.getProperties();
		for(XmlConfigPropertyAdapted propertyObj:propArray){
			Element property= marshallProperty(propertyObj, doc);
			if(property != null){
				properties.appendChild(property);
			}
		}
	}

	private static Element marshallProperty(XmlConfigPropertyAdapted propertyObj, Document doc) {
		String name= propertyObj.getName();
		Boolean array= propertyObj.getArray();
		Boolean encrypted= propertyObj.isEncrypted();
		ConfigPropertyType cpt= propertyObj.getType();
		String[] values= propertyObj.getValues();

		if(values != null){
			Element property= doc.createElement(nameSpace + ":" +CONFIGURATION_PROPERTY);
			Attr attName= doc.createAttribute(CONFIGURATION_PROPERTY_NAME);
			attName.setNodeValue(name);
			property.setAttributeNode(attName);

			Attr attArray= doc.createAttribute(CONFIGURATION_PROPERTY_ARRAY);
			attArray.setNodeValue(array.toString());
			property.setAttributeNode(attArray);

			Attr attEncrypted= doc.createAttribute(CONFIGURATION_PROPERTY_ENCRYPTED);
			attEncrypted.setNodeValue(encrypted.toString());
			property.setAttributeNode(attEncrypted);

			Attr attType= doc.createAttribute(CONFIGURATION_PROPERTY_TYPE);

			attType.setNodeValue(getStringValue(cpt));
			property.setAttributeNode(attType);


			for(String value:values){
				Element valueElem= doc.createElement(nameSpace + ":" +CONFIGURATION_PROPERTY_VALUE);
				valueElem.setTextContent(value);
				property.appendChild(valueElem);
			}
			return property;
		}
		return null;
	}

	public static <T> T unmarshal(String s, Class<T> clazz) 
			throws JAXBException, XMLStreamException, FactoryConfigurationError
	{
		StringReader sr = new StringReader(s);
		T result=unmarshal(sr, clazz);
		return result;
	}


	public static <T> T unmarshal(Reader r, Class<T> clazz) 
			throws JAXBException, XMLStreamException, FactoryConfigurationError
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
		NodeList configurationList = document.getElementsByTagName(nameSpace + ":" + CONFIGURATION);

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

	private static String getStringValue(ConfigPropertyType type) {
		if(type == null){
			return "String";
		}
		if(type.equals(ConfigPropertyType.stringType)){
			return "String";
		}else if(type.equals(ConfigPropertyType.longType)){
			return "Long";
		}else if(type.equals(ConfigPropertyType.doubleType)){
			return "Double";
		}else if(type.equals(ConfigPropertyType.floatType)){
			return "Float";
		}else if(type.equals(ConfigPropertyType.integerType)){
			return "Integer";
		}else if(type.equals(ConfigPropertyType.byteType)){
			return "Byte";
		}else if(type.equals(ConfigPropertyType.charType)){
			return "Char";
		}else if(type.equals(ConfigPropertyType.booleanType)){
			return "Boolean";
		}else if(type.equals(ConfigPropertyType.shortType)){
			return "Short";
		}else if(type.equals(ConfigPropertyType.passwordType)){
			return "Password";
		}
		return "String";
	}


	//metadata parsing
	@SuppressWarnings("unchecked")
	private static <T> T unmarshalMetaData(Document document) {
		Element metadata = document.getDocumentElement();
		Tmetadata tMetadata= parseMetadataAttributes(metadata);

		NodeList metadataChilds= metadata.getChildNodes();
		Element[] metadataChildsArray= getElementNodes(metadataChilds);

		for(int metadataChildsIndex=0; metadataChildsIndex < metadataChildsArray.length; metadataChildsIndex++){
			Element node= metadataChildsArray[metadataChildsIndex];
			String localName= node.getNodeName();
			if(localName.equals("OCD")){
				Tocd tocd= parseOCD(node);
				tMetadata.setOCD(tocd);
			}else if(localName.equals("Designate")){
				Tdesignate tDesignate= parseDesignate(node);
				tMetadata.setDesignate(tDesignate);
			}
		}


		return (T) tMetadata; 
	}

	private static Tocd parseOCD(Element ocd) {

		String ocdName= ocd.getAttribute(METADATA_OCD_NAME);
		String ocdID= ocd.getAttribute(METADATA_OCD_ID);
		String ocdDescription= ocd.getAttribute(METADATA_OCD_DESCRIPTION);
		Tocd tocd= new Tocd();

		if(!ocdID.trim().isEmpty()){
			tocd.setId(ocdID);
		}
		if(!ocdName.trim().isEmpty()){
			tocd.setName(ocdName);
		}
		if(!ocdDescription.trim().isEmpty()){
			tocd.setDescription(ocdDescription);
		}

		NodeList ocdChilds= ocd.getChildNodes();
		Element[] ocdChildElements= getElementNodes(ocdChilds);

		for(int ocdChildsIndex=0; ocdChildsIndex < ocdChildElements.length; ocdChildsIndex++){
			Element node= ocdChildElements[ocdChildsIndex];
			String localName= node.getNodeName();
			if(localName.equals("Icon")){
				//parse Icon
				Ticon tIcon= parseIcon(node);
				tocd.setIcon(tIcon);
			}else if(localName.equals("AD")){
				//parse AD
				Tad tad= parseAD(node);
				tocd.addAD(tad);
			}
		}

		return tocd;
	}

	private static Tdesignate parseDesignate(Element designate) {
		String pid= designate.getAttribute(METADATA_DESIGNATE_PID);
		String factoryPid= designate.getAttribute(METADATA_DESIGNATE_FACTORY_PID);
		String bundle= designate.getAttribute(METADATA_DESIGNATE_BUNDLE);
		Boolean optional= Boolean.parseBoolean(designate.getAttribute(METADATA_DESIGNATE_OPTIONAL));
		Boolean merge= Boolean.parseBoolean(designate.getAttribute(METADATA_DESIGNATE_MERGE));

		Tdesignate tDesignate= new Tdesignate();
		if(!pid.trim().isEmpty()){
			tDesignate.setPid(pid);
		}
		if(!factoryPid.trim().isEmpty()){
			tDesignate.setFactoryPid(factoryPid);
		}
		if(!bundle.trim().isEmpty()){
			tDesignate.setBundle(bundle);
		}
		tDesignate.setOptional(optional);
		tDesignate.setMerge(merge);

		NodeList objectsChilds= designate.getChildNodes();
		Element[] objectsChildElements= getElementNodes(objectsChilds);

		for(int objectsChildsIndex=0; objectsChildsIndex < objectsChildElements.length; objectsChildsIndex++){
			Element node= objectsChildElements[objectsChildsIndex];
			String localName= node.getNodeName();
			if(localName.equals(METADATA_DESIGNATE_OBJECT)){
				//parse Object
				Tobject tObject= parseObject(node);
				tDesignate.setObject(tObject);
			}
		}

		return tDesignate;

	}

	private static Tobject parseObject(Element object) {
		String ocdref= object.getAttribute(METADATA_DESIGNATE_OBJECT_OCDREF);

		Tobject tObject= new Tobject();
		if(!ocdref.trim().isEmpty()){
			tObject.setOcdref(ocdref);
		}


		NodeList attributeChilds= object.getChildNodes();
		Element[] attributeChildElements= getElementNodes(attributeChilds);
		for(int attributeChildsIndex=0; attributeChildsIndex < attributeChildElements.length; attributeChildsIndex++){
			Element node= attributeChildElements[attributeChildsIndex];
			String localName= node.getNodeName();
			if(localName.equals(METADATA_DESIGNATE_OBJECT_ATTRIBUTE)){
				//parse Attribute
				// TODO
			}
		}

		return tObject;

	}

	private static Ticon parseIcon(Element icon) {
		Ticon result= new Ticon();

		String resource= icon.getAttribute(METADATA_IMAGE_RESOURCE);
		BigInteger size= new BigInteger(icon.getAttribute(METADATA_IMAGE_SIZE));
		if(size.signum() >= 0){
			result.setSize(size);
		}else{
			result.setSize(new BigInteger("0"));
		}
		if(!resource.trim().isEmpty()){
			result.setResource(resource);
		}

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

		if(!id.trim().isEmpty()){
			tad.setId(id);
		}
		if(!name.trim().isEmpty()){
			tad.setName(name);
		}

		tad.setType(type);
		tad.setCardinality(cardinality);
		tad.setRequired(required);

		if(!defaultVal.trim().isEmpty()){
			tad.setDefault(defaultVal);
		}
		if(!description.trim().isEmpty()){
			tad.setDescription(description);
		}
		if(!min.trim().isEmpty()){
			tad.setMin(min);
		}
		if(!max.trim().isEmpty()){
			tad.setMax(max);
		}

		//parse Option
		NodeList optionChilds= adElement.getChildNodes();
		Element[] optionChildElements= getElementNodes(optionChilds);
		for(int optionChildsIndex=0; optionChildsIndex < optionChildElements.length; optionChildsIndex++){
			Element node= optionChildElements[optionChildsIndex];
			String localName= node.getNodeName();
			if(localName.equals(METADATA_AD_OPTION)){
				//parse Option
				Toption tOption= parseOption(node);
				tad.setOption(tOption);
			}
		}

		return tad;
	}

	private static Toption parseOption(Element option) {
		Toption tOption= new Toption();

		String label= option.getAttribute(METADATA_AD_OPTION_LABEL);
		String value= option.getAttribute(METADATA_AD_OPTION_VALUE);

		if(!label.trim().isEmpty()){
			tOption.setLabel(label);
		}
		if(!value.trim().isEmpty()){
			tOption.setValue(value);
		}
		return tOption;
	}

	private static Tmetadata parseMetadataAttributes(Element metadata) {
		Tmetadata tMetadata= new Tmetadata();
		String localization= metadata.getAttribute(METADATA_LOCALIZATION);

		if (!localization.trim().isEmpty()){ 
			tMetadata.setLocalization(localization);
		}

		return tMetadata;
	}
}
