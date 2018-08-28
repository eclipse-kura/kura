/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.asset.cloudlet;

import static org.eclipse.kura.cloudconnection.request.RequestHandlerConstants.ARGS_KEY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.type.DataType;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;

public class AssetCloudletTest {

    @Test
    public void testActivateDeactivate() throws KuraException {
        // activate and deactivate

        AssetCloudlet svc = new AssetCloudlet();

        BundleContext bcMock = mock(BundleContext.class);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        svc.activate(ccMock);

        svc.deactivate(ccMock);
    }

    @Test(expected = KuraException.class)
    public void testDoGetBadTopic() throws NoSuchFieldException, KuraException {
        // activate, then doGet with an invalid app topic

        AssetCloudlet svc = new AssetCloudlet();

        BundleContext bcMock = mock(BundleContext.class);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        svc.activate(ccMock);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("topic");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        svc.doGet(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoGetBadRequestBody() throws NoSuchFieldException, KuraException {
        // activate and doGet with proper topic, but bad request body (not in JSON format)

        AssetCloudlet svc = new AssetCloudlet();

        BundleContext bcMock = mock(BundleContext.class);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        AssetService asMock = mock(AssetService.class);
        svc.bindAssetService(asMock);

        svc.activate(ccMock);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("assets");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.setBody("{bad[body}".getBytes());

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        svc.doGet(null, message);
    }

    @Test
    public void testDoGetEmptyRequest() throws NoSuchFieldException, KuraException {
        // activate and doGet with proper body with no assets, so go the empty request route

        AssetCloudlet svc = new AssetCloudlet();

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

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("assets");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.setBody("[]".getBytes());

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        KuraMessage response = svc.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) response.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(
                "[{\"name\":\"asset2\",\"channels\":[{\"name\":\"ch1\",\"type\":\"INTEGER\",\"mode\":\"READ_WRITE\"}]}]",
                new String(resPayload.getBody()));

    }

    @Test(expected = KuraException.class)
    public void testDoGetNotMetadataRequest() throws NoSuchFieldException, KuraException {
        // activate, doGet and fail due to JSON body not describing objects when creating a new MetadataRequest

        AssetCloudlet svc = new AssetCloudlet();

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

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("assets");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.setBody("[\"asset1\", \"asset2\"]".getBytes()); // not objects => fail

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        svc.doGet(null, message);
    }

    @Test
    public void testDoGet() throws NoSuchFieldException, KuraException {
        // activate, doGet, create MetadataRequest, get its metadata

        AssetCloudlet svc = new AssetCloudlet();

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

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("assets");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.setBody("[{\"name\":\"asset2\"}]".getBytes());

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        KuraMessage response = svc.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) response.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(
                "[{\"name\":\"asset2\",\"channels\":[{\"name\":\"ch1\",\"type\":\"INTEGER\",\"mode\":\"READ_WRITE\"}]}]",
                new String(resPayload.getBody()));
    }

    @Test(expected = KuraException.class)
    public void testDoExecTooManyResources() throws KuraException {
        // test doExec with a bad topic

        AssetCloudlet svc = new AssetCloudlet();

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("assets");
        resourcesList.add("read");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        svc.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecWrongFirstResource() throws KuraException {
        // test doExec with another bad topic

        AssetCloudlet svc = new AssetCloudlet();

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("assets");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        svc.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecReadJsonParseException() throws KuraException, NoSuchFieldException {
        // test doExec with non-parsable request body

        AssetCloudlet svc = new AssetCloudlet();

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

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("read");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.setBody("{[test".getBytes());

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        svc.doExec(null, message);
    }

    @Test
    public void testDoExecReadNull() throws KuraException, NoSuchFieldException {
        // test doExec with null request parsing result

        AssetCloudlet svc = new AssetCloudlet();

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

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("read");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        KuraMessage response = svc.doExec(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) response.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertTrue(new String(resPayload.getBody()).contains("{\"name\":\"asset2\""));

        verify(assetMock, times(1)).readAllChannels();
    }

    @Test
    public void testDoExecRead() throws KuraException, NoSuchFieldException {
        // test doExec for channel reading

        AssetCloudlet svc = new AssetCloudlet();

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

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("read");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        reqPayload.setBody("[{\"name\":\"asset2\",\"channels\":[{\"name\":\"ch1\"}]}]".getBytes());

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        KuraMessage response = svc.doExec(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) response.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertTrue(new String(resPayload.getBody()).contains("{\"name\":\"asset2\""));
    }

    @Test(expected = KuraException.class)
    public void testDoExecWriteNoRequestBody() throws NoSuchFieldException, InvalidSyntaxException, KuraException {
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

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("write");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        svc.doExec(null, message);
    }

    @Test
    public void testDoExecWrite() throws NoSuchFieldException, InvalidSyntaxException, KuraException {
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

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("write");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        reqPayload.setBody(
                "[{\"name\":\"asset2\",\"channels\":[{\"name\":\"ch1\",\"type\":\"INTEGER\",\"value\":\"10\"}]}]"
                        .getBytes());

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        KuraMessage response = svc.doExec(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) response.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
    }

}
