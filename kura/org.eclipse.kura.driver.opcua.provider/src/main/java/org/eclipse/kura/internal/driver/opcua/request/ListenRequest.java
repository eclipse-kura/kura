/**
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 */

package org.eclipse.kura.internal.driver.opcua.request;

import java.util.Map;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.internal.driver.opcua.OpcUaChannelDescriptor;
import org.eclipse.kura.internal.driver.opcua.Utils;
import org.eclipse.kura.type.DataType;

public class ListenRequest extends Request<ListenParams> {

    private final ChannelListener listener;

    public ListenRequest(final ListenParams params, final ChannelRecord record, final ChannelListener listener) {
        super(params, record);
        this.listener = listener;
    }

    public static ListenRequest extractListenRequest(final Map<String, Object> channelConfig,
            final ChannelListener listener) {
        final String channelName = Utils.tryExtract(channelConfig, config -> (String) config.get("+name"),
                "Error while retrieving Channel Name");
        final DataType valueType = Utils.tryExtract(channelConfig,
                config -> DataType.valueOf((String) config.get("+value.type")), "Error while retrieving value type");

        final boolean subscribeToChildren = Utils.tryExtract(channelConfig,
                OpcUaChannelDescriptor::getSubscribeToChildren,
                "Error while retrieving Subscribe to Children property");

        final SingleNodeListenParams params;

        if (subscribeToChildren) {

            if (valueType != DataType.STRING) {
                throw new IllegalArgumentException("Only String is supported as value type for subtree subscriptions");
            }

            params = new TreeListenParams(channelConfig);
        } else {
            params = new SingleNodeListenParams(channelConfig);
        }

        return new ListenRequest(params, ChannelRecord.createReadRecord(channelName, valueType), listener);
    }

    public ChannelListener getChannelListener() {
        return this.listener;
    }
}
