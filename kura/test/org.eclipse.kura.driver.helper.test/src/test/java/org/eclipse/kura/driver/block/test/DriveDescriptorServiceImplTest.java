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
import org.eclipse.kura.driver.descriptor.DriverDescriptor;
import org.eclipse.kura.driver.descriptor.DriverDescriptorService;
import org.eclipse.kura.internal.driver.DriverDescriptorServiceImpl;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class DriveDescriptorServiceImplTest {

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

        DriverDescriptorService driverDescriptorService = new DriverDescriptorServiceImpl();
        DriverDescriptor driverDescriptor = (DriverDescriptor) TestUtil.invokePrivate(driverDescriptorService,
                "newDriverDescriptor", kuraServicePid, factoryPid, driver);

        assertEquals(kuraServicePid, driverDescriptor.getPid());
        assertEquals(factoryPid, driverDescriptor.getFactoryPid());
        assertEquals(adChannelDescriptor, driverDescriptor.getChannelDescriptor());
    }

    @Test(expected = NullPointerException.class)
    public void testGetDriverDescriptorNullPid() {
        DriverDescriptorService driverDescriptorService = new DriverDescriptorServiceImpl();
        driverDescriptorService.getDriverDescriptor(null);
    }

    @Test
    public void testGetDriverDescriptorNotMatchingPid() {
        DriverDescriptorService driverService = getDriverDescriptorServiceImplEmptyDriverServiceReferenceArray();

        Optional<DriverDescriptor> result = driverService.getDriverDescriptor("fakePid");

        assertNotNull(result);
        assertEquals(false, result.isPresent());

    }

    @Test
    public void testGetDriverDescriptorMatchingPid() throws NoSuchFieldException {
        ServiceReference<Driver> driverSr = mock(ServiceReference.class);

        DriverDescriptorService driverDescriptorService = getDriverDescriptorServiceImplOneElementDriverServiceReferenceArray(
                driverSr);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(driverDescriptorService, "bundleContext", context);

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

        Optional<DriverDescriptor> result = driverDescriptorService.getDriverDescriptor(kuraServicePid);

        assertNotNull(result);
        assertEquals(true, result.isPresent());

        DriverDescriptor driverDescriptor = result.get();
        assertEquals(kuraServicePid, driverDescriptor.getPid());
        assertEquals(factoryPid, driverDescriptor.getFactoryPid());
        assertEquals(adChannelDescriptor, driverDescriptor.getChannelDescriptor());
    }

    @Test
    public void testListDriverDescriptorEmptyDrivers() {

        DriverDescriptorService driverService = getDriverDescriptorServiceImplEmptyDriverServiceReferenceArray();

        List<DriverDescriptor> driverList = driverService.listDriverDescriptors();
        assertNotNull(driverList);
        assertEquals(0, driverList.size());
    }

    @Test
    public void testListDriverDescrptorOneDriver() throws NoSuchFieldException {
        ServiceReference<Driver> driverSr = mock(ServiceReference.class);

        DriverDescriptorService driverService = getDriverDescriptorServiceImplOneElementDriverServiceReferenceArray(
                driverSr);
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

    private DriverDescriptorServiceImpl getDriverDescriptorServiceImplEmptyDriverServiceReferenceArray() {
        return new DriverDescriptorServiceImpl() {

            @Override
            protected ServiceReference<Driver>[] getDriverServiceReferences(final String filter) {
                return new ServiceReference[0];
            }

            @Override
            protected void ungetDriverServiceReferences(final ServiceReference<Driver>[] refs) {
            }
        };
    }

    private DriverDescriptorServiceImpl getDriverDescriptorServiceImplOneElementDriverServiceReferenceArray(
            ServiceReference<Driver> driverSr) {
        return new DriverDescriptorServiceImpl() {

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
