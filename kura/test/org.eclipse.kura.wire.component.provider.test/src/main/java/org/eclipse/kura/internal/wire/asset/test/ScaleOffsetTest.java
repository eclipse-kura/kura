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

import java.util.Optional;

import org.eclipse.kura.channel.ScaleOffsetType;
import org.eclipse.kura.type.DataType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ScaleOffsetTest extends WireAssetTestBase {

    @Test
    public void shouldSupportMissingScaleOffset() {
        givenAssetChannel("foo", DataType.DOUBLE, ScaleOffsetType.DEFINED_BY_VALUE_TYPE, Optional.empty(),
                Optional.empty());

        whenDriverProducesValue("foo", 1.0d);

        thenAssetOutputContains(0, "foo", 1.0d);
    }

    @Test
    public void shouldApplyScaleToDouble() {
        givenAssetChannel("foo", DataType.DOUBLE, ScaleOffsetType.DOUBLE, Optional.of(3.0d), Optional.empty());

        whenDriverProducesValue("foo", 1.0d);

        thenAssetOutputContains(0, "foo", 3.0d);
    }

    @Test
    public void shouldApplyScaleToFloat() {
        givenAssetChannel("foo", DataType.FLOAT, ScaleOffsetType.DOUBLE, Optional.of(3.0d), Optional.empty());

        whenDriverProducesValue("foo", 1.0f);

        thenAssetOutputContains(0, "foo", 3.0f);
    }

    @Test
    public void shouldApplyScaleToInteger() {
        givenAssetChannel("foo", DataType.INTEGER, ScaleOffsetType.DOUBLE, Optional.of(3.0d), Optional.empty());

        whenDriverProducesValue("foo", 1);

        thenAssetOutputContains(0, "foo", 3);
    }

    @Test
    public void shouldApplyScaleToIntegerWithDoubleScale() {
        givenAssetChannel("foo", DataType.INTEGER, ScaleOffsetType.DOUBLE, Optional.of(3.0f), Optional.empty());

        whenDriverProducesValue("foo", 1);

        thenAssetOutputContains(0, "foo", 3);
    }

    @Test
    public void shouldApplyScaleToIntegerWithIntegerScale() {
        givenAssetChannel("foo", DataType.INTEGER, ScaleOffsetType.DEFINED_BY_VALUE_TYPE, Optional.of(3),
                Optional.empty());

        whenDriverProducesValue("foo", 1);

        thenAssetOutputContains(0, "foo", 3);
    }

    @Test
    public void shouldApplyScaleToLongWithDoubleScale() {
        givenAssetChannel("foo", DataType.LONG, ScaleOffsetType.DOUBLE, Optional.of(3.0d), Optional.empty());

        whenDriverProducesValue("foo", 1l);

        thenAssetOutputContains(0, "foo", 3l);
    }

    @Test
    public void shouldApplyScaleToLongWithLongScale() {
        givenAssetChannel("foo", DataType.LONG, ScaleOffsetType.LONG, Optional.of(3), Optional.empty());

        whenDriverProducesValue("foo", 40l);

        thenAssetOutputContains(0, "foo", 120l);
    }

    @Test
    public void shouldApplyOffsetToLongWithLongScale() {
        givenAssetChannel("foo", DataType.LONG, ScaleOffsetType.LONG, Optional.empty(), Optional.of(55));

        whenDriverProducesValue("foo", 40l);

        thenAssetOutputContains(0, "foo", 95l);
    }

    @Test
    public void shouldApplyBothScaleAndOffsetToLongWithLongScale() {
        givenAssetChannel("foo", DataType.LONG, ScaleOffsetType.LONG, Optional.of(2), Optional.of(55));

        whenDriverProducesValue("foo", 40l);

        thenAssetOutputContains(0, "foo", 135l);
    }

    @Test
    public void shouldApplyOffsetToDouble() {
        givenAssetChannel("foo", DataType.DOUBLE, ScaleOffsetType.DOUBLE, Optional.empty(), Optional.of(10.0d));

        whenDriverProducesValue("foo", 1.0d);

        thenAssetOutputContains(0, "foo", 11.0d);
    }

    @Test
    public void shouldApplyOffsetToFloat() {
        givenAssetChannel("foo", DataType.FLOAT, ScaleOffsetType.DOUBLE, Optional.empty(), Optional.of(-2.0d));

        whenDriverProducesValue("foo", 1.0f);

        thenAssetOutputContains(0, "foo", -1.0f);
    }

    @Test
    public void shouldApplyOffsetToIngeger() {
        givenAssetChannel("foo", DataType.INTEGER, ScaleOffsetType.DOUBLE, Optional.empty(), Optional.of(10.0d));

        whenDriverProducesValue("foo", 1);

        thenAssetOutputContains(0, "foo", 11);
    }

    @Test
    public void shouldApplyOffsetToLong() {
        givenAssetChannel("foo", DataType.LONG, ScaleOffsetType.DOUBLE, Optional.empty(), Optional.of(-2.0d));

        whenDriverProducesValue("foo", 1l);

        thenAssetOutputContains(0, "foo", -1l);
    }

    @Test
    public void shouldApplyBothScaleAndOffset() {
        givenAssetChannel("foo", DataType.LONG, ScaleOffsetType.DOUBLE, Optional.of(6.0f), Optional.of(-2.0d));

        whenDriverProducesValue("foo", 2l);

        thenAssetOutputContains(0, "foo", 10l);
    }

    @Test
    public void shouldTolerateScaleAndOffsetOnBoolean() {
        givenAssetChannel("foo", DataType.BOOLEAN, ScaleOffsetType.DOUBLE, Optional.of(6.0f), Optional.of(-2.0d));

        whenDriverProducesValue("foo", true);

        thenAssetOutputContains(0, "foo", true);
    }

    @Test
    public void shouldTolerateScaleAndOffsetOnString() {
        givenAssetChannel("foo", DataType.STRING, ScaleOffsetType.DOUBLE, Optional.of(6.0f), Optional.of(-2.0d));

        whenDriverProducesValue("foo", "bar");

        thenAssetOutputContains(0, "foo", "bar");
    }

    @Test
    public void shouldTolerateScaleAndOffsetOnByteArray() {
        givenAssetChannel("foo", DataType.BYTE_ARRAY, ScaleOffsetType.DOUBLE, Optional.of(6.0f), Optional.of(-2.0d));

        whenDriverProducesValue("foo", new byte[] { 1, 2, 3, 4 });

        thenAssetOutputContains(0, "foo", new byte[] { 1, 2, 3, 4 });
    }

    @Parameterized.Parameters(name = "{0}")
    public static TriggerMode[] parameters() {
        return TriggerMode.values();
    }

    private enum TriggerMode {
        READ,
        LISTEN
    }

    private final TriggerMode triggerMode;

    public ScaleOffsetTest(final TriggerMode triggerMode) {
        this.triggerMode = triggerMode;
    }

    private void givenAssetChannel(String name, DataType dataType, ScaleOffsetType scaleOffsetType,
            Optional<? extends Number> scale, Optional<? extends Number> offset) {
        super.givenAssetChannel(name, this.triggerMode == TriggerMode.LISTEN, dataType, scaleOffsetType, scale, offset);
    }

    private void whenDriverProducesValue(final String channelName, final Object value) {
        if (this.triggerMode == TriggerMode.READ) {
            givenChannelValues(channelName, value);
            whenAssetReceivesEnvelopes(1);
        } else {
            whenDriverEmitsEvents(channelName, value);
        }
    }
}
