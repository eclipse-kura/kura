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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tdesignate;
import org.eclipse.kura.core.configuration.metatype.Ticon;
import org.eclipse.kura.core.configuration.metatype.Tmetadata;
import org.eclipse.kura.core.configuration.metatype.Tobject;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlJavaMetadataMapper implements XmlJavaDataMapper {

    private static final String METADATA_LOCALIZATION = "localization";

    private static final String METADATA_OCD = "OCD";
    private static final String METADATA_OCD_NAME = "name";
    private static final String METADATA_OCD_ID = "id";
    private static final String METADATA_OCD_DESCRIPTION = "description";

    private static final String METADATA_ICON = "Icon";
    private static final String METADATA_ICON_RESOURCE = "resource";
    private static final String METADATA_ICON_SIZE = "size";

    private static final String METADATA_AD = "AD";
    private static final String METADATA_AD_ID = "id";
    private static final String METADATA_AD_NAME = "name";
    private static final String METADATA_AD_TYPE = "type";
    private static final String METADATA_AD_CARDINALITY = "cardinality";
    private static final String METADATA_AD_REQUIRED = "required";
    private static final String METADATA_AD_DEFAULT = "default";
    private static final String METADATA_AD_DESCRIPTION = "description";
    private static final String METADATA_AD_MIN = "min";
    private static final String METADATA_AD_MAX = "max";

    private static final String METADATA_AD_OPTION = "Option";
    private static final String METADATA_AD_OPTION_LABEL = "label";
    private static final String METADATA_AD_OPTION_VALUE = "value";

    private static final String METADATA_DESIGNATE_OBJECT = "Object";
    private static final String METADATA_DESIGNATE_PID = "pid";
    private static final String METADATA_DESIGNATE_FACTORY_PID = "factoryPid";
    private static final String METADATA_DESIGNATE_BUNDLE = "bundle";
    private static final String METADATA_DESIGNATE_OPTIONAL = "optional";
    private static final String METADATA_DESIGNATE_MERGE = "merge";

    private static final String METADATA_DESIGNATE_OBJECT_ATTRIBUTE = "Attribute";
    private static final String METADATA_DESIGNATE_OBJECT_OCDREF = "ocdref";

    private Document marshallDoc = null;

    //
    // Public methods
    //
    @Override
    public Element marshal(Document doc, Object o) throws Exception {
        this.marshallDoc = doc;
        if (o instanceof Tocd) {
            Tocd configOCD = (Tocd) o;

            String ocdName = configOCD.getName();
            String ocdDescription = configOCD.getDescription();
            String ocdID = configOCD.getId();
            List<Icon> ocdIcons = configOCD.getIcon();
            List<AD> ocdADs = configOCD.getAD();
            configOCD.getAny();
            configOCD.getOtherAttributes();

            Element ocd = this.marshallDoc.createElement(OCD_NAMESPACE + ":" + METADATA_OCD);

            if (ocdName != null && !ocdName.trim().isEmpty()) {
                Attr ocdAttrName = this.marshallDoc.createAttribute(METADATA_OCD_NAME);
                ocdAttrName.setNodeValue(ocdName);
                ocd.setAttributeNode(ocdAttrName);
            }

            if (ocdDescription != null && !ocdDescription.trim().isEmpty()) {
                Attr ocdAttrDescription = this.marshallDoc.createAttribute(METADATA_OCD_DESCRIPTION);
                ocdAttrDescription.setNodeValue(ocdDescription);
                ocd.setAttributeNode(ocdAttrDescription);
            }

            if (ocdID != null && !ocdID.trim().isEmpty()) {
                Attr ocdAttrId = this.marshallDoc.createAttribute(METADATA_OCD_ID);
                ocdAttrId.setNodeValue(ocdID);
                ocd.setAttributeNode(ocdAttrId);
            }

            if (ocdADs != null) {
                for (AD ocdAD : ocdADs) {
                    Element ad = this.marshallDoc.createElement(OCD_NAMESPACE + ":" + METADATA_AD);
                    marshallAD(ocdAD, ad);
                    ocd.appendChild(ad);
                }
            }

            if (ocdIcons != null) {
                for (Icon ocdIcon : ocdIcons) {
                    Element icon = this.marshallDoc.createElement(OCD_NAMESPACE + ":" + METADATA_ICON);
                    marshallIcon(ocdIcon, icon);
                    ocd.appendChild(icon);
                }
            }
            return ocd;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unmarshal(Document doc) {
        Element metadata = doc.getDocumentElement();
        Tmetadata tMetadata = parseMetadataAttributes(metadata);

        NodeList metadataChilds = metadata.getChildNodes();
        Element[] metadataChildsArray = getElementNodes(metadataChilds);

        for (Element node : metadataChildsArray) {
            String localName = node.getNodeName();
            if (localName.equals("OCD")) {
                Tocd tocd = parseOCD(node);
                tMetadata.setOCD(tocd);
            } else if (localName.equals("Designate")) {
                Tdesignate tDesignate = parseDesignate(node);
                tMetadata.setDesignate(tDesignate);
            }
        }

        return (T) tMetadata;
    }

    //
    // Private methods
    //
    private void marshallIcon(Icon ocdIcon, Element icon) {
        String iconResource = ocdIcon.getResource();
        BigInteger iconSize = ocdIcon.getSize();

        if (iconResource != null && !iconResource.trim().isEmpty()) {
            Attr attrResource = this.marshallDoc.createAttribute(METADATA_ICON_RESOURCE);
            attrResource.setNodeValue(iconResource);
            icon.setAttributeNode(attrResource);
        }
        if (iconSize != null) {
            Attr attrSize = this.marshallDoc.createAttribute(METADATA_ICON_SIZE);
            attrSize.setNodeValue(iconSize.toString());
            icon.setAttributeNode(attrSize);
        }
    }

    private void marshallAD(AD ocdAD, Element ad) {
        String adId = ocdAD.getId();
        String adName = ocdAD.getName();
        Scalar adType = ocdAD.getType();
        Integer adCardinality = ocdAD.getCardinality();
        Boolean adRequired = ocdAD.isRequired();
        String adDefault = ocdAD.getDefault();
        String adDescription = ocdAD.getDescription();
        String adMin = ocdAD.getMin();
        String adMax = ocdAD.getMax();
        List<Option> adOptions = ocdAD.getOption();

        if (adName != null) {
            Attr attrName = this.marshallDoc.createAttribute(METADATA_AD_NAME);
            attrName.setNodeValue(adName);
            ad.setAttributeNode(attrName);
        }
        if (adId != null) {
            Attr attrId = this.marshallDoc.createAttribute(METADATA_AD_ID);
            attrId.setNodeValue(adId);
            ad.setAttributeNode(attrId);
        }
        if (adType != null) {
            Attr attrType = this.marshallDoc.createAttribute(METADATA_AD_TYPE);
            attrType.setNodeValue(adType.value());
            ad.setAttributeNode(attrType);
        }
        if (adCardinality != null) {
            Attr attrCardinality = this.marshallDoc.createAttribute(METADATA_AD_CARDINALITY);
            attrCardinality.setNodeValue(adCardinality.toString());
            ad.setAttributeNode(attrCardinality);
        }
        if (adRequired != null) {
            Attr attrRequired = this.marshallDoc.createAttribute(METADATA_AD_REQUIRED);
            attrRequired.setNodeValue(adRequired.toString());
            ad.setAttributeNode(attrRequired);
        }
        if (adDefault != null) {
            Attr attrDefault = this.marshallDoc.createAttribute(METADATA_AD_DEFAULT);
            attrDefault.setNodeValue(adDefault);
            ad.setAttributeNode(attrDefault);
        }
        if (adDescription != null) {
            Attr attrDescription = this.marshallDoc.createAttribute(METADATA_AD_DESCRIPTION);
            attrDescription.setNodeValue(adDescription);
            ad.setAttributeNode(attrDescription);
        }
        if (adMin != null) {
            Attr attrMin = this.marshallDoc.createAttribute(METADATA_AD_MIN);
            attrMin.setNodeValue(adMin);
            ad.setAttributeNode(attrMin);
        }
        if (adMax != null) {
            Attr attrMax = this.marshallDoc.createAttribute(METADATA_AD_MAX);
            attrMax.setNodeValue(adMax);
            ad.setAttributeNode(attrMax);
        }

        if (adOptions != null) {
            for (Option adOption : adOptions) {
                Element option = this.marshallDoc.createElement(OCD_NAMESPACE + ":" + METADATA_AD_OPTION);
                marshallOption(adOption, option);
                ad.appendChild(option);
            }
        }
    }

    private void marshallOption(Option adOption, Element option) {
        String label = adOption.getLabel();
        String value = adOption.getValue();

        if (!label.trim().isEmpty()) {
            Attr attrLabel = this.marshallDoc.createAttribute(METADATA_AD_OPTION_LABEL);
            attrLabel.setNodeValue(label);
            option.setAttributeNode(attrLabel);
        }
        if (!value.trim().isEmpty()) {
            Attr attrValue = this.marshallDoc.createAttribute(METADATA_AD_OPTION_VALUE);
            attrValue.setNodeValue(value);
            option.setAttributeNode(attrValue);
        }
    }

    private Tocd parseOCD(Element ocd) {
        String ocdName = ocd.getAttribute(METADATA_OCD_NAME);
        String ocdID = ocd.getAttribute(METADATA_OCD_ID);
        String ocdDescription = ocd.getAttribute(METADATA_OCD_DESCRIPTION);
        Tocd tocd = new Tocd();

        if (ocdID != null && !ocdID.trim().isEmpty()) {
            tocd.setId(ocdID);
        }
        if (ocdName != null && !ocdName.trim().isEmpty()) {
            tocd.setName(ocdName);
        }
        if (ocdDescription != null && !ocdDescription.trim().isEmpty()) {
            tocd.setDescription(ocdDescription);
        }

        NodeList ocdChilds = ocd.getChildNodes();
        Element[] ocdChildElements = getElementNodes(ocdChilds);

        for (Element node : ocdChildElements) {
            String localName = node.getNodeName();
            if (localName.equals(METADATA_ICON)) {
                // parse Icon
                Ticon tIcon = parseIcon(node);
                tocd.setIcon(tIcon);
            } else if (localName.equals(METADATA_AD)) {
                // parse AD
                Tad tad = parseAD(node);
                tocd.addAD(tad);
            }
        }

        return tocd;
    }

    private Tdesignate parseDesignate(Element designate) {
        String pid = designate.getAttribute(METADATA_DESIGNATE_PID);
        String factoryPid = designate.getAttribute(METADATA_DESIGNATE_FACTORY_PID);
        String bundle = designate.getAttribute(METADATA_DESIGNATE_BUNDLE);
        Boolean optional = Boolean.parseBoolean(designate.getAttribute(METADATA_DESIGNATE_OPTIONAL));
        Boolean merge = Boolean.parseBoolean(designate.getAttribute(METADATA_DESIGNATE_MERGE));

        Tdesignate tDesignate = new Tdesignate();
        if (!pid.trim().isEmpty()) {
            tDesignate.setPid(pid);
        }
        if (!factoryPid.trim().isEmpty()) {
            tDesignate.setFactoryPid(factoryPid);
        }
        if (!bundle.trim().isEmpty()) {
            tDesignate.setBundle(bundle);
        }
        tDesignate.setOptional(optional);
        tDesignate.setMerge(merge);

        NodeList objectsChilds = designate.getChildNodes();
        Element[] objectsChildElements = getElementNodes(objectsChilds);

        for (Element node : objectsChildElements) {
            String localName = node.getNodeName();
            if (localName.equals(METADATA_DESIGNATE_OBJECT)) {
                // parse Object
                Tobject tObject = parseObject(node);
                tDesignate.setObject(tObject);
            }
        }

        return tDesignate;
    }

    private Tobject parseObject(Element object) {
        String ocdref = object.getAttribute(METADATA_DESIGNATE_OBJECT_OCDREF);

        Tobject tObject = new Tobject();
        if (!ocdref.trim().isEmpty()) {
            tObject.setOcdref(ocdref);
        }

        NodeList attributeChilds = object.getChildNodes();
        Element[] attributeChildElements = getElementNodes(attributeChilds);
        for (Element node : attributeChildElements) {
            String localName = node.getNodeName();
            if (localName.equals(METADATA_DESIGNATE_OBJECT_ATTRIBUTE)) {
                // parse Attribute
                // TODO
            }
        }

        return tObject;
    }

    private Ticon parseIcon(Element icon) {
        Ticon result = new Ticon();

        String resource = icon.getAttribute(METADATA_ICON_RESOURCE);
        if (resource != null && !resource.trim().isEmpty()) {
            result.setResource(resource);
        }

        String iconSize = icon.getAttribute(METADATA_ICON_SIZE);
        if (iconSize != null) {
            try {
                BigInteger size = new BigInteger(iconSize);
                if (size.signum() >= 0) {
                    result.setSize(size);
                } else {
                    result.setSize(new BigInteger("0"));
                }
            } catch (NumberFormatException e) {
                result.setSize(new BigInteger("0"));
            }
        }

        return result;
    }

    private Tad parseAD(Element adElement) {
        Tad tad = new Tad();

        String id = adElement.getAttribute(METADATA_AD_ID);
        String name = adElement.getAttribute(METADATA_AD_NAME);
        Tscalar type = Tscalar.fromValue(adElement.getAttribute(METADATA_AD_TYPE));
        Integer cardinality;
        try {
            cardinality = Integer.parseInt(adElement.getAttribute(METADATA_AD_CARDINALITY));
        } catch (NumberFormatException e) {
            cardinality = null;
        }

        Boolean required = null;
        String requiredAttr = adElement.getAttribute(METADATA_AD_REQUIRED);
        if (requiredAttr != null && !requiredAttr.trim().isEmpty()) {
            required = Boolean.parseBoolean(adElement.getAttribute(METADATA_AD_REQUIRED));
        }

        String defaultVal = adElement.getAttribute(METADATA_AD_DEFAULT);
        String description = adElement.getAttribute(METADATA_AD_DESCRIPTION);
        String min = adElement.getAttribute(METADATA_AD_MIN);
        String max = adElement.getAttribute(METADATA_AD_MAX);

        if (id != null && !id.trim().isEmpty()) {
            tad.setId(id);
        }
        if (name != null && !name.trim().isEmpty()) {
            tad.setName(name);
        }

        if (type != null) {
            tad.setType(type);
        }
        if (cardinality != null) {
            tad.setCardinality(cardinality);
        }
        if (required != null) {
            tad.setRequired(required);
        }

        if (defaultVal != null && !defaultVal.trim().isEmpty()) {
            tad.setDefault(defaultVal);
        }
        if (description != null && !description.trim().isEmpty()) {
            tad.setDescription(description);
        }
        if (min != null && !min.trim().isEmpty()) {
            tad.setMin(min);
        }
        if (max != null && !max.trim().isEmpty()) {
            tad.setMax(max);
        }

        // parse Option
        NodeList optionChilds = adElement.getChildNodes();
        Element[] optionChildElements = getElementNodes(optionChilds);
        for (Element node : optionChildElements) {
            String localName = node.getNodeName();
            if (localName.equals(METADATA_AD_OPTION)) {
                // parse Option
                Toption tOption = parseOption(node);
                tad.setOption(tOption);
            }
        }

        return tad;
    }

    private Toption parseOption(Element option) {
        Toption tOption = new Toption();

        String label = option.getAttribute(METADATA_AD_OPTION_LABEL);
        String value = option.getAttribute(METADATA_AD_OPTION_VALUE);

        if (label != null && !label.trim().isEmpty()) {
            tOption.setLabel(label);
        }
        if (value != null && !value.trim().isEmpty()) {
            tOption.setValue(value);
        }
        return tOption;
    }

    private Tmetadata parseMetadataAttributes(Element metadata) {
        Tmetadata tMetadata = new Tmetadata();
        String localization = metadata.getAttribute(METADATA_LOCALIZATION);

        if (localization != null && !localization.trim().isEmpty()) {
            tMetadata.setLocalization(localization);
        }

        return tMetadata;
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

}
