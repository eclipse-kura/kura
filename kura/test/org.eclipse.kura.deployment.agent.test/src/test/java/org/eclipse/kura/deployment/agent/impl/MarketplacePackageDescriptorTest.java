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

    MarketplacePackageDescriptorBuilder builder;
    MarketplacePackageDescriptorBuilder otherBuilder;

    MarketplacePackageDescriptor descriptor;
    MarketplacePackageDescriptor otherDescriptor;

    @Test
    public void marketplacePackageDescriptorBuilderWorksWithNoData() {
        givenAMarketplacePackageDescriptorBuilder();
        whenBuildIsCalledForThisBuilder();

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
        givenAMarketplacePackageDescriptorBuilder();
        givenBuilderWithNodeId(this.builder, "nodeId");
        givenBuilderWithUrl(this.builder, "url");
        givenBuilderWithDpUrl(this.builder, "dpUrl");
        givenBuilderWithMinKuraVersion(this.builder, "minKuraVersion");
        givenBuilderWithMaxKuraVersion(this.builder, "maxKuraVersion");
        givenBuilderWithCurrentKuraVersion(this.builder, "currentKuraVersion");
        givenBuilderWithCompatibility(this.builder, true);

        whenBuildIsCalledForThisBuilder();

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
        givenAMarketplacePackageDescriptorBuilder();
        givenBuilderWithNodeId(this.builder, "nodeId");
        givenBuilderWithUrl(this.builder, "url");
        givenBuilderWithDpUrl(this.builder, "dpUrl");
        givenBuilderWithMinKuraVersion(this.builder, "minKuraVersion");
        givenBuilderWithMaxKuraVersion(this.builder, "maxKuraVersion");
        givenBuilderWithCurrentKuraVersion(this.builder, "currentKuraVersion");
        givenBuilderWithCompatibility(this.builder, true);

        whenBuildIsCalledForThisBuilder();

        thenResultingDescriptorsAreEqual(this.descriptor, this.descriptor, true);
        thenResultingHashesAreEqual(this.descriptor, this.descriptor, true);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithEquals() {
        givenAMarketplacePackageDescriptorBuilder();
        givenBuilderWithNodeId(this.builder, "nodeId");
        givenBuilderWithUrl(this.builder, "url");
        givenBuilderWithDpUrl(this.builder, "dpUrl");
        givenBuilderWithMinKuraVersion(this.builder, "minKuraVersion");
        givenBuilderWithMaxKuraVersion(this.builder, "maxKuraVersion");
        givenBuilderWithCurrentKuraVersion(this.builder, "currentKuraVersion");
        givenBuilderWithCompatibility(this.builder, true);

        whenBuildIsCalledForThisBuilder();

        givenAnotherMarketplacePackageDescriptorBuilder();
        givenBuilderWithNodeId(this.otherBuilder, "nodeId");
        givenBuilderWithUrl(this.otherBuilder, "url");
        givenBuilderWithDpUrl(this.otherBuilder, "dpUrl");
        givenBuilderWithMinKuraVersion(this.otherBuilder, "minKuraVersion");
        givenBuilderWithMaxKuraVersion(this.otherBuilder, "maxKuraVersion");
        givenBuilderWithCurrentKuraVersion(this.otherBuilder, "currentKuraVersion");
        givenBuilderWithCompatibility(this.otherBuilder, true);

        whenBuildIsCalledForTheOtherBuilder();

        thenResultingDescriptorsAreEqual(this.descriptor, this.otherDescriptor, true);
        thenResultingHashesAreEqual(this.descriptor, this.otherDescriptor, true);
    }

    @Test
    public void marketplacePackageDescriptorEqualsWorksWithNotEquals() {
        givenAMarketplacePackageDescriptorBuilder();
        givenBuilderWithNodeId(this.builder, "nodeId");
        givenBuilderWithUrl(this.builder, "url");
        givenBuilderWithDpUrl(this.builder, "dpUrl");
        givenBuilderWithMinKuraVersion(this.builder, "minKuraVersion");
        givenBuilderWithMaxKuraVersion(this.builder, "maxKuraVersion");
        givenBuilderWithCurrentKuraVersion(this.builder, "currentKuraVersion");
        givenBuilderWithCompatibility(this.builder, true);

        whenBuildIsCalledForThisBuilder();

        givenAnotherMarketplacePackageDescriptorBuilder();
        givenBuilderWithNodeId(this.otherBuilder, "nodeId");
        givenBuilderWithUrl(this.otherBuilder, "url");
        givenBuilderWithDpUrl(this.otherBuilder, "dpUrl2");
        givenBuilderWithMinKuraVersion(this.otherBuilder, "minKuraVersion");
        givenBuilderWithMaxKuraVersion(this.otherBuilder, "maxKuraVersion");
        givenBuilderWithCurrentKuraVersion(this.otherBuilder, "currentKuraVersion");
        givenBuilderWithCompatibility(this.otherBuilder, true);

        whenBuildIsCalledForTheOtherBuilder();

        thenResultingDescriptorsAreEqual(this.descriptor, this.otherDescriptor, false);
        thenResultingHashesAreEqual(this.descriptor, this.otherDescriptor, false);
    }

    /*
     * GIVEN
     */
    private void givenAMarketplacePackageDescriptorBuilder() {
        this.builder = MarketplacePackageDescriptor.builder();
    }

    private void givenAnotherMarketplacePackageDescriptorBuilder() {
        this.otherBuilder = MarketplacePackageDescriptor.builder();
    }

    private void givenBuilderWithCompatibility(MarketplacePackageDescriptorBuilder passedBuilder,
            boolean compatibility) {
        passedBuilder.isCompatible(compatibility);
    }

    private void givenBuilderWithCurrentKuraVersion(MarketplacePackageDescriptorBuilder passedBuilder,
            String currentKuraVersion) {
        passedBuilder.currentKuraVersion(currentKuraVersion);
    }

    private void givenBuilderWithMaxKuraVersion(MarketplacePackageDescriptorBuilder passedBuilder,
            String maxKuraVersion) {
        passedBuilder.maxKuraVersion(maxKuraVersion);
    }

    private void givenBuilderWithMinKuraVersion(MarketplacePackageDescriptorBuilder passedBuilder,
            String minKuraVersion) {
        passedBuilder.minKuraVersion(minKuraVersion);
    }

    private void givenBuilderWithDpUrl(MarketplacePackageDescriptorBuilder passedBuilder, String dpUrl) {
        passedBuilder.dpUrl(dpUrl);
    }

    private void givenBuilderWithUrl(MarketplacePackageDescriptorBuilder passedBuilder, String url) {
        passedBuilder.url(url);
    }

    private void givenBuilderWithNodeId(MarketplacePackageDescriptorBuilder passedBuilder, String nodeId) {
        passedBuilder.nodeId(nodeId);
    }

    /*
     * WHEN
     */
    private void whenBuildIsCalledForThisBuilder() {
        this.descriptor = this.builder.build();
    }

    private void whenBuildIsCalledForTheOtherBuilder() {
        this.otherDescriptor = this.otherBuilder.build();
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
