/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.driver.block.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.internal.driver.DriverServiceImpl;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class DriverServiceImplTest {

    @Test
    public void testListDriversEmptyDrivers() {

        DriverService driverService = getDriverServiceImplEmptyDriverServiceReferenceArray();

        List<Driver> driverList = driverService.listDrivers();
        assertNotNull(driverList);
        assertEquals(0, driverList.size());
    }

    @Test
    public void testListDriversOneDriver() throws NoSuchFieldException {
        BundleContext context = mock(BundleContext.class);

        ServiceReference<Driver> driverSr = mock(ServiceReference.class);

        Driver driverInstance = mock(Driver.class);

        DriverService driverService = getDriverServiceImplOneElementDriverServiceReferenceArray(driverSr);
        TestUtil.setFieldValue(driverService, "bundleContext", context);

        when(context.getService(driverSr)).thenReturn(driverInstance);

        List<Driver> driverList = driverService.listDrivers();
        assertNotNull(driverList);
        assertEquals(1, driverList.size());
        assertEquals(driverInstance, driverList.get(0));
    }

    private DriverServiceImpl getDriverServiceImplEmptyDriverServiceReferenceArray() {
        return new DriverServiceImpl() {

            @Override
            protected ServiceReference<Driver>[] getDriverServiceReferences(final String filter) {
                return new ServiceReference[0];
            }

            @Override
            protected void ungetDriverServiceReferences(final ServiceReference<Driver>[] refs) {
            }
        };
    }

    private DriverServiceImpl getDriverServiceImplOneElementDriverServiceReferenceArray(
            ServiceReference<Driver> driverSr) {
        return new DriverServiceImpl() {

            @Override
            protected ServiceReference<Driver>[] getDriverServiceReferences(final String filter) {
                ServiceReference[] driverSrArray = new ServiceReference[1];
                driverSrArray[0] = driverSr;
                return driverSrArray;
            }

            @Override
            protected void ungetDriverServiceReferences(final ServiceReference<Driver>[] refs) {
            }
        };
    }
}
