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