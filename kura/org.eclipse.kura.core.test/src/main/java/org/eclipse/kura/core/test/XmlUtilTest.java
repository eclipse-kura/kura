/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - fix issue #590
 *******************************************************************************/
package org.eclipse.kura.core.test;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.XmlSnapshotIdResult;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.deployment.xml.XmlBundle;
import org.eclipse.kura.core.deployment.xml.XmlBundleInfo;
import org.eclipse.kura.core.deployment.xml.XmlBundles;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackage;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackages;
import org.eclipse.kura.core.deployment.xml.XmlUtil;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;
import junit.framework.TestCase;

public class XmlUtilTest extends TestCase {

    private static final Logger s_logger = LoggerFactory.getLogger(XmlUtilTest.class);

    private static final String stringWriter = "String Writer";
    private static final String string = "String";
    private static final String id = "Id";
    private static final String name = "Name";
    private static final String state = "State";
    private static final String version = "Version";
    private static final String description = "Description";
    private static final String bundleInfo = "Bundle Info";
    private static final String pid = "Pid";
    private static final String tocdDescription = "Tocd Description";
    private static final String hashmapValues = "Hashmap values";
    private static final String differentInstanceMessage = "Unmarshalled Object from String is not of type %s, but was %s";
    private static final String missingItemsMessage = "Unmarshalled object does not contain all the value from the marshalled object. Missing value : %s ;";
    private static final String additionalItmesMessage = "Unmarshalled object contains additional value than the marshalled object.";
    private static final String propertyValueDiffersMessage = "Property value  %s of unmarshalled object from %s differs from original. Orignal : %s ; Received : %s ;";
    private static final String whiteSpace = "   ";
    private static final String marshalled = "marshalled " + whiteSpace;
    private static final String unmarshalled = "unmarshalled" + whiteSpace;
    private static final String snapshotFilePath = "/src/main/resources/snapshot_0.xml";

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testXmlComponentConfigurationsUnmarshalling() {

        try {

            XmlComponentConfigurations xmlComponentConfigurations = getSampleXmlComponentConfigurationsObject();

            // test String unmarshalling
            String marshalledString = org.eclipse.kura.core.configuration.util.XmlUtil
                    .marshal(xmlComponentConfigurations);
            Object unmarshalledObjectFromString = org.eclipse.kura.core.configuration.util.XmlUtil
                    .unmarshal(marshalledString, XmlComponentConfigurations.class);
            Assert.assertTrue(
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
            Reader marshalledStringReader = new StringReader(marshalledString);
            Object unmarshalledObjectFromStringReader = org.eclipse.kura.core.configuration.util.XmlUtil
                    .unmarshal(marshalledStringReader, XmlComponentConfigurations.class);
            Assert.assertTrue(
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

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testXmlSnapshotIdResultMarshalling() {

        try {
            XmlSnapshotIdResult xmlSnapshotIdResult = getSampleXmlSnapshotIdResultObject();
            String marshalledString = org.eclipse.kura.core.configuration.util.XmlUtil.marshal(xmlSnapshotIdResult);

            for (Long value : xmlSnapshotIdResult.getSnapshotIds()) {
                Assert.assertTrue(String.format(missingItemsMessage, value),
                        marshalledString.contains(Long.toString(value)));
            }

        } catch (Exception e) {

            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testXmlDeploymentPackagesMarshalling() {
        try {

            XmlDeploymentPackages xmlDeploymentPackages = getSampleXmlDeploymentPackagesObject();
            String marshalledString = XmlUtil.marshal(xmlDeploymentPackages);
            Assert.assertTrue(
                    String.format(missingItemsMessage, xmlDeploymentPackages.getDeploymentPackages()[0].getName()),
                    marshalledString.contains(xmlDeploymentPackages.getDeploymentPackages()[0].getName()));
            Assert.assertTrue(
                    String.format(missingItemsMessage, xmlDeploymentPackages.getDeploymentPackages()[0].getVersion()),
                    marshalledString.contains(xmlDeploymentPackages.getDeploymentPackages()[0].getVersion()));
            Assert.assertTrue(
                    String.format(missingItemsMessage,
                            xmlDeploymentPackages.getDeploymentPackages()[0].getBundleInfos()[0].getName()),
                    marshalledString
                            .contains(xmlDeploymentPackages.getDeploymentPackages()[0].getBundleInfos()[0].getName()));
            Assert.assertTrue(
                    String.format(missingItemsMessage,
                            xmlDeploymentPackages.getDeploymentPackages()[0].getBundleInfos()[0].getVersion()),
                    marshalledString.contains(
                            xmlDeploymentPackages.getDeploymentPackages()[0].getBundleInfos()[0].getVersion()));

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testXmlBundlesMarshalling() {
        try {

            XmlBundles xmlBundles = getSampleXmlBundlesObject();
            String marshalledString = XmlUtil.marshal(xmlBundles);
            Assert.assertTrue(String.format(missingItemsMessage, xmlBundles.getBundles()[0].getId()),
                    marshalledString.contains(Long.toString(xmlBundles.getBundles()[0].getId())));
            Assert.assertTrue(String.format(missingItemsMessage, xmlBundles.getBundles()[0].getName()),
                    marshalledString.contains(xmlBundles.getBundles()[0].getName()));
            Assert.assertTrue(String.format(missingItemsMessage, xmlBundles.getBundles()[0].getState()),
                    marshalledString.contains(xmlBundles.getBundles()[0].getState()));
            Assert.assertTrue(String.format(missingItemsMessage, xmlBundles.getBundles()[0].getVersion()),
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

        List<Long> snapshotIds = new ArrayList<Long>();
        snapshotIds.add(102540L);
        snapshotIds.add(27848415L);
        snapshotIds.add(378485484848L);

        XmlSnapshotIdResult xmlSnapshotIdResult = new XmlSnapshotIdResult();
        xmlSnapshotIdResult.setSnapshotIds(snapshotIds);

        return xmlSnapshotIdResult;
    }

    private static XmlComponentConfigurations getSampleXmlComponentConfigurationsObject() {

        Map<String, Object> sampleMap = new HashMap<String, Object>();

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
        xmlComponentConfigurations
                .setConfigurations(new ArrayList<ComponentConfiguration>(Arrays.asList(componentConfigurationImpl)));

        return xmlComponentConfigurations;
    }

    private static void assertValuesForEquality(String name, Object orignal, Object unmarshalled,
            boolean isStringWriter) {

        String source = isStringWriter ? stringWriter : string;

        Assert.assertTrue(String.format(propertyValueDiffersMessage, name, source, orignal, unmarshalled),
                orignal.equals(unmarshalled));
    }

    private static String printList(List<Long> list) {

        StringBuilder sb = new StringBuilder();
        for (Long id : list) {
            sb.append(id + whiteSpace);
        }
        return sb.toString();
    }

    private static void info(Object object) {
        s_logger.info(String.valueOf(object));
        System.out.println(object);
    }

}