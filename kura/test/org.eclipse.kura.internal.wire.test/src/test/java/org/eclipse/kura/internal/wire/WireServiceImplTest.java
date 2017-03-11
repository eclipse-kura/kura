/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.wire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireHelperService;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

public class WireServiceImplTest {

    @Test
    public void testCreateWiresNoEmitterNoReceiver() throws NoSuchFieldException, InvalidSyntaxException {
        WireServiceImpl wsi = new WireServiceImpl();

        Set<WireConfiguration> wireConfigs = new HashSet<WireConfiguration>();
        String emitterPid = "emmiter";
        String receiverPid = "receiver";
        WireConfiguration wc = new WireConfiguration(emitterPid, receiverPid);
        wireConfigs.add(wc);

        TestUtil.setFieldValue(wsi, "wireConfigs", wireConfigs);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        WireComponentTrackerCustomizer wctc = new WireComponentTrackerCustomizer(bundleCtxMock, wsi);

        TestUtil.setFieldValue(wsi, "wireComponentTrackerCustomizer", wctc);

        wsi.createWires();
    }

    @Test
    public void testCreateWires() throws NoSuchFieldException, InvalidSyntaxException {
        WireServiceImpl wsi = new WireServiceImpl();

        Set<WireConfiguration> wireConfigs = new HashSet<WireConfiguration>();
        String emitterPid = "emmiter";
        String receiverPid = "receiver";
        WireConfiguration wc = new WireConfiguration(emitterPid, receiverPid);
        wireConfigs.add(wc);

        TestUtil.setFieldValue(wsi, "wireConfigs", wireConfigs);

        BundleContext bundleCtxMock = mock(BundleContext.class);
        WireComponentTrackerCustomizer wctc = new WireComponentTrackerCustomizer(bundleCtxMock, wsi);

        TestUtil.setFieldValue(wsi, "wireComponentTrackerCustomizer", wctc);

        List<String> wireEmitterPids = new ArrayList<String>();
        wireEmitterPids.add(emitterPid);
        List<String> wireReceiverPids = new ArrayList<String>();
        wireReceiverPids.add(receiverPid);

        TestUtil.setFieldValue(wctc, "wireEmitterPids", wireEmitterPids);
        TestUtil.setFieldValue(wctc, "wireReceiverPids", wireReceiverPids);

        WireHelperService whsMock = mock(WireHelperService.class);

        TestUtil.setFieldValue(wsi, "wireHelperService", whsMock);

        wsi.createWires();

        verify(whsMock, times(1)).getServicePid(emitterPid);
        verify(whsMock, times(1)).getServicePid(receiverPid);
    }

    @Test
    public void testUpdatedCallback() throws NoSuchFieldException {
        WireServiceImpl wsi = new WireServiceImpl();

        assertNull(TestUtil.getFieldValue(wsi, "properties"));
        assertNull(TestUtil.getFieldValue(wsi, "wireServiceOptions"));

        Map<String, Object> properties = new HashMap<String, Object>();
        wsi.updated(properties);

        assertEquals(properties, TestUtil.getFieldValue(wsi, "properties"));
        assertNotNull(TestUtil.getFieldValue(wsi, "wireServiceOptions"));
    }
}
