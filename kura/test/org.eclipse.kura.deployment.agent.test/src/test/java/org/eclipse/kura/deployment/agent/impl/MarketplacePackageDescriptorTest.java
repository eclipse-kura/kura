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
 *******************************************************************************/
package org.eclipse.kura.deployment.agent.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.eclipse.kura.deployment.agent.MarketplacePackageDescriptor;
import org.eclipse.kura.deployment.agent.MarketplacePackageDescriptor.MarketplacePackageDescriptorBuilder;
import org.junit.Test;

public class MarketplacePackageDescriptorTest {

    @Test
    public void marketplacePackageDescriptorBuilderWorksWithNoData() {
        MarketplacePackageDescriptorBuilder builder = MarketplacePackageDescriptor.builder();
        MarketplacePackageDescriptor descriptor = builder.build();

        assertEquals("", descriptor.getNodeId());
        assertEquals("", descriptor.getUrl());
        assertEquals("", descriptor.getDpUrl());
        assertEquals("", descriptor.getMinKuraVersion());
        assertEquals("", descriptor.getMaxKuraVersion());
        assertEquals("", descriptor.getCurrentKuraVersion());
        assertEquals(false, descriptor.isCompatible());
    }

    @Test
    public void marketplacePackageDescriptorBuilderWorks() {
        MarketplacePackageDescriptorBuilder builder = MarketplacePackageDescriptor.builder();
        builder.nodeId("nodeId");
        builder.url("url");
        builder.dpUrl("dpUrl");
        builder.minKuraVersion("minKuraVersion");
        builder.maxKuraVersion("maxKuraVersion");
        builder.currentKuraVersion("currentKuraVersion");
        builder.isCompatible(true);

        MarketplacePackageDescriptor descriptor = builder.build();

        assertEquals("nodeId", descriptor.getNodeId());
        assertEquals("url", descriptor.getUrl());
        assertEquals("dpUrl", descriptor.getDpUrl());
        assertEquals("minKuraVersion", descriptor.getMinKuraVersion());
        assertEquals("maxKuraVersion", descriptor.getMaxKuraVersion());
        assertEquals("currentKuraVersion", descriptor.getCurrentKuraVersion());
        assertEquals(true, descriptor.isCompatible());
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithSame() {
        MarketplacePackageDescriptorBuilder builder1 = MarketplacePackageDescriptor.builder();
        builder1.nodeId("nodeId");
        builder1.url("url");
        builder1.dpUrl("dpUrl");
        builder1.minKuraVersion("minKuraVersion");
        builder1.maxKuraVersion("maxKuraVersion");
        builder1.currentKuraVersion("currentKuraVersion");
        builder1.isCompatible(true);
        MarketplacePackageDescriptor descriptor1 = builder1.build();

        assertEquals(descriptor1, descriptor1);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithEquals() {
        MarketplacePackageDescriptorBuilder builder1 = MarketplacePackageDescriptor.builder();
        builder1.nodeId("nodeId");
        builder1.url("url");
        builder1.dpUrl("dpUrl");
        builder1.minKuraVersion("minKuraVersion");
        builder1.maxKuraVersion("maxKuraVersion");
        builder1.currentKuraVersion("currentKuraVersion");
        builder1.isCompatible(true);
        MarketplacePackageDescriptor descriptor1 = builder1.build();

        MarketplacePackageDescriptorBuilder builder2 = MarketplacePackageDescriptor.builder();
        builder2.nodeId("nodeId");
        builder2.url("url");
        builder2.dpUrl("dpUrl");
        builder2.minKuraVersion("minKuraVersion");
        builder2.maxKuraVersion("maxKuraVersion");
        builder2.currentKuraVersion("currentKuraVersion");
        builder2.isCompatible(true);

        MarketplacePackageDescriptor descriptor2 = builder2.build();

        assertEquals(descriptor1, descriptor2);
        assertEquals(descriptor1.hashCode(), descriptor2.hashCode());
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithNotEquals() {
        MarketplacePackageDescriptorBuilder builder1 = MarketplacePackageDescriptor.builder();
        builder1.nodeId("nodeId");
        builder1.url("url");
        builder1.dpUrl("dpUrl");
        builder1.minKuraVersion("minKuraVersion");
        builder1.maxKuraVersion("maxKuraVersion");
        builder1.currentKuraVersion("currentKuraVersion");
        builder1.isCompatible(true);
        MarketplacePackageDescriptor descriptor1 = builder1.build();

        MarketplacePackageDescriptorBuilder builder2 = MarketplacePackageDescriptor.builder();
        builder2.nodeId("nodeId");
        builder2.url("url");
        builder2.dpUrl("dpUrl2");
        builder2.minKuraVersion("minKuraVersion");
        builder2.maxKuraVersion("maxKuraVersion");
        builder2.currentKuraVersion("currentKuraVersion");
        builder2.isCompatible(true);

        MarketplacePackageDescriptor descriptor2 = builder2.build();

        assertNotEquals(descriptor1, descriptor2);
        assertNotEquals(descriptor1.hashCode(), descriptor2.hashCode());
    }

}
