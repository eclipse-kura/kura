/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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

import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.junit.Test;

public class AssetErrorTest extends WireAssetTestBase {

    @Test
    public void shouldNotEmitAssetErrorIfNotEnabled() {
        givenAssetConfig(map("emit.errors", false, "emit.connection.errors", true));
        givenConnectionException(new ConnectionException());

        whenAssetReceivesEnvelope();

        thenTotalEmittedEnvelopeCountAfter1SecIs(0);
    }

    @Test
    public void shouldNotEmitAssetErrorIfEmitErrorsIsNotEnabled() {
        givenAssetConfig(map("emit.errors", false, "emit.connection.errors", true));
        givenConnectionException(new ConnectionException());

        whenAssetReceivesEnvelope();

        thenTotalEmittedEnvelopeCountAfter1SecIs(0);
    }

    @Test
    public void shouldNotEmitAssetErrorIfEnabledButEmitErrorsIsNotEnabled() {
        givenAssetConfig(map("emit.errors", true, "emit.connection.errors", false));
        givenConnectionException(new ConnectionException());

        whenAssetReceivesEnvelope();

        thenTotalEmittedEnvelopeCountAfter1SecIs(0);
    }

    @Test
    public void shouldEmitErrorEnvelopeProperty() {
        givenAssetConfig(map("emit.errors", true, "emit.connection.errors", true));
        givenConnectionException(new ConnectionException("exception message"));

        whenAssetReceivesEnvelope();

        thenTotalEmittedEnvelopeCountAfter1SecIs(1);
        thenAssetOutputContains(0, "assetError", "exception message");
        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputContainsAssetTimestamp();
    }

    @Test
    public void shouldTolerateNullMessage() {
        givenAssetConfig(map("emit.errors", true, "emit.connection.errors", true));
        givenConnectionException(new ConnectionException((String) null));

        whenAssetReceivesEnvelope();

        thenTotalEmittedEnvelopeCountAfter1SecIs(1);
        thenAssetOutputContains(0, "assetError", "ConnectionException");
        thenAssetOutputContainsAssetTimestamp();
    }

    @Test
    public void shouldNotEmitTimestampIfDisabled() {
        givenAssetConfig(map("emit.errors", true, "emit.connection.errors", true, "timestamp.mode", "NO_TIMESTAMPS"));
        givenConnectionException(new ConnectionException("exception message"));

        whenAssetReceivesEnvelope();

        thenTotalEmittedEnvelopeCountAfter1SecIs(1);
        thenAssetOutputContains(0, "assetError", "exception message");
        thenAssetOutputContains(0, "assetName", "testAsset");
        thenAssetOutputDoesNotContain(0, "assetTimestamp");
    }

    @Override
    protected void givenAssetConfig(Map<String, Object> assetConfig) {

        assetConfig.put("foo#+name", "foo");
        assetConfig.put("foo#+type", ChannelType.READ.name());
        assetConfig.put("foo#+value.type", DataType.STRING.name());

        super.givenAssetConfig(assetConfig);
    }

    private void thenAssetOutputContainsAssetTimestamp() {
        final WireEnvelope envelope = awaitEnvelope(0);

        final long timestamp = Optional.ofNullable(envelope.getRecords().get(0).getProperties())
                .map(p -> p.get("assetTimestamp")).filter(LongValue.class::isInstance).map(LongValue.class::cast)
                .map(TypedValue::getValue)
                .orElseThrow(() -> new IllegalStateException("asset timestamp property not found"));

        assertTrue(Math.abs(timestamp - System.currentTimeMillis()) < 30000);
    }

}
