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

import static org.eclipse.kura.internal.wire.asset.WireAssetConstants.LISTEN_PROP_NAME;

import org.eclipse.kura.asset.provider.BaseChannelDescriptor;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tscalar;

public class WireAssetChannelDescriptor extends BaseChannelDescriptor {

    private static final WireAssetChannelDescriptor instance = new WireAssetChannelDescriptor();

    protected WireAssetChannelDescriptor() {
        super();

        final Tad listen = new Tad();
        listen.setName(LISTEN_PROP_NAME.value().substring(1));
        listen.setId(LISTEN_PROP_NAME.value());
        listen.setDescription("Specifies if WireAsset should emit envelopes on Channel events");
        listen.setType(Tscalar.BOOLEAN);
        listen.setRequired(true);
        listen.setDefault("false");

        this.defaultElements.add(listen);

    }

    public static WireAssetChannelDescriptor get() {
        return instance;
    }

}
