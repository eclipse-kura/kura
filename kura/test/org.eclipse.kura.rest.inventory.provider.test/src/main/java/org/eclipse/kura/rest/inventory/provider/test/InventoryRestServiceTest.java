/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.rest.inventory.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.core.inventory.InventoryHandlerV1;
import org.eclipse.kura.internal.rest.inventory.InventoryRestService;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class InventoryRestServiceTest {

    public static final String RESOURCE_DEPLOYMENT_PACKAGES = "deploymentPackages";
    public static final String RESOURCE_BUNDLES = "bundles";
    public static final String RESOURCE_SYSTEM_PACKAGES = "systemPackages";
    public static final String RESOURCE_DOCKER_CONTAINERS = "containers";
    public static final String RESOURCE_CONTAINER_IMAGES = "images";
    public static final String INVENTORY = "inventory";

    private static final String START = "_start";
    private static final String STOP = "_stop";
    private static final String DELETE = "_delete";

    public static final List<String> START_BUNDLE = Arrays.asList(RESOURCE_BUNDLES, START);
    public static final List<String> STOP_BUNDLE = Arrays.asList(RESOURCE_BUNDLES, STOP);

    public static final List<String> START_CONTAINER = Arrays.asList(RESOURCE_DOCKER_CONTAINERS, START);
    public static final List<String> STOP_CONTAINER = Arrays.asList(RESOURCE_DOCKER_CONTAINERS, STOP);

    public static final List<String> DELETE_IMAGE = Arrays.asList(RESOURCE_CONTAINER_IMAGES, DELETE);

    private static final String ARGS_KEY = "args";

    InventoryHandlerV1 inventoryHandlerV1;
    InventoryRestService inventoryRestService;
    Boolean hasExceptionOccured = false;

    private ArgumentCaptor<KuraMessage> kuraPayloadArgumentCaptor = ArgumentCaptor.forClass(KuraMessage.class);

    @Test
    public void listInventoryTest() throws KuraException {
        givenInventoryHandler();
        givenInventoryRestService();

        whenGetInventorySummary();

        thenVerifyDoGetIsRun();
        thenInventoryRequestIs(Arrays.asList(INVENTORY), "");
        thenVerifyNoExceptionOccured();
    }

    private void givenInventoryHandler() throws KuraException {
        inventoryHandlerV1 = mock(InventoryHandlerV1.class);

        KuraPayload fakeKuraPayload = new KuraPayload();
        KuraMessage fakeKuraMessage = new KuraMessage(fakeKuraPayload);

        when(inventoryHandlerV1.doGet(any(), any())).thenReturn(fakeKuraMessage);
        when(inventoryHandlerV1.doExec(any(), any())).thenReturn(fakeKuraMessage);
    }

    private void givenInventoryRestService() {
        inventoryRestService = new InventoryRestService();
        inventoryRestService.setInventoryHandlerV1(inventoryHandlerV1);
    }

    private void whenGetInventorySummary() {
        try {
            inventoryRestService.getInventorySummary();
        } catch (Exception e) {
            e.printStackTrace();
            hasExceptionOccured = true;
        }
    }

    private void thenVerifyDoGetIsRun() throws KuraException {
        verify(inventoryHandlerV1).doGet(any(), kuraPayloadArgumentCaptor.capture());
    }

    private void thenInventoryRequestIs(List<String> expectedArgs, String expectedBody) {
        KuraMessage receivedKuraPayload = kuraPayloadArgumentCaptor.getValue();

        assertEquals(expectedArgs, receivedKuraPayload.getProperties().get(ARGS_KEY));
        assertEquals(expectedBody, new String(receivedKuraPayload.getPayload().getBody()));
    }

    private void thenVerifyNoExceptionOccured() {
        assertFalse(hasExceptionOccured);
    }

}
