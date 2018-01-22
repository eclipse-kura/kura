/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.asset;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.kura.asset.Asset;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class AssetServiceImplTest {

    @Test(expected = NullPointerException.class)
    public void testGetAssetNullReferences() {
        // null references result in NPE

        ServiceReference[] references = null;
        AssetServiceImpl svc = getService(references, null);

        String pid = "testPid";
        svc.getAsset(pid);
    }

    @Test
    public void testGetAssetNoReferenceMatch() {
        // a reference exists, but not for the sought pid

        ServiceReference<Asset> refMock = mock(ServiceReference.class);
        when(refMock.getProperty(KURA_SERVICE_PID)).thenReturn("somePid");

        ServiceReference[] references = { refMock };
        Asset assetMock = mock(Asset.class);

        AssetServiceImpl svc = getService(references, assetMock);

        String pid = "testPid";
        Asset asset = svc.getAsset(pid);

        assertNull(asset);
    }

    @Test
    public void testGetAssetReferenceMatch() {
        // a reference matches

        String pid = "testPid";

        ServiceReference<Asset> refMock = mock(ServiceReference.class);
        when(refMock.getProperty(KURA_SERVICE_PID)).thenReturn(pid);

        ServiceReference[] references = { refMock };
        Asset assetMock = mock(Asset.class);

        AssetServiceImpl svc = getService(references, assetMock);

        Asset asset = svc.getAsset(pid);

        assertNotNull(asset);
        assertEquals(assetMock, asset);
    }

    @Test(expected = NullPointerException.class)
    public void testGetAssetPidNullAsset() {
        // null parameter => NPE

        AssetServiceImpl svc = getService(null, null);
        svc.getAssetPid(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetAssetPidNullReferences() {
        // null references => NPE

        ServiceReference[] references = null;
        AssetServiceImpl svc = getService(references, mock(Asset.class));

        Asset assetMock = mock(Asset.class);
        String pid = svc.getAssetPid(assetMock);

        assertNull(pid);
    }

    @Test
    public void testGetAssetPidNoAssetMatch() {
        // no asset reference matches => null

        ServiceReference<Asset> refMock = mock(ServiceReference.class);
        when(refMock.getProperty(KURA_SERVICE_PID)).thenReturn("somePid");

        ServiceReference[] references = { refMock };
        Asset assetMock = mock(Asset.class);

        AssetServiceImpl svc = getService(references, assetMock);

        Asset asset = mock(Asset.class);
        String result = svc.getAssetPid(asset);

        assertNull(result);
    }

    @Test
    public void testGetAssetPidReferenceMatch() {
        // asset is found

        ServiceReference<Asset> refMock = mock(ServiceReference.class);
        String pid = "testPid";
        when(refMock.getProperty(KURA_SERVICE_PID)).thenReturn(pid);

        ServiceReference[] references = { refMock };
        Asset assetMock = mock(Asset.class);

        AssetServiceImpl svc = getService(references, assetMock);

        String result = svc.getAssetPid(assetMock);

        assertNotNull(result);
        assertEquals(result, pid);
    }

    @Test
    public void testListAssets() {
        // return all (1) assets

        ServiceReference<Asset> refMock = mock(ServiceReference.class);
        ServiceReference[] references = { refMock };

        Asset assetMock = mock(Asset.class);

        AssetServiceImpl svc = getService(references, assetMock);

        List<Asset> result = svc.listAssets();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(assetMock, result.get(0));
    }

    private AssetServiceImpl getService(ServiceReference<Asset>[] references, Asset asset) {
        BundleContext ctxMock = mock(BundleContext.class);
        when(ctxMock.getService(anyObject())).thenReturn(asset);

        return new AssetServiceImpl() {

            @Override
            protected ServiceReference<Asset>[] getAssetServiceReferences(BundleContext context) {
                return references;
            }

            @Override
            protected BundleContext getBundleContext() {
                return ctxMock;
            }

            @Override
            protected void ungetServiceReferences(BundleContext context, ServiceReference<Asset>[] refs) {
                // nothing to do, here
            }
        };
    }

}
