/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.status;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.eclipse.kura.core.testutil.TestUtil;

import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
import org.eclipse.kura.system.SystemService;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.component.ComponentContext;

public class CloudConnectionStatusServiceImplTest {

    private static final String STATUS_NOTIFICATION_URL = "ccs.status.notification.url";

    @Test
    public void testActivateNone() throws NoSuchFieldException {
        CloudConnectionStatusServiceImpl service = initCloudConnectionStatusService("ccs:none");

        ComponentContext componentContext = mock(ComponentContext.class);
        service.activate(componentContext);

        assertNotNull(TestUtil.getFieldValue(service, "properties"));
        assertNotNull(TestUtil.getFieldValue(service, "idleComponent"));
        HashSet<CloudConnectionStatusComponent> componentRegistry = (HashSet) TestUtil.getFieldValue(service,
                "componentRegistry");
        assertEquals(1, componentRegistry.size());
    }

    @Test
    public void testActivateLog() throws NoSuchFieldException {
        CloudConnectionStatusServiceImpl service = initCloudConnectionStatusService("ccs:log");

        ComponentContext componentContext = mock(ComponentContext.class);
        service.activate(componentContext);

        assertNotNull(TestUtil.getFieldValue(service, "properties"));
        assertNotNull(TestUtil.getFieldValue(service, "idleComponent"));
        HashSet<CloudConnectionStatusComponent> componentRegistry = (HashSet) TestUtil.getFieldValue(service,
                "componentRegistry");
        assertEquals(1, componentRegistry.size());
    }

    @Test
    public void testActivateGpioLed() throws NoSuchFieldException {
        CloudConnectionStatusServiceImpl service = initCloudConnectionStatusService("ccs:led:44");

        ComponentContext componentContext = mock(ComponentContext.class);
        service.activate(componentContext);

        assertNotNull(TestUtil.getFieldValue(service, "properties"));
        assertNotNull(TestUtil.getFieldValue(service, "idleComponent"));
        HashSet<CloudConnectionStatusComponent> componentRegistry = (HashSet) TestUtil.getFieldValue(service,
                "componentRegistry");
        assertEquals(1, componentRegistry.size());
    }

    @Test
    public void testActivateLinuxGpioLed() throws NoSuchFieldException {
        CloudConnectionStatusServiceImpl service = initCloudConnectionStatusService(
                "ccs:linux_led:/sys/class/led/led1_green" + ";" + "ccs:led:44");

        ComponentContext componentContext = mock(ComponentContext.class);
        service.activate(componentContext);

        assertNotNull(TestUtil.getFieldValue(service, "properties"));
        assertNotNull(TestUtil.getFieldValue(service, "idleComponent"));
        HashSet<CloudConnectionStatusComponent> componentRegistry = (HashSet) TestUtil.getFieldValue(service,
                "componentRegistry");
        assertEquals(1, componentRegistry.size());
    }

    @Test
    public void testDeactivate() throws NoSuchFieldException {
        CloudConnectionStatusServiceImpl service = initCloudConnectionStatusService("ccs:log");

        ComponentContext componentContext = mock(ComponentContext.class);
        service.activate(componentContext);

        service.deactivate(componentContext);
        ExecutorService executorservice = (ExecutorService) TestUtil.getFieldValue(service, "notificationExecutor");

        assertTrue(executorservice.isShutdown());

        HashSet<CloudConnectionStatusComponent> componentRegistry = (HashSet) TestUtil.getFieldValue(service,
                "componentRegistry");
        assertEquals(0, componentRegistry.size());
    }

    @Test
    public void testRegisterMinPriority() throws NoSuchFieldException {
        // Prepare mock component with min priority
        CloudConnectionStatusComponent mockComponent = createMockComponent(CloudConnectionStatusService.PRIORITY_MIN,
                CloudConnectionStatusEnum.ON);

        // Create service
        CloudConnectionStatusServiceImpl service = createCloudConnStatusServiceImplLog();

        // Make sure mockComponent is initially not in componentRegistry
        assertFalse(isInComponentRegistry(service, mockComponent));

        // Register component
        service.register(mockComponent);

        // Check if mockComponent is now in componentRegistry
        assertTrue(isInComponentRegistry(service, mockComponent));

        // Check if currentStatus is set correctly (status from idle component is used)
        assertEquals(CloudConnectionStatusEnum.OFF, getStatus(service));
    }

    @Test
    public void testRegisterMaxPriority() throws NoSuchFieldException {
        // Prepare mock component with max priority
        CloudConnectionStatusComponent mockComponent = createMockComponent(CloudConnectionStatusService.PRIORITY_MAX,
                CloudConnectionStatusEnum.ON);

        CloudConnectionStatusServiceImpl service = createCloudConnStatusServiceImplLog();

        // Make sure mockComponent is initially not in componentRegistry
        assertFalse(isInComponentRegistry(service, mockComponent));

        // Register component
        service.register(mockComponent);

        // Check if mockComponent is now in componentRegistry
        assertTrue(isInComponentRegistry(service, mockComponent));

        // Check if currentStatus is set correctly
        assertEquals(CloudConnectionStatusEnum.ON, getStatus(service));
    }

    @Test
    public void testRegisterNullStatus() throws NoSuchFieldException {
        // Prepare mock component with max priority with status set to "null"
        CloudConnectionStatusComponent mockComponent = createMockComponent(CloudConnectionStatusService.PRIORITY_MAX,
                null);

        // Create service
        CloudConnectionStatusServiceImpl service = createCloudConnStatusServiceImplLog();

        // Make sure mockComponent is initially not in componentRegistry
        assertFalse(isInComponentRegistry(service, mockComponent));

        // Register component
        service.register(mockComponent);

        // Check if mockComponent is now in componentRegistry
        assertTrue(isInComponentRegistry(service, mockComponent));

        // Check if currentStatus is set correctly
        assertEquals(CloudConnectionStatusEnum.OFF, getStatus(service));
    }

    @Test
    public void testRegisterStatusesLog() throws NoSuchFieldException {
        CloudConnectionStatusEnum[] statuses = { CloudConnectionStatusEnum.OFF, CloudConnectionStatusEnum.FAST_BLINKING,
                CloudConnectionStatusEnum.SLOW_BLINKING, CloudConnectionStatusEnum.HEARTBEAT,
                CloudConnectionStatusEnum.ON };

        for (CloudConnectionStatusEnum selectedStatus : statuses) {
            // Prepare mock component with max priority with selected status
            CloudConnectionStatusComponent mockComponent = createMockComponent(
                    CloudConnectionStatusService.PRIORITY_MAX, selectedStatus);

            // Create service
            CloudConnectionStatusServiceImpl service = createCloudConnStatusServiceImplLog();

            // Make sure mockComponent is initially not in componentRegistry
            assertFalse(isInComponentRegistry(service, mockComponent));

            // Register component
            service.register(mockComponent);

            // Check if mockComponent is now in componentRegistry
            assertTrue(isInComponentRegistry(service, mockComponent));

            // Check if currentStatus is set correctly
            assertEquals(selectedStatus, getStatus(service));
        }
    }

    @Test
    public void testRegisterStatusesGpio() throws NoSuchFieldException {
        CloudConnectionStatusEnum[] statuses = { CloudConnectionStatusEnum.OFF, CloudConnectionStatusEnum.FAST_BLINKING,
                CloudConnectionStatusEnum.SLOW_BLINKING, CloudConnectionStatusEnum.HEARTBEAT,
                CloudConnectionStatusEnum.ON };

        for (CloudConnectionStatusEnum selectedStatus : statuses) {
            // Prepare mock component with max priority with selected status
            CloudConnectionStatusComponent mockComponent = createMockComponent(
                    CloudConnectionStatusService.PRIORITY_MAX, selectedStatus);

            // Create service
            CloudConnectionStatusServiceImpl service = initCloudConnectionStatusService("ccs:led:44");

            ComponentContext componentContext = mock(ComponentContext.class);
            service.activate(componentContext);

            // Make sure mockComponent is initially not in componentRegistry
            assertFalse(isInComponentRegistry(service, mockComponent));

            // Register component
            service.register(mockComponent);

            // Check if mockComponent is now in componentRegistry
            assertTrue(isInComponentRegistry(service, mockComponent));

            // Check if currentStatus is set correctly
            assertEquals(selectedStatus, getStatus(service));
        }
    }

    @Test
    public void testUnregister() throws NoSuchFieldException {
        // Prepare mock component with max priority with status set to "ON"
        CloudConnectionStatusComponent mockComponent = createMockComponent(CloudConnectionStatusService.PRIORITY_MAX,
                CloudConnectionStatusEnum.ON);

        // Create service
        CloudConnectionStatusServiceImpl service = createCloudConnStatusServiceImplLog();

        // Make sure mockComponent is initially not in componentRegistry
        assertFalse(isInComponentRegistry(service, mockComponent));

        // Check if currentStatus is set correctly
        assertNotEquals(CloudConnectionStatusEnum.ON, getStatus(service));

        // Register component
        service.register(mockComponent);

        // Check if mockComponent is now in componentRegistry
        assertTrue(isInComponentRegistry(service, mockComponent));

        // Check if currentStatus is set correctly
        assertEquals(CloudConnectionStatusEnum.ON, getStatus(service));

        // Now unregister the service
        service.unregister(mockComponent);

        // Make sure mockComponent is no longer in componentRegistry
        assertFalse(isInComponentRegistry(service, mockComponent));

        // Check if currentStatus is set correctly
        assertEquals(CloudConnectionStatusEnum.OFF, getStatus(service));
    }

    @Test
    public void testUpdateStatus() throws NoSuchFieldException {
        // Prepare mock component with max priority with status set to "ON"
        CloudConnectionStatusComponent mockComponent = createMockComponent(CloudConnectionStatusService.PRIORITY_MAX,
                CloudConnectionStatusEnum.ON);

        // Create service
        CloudConnectionStatusServiceImpl service = createCloudConnStatusServiceImplLog();

        // Make sure mockComponent is initially not in componentRegistry
        assertFalse(isInComponentRegistry(service, mockComponent));

        // Register component
        service.register(mockComponent);

        // Check if mockComponent is now in componentRegistry
        assertTrue(isInComponentRegistry(service, mockComponent));

        // Check if currentStatus is set correctly
        assertEquals(CloudConnectionStatusEnum.ON, getStatus(service));

        // Update the status to "fast blinking"
        Mockito.doNothing().when(mockComponent).setNotificationStatus(CloudConnectionStatusEnum.FAST_BLINKING);
        Mockito.doReturn(CloudConnectionStatusEnum.FAST_BLINKING).when(mockComponent).getNotificationStatus();

        assertTrue(service.updateStatus(mockComponent, CloudConnectionStatusEnum.FAST_BLINKING));

        // Check that notification status was really set
        Mockito.verify(mockComponent).setNotificationStatus(CloudConnectionStatusEnum.FAST_BLINKING);

        // Check if currentStatus is set correctly
        assertEquals(CloudConnectionStatusEnum.FAST_BLINKING, getStatus(service));
    }

    CloudConnectionStatusComponent createMockComponent(int priority, CloudConnectionStatusEnum status)
            throws NoSuchFieldException {
        CloudConnectionStatusComponent mockComponent = mock(CloudConnectionStatusComponent.class);
        Mockito.doReturn(priority).when(mockComponent).getNotificationPriority();
        Mockito.doReturn(status).when(mockComponent).getNotificationStatus();

        return mockComponent;
    }

    private CloudConnectionStatusServiceImpl initCloudConnectionStatusService(String type) throws NoSuchFieldException {
        CloudConnectionStatusServiceImpl service = new CloudConnectionStatusServiceImpl();

        SystemService systemService = mock(SystemService.class);

        Properties systemServiceProps = new Properties();
        systemServiceProps.setProperty(STATUS_NOTIFICATION_URL, type);

        TestUtil.setFieldValue(service, "systemService", systemService);
        when(systemService.getProperties()).thenReturn(systemServiceProps);

        return service;
    }

    private CloudConnectionStatusServiceImpl createCloudConnStatusServiceImplLog() throws NoSuchFieldException {
        // Create service
        CloudConnectionStatusServiceImpl service = new CloudConnectionStatusServiceImpl();

        Properties properties = new Properties();
        properties.put(CloudConnectionStatusURL.NOTIFICATION_TYPE, StatusNotificationTypeEnum.LOG);
        TestUtil.setFieldValue(service, "properties", properties);
        return service;
    }

    boolean isInComponentRegistry(CloudConnectionStatusServiceImpl service, CloudConnectionStatusComponent component)
            throws NoSuchFieldException {
        HashSet<CloudConnectionStatusComponent> componentRegistry = (HashSet<CloudConnectionStatusComponent>) TestUtil
                .getFieldValue(service, "componentRegistry");

        return componentRegistry.contains(component);
    }

    CloudConnectionStatusEnum getStatus(CloudConnectionStatusServiceImpl service) throws NoSuchFieldException {
        return (CloudConnectionStatusEnum) TestUtil.getFieldValue(service, "currentStatus");
    }
}
