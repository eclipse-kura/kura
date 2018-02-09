/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.wire.asset;

import java.util.List;

import org.eclipse.kura.asset.provider.BaseAssetOCD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;

public class WireAssetOCD extends BaseAssetOCD {

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private static void addOptions(Tad target, Enum<?>[] values) {
        final List<Option> options = target.getOption();
        for (Enum<?> value : values) {
            final String name = value.name();
            final Toption option = new Toption();
            option.setLabel(name);
            option.setValue(name);
            options.add(option);
        }
    }

    public WireAssetOCD() {
        super();

        final Tad emitAllChannelsAd = new Tad();
        emitAllChannelsAd.setId(WireAssetOptions.EMIT_ALL_CHANNELS_PROP_NAME);
        emitAllChannelsAd.setName(WireAssetOptions.EMIT_ALL_CHANNELS_PROP_NAME);
        emitAllChannelsAd.setCardinality(0);
        emitAllChannelsAd.setType(Tscalar.BOOLEAN);
        emitAllChannelsAd.setDescription(message.emitAllChannelsDescription());
        emitAllChannelsAd.setRequired(true);
        emitAllChannelsAd.setDefault("false");

        addAD(emitAllChannelsAd);

        final Tad timestampModeAd = new Tad();
        timestampModeAd.setId(WireAssetOptions.TIMESTAMP_MODE_PROP_NAME);
        timestampModeAd.setName(WireAssetOptions.TIMESTAMP_MODE_PROP_NAME);
        timestampModeAd.setCardinality(0);
        timestampModeAd.setType(Tscalar.STRING);
        timestampModeAd.setDescription(message.timestampModeDescription());
        timestampModeAd.setRequired(true);
        timestampModeAd.setDefault(TimestampMode.PER_CHANNEL.name());

        addOptions(timestampModeAd, TimestampMode.values());

        addAD(timestampModeAd);

        final Tad emitErrorsAd = new Tad();
        emitErrorsAd.setId(WireAssetOptions.EMIT_ERRORS_PROP_NAME);
        emitErrorsAd.setName(WireAssetOptions.EMIT_ERRORS_PROP_NAME);
        emitErrorsAd.setCardinality(0);
        emitErrorsAd.setType(Tscalar.BOOLEAN);
        emitErrorsAd.setDescription(message.emitErrorsDescription());
        emitErrorsAd.setRequired(true);
        emitErrorsAd.setDefault("false");

        addAD(emitErrorsAd);

    }

}
