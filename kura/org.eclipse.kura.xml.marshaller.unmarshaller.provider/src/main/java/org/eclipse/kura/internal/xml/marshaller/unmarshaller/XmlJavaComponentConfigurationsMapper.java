/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.XmlConfigPropertiesAdapted;
import org.eclipse.kura.core.configuration.XmlConfigPropertiesAdapter;
import org.eclipse.kura.core.configuration.XmlConfigPropertyAdapted;
import org.eclipse.kura.core.configuration.XmlConfigPropertyAdapted.ConfigPropertyType;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlJavaComponentConfigurationsMapper implements XmlJavaDataMapper {

    private static final String CONFIGURATIONS = "configurations";
    private static final String PROPERTIES = "properties";

    private static final String CONFIGURATION_PID = "pid";

    private static final String CONFIGURATIONS_CONFIGURATION = "configuration";
    private static final String CONFIGURATIONS_CONFIGURATION_PROPERTY = "property";
    private static final String CONFIGURATIONS_CONFIGURATION_PROPERTY_NAME = "name";
    private static final String CONFIGURATIONS_CONFIGURATION_PROPERTY_ARRAY = "array";
    private static final String CONFIGURATIONS_CONFIGURATION_PROPERTY_ENCRYPTED = "encrypted";
    private static final String CONFIGURATIONS_CONFIGURATION_PROPERTY_TYPE = "type";
    private static final String CONFIGURATIONS_CONFIGURATION_PROPERTY_VALUE = "value";

    private Document marshallDoc = null;

    @Override
    public Element marshal(Document doc, Object object) throws Exception {
        this.marshallDoc = doc;
        Element configurations = doc.createElement(ESF_NAMESPACE + ":" + CONFIGURATIONS);
        configurations.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:esf", "http://eurotech.com/esf/2.0");
        configurations.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ocd",
                "http://www.osgi.org/xmlns/metatype/v1.2.0");
        doc.appendChild(configurations);

        XmlComponentConfigurations xmlCompConfig = (XmlComponentConfigurations) object;
        List<ComponentConfiguration> configs = xmlCompConfig.getConfigurations();

        if (configs != null) {
            for (ComponentConfiguration config : configs) {
                Element configuration = marshallConfiguration(config);
                configurations.appendChild(configuration);
            }
        }
        return configurations;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unmarshal(Document doc) throws Exception {
        XmlComponentConfigurations xcc = new XmlComponentConfigurations();

        // Get all configurations
        NodeList configurationList = doc.getElementsByTagName(ESF_NAMESPACE + ":" + CONFIGURATIONS_CONFIGURATION);

        List<ComponentConfiguration> compConfList = new ArrayList<>();
        // Iterate through all the configuration elements inside configurations tag
        for (int configIndex = 0; configIndex < configurationList.getLength(); configIndex++) {
            Element configuration = (Element) configurationList.item(configIndex);
            ComponentConfiguration cci = parseConfiguration(configuration);
            compConfList.add(cci);
        }
        xcc.setConfigurations(compConfList);
        return (T) xcc;
    }

    //
    // Marshaller's private methods
    //
    private Element marshallConfiguration(ComponentConfiguration config) throws Exception {
        // get ComponentConfigurationImpl Object data
        String configPid = config.getPid();
        Map<String, Object> configProperty = config.getConfigurationProperties();
        OCD configOCD = config.getDefinition();

        // create configuration element
        Element configurationElement = this.marshallDoc
                .createElement(ESF_NAMESPACE + ":" + CONFIGURATIONS_CONFIGURATION);
        Attr propertiesAttribute = this.marshallDoc.createAttribute(CONFIGURATION_PID);
        propertiesAttribute.setNodeValue(configPid);
        configurationElement.setAttributeNode(propertiesAttribute);

        // Add OCD node and marshall definitions
        if (configOCD != null) {
            Element ocd = new XmlJavaMetadataMapper().marshal(this.marshallDoc, configOCD);
            configurationElement.appendChild(ocd);
        }

        // Add properties Node and marshall properties
        if (configProperty != null) {
            Element properties = this.marshallDoc.createElement(ESF_NAMESPACE + ":" + PROPERTIES);
            marshallProperties(configProperty, properties);
            configurationElement.appendChild(properties);
        }

        return configurationElement;
    }

    private void marshallProperties(Map<String, Object> propertyMap, Element properties) throws Exception {
        XmlConfigPropertiesAdapter xmlPropAdapter = new XmlConfigPropertiesAdapter();
        XmlConfigPropertiesAdapted configPropAdapted = xmlPropAdapter.marshal(propertyMap);

        XmlConfigPropertyAdapted[] propArray = configPropAdapted.getProperties();
        for (XmlConfigPropertyAdapted propertyObj : propArray) {
            Element property = marshallProperty(propertyObj);
            if (property != null) {
                properties.appendChild(property);
            }
        }
    }

    private Element marshallProperty(XmlConfigPropertyAdapted propertyObj) {
        String name = propertyObj.getName();
        Boolean array = propertyObj.getArray();
        Boolean encrypted = propertyObj.isEncrypted();
        ConfigPropertyType cpt = propertyObj.getType();
        String[] values = propertyObj.getValues();

        if (values != null) {
            Element property = this.marshallDoc
                    .createElement(ESF_NAMESPACE + ":" + CONFIGURATIONS_CONFIGURATION_PROPERTY);
            Attr attName = this.marshallDoc.createAttribute(CONFIGURATIONS_CONFIGURATION_PROPERTY_NAME);
            attName.setNodeValue(name);
            property.setAttributeNode(attName);

            Attr attArray = this.marshallDoc.createAttribute(CONFIGURATIONS_CONFIGURATION_PROPERTY_ARRAY);
            attArray.setNodeValue(array.toString());
            property.setAttributeNode(attArray);

            Attr attEncrypted = this.marshallDoc.createAttribute(CONFIGURATIONS_CONFIGURATION_PROPERTY_ENCRYPTED);
            attEncrypted.setNodeValue(encrypted.toString());
            property.setAttributeNode(attEncrypted);

            Attr attType = this.marshallDoc.createAttribute(CONFIGURATIONS_CONFIGURATION_PROPERTY_TYPE);

            attType.setNodeValue(getStringValue(cpt));
            property.setAttributeNode(attType);

            for (String value : values) {
                Element valueElem = this.marshallDoc
                        .createElement(ESF_NAMESPACE + ":" + CONFIGURATIONS_CONFIGURATION_PROPERTY_VALUE);
                valueElem.setTextContent(value);
                property.appendChild(valueElem);
            }
            return property;
        }
        return null;
    }

    private String getStringValue(ConfigPropertyType type) {
        if (type == null) {
            return "String";
        }
        if (type.equals(ConfigPropertyType.STRING_TYPE)) {
            return "String";
        } else if (type.equals(ConfigPropertyType.LONG_TYPE)) {
            return "Long";
        } else if (type.equals(ConfigPropertyType.DOUBLE_TYPE)) {
            return "Double";
        } else if (type.equals(ConfigPropertyType.FLOAT_TYPE)) {
            return "Float";
        } else if (type.equals(ConfigPropertyType.INTEGER_TYPE)) {
            return "Integer";
        } else if (type.equals(ConfigPropertyType.BYTE_TYPE)) {
            return "Byte";
        } else if (type.equals(ConfigPropertyType.CHAR_TYPE)) {
            return "Char";
        } else if (type.equals(ConfigPropertyType.BOOLEAN_TYPE)) {
            return "Boolean";
        } else if (type.equals(ConfigPropertyType.SHORT_TYPE)) {
            return "Short";
        } else if (type.equals(ConfigPropertyType.PASSWORD_TYPE)) {
            return "Password";
        }
        return "String";
    }

    //
    // Unmarshaller's private methods
    //
    private ComponentConfiguration parseConfiguration(Element configuration) throws Exception {
        XmlConfigPropertiesAdapter xmlPropAdapter = new XmlConfigPropertiesAdapter();

        // get configuration's properties
        NodeList propertiesList = configuration.getChildNodes();

        // get effective elements
        Element[] propertiesArray = getElementNodes(propertiesList);

        XmlConfigPropertiesAdapted xmlPropertiesAdapted = new XmlConfigPropertiesAdapted();
        for (Element element : propertiesArray) {
            // parse property elements
            NodeList propertyList = element.getChildNodes();
            Element[] propertyArray = getElementNodes(propertyList);

            XmlConfigPropertyAdapted[] xmlConfigProperties = new XmlConfigPropertyAdapted[propertyArray.length];
            for (int propertyIndex = 0; propertyIndex < propertyArray.length; propertyIndex++) {
                XmlConfigPropertyAdapted xmlProperty = parseProperty(propertyArray[propertyIndex]);
                xmlConfigProperties[propertyIndex] = xmlProperty;
            }
            xmlPropertiesAdapted.setProperties(xmlConfigProperties);
        }

        Map<String, Object> propertiesMap = xmlPropAdapter.unmarshal(xmlPropertiesAdapted);

        String pid = configuration.getAttribute(CONFIGURATION_PID);
        return new ComponentConfigurationImpl(pid, null, propertiesMap);
    }

    private XmlConfigPropertyAdapted parseProperty(Element property) {
        NodeList valuesList = property.getChildNodes();
        Element[] valuesArray = getElementNodes(valuesList);
        String[] values = new String[valuesArray.length];

        // get values
        for (int valIndex = 0; valIndex < valuesArray.length; valIndex++) {
            values[valIndex] = valuesArray[valIndex].getTextContent();
        }

        String name = property.getAttribute(CONFIGURATIONS_CONFIGURATION_PROPERTY_NAME);
        String type = property.getAttribute(CONFIGURATIONS_CONFIGURATION_PROPERTY_TYPE);
        String array = property.getAttribute(CONFIGURATIONS_CONFIGURATION_PROPERTY_ARRAY);
        String encrypted = property.getAttribute(CONFIGURATIONS_CONFIGURATION_PROPERTY_ENCRYPTED);

        ConfigPropertyType cct = getType(type);

        XmlConfigPropertyAdapted xmlProperty = new XmlConfigPropertyAdapted(name, cct, values);
        xmlProperty.setArray(Boolean.parseBoolean(array));
        xmlProperty.setEncrypted(Boolean.parseBoolean(encrypted));

        return xmlProperty;
    }

    private Element[] getElementNodes(NodeList propertiesList) {
        List<Element> elementList = new ArrayList<>();
        for (int propIndex = 0; propIndex < propertiesList.getLength(); propIndex++) {
            Node currentNode = propertiesList.item(propIndex);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) currentNode;
                elementList.add(el);
            }
        }
        return elementList.toArray(new Element[0]);
    }

    private ConfigPropertyType getType(String type) {
        if (type.equals("String")) {
            return ConfigPropertyType.STRING_TYPE;
        } else if (type.equals("Long")) {
            return ConfigPropertyType.LONG_TYPE;
        } else if (type.equals("Double")) {
            return ConfigPropertyType.DOUBLE_TYPE;
        } else if (type.equals("Float")) {
            return ConfigPropertyType.FLOAT_TYPE;
        } else if (type.equals("Integer")) {
            return ConfigPropertyType.INTEGER_TYPE;
        } else if (type.equals("Byte")) {
            return ConfigPropertyType.BYTE_TYPE;
        } else if (type.equals("Char")) {
            return ConfigPropertyType.CHAR_TYPE;
        } else if (type.equals("Boolean")) {
            return ConfigPropertyType.BOOLEAN_TYPE;
        } else if (type.equals("Short")) {
            return ConfigPropertyType.SHORT_TYPE;
        } else if (type.equals("Password")) {
            return ConfigPropertyType.PASSWORD_TYPE;
        }
        return null;
    }
}
