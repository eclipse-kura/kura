/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.core.cloud;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.system.SystemService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;


public class CloudServiceImplTest {
    
    static CloudServiceImpl cloudServiceImpl;
    
    
    @BeforeClass
    public static void testSetup() {
        DataService dataService = mock(DataService.class);
        SystemService systemService = mock(SystemService.class);
        
        cloudServiceImpl = new CloudServiceImpl();
        
        cloudServiceImpl.setDataService(dataService);
        cloudServiceImpl.setSystemService(systemService);
    }

    @Test
    public void testNewCloudClientDataServiceNotConnected() throws KuraException {
        CloudServiceImpl cloudServiceImpl = new CloudServiceImpl();
        
        CloudClient cloudClient = cloudServiceImpl.newCloudClient("testAPP");
        
        assertNotNull(cloudClient);
    }
    
    @Test
    public void testNewCloudClientGetCloudApplicationIdentifiersEmpty() throws KuraException {
        CloudServiceImpl cloudServiceImpl = new CloudServiceImpl();
        
        String[] cloudAppsIds = cloudServiceImpl.getCloudApplicationIdentifiers();
        
        assertNotNull(cloudAppsIds);
        assertArrayEquals(new String[0], cloudAppsIds);
    }
    
    @Test
    public void testNewCloudClientGetCloudApplicationIdentifiersOneCloudClient() throws KuraException {
        CloudServiceImpl cloudServiceImpl = new CloudServiceImpl();
        
        String testAppId = "testAPP";
        cloudServiceImpl.newCloudClient(testAppId);
        
        String[] cloudAppsIds = cloudServiceImpl.getCloudApplicationIdentifiers();
        
        assertNotNull(cloudAppsIds);
        assertEquals(1, cloudAppsIds.length);
        assertEquals(testAppId, cloudAppsIds[0]);
    }
    
    @Test
    public void testNewCloudClientGetCloudApplicationIdentifiersOneRequestHandler() throws KuraException {
        CloudServiceImpl cloudServiceImpl = new CloudServiceImpl();
        
        String testAppId = "testAPP";
        RequestHandler requestHandler = mock(RequestHandler.class);
        cloudServiceImpl.registerRequestHandler(testAppId, requestHandler);
        
        String[] cloudAppsIds = cloudServiceImpl.getCloudApplicationIdentifiers();
        
        assertNotNull(cloudAppsIds);
        assertEquals(1, cloudAppsIds.length);
        assertEquals(testAppId, cloudAppsIds[0]);
    }
    
    @Test
    public void testGetCloudServiceOptions() {
        ComponentContext componentContext = mock(ComponentContext.class);
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationService.KURA_SERVICE_PID, "Cloud Service");
        
        BundleContext bundleContext = mock(BundleContext.class);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
        
        cloudServiceImpl.activate(componentContext, properties);
        
        CloudServiceOptions options = cloudServiceImpl.getCloudServiceOptions();
        
        assertNotNull(options);
    }

}
