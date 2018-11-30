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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.metatype.MetaData;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.XmlSnapshotIdResult;
import org.eclipse.kura.core.configuration.metatype.Tmetadata;
import org.eclipse.kura.core.deployment.xml.XmlBundles;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackages;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlMarshallUnmarshallImpl implements Marshaller, Unmarshaller {

    private static final Logger logger = LoggerFactory.getLogger(XmlMarshallUnmarshallImpl.class);

    @Override
    public String marshal(Object object) throws KuraException {
        StringWriter sw = new StringWriter();
        try {
            marshal(object, sw);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR);
        }
        return sw.toString();
    }

    private void marshal(Object object, Writer w) throws Exception {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            doc.setXmlStandalone(true);

            if (object instanceof XmlSnapshotIdResult) {
                // Resulting xml:
                // <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                // <esf:snapshot-ids xmlns:ocd="http://www.osgi.org/xmlns/metatype/v1.2.0"
                // xmlns:esf="http://eurotech.com/esf/2.0">
                // <esf:snapshotIds>1434122113492</esf:snapshotIds>
                // <esf:snapshotIds>1434122124387</esf:snapshotIds>
                // </esf:snapshot-ids>

                new XmlJavaSnapshotIdResultMapper().marshal(doc, object);

            } else if (object instanceof XmlComponentConfigurations) {
                new XmlJavaComponentConfigurationsMapper().marshal(doc, object);
            } else if (object instanceof XmlDeploymentPackages) {
                // Expected resulting xml:
                // <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                // <packages>
                // <package>
                // <name>org.eclipse.kura.demo.heater</name>
                // <version>1.2.0.qualifier</version>
                // <bundles>
                // <bundle>
                // <name>org.eclipse.kura.demo.heater</name>
                // <version>1.0.1</version>
                // </bundle>
                // </bundles>
                // </package>
                // </packages>

                new XmlJavaPackagesMapper().marshal(doc, object);

            } else if (object instanceof XmlBundles) {
                // Expected resulting xml:
                // <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                // <bundles>
                // <bundle>
                // <name>org.eclipse.osgi</name>
                // <version>3.8.1.v20120830-144521</version>
                // <id>0</id>
                // <state>ACTIVE</state>
                // </bundle>
                // </bundles>

                new XmlJavaBundlesMapper().marshal(doc, object);

            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(w); // System.out
            transformer.transform(source, result);
        } catch (ParserConfigurationException pce) {
            logger.warn("Parser Exception", pce);
        } catch (TransformerException tfe) {
            logger.warn("Transformer Exception", tfe);
        }
    }

    // un-marshalling
    @Override
    public <T> T unmarshal(String s, Class<T> clazz) throws KuraException {
        StringReader sr = new StringReader(s);
        return unmarshal(sr, clazz);
    }

    private <T> T unmarshal(Reader r, Class<T> clazz) throws KuraException {
        DocumentBuilderFactory factory = null;
        DocumentBuilder parser = null;

        try {
            factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            parser = factory.newDocumentBuilder();
        } catch (FactoryConfigurationError fce) {
            // The implementation is not available or cannot be instantiated
            logger.error("Parser Factory configuration Error");
            throw fce;
        } catch (ParserConfigurationException pce) {
            // the parser cannot be created with the specified configuration
            logger.error("Parser configuration exception");
            throw new FactoryConfigurationError(pce);
        }

        // parse the document
        Document doc = null;
        try {
            InputSource is = new InputSource(r);
            doc = parser.parse(is);
            doc.getDocumentElement().normalize();
        } catch (SAXException | IOException | IllegalArgumentException se) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR, se);
        }

        // identify the correct parser that has to execute
        if (clazz.equals(XmlComponentConfigurations.class)) {
            try {
                // Snapshot parser
                return new XmlJavaComponentConfigurationsMapper().unmarshal(doc);
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.DECODER_ERROR, e);
            }
        } else if (clazz.equals(MetaData.class) || clazz.equals(Tmetadata.class)) {
            // MetaData parser
            return new XmlJavaMetadataMapper().unmarshal(doc);
        } else {
            throw new IllegalArgumentException("Class not supported!");
        }
    }
}
