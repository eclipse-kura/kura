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
package org.eclipse.kura.core.test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.XmlSnapshotIdResult;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.configuration.util.XmlUtil;
import org.eclipse.kura.core.deployment.XmlBundle;
import org.eclipse.kura.core.deployment.XmlBundleInfo;
import org.eclipse.kura.core.deployment.XmlBundles;
import org.eclipse.kura.core.deployment.XmlDeploymentPackage;
import org.eclipse.kura.core.deployment.XmlDeploymentPackages;
import org.eclipse.kura.core.util.IOUtil;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlUtilTest extends TestCase {

	private static final Logger s_logger = LoggerFactory.getLogger(XmlUtilTest.class);

	// Constants, Names and Messages
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
	public void testXmlBundlesUnmarshalling() {

		try {

			XmlBundles xmlBundles = getSampleXmlBundlesObject();

			// test String unmarshalling
			String marshalledString = XmlUtil.marshal(xmlBundles);
			Object unmarshalledObjectFromString = XmlUtil.unmarshal(marshalledString, XmlBundles.class);
			Assert.assertTrue(String.format(differentInstanceMessage, XmlBundles.class, unmarshalledObjectFromString.getClass()), unmarshalledObjectFromString instanceof XmlBundles);

			XmlBundles outputXmlBundles = (XmlBundles) unmarshalledObjectFromString;

			assertValuesForEquality(id, Long.toString(xmlBundles.bundles[0].getId()), Long.toString(outputXmlBundles.bundles[0].getId()), false);
			assertValuesForEquality(name, xmlBundles.bundles[0].getName(), outputXmlBundles.bundles[0].getName(), false);
			assertValuesForEquality(state, xmlBundles.bundles[0].getState(), outputXmlBundles.bundles[0].getState(), false);
			assertValuesForEquality(version, xmlBundles.bundles[0].getVersion(), outputXmlBundles.bundles[0].getVersion(), false);

			// test Reader unmarshalling
			Reader marshalledStringReader = new StringReader(marshalledString);
			Object unmarshalledObjectFromStringReader = XmlUtil.unmarshal(marshalledStringReader, XmlBundles.class);
			Assert.assertTrue(String.format(differentInstanceMessage, XmlBundles.class, unmarshalledObjectFromStringReader.getClass()), unmarshalledObjectFromStringReader instanceof XmlBundles);

			outputXmlBundles = (XmlBundles) unmarshalledObjectFromStringReader;

			assertValuesForEquality(id, Long.toString(xmlBundles.bundles[0].getId()), Long.toString(outputXmlBundles.bundles[0].getId()), true);
			assertValuesForEquality(name, xmlBundles.bundles[0].getName(), outputXmlBundles.bundles[0].getName(), true);
			assertValuesForEquality(state, xmlBundles.bundles[0].getState(), outputXmlBundles.bundles[0].getState(), true);
			assertValuesForEquality(version, xmlBundles.bundles[0].getVersion(), outputXmlBundles.bundles[0].getVersion(), true);

		} catch (Exception e) {

			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test
	public void testXmlDeploymentPackagesUnmarshalling() {

		try {
			XmlDeploymentPackages xmlDeploymentPackages = getSampleXmlDeploymentPackagesObject();

			// test String unmarshalling
			String marshalledString = XmlUtil.marshal(xmlDeploymentPackages);
			Object unmarshalledObjectFromString = XmlUtil.unmarshal(marshalledString, XmlDeploymentPackages.class);
			Assert.assertTrue(String.format(differentInstanceMessage, XmlDeploymentPackages.class, unmarshalledObjectFromString.getClass()), unmarshalledObjectFromString instanceof XmlDeploymentPackages);

			XmlDeploymentPackages outputXmlDeploymentPackages = (XmlDeploymentPackages) unmarshalledObjectFromString;

			assertValuesForEquality(name, xmlDeploymentPackages.deploymentPackages[0].getName(), outputXmlDeploymentPackages.deploymentPackages[0].getName(), false);
			assertValuesForEquality(version, xmlDeploymentPackages.deploymentPackages[0].getVersion(), outputXmlDeploymentPackages.deploymentPackages[0].getVersion(), false);
			assertValuesForEquality(bundleInfo + name, xmlDeploymentPackages.deploymentPackages[0].getBundleInfos()[0].getName(), outputXmlDeploymentPackages.deploymentPackages[0].getBundleInfos()[0].getName(), false);
			assertValuesForEquality(bundleInfo + version, xmlDeploymentPackages.deploymentPackages[0].getBundleInfos()[0].getVersion(), outputXmlDeploymentPackages.deploymentPackages[0].getBundleInfos()[0].getVersion(), false);

			// test Reader unmarshalling
			Reader marshalledStringReader = new StringReader(marshalledString);
			Object unmarshalledObjectFromStringReader = XmlUtil.unmarshal(marshalledStringReader, XmlDeploymentPackages.class);
			Assert.assertTrue(String.format(differentInstanceMessage, XmlDeploymentPackages.class, unmarshalledObjectFromStringReader.getClass()), unmarshalledObjectFromStringReader instanceof XmlDeploymentPackages);

			outputXmlDeploymentPackages = (XmlDeploymentPackages) unmarshalledObjectFromStringReader;

			assertValuesForEquality(name, outputXmlDeploymentPackages.deploymentPackages[0].getName(), xmlDeploymentPackages.deploymentPackages[0].getName(), true);
			assertValuesForEquality(version, xmlDeploymentPackages.deploymentPackages[0].getVersion(), outputXmlDeploymentPackages.deploymentPackages[0].getVersion(), true);
			assertValuesForEquality(bundleInfo + name, xmlDeploymentPackages.deploymentPackages[0].getBundleInfos()[0].getName(), outputXmlDeploymentPackages.deploymentPackages[0].getBundleInfos()[0].getName(), true);
			assertValuesForEquality(bundleInfo + version, xmlDeploymentPackages.deploymentPackages[0].getBundleInfos()[0].getVersion(), outputXmlDeploymentPackages.deploymentPackages[0].getBundleInfos()[0].getVersion(), true);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test
	public void testTocdUnmarshalling() {

		try {
			Tocd tocd = getSampleTocdObject();

			// test String unmarshalling
			String marshalledString = XmlUtil.marshal(tocd);
			Object unmarshalledObjectFromString = XmlUtil.unmarshal(marshalledString, Tocd.class);
			Assert.assertTrue(String.format(differentInstanceMessage, Tocd.class, unmarshalledObjectFromString.getClass()), unmarshalledObjectFromString instanceof Tocd);

			Tocd outputXmlDeploymentPackages = (Tocd) unmarshalledObjectFromString;

			assertValuesForEquality(id, tocd.getId(), outputXmlDeploymentPackages.getId(), false);
			assertValuesForEquality(name, tocd.getName(), outputXmlDeploymentPackages.getName(), false);
			assertValuesForEquality(description, tocd.getDescription(), outputXmlDeploymentPackages.getDescription(), false);

			// test Reader unmarshalling
			Reader marshalledStringReader = new StringReader(marshalledString);
			Object unmarshalledObjectFromStringReader = XmlUtil.unmarshal(marshalledStringReader, Tocd.class);
			Assert.assertTrue(String.format(differentInstanceMessage, Tocd.class, unmarshalledObjectFromStringReader.getClass()), unmarshalledObjectFromStringReader instanceof Tocd);

			outputXmlDeploymentPackages = (Tocd) unmarshalledObjectFromStringReader;

			assertValuesForEquality(id, tocd.getId(), outputXmlDeploymentPackages.getId(), true);
			assertValuesForEquality(name, tocd.getName(), outputXmlDeploymentPackages.getName(), true);
			assertValuesForEquality(description, tocd.getDescription(), outputXmlDeploymentPackages.getDescription(), true);

		} catch (Exception e) {

			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test
	public void testXmlSnapshotIdResultUnmarshalling() {

		try {
			XmlSnapshotIdResult xmlSnapshotIdResult = getSampleXmlSnapshotIdResultObject();

			// test String unmarshalling
			String marshalledString = XmlUtil.marshal(xmlSnapshotIdResult);
			Object unmarshalledObjectFromString = XmlUtil.unmarshal(marshalledString, XmlSnapshotIdResult.class);
			Assert.assertTrue(String.format(differentInstanceMessage, XmlSnapshotIdResult.class, unmarshalledObjectFromString.getClass()), unmarshalledObjectFromString instanceof XmlSnapshotIdResult);
			XmlSnapshotIdResult outputXmlSnapshotIdResult = (XmlSnapshotIdResult) unmarshalledObjectFromString;
			// Checking for missing items
			for (Long snapshotId : xmlSnapshotIdResult.getSnapshotIds()) {
				Assert.assertTrue(String.format(missingItemsMessage, snapshotId), outputXmlSnapshotIdResult.getSnapshotIds().contains(snapshotId));
			}
			// Checking for additional items
			if (xmlSnapshotIdResult.getSnapshotIds().size() < outputXmlSnapshotIdResult.getSnapshotIds().size()) {
				info(marshalled + printList(xmlSnapshotIdResult.getSnapshotIds()));
				info(whiteSpace);
				info(unmarshalled + printList(outputXmlSnapshotIdResult.getSnapshotIds()));
				Assert.fail(String.format(additionalItmesMessage));
			}

			// test Reader unmarshalling
			Reader marshalledStringReader = new StringReader(marshalledString);
			Object unmarshalledObjectFromStringReader = XmlUtil.unmarshal(marshalledStringReader, XmlSnapshotIdResult.class);
			Assert.assertTrue(String.format(differentInstanceMessage, XmlSnapshotIdResult.class, unmarshalledObjectFromStringReader.getClass()), unmarshalledObjectFromStringReader instanceof XmlSnapshotIdResult);
			outputXmlSnapshotIdResult = (XmlSnapshotIdResult) unmarshalledObjectFromStringReader;
			// Checking for missing items
			for (Long snapshotId : xmlSnapshotIdResult.getSnapshotIds()) {
				Assert.assertTrue(String.format(missingItemsMessage, snapshotId), outputXmlSnapshotIdResult.getSnapshotIds().contains(snapshotId));
			}
			// Checking for additional items
			if (xmlSnapshotIdResult.getSnapshotIds().size() < outputXmlSnapshotIdResult.getSnapshotIds().size()) {
				info(marshalled + printList(xmlSnapshotIdResult.getSnapshotIds()));
				info(whiteSpace);
				info(unmarshalled + printList(outputXmlSnapshotIdResult.getSnapshotIds()));
				Assert.fail(String.format(additionalItmesMessage));
			}

		} catch (Exception e) {

			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test
	public void testXmlComponentConfigurationsUnmarshalling() {

		try {

			XmlComponentConfigurations xmlComponentConfigurations = getSampletestXmlComponentConfigurationsObject();

			// test String unmarshalling
			String marshalledString = XmlUtil.marshal(xmlComponentConfigurations);
			Object unmarshalledObjectFromString = XmlUtil.unmarshal(marshalledString, XmlComponentConfigurations.class);
			Assert.assertTrue(String.format(differentInstanceMessage, XmlComponentConfigurations.class, unmarshalledObjectFromString.getClass()), unmarshalledObjectFromString instanceof XmlComponentConfigurations);

			XmlComponentConfigurations outputXmlComponentConfigurations = (XmlComponentConfigurations) unmarshalledObjectFromString;

			assertValuesForEquality(pid, xmlComponentConfigurations.getConfigurations().get(0).getPid(), outputXmlComponentConfigurations.getConfigurations().get(0).getPid(), false);
			assertValuesForEquality(tocdDescription + id, xmlComponentConfigurations.getConfigurations().get(0).getDefinition().getId(), outputXmlComponentConfigurations.getConfigurations().get(0).getDefinition().getId(), false);
			assertValuesForEquality(tocdDescription + name, xmlComponentConfigurations.getConfigurations().get(0).getDefinition().getName(), outputXmlComponentConfigurations.getConfigurations().get(0).getDefinition().getName(), false);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("int"), outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("int"),
					false);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("long"), outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("long"),
					false);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("string"),
					outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("string"), false);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("boolean"),
					outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("boolean"), false);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("double"),
					outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("double"), false);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("float"),
					outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("float"), false);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("char"), outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("char"),
					false);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("short"),
					outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("short"), false);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("byte"), outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("byte"),
					false);
			// junit.framework.AssertionFailedError: Property value Hashmap
			// values of unmarshalled object from String differs from original.
			// Orignal : [C@1915e83 ; Received : [C@d3ee56 ;

			// assertValuesForEquality(hashmapValues, ((Password)
			// xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("password")).getPassword(),
			// ((Password)
			// outputXmlComponentConfigurations.getConfigurations().get(0)
			// .getConfigurationProperties().get("password")).getPassword(),
			// false);

			// test Reader unmarshalling
			Reader marshalledStringReader = new StringReader(marshalledString);
			Object unmarshalledObjectFromStringReader = XmlUtil.unmarshal(marshalledStringReader, XmlComponentConfigurations.class);
			Assert.assertTrue(String.format(differentInstanceMessage, XmlComponentConfigurations.class, unmarshalledObjectFromStringReader.getClass()), unmarshalledObjectFromStringReader instanceof XmlComponentConfigurations);

			outputXmlComponentConfigurations = (XmlComponentConfigurations) unmarshalledObjectFromStringReader;

			assertValuesForEquality(pid, xmlComponentConfigurations.getConfigurations().get(0).getPid(), outputXmlComponentConfigurations.getConfigurations().get(0).getPid(), true);
			assertValuesForEquality(tocdDescription + id, xmlComponentConfigurations.getConfigurations().get(0).getDefinition().getId(), outputXmlComponentConfigurations.getConfigurations().get(0).getDefinition().getId(), true);
			assertValuesForEquality(tocdDescription + name, xmlComponentConfigurations.getConfigurations().get(0).getDefinition().getName(), outputXmlComponentConfigurations.getConfigurations().get(0).getDefinition().getName(), true);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("int"), outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("int"),
					true);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("long"), outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("long"),
					true);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("string"),
					outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("string"), true);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("boolean"),
					outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("boolean"), true);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("double"),
					outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("double"), true);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("float"),
					outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("float"), true);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("char"), outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("char"),
					true);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("short"),
					outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("short"), true);
			assertValuesForEquality(hashmapValues, xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("byte"), outputXmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("byte"),
					true);
			// assertValuesForEquality(hashmapValues, ((Password)
			// xmlComponentConfigurations.getConfigurations().get(0).getConfigurationProperties().get("password")).getPassword(),
			// ((Password)
			// outputXmlComponentConfigurations.getConfigurations().get(0)
			// .getConfigurationProperties().get("password")).getPassword(),
			// true);

		} catch (Exception e) {

			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
	@Test
	public void testRaspberrySnapshotXmlUnmarshalling() throws IOException {

		try {

			String content = IOUtil.readResource(FrameworkUtil.getBundle(getClass()), snapshotFilePath);
			Object unmarshalledObjectFromFileReader = XmlUtil.unmarshal(content, Tocd.class);
			Assert.assertTrue(String.format(differentInstanceMessage, Tocd.class, unmarshalledObjectFromFileReader.getClass()), unmarshalledObjectFromFileReader instanceof Tocd);

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
		snapshotIds.add(1L);
		snapshotIds.add(2L);
		snapshotIds.add(3L);

		XmlSnapshotIdResult xmlSnapshotIdResult = new XmlSnapshotIdResult();
		xmlSnapshotIdResult.setSnapshotIds(snapshotIds);

		return xmlSnapshotIdResult;
	}

	private static XmlComponentConfigurations getSampletestXmlComponentConfigurationsObject() {

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
		Password password = new Password("password".toCharArray());
		sampleMap.put("password", password);

		ComponentConfigurationImpl componentConfigurationImpl = new ComponentConfigurationImpl();
		componentConfigurationImpl.setPid("8236");
		componentConfigurationImpl.setDefinition(getSampleTocdObject());
		componentConfigurationImpl.setProperties(sampleMap);

		XmlComponentConfigurations xmlComponentConfigurations = new XmlComponentConfigurations();
		xmlComponentConfigurations.setConfigurations(new ArrayList<ComponentConfigurationImpl>(Arrays.asList(componentConfigurationImpl)));

		return xmlComponentConfigurations;
	}

	private static void assertValuesForEquality(String name, Object orignal, Object unmarshalled, boolean isStringWriter) {

		String source = isStringWriter ? stringWriter : string;

		Assert.assertTrue(String.format(propertyValueDiffersMessage, name, source, orignal, unmarshalled), orignal.equals(unmarshalled));
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