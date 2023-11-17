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
import org.junit.Test;

public class MarketplacePackageDescriptorTest {

    MarketplacePackageDescriptor descriptor;
    MarketplacePackageDescriptor otherDescriptor;

    @Test
    public void marketplacePackageDescriptorBuilderWorksWithNoData() {
        givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().build());

        thenNodeIdEquals(this.descriptor, "");
        thenUrlEquals(this.descriptor, "");
        thenDpUrlEquals(this.descriptor, "");
        thenMinKuraVersionEquals(this.descriptor, "");
        thenMaxKuraVersionEquals(this.descriptor, "");
        thenCurrentKuraVersionEquals(this.descriptor, "");
        thenIsCompatibleEquals(this.descriptor, false);
    }

    @Test
    public void marketplacePackageDescriptorBuilderWorks() {
        givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        thenNodeIdEquals(this.descriptor, "nodeId");
        thenUrlEquals(this.descriptor, "url");
        thenDpUrlEquals(this.descriptor, "dpUrl");
        thenMinKuraVersionEquals(this.descriptor, "minKuraVersion");
        thenMaxKuraVersionEquals(this.descriptor, "maxKuraVersion");
        thenCurrentKuraVersionEquals(this.descriptor, "currentKuraVersion");
        thenIsCompatibleEquals(this.descriptor, true);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithSame() {
        givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        thenResultingDescriptorsAreEqual(this.descriptor, this.descriptor, true);
        thenResultingHashesAreEqual(this.descriptor, this.descriptor, true);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithEquals() {
        givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        givenAnotherMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        thenResultingDescriptorsAreEqual(this.descriptor, this.otherDescriptor, true);
        thenResultingHashesAreEqual(this.descriptor, this.otherDescriptor, true);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithNotEqualsNodeId() {
        givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        givenAnotherMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("lol").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        thenResultingDescriptorsAreEqual(this.descriptor, this.otherDescriptor, false);
        thenResultingHashesAreEqual(this.descriptor, this.otherDescriptor, false);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithNotEqualsUrl() {
        givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        givenAnotherMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId")
                .url("anotherurl").dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        thenResultingDescriptorsAreEqual(this.descriptor, this.otherDescriptor, false);
        thenResultingHashesAreEqual(this.descriptor, this.otherDescriptor, false);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithNotEqualsDpUrl() {
        givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        givenAnotherMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("anotherdpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        thenResultingDescriptorsAreEqual(this.descriptor, this.otherDescriptor, false);
        thenResultingHashesAreEqual(this.descriptor, this.otherDescriptor, false);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithNotEqualsMinKuraVersion() {
        givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        givenAnotherMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("anotherminKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        thenResultingDescriptorsAreEqual(this.descriptor, this.otherDescriptor, false);
        thenResultingHashesAreEqual(this.descriptor, this.otherDescriptor, false);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithNotEqualsMaxKuraVersion() {
        givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        givenAnotherMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("anothermaxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        thenResultingDescriptorsAreEqual(this.descriptor, this.otherDescriptor, false);
        thenResultingHashesAreEqual(this.descriptor, this.otherDescriptor, false);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithNotEqualsCurrentKuraVersion() {
        givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        givenAnotherMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("anothercurrentKuraVersion").isCompatible(true).build());

        thenResultingDescriptorsAreEqual(this.descriptor, this.otherDescriptor, false);
        thenResultingHashesAreEqual(this.descriptor, this.otherDescriptor, false);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithNotEqualsCompatibility() {
        givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(true).build());

        givenAnotherMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder().nodeId("nodeId").url("url")
                .dpUrl("dpUrl").minKuraVersion("minKuraVersion").maxKuraVersion("maxKuraVersion")
                .currentKuraVersion("currentKuraVersion").isCompatible(false).build());

        thenResultingDescriptorsAreEqual(this.descriptor, this.otherDescriptor, false);
        thenResultingHashesAreEqual(this.descriptor, this.otherDescriptor, false);
    }

    /*
     * GIVEN
     */
    private void givenAMarketplacePackageDescriptor(MarketplacePackageDescriptor passedDescriptor) {
        this.descriptor = passedDescriptor;
    }

    private void givenAnotherMarketplacePackageDescriptor(MarketplacePackageDescriptor passedDescriptor) {
        this.otherDescriptor = passedDescriptor;
    }

    /*
     * THEN
     */
    private void thenIsCompatibleEquals(MarketplacePackageDescriptor passedDescriptor, boolean expectedCompatibility) {
        assertEquals(expectedCompatibility, passedDescriptor.isCompatible());
    }

    private void thenCurrentKuraVersionEquals(MarketplacePackageDescriptor passedDescriptor,
            String expectedCurrentKuraVersion) {
        assertEquals(expectedCurrentKuraVersion, passedDescriptor.getCurrentKuraVersion());
    }

    private void thenMaxKuraVersionEquals(MarketplacePackageDescriptor passedDescriptor,
            String expectedMaxKuraVersion) {
        assertEquals(expectedMaxKuraVersion, passedDescriptor.getMaxKuraVersion());
    }

    private void thenMinKuraVersionEquals(MarketplacePackageDescriptor passedDescriptor,
            String expectedMinKuraVersion) {
        assertEquals(expectedMinKuraVersion, passedDescriptor.getMinKuraVersion());
    }

    private void thenDpUrlEquals(MarketplacePackageDescriptor passedDescriptor, String expectedDpUrl) {
        assertEquals(expectedDpUrl, passedDescriptor.getDpUrl());
    }

    private void thenUrlEquals(MarketplacePackageDescriptor passedDescriptor, String expectedUrl) {
        assertEquals(expectedUrl, passedDescriptor.getUrl());
    }

    private void thenNodeIdEquals(MarketplacePackageDescriptor passedDescriptor, String expectedNodeId) {
        assertEquals(expectedNodeId, passedDescriptor.getNodeId());
    }

    private void thenResultingDescriptorsAreEqual(MarketplacePackageDescriptor descriptor1,
            MarketplacePackageDescriptor descriptor2, boolean areEqual) {
        if (areEqual) {
            assertEquals(descriptor1, descriptor2);
        } else {
            assertNotEquals(descriptor1, descriptor2);
        }
    }

    private void thenResultingHashesAreEqual(MarketplacePackageDescriptor descriptor1,
            MarketplacePackageDescriptor descriptor2, boolean areEqual) {
        if (areEqual) {
            assertEquals(descriptor1.hashCode(), descriptor2.hashCode());
        } else {
            assertNotEquals(descriptor1.hashCode(), descriptor2.hashCode());
        }
    }

}
