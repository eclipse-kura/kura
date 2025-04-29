/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server.util;

import java.util.HashMap;

import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.internal.wire.asset.WireAssetChannelDescriptor;
import org.eclipse.kura.internal.wire.asset.WireAssetOCD;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;

public class GwtWireAssetConstants {

    public static final String WIRE_ASSET_PID = "org.eclipse.kura.wire.WireAsset";

    public static final GwtConfigComponent WIRE_ASSET_OCD = GwtServerUtil
            .toGwtConfigComponent(new ComponentConfigurationImpl(WIRE_ASSET_PID, new WireAssetOCD(), new HashMap<>()));

    public static final GwtConfigComponent WIRE_ASSET_CHANNEL_DESCRIPTOR = GwtServerUtil.toGwtConfigComponent(null,
            WireAssetChannelDescriptor.get().getDescriptor());

    private GwtWireAssetConstants() {
    }
}
