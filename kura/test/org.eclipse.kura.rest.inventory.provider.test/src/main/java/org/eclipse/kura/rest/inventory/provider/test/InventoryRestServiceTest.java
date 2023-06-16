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
import org.eclipse.kura.message.KuraResponsePayload;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class InventoryRestServiceTest {

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
        thenInventoryRequestIs(Arrays.asList("inventory"), "");
        thenVerifyNoExceptionOccurred();
    }

    @Test
    public void listBundlesTest() throws KuraException {
        givenInventoryHandler();
        givenInventoryRestService();

        whenGetBundles();

        thenVerifyDoGetIsRun();
        thenInventoryRequestIs(Arrays.asList("bundles"), "");
        thenVerifyNoExceptionOccurred();
    }

    @Test
    public void startBundlesTest() throws KuraException {
        givenInventoryHandler();
        givenInventoryRestService();

        whenStartBundles("{ \"name\":\"org.eclipse.kura.example.publisher\"}");

        thenVerifyDoExecIsRun();
        thenInventoryRequestIs(Arrays.asList("bundles", "_start"),
                "{ \"name\":\"org.eclipse.kura.example.publisher\"}");
        thenVerifyNoExceptionOccurred();
    }

    @Test
    public void stopBundlesTest() throws KuraException {
        givenInventoryHandler();
        givenInventoryRestService();

        whenStopBundles("{ \"name\":\"org.eclipse.kura.example.publisher\"}");

        thenVerifyDoExecIsRun();
        thenInventoryRequestIs(Arrays.asList("bundles", "_stop"), "{ \"name\":\"org.eclipse.kura.example.publisher\"}");
        thenVerifyNoExceptionOccurred();
    }

    @Test
    public void listDeploymentPackagesTest() throws KuraException {
        givenInventoryHandler();
        givenInventoryRestService();

        whenGetDeploymentPackages();

        thenVerifyDoGetIsRun();
        thenInventoryRequestIs(Arrays.asList("deploymentPackages"), "");
        thenVerifyNoExceptionOccurred();
    }

    @Test
    public void listSystemPackagesTest() throws KuraException {
        givenInventoryHandler();
        givenInventoryRestService();

        whenGetSystemPackages();

        thenVerifyDoGetIsRun();
        thenInventoryRequestIs(Arrays.asList("systemPackages"), "");
        thenVerifyNoExceptionOccurred();
    }

    @Test
    public void listContainersTest() throws KuraException {
        givenInventoryHandler();
        givenInventoryRestService();

        whenGetContainers();

        thenVerifyDoGetIsRun();
        thenInventoryRequestIs(Arrays.asList("containers"), "");
        thenVerifyNoExceptionOccurred();
    }

    @Test
    public void startContainerTest() throws KuraException {
        givenInventoryHandler();
        givenInventoryRestService();

        whenStartContainer("{ \"name\":\"kura-test-container\"}");

        thenVerifyDoExecIsRun();
        thenInventoryRequestIs(Arrays.asList("containers", "_start"), "{ \"name\":\"kura-test-container\"}");
        thenVerifyNoExceptionOccurred();
    }

    @Test
    public void stopContainerTest() throws KuraException {
        givenInventoryHandler();
        givenInventoryRestService();

        whenStopContainer("{ \"name\":\"kura-test-container\"}");

        thenVerifyDoExecIsRun();
        thenInventoryRequestIs(Arrays.asList("containers", "_stop"), "{ \"name\":\"kura-test-container\"}");
        thenVerifyNoExceptionOccurred();
    }

    @Test
    public void listImagesTest() throws KuraException {
        givenInventoryHandler();
        givenInventoryRestService();

        whenGetImages();

        thenVerifyDoGetIsRun();
        thenInventoryRequestIs(Arrays.asList("images"), "");
        thenVerifyNoExceptionOccurred();
    }

    @Test
    public void deleteImageTest() throws KuraException {
        givenInventoryHandler();
        givenInventoryRestService();

        whenDeleteImage("{ \"name\":\"nginx\"}");

        thenVerifyDoExecIsRun();
        thenInventoryRequestIs(Arrays.asList("images", "_delete"), "{ \"name\":\"nginx\"}");
        thenVerifyNoExceptionOccurred();
    }

    private void givenInventoryHandler() throws KuraException {
        inventoryHandlerV1 = mock(InventoryHandlerV1.class);

        KuraResponsePayload fakeKuraPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
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
            hasExceptionOccured = true;
        }
    }

    private void whenGetBundles() {
        try {
            inventoryRestService.getBundles();
        } catch (Exception e) {
            hasExceptionOccured = true;
        }
    }

    private void whenStartBundles(String jsonArgument) {
        try {
            inventoryRestService.startBundle(jsonArgument);
        } catch (Exception e) {
            hasExceptionOccured = true;
        }
    }

    private void whenStopBundles(String jsonArgument) {
        try {
            inventoryRestService.stopBundle(jsonArgument);
        } catch (Exception e) {
            hasExceptionOccured = true;
        }
    }

    private void whenGetDeploymentPackages() {
        try {
            inventoryRestService.getDeploymentPackages();
        } catch (Exception e) {
            hasExceptionOccured = true;
        }
    }

    private void whenGetSystemPackages() {
        try {
            inventoryRestService.getSystemPackages();
        } catch (Exception e) {
            hasExceptionOccured = true;
        }
    }

    private void whenGetContainers() {
        try {
            inventoryRestService.getContainers();
        } catch (Exception e) {
            hasExceptionOccured = true;
        }
    }

    private void whenStartContainer(String jsonArgument) {
        try {
            inventoryRestService.startContainer(jsonArgument);
        } catch (Exception e) {
            hasExceptionOccured = true;
        }
    }

    private void whenStopContainer(String jsonArgument) {
        try {
            inventoryRestService.stopContainer(jsonArgument);
        } catch (Exception e) {
            hasExceptionOccured = true;
        }
    }

    private void whenGetImages() {
        try {
            inventoryRestService.getImages();
        } catch (Exception e) {
            hasExceptionOccured = true;
        }
    }

    private void whenDeleteImage(String jsonArgument) {
        try {
            inventoryRestService.deleteImage(jsonArgument);
        } catch (Exception e) {
            hasExceptionOccured = true;
        }
    }

    private void thenVerifyDoGetIsRun() throws KuraException {
        verify(inventoryHandlerV1).doGet(any(), kuraPayloadArgumentCaptor.capture());
    }

    private void thenVerifyDoExecIsRun() throws KuraException {
        verify(inventoryHandlerV1).doExec(any(), kuraPayloadArgumentCaptor.capture());
    }

    private void thenInventoryRequestIs(List<String> expectedArgs, String expectedBody) {
        KuraMessage receivedKuraPayload = kuraPayloadArgumentCaptor.getValue();

        assertEquals(expectedArgs, receivedKuraPayload.getProperties().get(ARGS_KEY));
        assertEquals(expectedBody, new String(receivedKuraPayload.getPayload().getBody()));
    }

    private void thenVerifyNoExceptionOccurred() {
        assertFalse(hasExceptionOccured);
    }

}
