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
package org.eclipse.kura.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.kura.system.SystemService;
import org.junit.BeforeClass;
import org.junit.Test;

public class SystemServiceTest {

	private static SystemService s_systemService;
	private static CountDownLatch s_dependencyLatch = new CountDownLatch(1);	// initialize with number of dependencies
	private static boolean onCloudbees;

	@BeforeClass
	public static void setUp() {
		// Wait for OSGi dependencies
		try {
			if (!s_dependencyLatch.await(5, TimeUnit.SECONDS)) {
				fail("OSGi dependencies unfulfilled");
			}
		} catch (InterruptedException e) {
			fail("Interrupted waiting for OSGi dependencies");
		}
	}

	public static void setSystemService(SystemService sms) {
		s_systemService = sms;
		onCloudbees = s_systemService.getOsName().contains("Cloudbees");		
		s_dependencyLatch.countDown();
	}
	
	public static void unsetSystemService(SystemService sms) {
		s_systemService = null;
	}	

	@Test
	public void testDummy() {
		assertTrue(true);
	}

	@Test
	public void testServiceExists() {
		assertNotNull(s_systemService);
	}

	@Test
	public void testGetPrimaryMacAddress() {

		String actual = s_systemService.getPrimaryMacAddress();
		System.out.println("MAC: " + actual);

		Pattern regex = Pattern.compile("[0-9a-fA-F:]{12}");
		Matcher match = regex.matcher(actual);

		assertEquals("getPrimaryMacAddress() length", 17, actual.length());
		assertTrue("getPrimaryMacAddress() is string with colons", match.find());
	}

	@Test
	public void testGetPlatform() {
		String[] expected = { "dynacor", 					//emulated
				"Ubuntu", 									//Ubuntu
				"BeagleBone"								//BeagleBone
		};

		try {
			boolean foundMatch = false;
			for(String possibility : expected) {
				if(s_systemService.getPlatform().equals(possibility)) {
					foundMatch = true;
					break;
				}
			}
			assertTrue(foundMatch);
		} catch (Exception e) {
			fail("getPlatform() failed: " + e.getMessage());
		}	
	}

	@Test
	public void testGetOsDistro() {
		String[] expected = { "DevOsDitribution", 			//emulated
				"Linux" 									//Ubuntu
		};

		try {
			boolean foundMatch = false;
			for(String possibility : expected) {
				if(s_systemService.getOsDistro().equals(possibility)) {
					foundMatch = true;
					break;
				}
			}
			assertTrue(foundMatch);
		} catch (Exception e) {
			fail("getOsDistro() failed: " + e.getMessage());
		}	
	}

	@Test
	public void testGetOsDistroVersion() {
		String[] expected = { "DevOsDitributionVersion", 	//emulated
				"N/A" 										//Ubuntu
		};

		try {
			boolean foundMatch = false;
			for(String possibility : expected) {
				if(s_systemService.getOsDistroVersion().equals(possibility)) {
					foundMatch = true;
					break;
				}
			}
			assertTrue(foundMatch);
		} catch (Exception e) {
			fail("getOsDistroVersion() failed: " + e.getMessage());
		}		
	}

	@Test
	public void testGetOsArch() {
		String expected = System.getProperty("os.arch");		
		String actual = s_systemService.getOsArch();

		assertNotNull("getOsArch() not null", actual);
		assertEquals("getOsArch() value", expected, actual);
	}

	@Test
	public void testGetOsName() {
		String expected = System.getProperty("os.name");
		if(onCloudbees) expected = "Linux (Cloudbees)";

		String actual = s_systemService.getOsName();

		assertNotNull("getOsName() not null", actual);
		assertEquals("getOsName() value", expected, actual);
	}

	@Test
	public void testGetOsVersion() {
		String osVersion = System.getProperty("os.version");	
		StringBuilder sbOsVersion= new StringBuilder();
		sbOsVersion.append(osVersion);

		BufferedReader in= null;
		File linuxKernelVersion= null;
		FileReader fr= null;
		try{
			linuxKernelVersion = new File ("/proc/sys/kernel/version");
			if (linuxKernelVersion.exists()) {
				StringBuilder kernelVersionData= new StringBuilder();
				fr= new FileReader(linuxKernelVersion);
				in = new BufferedReader(fr);
				String tempLine= null;
				while ((tempLine = in.readLine()) != null) { 
					kernelVersionData.append(" ");
					kernelVersionData.append(tempLine);
				}
				sbOsVersion.append(kernelVersionData.toString());
			}
		} catch (FileNotFoundException e){
		} catch (IOException e){
		} finally {
			try {
				if(fr != null){
					fr.close();
				}
				if(in != null){
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		String expected= sbOsVersion.toString();
		String actual = s_systemService.getOsVersion();

		assertNotNull("getOsVersion() not null", actual);
		assertEquals("getOsVersion() value", expected, actual);
	}

	@Test
	public void testGetJavaVersion() {
		String expected = System.getProperty("java.runtime.version");
		String actual = s_systemService.getJavaVersion();

		assertNotNull("getJavaVersion() not null", actual);
		assertEquals("getJavaVersion() value", expected, actual);
	}

	@Test
	public void testGetJavaVmName() {
		String expected = System.getProperty("java.vm.name");
		String actual = s_systemService.getJavaVmName();

		assertNotNull("getJavaVmName() not null", actual);
		assertEquals("getJavaVmName() value", expected, actual);
	}

	@Test
	public void testGetJavaVmVersion() {
		String expected = System.getProperty("java.vm.version");
		String actual = s_systemService.getJavaVmVersion();

		assertNotNull("getJavaVmVersion() not null", actual);
		assertEquals("getJavaVmVersion() value", expected, actual);
	}

	@Test
	public void testGetFileSeparator() {
		String expected = System.getProperty("file.separator");
		String actual = s_systemService.getFileSeparator();

		assertNotNull("getFileSeparator() not null", actual);
		assertEquals("getFileSeparator() value", expected, actual);
	}

	@Test
	public void testJavaHome() {
		String actual = s_systemService.getJavaHome();
		assertNotNull("getJavaHome() not null", actual);
	}

	@Test
	public void testGetProductVersion() {
		assertTrue(true);
	}

	@Test
	public void testKuraTemporaryConfigDirectory() {
		assertNotNull(s_systemService.getKuraTemporaryConfigDirectory());
	}

	@Test
	public void testGetBiosVersion() {
		assertNotNull(s_systemService.getBiosVersion());
	}

	@Test
	public void getDeviceName() {
		assertNotNull(s_systemService.getDeviceName());
	}

	@Test
	public void getFirmwareVersion() {
		assertNotNull(s_systemService.getFirmwareVersion());
	}

	@Test
	public void getModelId() {
		assertNotNull(s_systemService.getModelId());
	}

	@Test
	public void getModelName() {
		assertNotNull(s_systemService.getModelName());
	}

	@Test
	public void getPartNumber() {
		assertNotNull(s_systemService.getPartNumber());
	}

	@Test
	public void getSerialNumber() {
		assertNotNull(s_systemService.getSerialNumber());
	}
}
