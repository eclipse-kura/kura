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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.system.SystemAdminService;
import org.junit.BeforeClass;
import org.junit.Test;

public class SystemAdminServiceTest {

	private static CountDownLatch s_dependencyLatch = new CountDownLatch(1);	// initialize with number of dependencies
	
	private static SystemAdminService s_sysAdminService;
	
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
	
	public static void setSystemAdminService(SystemAdminService sas) {
		s_sysAdminService = sas;
		s_dependencyLatch.countDown();
	}
	
	@Test
	public void testServiceExists() {
		assertNotNull(s_sysAdminService);
	}

	@Test
  	public void testGetUptime() {
		String actual = s_sysAdminService.getUptime();
		assertTrue(Long.parseLong(actual) > 0);
	}
}