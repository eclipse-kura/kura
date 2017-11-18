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

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverDescriptor;
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

    @Test
    public void testNewDriverDescriptor() throws Throwable {
        Driver driver = mock(Driver.class);

        AD adVal = mock(AD.class);
        List<AD> adChannelDescriptor = new ArrayList<>();
        adChannelDescriptor.add(adVal);

        ChannelDescriptor channelDescriptor = mock(ChannelDescriptor.class);

        when(driver.getChannelDescriptor()).thenReturn(channelDescriptor);
        when(channelDescriptor.getDescriptor()).thenReturn(adChannelDescriptor);

        String kuraServicePid = "kuraPid";
        String factoryPid = "factoryPid";

        DriverService driverService = new DriverServiceImpl();
        DriverDescriptor driverDescriptor = (DriverDescriptor) TestUtil.invokePrivate(driverService,
                "newDriverDescriptor", kuraServicePid, factoryPid, driver);

        assertEquals(kuraServicePid, driverDescriptor.getPid());
        assertEquals(factoryPid, driverDescriptor.getFactoryPid());
        assertEquals(adChannelDescriptor, driverDescriptor.getChannelDescriptor());
    }

    @Test(expected = NullPointerException.class)
    public void testGetDriverDescriptorNullPid() {
        DriverService driverService = new DriverServiceImpl();
        driverService.getDriverDescriptor(null);
    }

    @Test
    public void testGetDriverDescriptorNotMatchingPid() {
        DriverService driverService = getDriverServiceImplEmptyDriverServiceReferenceArray();

        Optional<DriverDescriptor> result = driverService.getDriverDescriptor("fakePid");

        assertNotNull(result);
        assertEquals(false, result.isPresent());

    }

    @Test
    public void testGetDriverDescriptorMatchingPid() throws NoSuchFieldException {
        ServiceReference<Driver> driverSr = mock(ServiceReference.class);

        DriverService driverService = getDriverServiceImplOneElementDriverServiceReferenceArray(driverSr);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(driverService, "bundleContext", context);

        Driver driverInstance = mock(Driver.class);
        AD adVal = mock(AD.class);
        List<AD> adChannelDescriptor = new ArrayList<>();
        adChannelDescriptor.add(adVal);

        ChannelDescriptor channelDescriptor = mock(ChannelDescriptor.class);

        String kuraServicePid = "fakePid";
        String factoryPid = "factoryPid";

        when(context.getService(driverSr)).thenReturn(driverInstance);
        when(driverSr.getProperty(SERVICE_FACTORYPID)).thenReturn(factoryPid);
        when(driverInstance.getChannelDescriptor()).thenReturn(channelDescriptor);
        when(channelDescriptor.getDescriptor()).thenReturn(adChannelDescriptor);

        Optional<DriverDescriptor> result = driverService.getDriverDescriptor(kuraServicePid);

        assertNotNull(result);
        assertEquals(true, result.isPresent());

        DriverDescriptor driverDescriptor = result.get();
        assertEquals(kuraServicePid, driverDescriptor.getPid());
        assertEquals(factoryPid, driverDescriptor.getFactoryPid());
        assertEquals(adChannelDescriptor, driverDescriptor.getChannelDescriptor());
    }

    @Test
    public void testListDriverDescriptorEmptyDrivers() {

        DriverService driverService = getDriverServiceImplEmptyDriverServiceReferenceArray();

        List<DriverDescriptor> driverList = driverService.listDriverDescriptors();
        assertNotNull(driverList);
        assertEquals(0, driverList.size());
    }

    @Test
    public void testListDriverDescrptorOneDriver() throws NoSuchFieldException {
        ServiceReference<Driver> driverSr = mock(ServiceReference.class);

        DriverService driverService = getDriverServiceImplOneElementDriverServiceReferenceArray(driverSr);
        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(driverService, "bundleContext", context);

        Driver driverInstance = mock(Driver.class);
        AD adVal = mock(AD.class);
        List<AD> adChannelDescriptor = new ArrayList<>();
        adChannelDescriptor.add(adVal);

        ChannelDescriptor channelDescriptor = mock(ChannelDescriptor.class);

        String kuraServicePid = "fakePid";
        String factoryPid = "factoryPid";

        when(context.getService(driverSr)).thenReturn(driverInstance);
        when(driverSr.getProperty(KURA_SERVICE_PID)).thenReturn(kuraServicePid);
        when(driverSr.getProperty(SERVICE_FACTORYPID)).thenReturn(factoryPid);
        when(driverInstance.getChannelDescriptor()).thenReturn(channelDescriptor);
        when(channelDescriptor.getDescriptor()).thenReturn(adChannelDescriptor);

        when(context.getService(driverSr)).thenReturn(driverInstance);

        List<DriverDescriptor> driverDescriptorList = driverService.listDriverDescriptors();
        assertNotNull(driverDescriptorList);
        assertEquals(1, driverDescriptorList.size());

        DriverDescriptor driverDescriptor = driverDescriptorList.get(0);
        assertEquals(kuraServicePid, driverDescriptor.getPid());
        assertEquals(factoryPid, driverDescriptor.getFactoryPid());
        assertEquals(adChannelDescriptor, driverDescriptor.getChannelDescriptor());
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
