/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.internal.xml.marshaller.unmarshaller.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.ConfigurationServiceImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.XmlSnapshotIdResult;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.configuration.util.ComponentUtil;
import org.eclipse.kura.core.deployment.xml.XmlBundle;
import org.eclipse.kura.core.deployment.xml.XmlBundleInfo;
import org.eclipse.kura.core.deployment.xml.XmlBundles;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackage;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackages;
import org.eclipse.kura.internal.xml.marshaller.unmarshaller.XmlMarshallUnmarshallImpl;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlEncoderDecoderTest {
    
    private static final Logger logger = LoggerFactory.getLogger(XmlEncoderDecoderTest.class);

    private static final String stringWriter = "String Writer";
    private static final String string = "String";
    private static final String pid = "Pid";
    private static final String hashmapValues = "Hashmap values";
    private static final String differentInstanceMessage = "Unmarshalled Object from String is not of type %s, but was %s";
    private static final String missingItemsMessage = "Unmarshalled object does not contain all the value from the marshalled object. Missing value : %s ;";
    private static final String propertyValueDiffersMessage = "Property value  %s of unmarshalled object from %s differs from original. Orignal : %s ; Received : %s ;";

    @Test
    public void testXmlComponentConfigurationsUnmarshalling() {

        try {

            XmlMarshallUnmarshallImpl xmlMarshallerImpl = new XmlMarshallUnmarshallImpl();
            XmlComponentConfigurations xmlComponentConfigurations = getSampleXmlComponentConfigurationsObject();

            // test String unmarshalling
            String marshalledString = xmlMarshallerImpl.marshal(xmlComponentConfigurations);
            Object unmarshalledObjectFromString = xmlMarshallerImpl.unmarshal(marshalledString,
                    XmlComponentConfigurations.class);
            assertTrue(
                    String.format(differentInstanceMessage, XmlComponentConfigurations.class,
                            unmarshalledObjectFromString.getClass()),
                    unmarshalledObjectFromString instanceof XmlComponentConfigurations);

            XmlComponentConfigurations outputXmlComponentConfigurations = (XmlComponentConfigurations) unmarshalledObjectFromString;

            assertValuesForEquality(pid, xmlComponentConfigurations.getConfigurations().get(0).getPid(),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getPid(), false);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("int"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("int"),
                    false);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("long"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("long"),
                    false);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("string"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("string"),
                    false);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("boolean"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("boolean"),
                    false);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("double"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("double"),
                    false);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("float"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("float"),
                    false);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("char"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("char"),
                    false);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("short"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("short"),
                    false);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("byte"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("byte"),
                    false);

            // test Reader unmarshalling
            Object unmarshalledObjectFromStringReader = xmlMarshallerImpl.unmarshal(marshalledString,
                    XmlComponentConfigurations.class);
            assertTrue(
                    String.format(differentInstanceMessage, XmlComponentConfigurations.class,
                            unmarshalledObjectFromStringReader.getClass()),
                    unmarshalledObjectFromStringReader instanceof XmlComponentConfigurations);

            outputXmlComponentConfigurations = (XmlComponentConfigurations) unmarshalledObjectFromStringReader;

            assertValuesForEquality(pid, xmlComponentConfigurations.getConfigurations().get(0).getPid(),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getPid(), true);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("int"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("int"),
                    true);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("long"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("long"),
                    true);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("string"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("string"),
                    true);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("boolean"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("boolean"),
                    true);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("double"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("double"),
                    true);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("float"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("float"),
                    true);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("char"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("char"),
                    true);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("short"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("short"),
                    true);
            assertValuesForEquality(hashmapValues,
                    xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("byte"),
                    outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties()
                            .get("byte"),
                    true);

        } catch (Exception e) {

            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @Test
    public void testXmlSnapshotIdResultMarshalling() {

        XmlMarshallUnmarshallImpl xmlMarshallerImpl = new XmlMarshallUnmarshallImpl();
        try {
            XmlSnapshotIdResult xmlSnapshotIdResult = getSampleXmlSnapshotIdResultObject();
            String marshalledString = xmlMarshallerImpl.marshal(xmlSnapshotIdResult);

            for (Long value : xmlSnapshotIdResult.getSnapshotIds()) {
                assertTrue(String.format(missingItemsMessage, value), marshalledString.contains(Long.toString(value)));
            }

        } catch (Exception e) {

            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @Test
    public void testXmlDeploymentPackagesMarshalling() {
        XmlMarshallUnmarshallImpl xmlMarshallerImpl = new XmlMarshallUnmarshallImpl();
        try {

            XmlDeploymentPackages xmlDeploymentPackages = getSampleXmlDeploymentPackagesObject();
            String marshalledString = xmlMarshallerImpl.marshal(xmlDeploymentPackages);
            assertTrue(String.format(missingItemsMessage, xmlDeploymentPackages.getDeploymentPackages()[0].getName()),
                    marshalledString.contains(xmlDeploymentPackages.getDeploymentPackages()[0].getName()));
            assertTrue(
                    String.format(missingItemsMessage, xmlDeploymentPackages.getDeploymentPackages()[0].getVersion()),
                    marshalledString.contains(xmlDeploymentPackages.getDeploymentPackages()[0].getVersion()));
            assertTrue(
                    String.format(missingItemsMessage,
                            xmlDeploymentPackages.getDeploymentPackages()[0].getBundleInfos()[0].getName()),
                    marshalledString
                            .contains(xmlDeploymentPackages.getDeploymentPackages()[0].getBundleInfos()[0].getName()));
            assertTrue(
                    String.format(missingItemsMessage,
                            xmlDeploymentPackages.getDeploymentPackages()[0].getBundleInfos()[0].getVersion()),
                    marshalledString.contains(
                            xmlDeploymentPackages.getDeploymentPackages()[0].getBundleInfos()[0].getVersion()));

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testXmlBundlesMarshalling() {
        XmlMarshallUnmarshallImpl xmlMarshallerImpl = new XmlMarshallUnmarshallImpl();
        try {

            XmlBundles xmlBundles = getSampleXmlBundlesObject();
            String marshalledString = xmlMarshallerImpl.marshal(xmlBundles);
            assertTrue(String.format(missingItemsMessage, xmlBundles.getBundles()[0].getId()),
                    marshalledString.contains(Long.toString(xmlBundles.getBundles()[0].getId())));
            assertTrue(String.format(missingItemsMessage, xmlBundles.getBundles()[0].getName()),
                    marshalledString.contains(xmlBundles.getBundles()[0].getName()));
            assertTrue(String.format(missingItemsMessage, xmlBundles.getBundles()[0].getState()),
                    marshalledString.contains(xmlBundles.getBundles()[0].getState()));
            assertTrue(String.format(missingItemsMessage, xmlBundles.getBundles()[0].getVersion()),
                    marshalledString.contains(xmlBundles.getBundles()[0].getVersion()));

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private static XmlBundles getSampleXmlBundlesObject() {

        XmlBundles xmlBundles = new XmlBundles();

        XmlBundle inputXmlBundle = new XmlBundle();
        inputXmlBundle.setId(1);
        inputXmlBundle.setName("raspberry");
        inputXmlBundle.setState("New York");
        inputXmlBundle.setVersion("3.0.1");
        xmlBundles.setBundles(new XmlBundle[] { inputXmlBundle });

        return xmlBundles;
    }

    private static XmlDeploymentPackages getSampleXmlDeploymentPackagesObject() {

        XmlDeploymentPackages xmlDeploymentPackages = new XmlDeploymentPackages();
        XmlBundleInfo inputXmlBundleInfo = new XmlBundleInfo();
        inputXmlBundleInfo.setName("XmlBundleInfo");
        inputXmlBundleInfo.setVersion("3.0.1.201");

        XmlDeploymentPackage inputXmlDeploymentPackage = new XmlDeploymentPackage();
        inputXmlDeploymentPackage.setName("raspberry");
        inputXmlDeploymentPackage.setVersion("3.0.1");
        inputXmlDeploymentPackage.setBundleInfos(new XmlBundleInfo[] { inputXmlBundleInfo });

        xmlDeploymentPackages.setDeploymentPackages(new XmlDeploymentPackage[] { inputXmlDeploymentPackage });

        return xmlDeploymentPackages;

    }

    private static Tocd getSampleTocdObject() {

        Tad tad = new Tad();
        tad.setCardinality(1);
        tad.setDefault("default");
        tad.setDescription("This is a sample description for tad");
        tad.setId("1");
        tad.setMax("10");
        tad.setMin("1");
        tad.setName("Tad");
        tad.setRequired(true);
        tad.setType(Tscalar.PASSWORD);

        Tocd tocd = new Tocd();
        tocd.setId("1");
        tocd.setName("Tocd");
        tocd.setDescription("This is a sample description for Tocd");
        tocd.addAD(tad);

        return tocd;
    }

    private static XmlSnapshotIdResult getSampleXmlSnapshotIdResultObject() {

        List<Long> snapshotIds = new ArrayList<>();
        snapshotIds.add(102540L);
        snapshotIds.add(27848415L);
        snapshotIds.add(378485484848L);

        XmlSnapshotIdResult xmlSnapshotIdResult = new XmlSnapshotIdResult();
        xmlSnapshotIdResult.setSnapshotIds(snapshotIds);

        return xmlSnapshotIdResult;
    }

    private static XmlComponentConfigurations getSampleXmlComponentConfigurationsObject() {

        Map<String, Object> sampleMap = new HashMap<>();

        sampleMap.put("int", 1);
        sampleMap.put("long", 2L);
        sampleMap.put("string", "StringValue");
        sampleMap.put("boolean", true);
        sampleMap.put("double", 2.2d);
        sampleMap.put("float", 2.3f);
        sampleMap.put("char", 'a');
        sampleMap.put("short", (short) 1);
        sampleMap.put("byte", (byte) 90);
        new Password("password".toCharArray());

        ComponentConfigurationImpl componentConfigurationImpl = new ComponentConfigurationImpl();
        componentConfigurationImpl.setPid("8236");
        componentConfigurationImpl.setDefinition(getSampleTocdObject());
        componentConfigurationImpl.setProperties(sampleMap);

        XmlComponentConfigurations xmlComponentConfigurations = new XmlComponentConfigurations();
        List<ComponentConfiguration> configs = new ArrayList<>();
        configs.add(componentConfigurationImpl);
        xmlComponentConfigurations.setConfigurations(configs);

        return xmlComponentConfigurations;
    }

    private static void assertValuesForEquality(String name, Object orignal, Object unmarshalled,
            boolean isStringWriter) {
        String source = isStringWriter ? stringWriter : string;

        assertTrue(String.format(propertyValueDiffersMessage, name, source, orignal, unmarshalled),
                orignal.equals(unmarshalled));
    }

    @Test
    public void testPropertiesMarshallUnmarshall() throws Exception {
        XmlMarshallUnmarshallImpl xmlMarshallerImpl = new XmlMarshallUnmarshallImpl();

        String pid = "org.eclipse.kura.cloud.CloudService";
        Tocd definition = null;
        Map<String, Object> properties = new HashMap<>();
        properties.put("prop.string", new String("prop.value"));
        properties.put("prop.long", Long.MAX_VALUE);
        properties.put("prop.double", Double.MAX_VALUE);
        properties.put("prop.float", Float.MAX_VALUE);
        properties.put("prop.integer", Integer.MAX_VALUE);
        properties.put("prop.byte", Byte.MAX_VALUE);
        properties.put("prop.character", 'a');
        properties.put("prop.short", Short.MAX_VALUE);

        XmlComponentConfigurations xcc = new XmlComponentConfigurations();
        List<ComponentConfiguration> ccis = new ArrayList<>();
        ComponentConfigurationImpl config = new ComponentConfigurationImpl(pid, definition, properties);
        ccis.add(config);
        xcc.setConfigurations(ccis);

        String s = xmlMarshallerImpl.marshal(xcc);
        logger.info(s);

        XmlComponentConfigurations config1 = xmlMarshallerImpl.unmarshal(s, XmlComponentConfigurations.class);
        String s1 = xmlMarshallerImpl.marshal(config1);
        logger.info(s1);

        Map<String, Object> properties1 = config1.getConfigurations().get(0).getConfigurationProperties();
        assertEquals(properties, properties1);
    }

    @Test
    public void testOCDMarshallUnmarshall() throws Exception {
        XmlMarshallUnmarshallImpl xmlMarshallerImpl = new XmlMarshallUnmarshallImpl();

        String pid = "org.eclipse.kura.cloud.CloudService";

        Tocd definition = ComponentUtil.readObjectClassDefinition(pid);

        Map<String, Object> properties = new HashMap<>();
        properties.put("prop.string", new String("prop.value"));
        properties.put("prop.long", Long.MAX_VALUE);
        properties.put("prop.double", Double.MAX_VALUE);
        properties.put("prop.float", Float.MAX_VALUE);
        properties.put("prop.integer", Integer.MAX_VALUE);
        properties.put("prop.byte", Byte.MAX_VALUE);
        properties.put("prop.character", 'a');
        properties.put("prop.short", Short.MAX_VALUE);

        XmlComponentConfigurations xcc = new XmlComponentConfigurations();
        List<ComponentConfiguration> ccis = new ArrayList<>();
        ComponentConfigurationImpl config = new ComponentConfigurationImpl(pid, definition, properties);
        ccis.add(config);
        xcc.setConfigurations(ccis);

        String s = xmlMarshallerImpl.marshal(xcc);
        logger.info(s);

        XmlComponentConfigurations config1 = xmlMarshallerImpl.unmarshal(s, XmlComponentConfigurations.class);
        String s1 = xmlMarshallerImpl.marshal(config1);
        logger.info(s1);

        Map<String, Object> properties1 = config1.getConfigurations().get(0).getConfigurationProperties();
        assertEquals(properties, properties1);
    }
}
