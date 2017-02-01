/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.status;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.Struct;
import java.util.HashSet;
import java.util.concurrent.Future;

import org.eclipse.kura.core.status.runnables.OnOffStatusRunnable;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
import org.junit.Test;
import org.mockito.Mockito;

public class CloudConnectionStatusServiceImplTest {

	@Test
	public void testRegisterMinPriority() throws NoSuchFieldException {
		// Prepare mock component with min priority
		CloudConnectionStatusComponent mockComponent = createMockComponent(CloudConnectionStatusService.PRIORITY_MIN,
				CloudConnectionStatusEnum.ON);

		// Create service
		CloudConnectionStatusServiceImpl service = new CloudConnectionStatusServiceImpl();

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
		
		// Create service
		CloudConnectionStatusServiceImpl service = new CloudConnectionStatusServiceImpl();

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
		CloudConnectionStatusServiceImpl service = new CloudConnectionStatusServiceImpl();

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
	public void testRegisterStatuses() throws NoSuchFieldException {
		CloudConnectionStatusEnum[] statuses = {
				CloudConnectionStatusEnum.OFF,
				CloudConnectionStatusEnum.FAST_BLINKING,
				CloudConnectionStatusEnum.SLOW_BLINKING,
				CloudConnectionStatusEnum.HEARTBEAT,
				CloudConnectionStatusEnum.ON
				};

		for (CloudConnectionStatusEnum selectedStatus : statuses) {
			// Prepare mock component with max priority with selected status
			CloudConnectionStatusComponent mockComponent = createMockComponent(CloudConnectionStatusService.PRIORITY_MAX,
					selectedStatus);

			// Create service
			CloudConnectionStatusServiceImpl service = new CloudConnectionStatusServiceImpl();

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
		CloudConnectionStatusServiceImpl service = new CloudConnectionStatusServiceImpl();

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
		CloudConnectionStatusServiceImpl service = new CloudConnectionStatusServiceImpl();

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
	
	CloudConnectionStatusComponent createMockComponent(int priority, CloudConnectionStatusEnum status) {
		CloudConnectionStatusComponent mockComponent = mock(CloudConnectionStatusComponent.class);
		Mockito.doReturn(priority).when(mockComponent).getNotificationPriority();
		Mockito.doReturn(status).when(mockComponent).getNotificationStatus();
		
		return mockComponent;
	}

	boolean isInComponentRegistry(CloudConnectionStatusServiceImpl service, CloudConnectionStatusComponent component)
			throws NoSuchFieldException {
		HashSet<CloudConnectionStatusComponent> componentRegistry =
				(HashSet<CloudConnectionStatusComponent>) TestUtil.getFieldValue(service, "componentRegistry");

		return componentRegistry.contains(component);
	}
	
	CloudConnectionStatusEnum getStatus(CloudConnectionStatusServiceImpl service) throws NoSuchFieldException {
		return (CloudConnectionStatusEnum) TestUtil.getFieldValue(service, "currentStatus");
	}
}
