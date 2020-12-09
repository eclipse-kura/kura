/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.test;

import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.TestCase;

public class SystemAdminServiceTest extends TestCase {

    private static SystemAdminService sysAdminService = null;

    @Override
    @BeforeClass
    public void setUp() {
    }

    protected void setSystemAdminService(SystemAdminService sas) {
        sysAdminService = sas;
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testServiceExists() {
        assertNotNull(sysAdminService);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetUptime() {
        String actual = sysAdminService.getUptime();
        assertTrue(Long.parseLong(actual) > 0);
    }
}