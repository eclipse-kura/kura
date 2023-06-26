/*******************************************************************************
 * Copyright (c) 2022, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.asset.test;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.type.DataType;
import org.junit.Test;

public class OnChangeCacheTest extends WireAssetTestBase {

    @Test
    public void shouldAywaysEmitAllValuesOnReadIfOnChangeIsDisalbed() {
        givenAssetWithChangeCacheDisabled();
        givenChannelValues("foo", "bar", "bar");
        givenChannelValues("bar", 15, 15);

        whenAssetReceivesEnvelopes(2);

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");
        thenAssetOutputContains(0, "bar", 15);
        thenAssetOutputContainsKey(0, "bar_timestamp");

        thenAssetOutputContains(1, "assetName", "testAsset");
        thenAssetOutputContains(1, "foo", "bar");
        thenAssetOutputContainsKey(1, "foo_timestamp");
        thenAssetOutputContains(1, "bar", 15);
        thenAssetOutputContainsKey(1, "bar_timestamp");
    }

    @Test
    public void shouldAywaysEmitEventsIfOnChangeIsDisalbed() {
        givenAssetWithChangeCacheDisabled();

        whenDriverEmitsEvents(
                "foo", "bar", //
                "bar", 15, //
                "foo", "bar", //
                "bar", 15 //
        );

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");

        thenAssetOutputContains(1, "assetName", "testAsset");
        thenAssetOutputContains(1, "bar", 15);
        thenAssetOutputContainsKey(1, "bar_timestamp");

        thenAssetOutputContains(2, "assetName", "testAsset");
        thenAssetOutputContains(2, "foo", "bar");
        thenAssetOutputContainsKey(2, "foo_timestamp");

        thenAssetOutputContains(3, "assetName", "testAsset");
        thenAssetOutputContains(3, "bar", 15);
        thenAssetOutputContainsKey(3, "bar_timestamp");

    }

    @Test
    public void shouldNotEmitSameValuesAgainOnReadIfNotChanged() {
        givenAssetWithChangeCacheEnabledAndEmitEmptyEnvelopesSetTo(false);
        givenChannelValues("foo", "bar", "bar");
        givenChannelValues("bar", 15, 15);

        whenAssetReceivesEnvelopes(2);

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");
        thenAssetOutputContains(0, "bar", 15);
        thenAssetOutputContainsKey(0, "bar_timestamp");

        thenTotalEmittedEnvelopeCountAfter1SecIs(1);
    }

    @Test
    public void shouldNotEmitSameValuesAgainOnChannelEventIfNotChanged() {
        givenAssetWithChangeCacheEnabledAndEmitEmptyEnvelopesSetTo(false);

        whenDriverEmitsEvents(
                "foo", "bar", //
                "bar", 15, //
                "foo", "bar", //
                "bar", 15 //
        );

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");

        thenAssetOutputContains(1, "assetName", "testAsset");
        thenAssetOutputContains(1, "bar", 15);
        thenAssetOutputContainsKey(1, "bar_timestamp");

        thenTotalEmittedEnvelopeCountAfter1SecIs(2);
    }

    @Test
    public void shouldEmitOnlyChangedValuesOnRead() {
        givenAssetWithChangeCacheEnabledAndEmitEmptyEnvelopesSetTo(false);
        givenChannelValues("foo", "bar", "bar");
        givenChannelValues("bar", 15, 16);

        whenAssetReceivesEnvelopes(2);

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");
        thenAssetOutputContains(0, "bar", 15);
        thenAssetOutputContainsKey(0, "bar_timestamp");

        thenAssetOutputContains(1, "assetName", "testAsset");
        thenAssetOutputDoesNotContain(1, "foo");
        thenAssetOutputDoesNotContain(1, "foo_timestamp");
        thenAssetOutputContains(1, "bar", 16);
        thenAssetOutputContainsKey(1, "bar_timestamp");

    }

    @Test
    public void shouldNotEmitOnlyChangedValuesOnChannelEvent() {
        givenAssetWithChangeCacheEnabledAndEmitEmptyEnvelopesSetTo(false);

        whenDriverEmitsEvents(
                "foo", "bar", //
                "bar", 15, //
                "foo", "bar", //
                "bar", 15, //
                "foo", "baz", //
                "bar", 16, //
                "foo", "baz", //
                "bar", 16 //
        );

        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContains(0, "foo", "bar");
        thenAssetOutputContainsKey(0, "foo_timestamp");

        thenAssetOutputContains(1, "assetName", "testAsset");
        thenAssetOutputContains(1, "bar", 15);
        thenAssetOutputContainsKey(1, "bar_timestamp");

        thenAssetOutputContains(2, "assetName", "testAsset");
        thenAssetOutputContains(2, "foo", "baz");
        thenAssetOutputContainsKey(2, "foo_timestamp");

        thenAssetOutputContains(3, "assetName", "testAsset");
        thenAssetOutputContains(3, "bar", 16);
        thenAssetOutputContainsKey(3, "bar_timestamp");

        thenTotalEmittedEnvelopeCountAfter1SecIs(4);
    }

    protected void givenAssetWithChangeCacheDisabled() {
        givenAssetConfig(testAssetConfig());
    }

    protected void givenAssetWithChangeCacheEnabledAndEmitEmptyEnvelopesSetTo(final boolean emitEmptyEnvelopes) {
        final Map<String, Object> assetConfig = new HashMap<>(testAssetConfig());
        assetConfig.put("emit.on.change", true);
        assetConfig.put("emit.empty.envelopes", emitEmptyEnvelopes);

        givenAssetConfig(assetConfig);
    }

    protected Map<String, Object> testAssetConfig() {
        final Map<String, Object> result = new HashMap<>();

        result.put("driver.pid", "testDriver");
        result.put("foo#+name", "foo");
        result.put("foo#+type", ChannelType.READ.name());
        result.put("foo#+value.type", DataType.STRING.name());
        result.put("foo#+enabled", true);
        result.put("foo#+listen", true);
        result.put("bar#+name", "bar");
        result.put("bar#+type", ChannelType.READ.name());
        result.put("bar#+value.type", DataType.INTEGER.name());
        result.put("bar#+enabled", true);
        result.put("bar#+listen", true);

        return result;
    }

}
