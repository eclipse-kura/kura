/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.asset.cloudlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.type.DataType;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

public class AssetCloudletTest {

    @Test
    public void testActivateException() throws KuraException {
        // activate with forced exception

        AssetCloudlet svc = new AssetCloudlet();

        CloudService csMock = mock(CloudService.class);
        svc.setCloudService(csMock);

        CloudClient clientMock = mock(CloudClient.class);
        when(csMock.newCloudClient("ASSET-V1")).thenThrow(new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "test"));

        ComponentContext ccMock = mock(ComponentContext.class);

        try {
            svc.activate(ccMock);
        } catch (ComponentException e) {
            assertTrue(e.getCause().getMessage().contains("test"));
        }
    }

    @Test
    public void testActivateDeactivate() throws KuraException {
        // activate and deactivate

        AssetCloudlet svc = new AssetCloudlet();

        CloudService csMock = mock(CloudService.class);
        svc.setCloudService(csMock);

        CloudClient clientMock = mock(CloudClient.class);
        when(csMock.newCloudClient("ASSET-V1")).thenReturn(clientMock);

        BundleContext bcMock = mock(BundleContext.class);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        svc.activate(ccMock);

        verify(clientMock, times(1)).addCloudClientListener(svc);

        svc.deactivate(ccMock);

        verify(clientMock, times(1)).release();
    }

    @Test
    public void testDoGetBadTopic() throws NoSuchFieldException, KuraException {
        // activate, then doGet with an invalid app topic

        AssetCloudlet svc = new AssetCloudlet();

        CloudService csMock = mock(CloudService.class);
        svc.setCloudService(csMock);

        CloudClient clientMock = mock(CloudClient.class);
        when(csMock.newCloudClient("ASSET-V1")).thenReturn(clientMock);

        BundleContext bcMock = mock(BundleContext.class);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        svc.activate(ccMock);

        CloudletTopic topic = CloudletTopic.parseAppTopic("GET/topic");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        svc.doGet(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST, respPayload.getResponseCode());
    }

    @Test
    public void testDoGetBadRequestBody() throws NoSuchFieldException, KuraException {
        // activate and doGet with proper topic, but bad request body (not in JSON format)

        AssetCloudlet svc = new AssetCloudlet();

        CloudService csMock = mock(CloudService.class);
        svc.setCloudService(csMock);

        CloudClient clientMock = mock(CloudClient.class);
        when(csMock.newCloudClient("ASSET-V1")).thenReturn(clientMock);

        BundleContext bcMock = mock(BundleContext.class);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        svc.activate(ccMock);

        CloudletTopic topic = CloudletTopic.parseAppTopic("GET/assets");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.setBody("{bad[body}".getBytes());

        svc.doGet(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST, respPayload.getResponseCode());
    }

    @Test
    public void testDoGetEmptyRequest() throws NoSuchFieldException, KuraException {
        // activate and doGet with proper body with no assets, so go the empty request route

        AssetCloudlet svc = new AssetCloudlet();

        CloudService csMock = mock(CloudService.class);
        svc.setCloudService(csMock);

        CloudClient clientMock = mock(CloudClient.class);
        when(csMock.newCloudClient("ASSET-V1")).thenReturn(clientMock);

        Asset assetMock = mock(Asset.class);
        Map<String, Channel> channels = new HashMap<>();
        Map<String, Object> channelConfig = new HashMap<>();
        Channel channel = new Channel("ch1", ChannelType.READ_WRITE, DataType.INTEGER, channelConfig);
        channels.put("ch1", channel);
        AssetConfiguration assetConfiguration = new AssetConfiguration("description", "driverPid", channels);
        when(assetMock.getAssetConfiguration()).thenReturn(assetConfiguration);

        BundleContext bcMock = mock(BundleContext.class);
        when(bcMock.getService(null)).thenReturn(assetMock);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        when(asMock.getAssetPid(assetMock)).thenReturn("asset2");

        svc.activate(ccMock);

        AssetTrackerCustomizer atc = (AssetTrackerCustomizer) TestUtil.getFieldValue(svc, "assetTrackerCustomizer");
        atc.addingService(null);

        CloudletTopic topic = CloudletTopic.parseAppTopic("GET/assets");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.setBody("[]".getBytes());

        svc.doGet(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, respPayload.getResponseCode());
        assertEquals(
                "[{\"name\":\"asset2\",\"channels\":[{\"name\":\"ch1\",\"type\":\"INTEGER\",\"mode\":\"READ_WRITE\"}]}]",
                new String(respPayload.getBody()));

    }

    @Test
    public void testDoGetNotMetadataRequest() throws NoSuchFieldException, KuraException {
        // activate, doGet and fail due to JSON body not describing objects when creating a new MetadataRequest

        AssetCloudlet svc = new AssetCloudlet();

        CloudService csMock = mock(CloudService.class);
        svc.setCloudService(csMock);

        CloudClient clientMock = mock(CloudClient.class);
        when(csMock.newCloudClient("ASSET-V1")).thenReturn(clientMock);

        BundleContext bcMock = mock(BundleContext.class);
        Asset assetMock = mock(Asset.class);
        when(bcMock.getService(null)).thenReturn(assetMock);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        when(asMock.getAssetPid(assetMock)).thenReturn("asset2");

        svc.activate(ccMock);

        AssetTrackerCustomizer atc = (AssetTrackerCustomizer) TestUtil.getFieldValue(svc, "assetTrackerCustomizer");
        atc.addingService(null);

        CloudletTopic topic = CloudletTopic.parseAppTopic("GET/assets");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.setBody("[\"asset1\", \"asset2\"]".getBytes()); // not objects => fail

        svc.doGet(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST, respPayload.getResponseCode());
    }

    @Test
    public void testDoGet() throws NoSuchFieldException, KuraException {
        // activate, doGet, create MetadataRequest, get its metadata

        AssetCloudlet svc = new AssetCloudlet();

        CloudService csMock = mock(CloudService.class);
        svc.setCloudService(csMock);

        CloudClient clientMock = mock(CloudClient.class);
        when(csMock.newCloudClient("ASSET-V1")).thenReturn(clientMock);

        Asset assetMock = mock(Asset.class);
        Map<String, Channel> channels = new HashMap<>();
        Map<String, Object> channelConfig = new HashMap<>();
        Channel channel = new Channel("ch1", ChannelType.READ_WRITE, DataType.INTEGER, channelConfig);
        channels.put("ch1", channel);
        AssetConfiguration assetConfiguration = new AssetConfiguration("description", "driverPid", channels);
        when(assetMock.getAssetConfiguration()).thenReturn(assetConfiguration);

        BundleContext bcMock = mock(BundleContext.class);
        when(bcMock.getService(null)).thenReturn(assetMock);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        when(asMock.getAssetPid(assetMock)).thenReturn("asset2");

        svc.activate(ccMock);

        AssetTrackerCustomizer atc = (AssetTrackerCustomizer) TestUtil.getFieldValue(svc, "assetTrackerCustomizer");
        atc.addingService(null);

        CloudletTopic topic = CloudletTopic.parseAppTopic("GET/assets");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.setBody("[{\"name\":\"asset2\"}]".getBytes());

        svc.doGet(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, respPayload.getResponseCode());
        assertEquals(
                "[{\"name\":\"asset2\",\"channels\":[{\"name\":\"ch1\",\"type\":\"INTEGER\",\"mode\":\"READ_WRITE\"}]}]",
                new String(respPayload.getBody()));
    }

    @Test
    public void testDoExecTooManyResources() {
        // test doExec with a bad topic

        AssetCloudlet svc = new AssetCloudlet();

        CloudletTopic topic = CloudletTopic.parseAppTopic("EXEC/assets/read");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        svc.doExec(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST, respPayload.getResponseCode());
    }

    @Test
    public void testDoExecWrongFirstResource() {
        // test doExec with another bad topic

        AssetCloudlet svc = new AssetCloudlet();

        CloudletTopic topic = CloudletTopic.parseAppTopic("EXEC/assets");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        svc.doExec(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST, respPayload.getResponseCode());
    }

    @Test
    public void testDoExecReadJsonParseException() throws KuraException, NoSuchFieldException {
        // test doExec with non-parsable request body

        AssetCloudlet svc = new AssetCloudlet();

        CloudService csMock = mock(CloudService.class);
        svc.setCloudService(csMock);

        CloudClient clientMock = mock(CloudClient.class);
        when(csMock.newCloudClient("ASSET-V1")).thenReturn(clientMock);

        Asset assetMock = mock(Asset.class);
        Map<String, Channel> channels = new HashMap<>();
        Map<String, Object> channelConfig = new HashMap<>();
        Channel channel = new Channel("ch1", ChannelType.READ_WRITE, DataType.INTEGER, channelConfig);
        channels.put("ch1", channel);
        AssetConfiguration assetConfiguration = new AssetConfiguration("description", "driverPid", channels);
        when(assetMock.getAssetConfiguration()).thenReturn(assetConfiguration);

        BundleContext bcMock = mock(BundleContext.class);
        when(bcMock.getService(null)).thenReturn(assetMock);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        when(asMock.getAssetPid(assetMock)).thenReturn("asset2");

        svc.activate(ccMock);

        AssetTrackerCustomizer atc = (AssetTrackerCustomizer) TestUtil.getFieldValue(svc, "assetTrackerCustomizer");
        atc.addingService(null);

        CloudletTopic topic = CloudletTopic.parseAppTopic("EXEC/read");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.setBody("{[test".getBytes());

        svc.doExec(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST, respPayload.getResponseCode());
    }

    @Test
    public void testDoExecReadNull() throws KuraException, NoSuchFieldException {
        // test doExec with null request parsing result

        AssetCloudlet svc = new AssetCloudlet();

        CloudService csMock = mock(CloudService.class);
        svc.setCloudService(csMock);

        CloudClient clientMock = mock(CloudClient.class);
        when(csMock.newCloudClient("ASSET-V1")).thenReturn(clientMock);

        Asset assetMock = mock(Asset.class);
        Map<String, Channel> channels = new HashMap<>();
        Map<String, Object> channelConfig = new HashMap<>();
        Channel channel = new Channel("ch1", ChannelType.READ_WRITE, DataType.INTEGER, channelConfig);
        channels.put("ch1", channel);
        AssetConfiguration assetConfiguration = new AssetConfiguration("description", "driverPid", channels);
        when(assetMock.getAssetConfiguration()).thenReturn(assetConfiguration);

        BundleContext bcMock = mock(BundleContext.class);
        when(bcMock.getService(null)).thenReturn(assetMock);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        when(asMock.getAssetPid(assetMock)).thenReturn("asset2");

        svc.activate(ccMock);

        AssetTrackerCustomizer atc = (AssetTrackerCustomizer) TestUtil.getFieldValue(svc, "assetTrackerCustomizer");
        atc.addingService(null);

        CloudletTopic topic = CloudletTopic.parseAppTopic("EXEC/read");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        svc.doExec(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, respPayload.getResponseCode());
        assertTrue(new String(respPayload.getBody()).contains("{\"name\":\"asset2\""));

        verify(assetMock, times(1)).readAllChannels();
    }

    @Test
    public void testDoExecRead() throws KuraException, NoSuchFieldException {
        // test doExec for channel reading

        AssetCloudlet svc = new AssetCloudlet();

        CloudService csMock = mock(CloudService.class);
        svc.setCloudService(csMock);

        CloudClient clientMock = mock(CloudClient.class);
        when(csMock.newCloudClient("ASSET-V1")).thenReturn(clientMock);

        Asset assetMock = mock(Asset.class);
        Map<String, Channel> channels = new HashMap<>();
        Map<String, Object> channelConfig = new HashMap<>();
        Channel channel = new Channel("ch1", ChannelType.READ_WRITE, DataType.INTEGER, channelConfig);
        channels.put("ch1", channel);
        AssetConfiguration assetConfiguration = new AssetConfiguration("description", "driverPid", channels);
        when(assetMock.getAssetConfiguration()).thenReturn(assetConfiguration);

        BundleContext bcMock = mock(BundleContext.class);
        when(bcMock.getService(null)).thenReturn(assetMock);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        when(asMock.getAssetPid(assetMock)).thenReturn("asset2");

        svc.activate(ccMock);

        AssetTrackerCustomizer atc = (AssetTrackerCustomizer) TestUtil.getFieldValue(svc, "assetTrackerCustomizer");
        atc.addingService(null);

        CloudletTopic topic = CloudletTopic.parseAppTopic("EXEC/read");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.setBody("[{\"name\":\"asset2\",\"channels\":[{\"name\":\"ch1\"}]}]".getBytes());

        svc.doExec(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, respPayload.getResponseCode());
        assertTrue(new String(respPayload.getBody()).contains("{\"name\":\"asset2\""));
    }

    @Test
    public void testDoExecWriteNoRequestBody() throws NoSuchFieldException, InvalidSyntaxException {
        // test doExec initiating write with no request body

        AssetCloudlet svc = new AssetCloudlet();

        BundleContext bcMock = mock(BundleContext.class);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        AssetTrackerCustomizer atc = new AssetTrackerCustomizer(bcMock, asMock);
        TestUtil.setFieldValue(svc, "assetTrackerCustomizer", atc);

        Asset assetMock = mock(Asset.class);

        when(bcMock.getService(null)).thenReturn(assetMock);

        when(asMock.getAssetPid(assetMock)).thenReturn("asset2");

        atc.addingService(null);

        CloudletTopic topic = CloudletTopic.parseAppTopic("EXEC/write");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        svc.doExec(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST, respPayload.getResponseCode());
    }

    @Test
    public void testDoExecWrite() throws NoSuchFieldException, InvalidSyntaxException {
        // test doExec initiating write

        AssetCloudlet svc = new AssetCloudlet();

        BundleContext bcMock = mock(BundleContext.class);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        AssetTrackerCustomizer atc = new AssetTrackerCustomizer(bcMock, asMock);
        TestUtil.setFieldValue(svc, "assetTrackerCustomizer", atc);

        Asset assetMock = mock(Asset.class);

        when(bcMock.getService(null)).thenReturn(assetMock);

        when(asMock.getAssetPid(assetMock)).thenReturn("asset2");

        atc.addingService(null);

        CloudletTopic topic = CloudletTopic.parseAppTopic("EXEC/write");
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        reqPayload.setBody(
                "[{\"name\":\"asset2\",\"channels\":[{\"name\":\"ch1\",\"type\":\"INTEGER\",\"value\":\"10\"}]}]"
                        .getBytes());

        svc.doExec(topic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, respPayload.getResponseCode());
    }

}
