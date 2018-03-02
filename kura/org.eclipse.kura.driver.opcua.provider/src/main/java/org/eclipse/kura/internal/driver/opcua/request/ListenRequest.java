/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.kura.internal.driver.opcua.request;

import java.util.Map;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.internal.driver.opcua.Utils;
import org.eclipse.kura.type.DataType;

public class ListenRequest extends Request<ListenParams> {

    private final ChannelListener listener;

    protected ListenRequest(final ListenParams params, final ChannelRecord record, final ChannelListener listener) {
        super(params, record);
        this.listener = listener;
    }

    public static ListenRequest extractListenRequest(final Map<String, Object> channelConfig,
            final ChannelListener listener) {
        final String channelName = Utils.tryExtract(channelConfig, config -> (String) config.get("+name"),
                "Error while retrieving Channel Name");
        final DataType valueType = Utils.tryExtract(channelConfig,
                config -> DataType.valueOf((String) config.get("+value.type")), "Error while retrieving value type");

        final ListenParams params = new ListenParams(channelConfig);
        return new ListenRequest(params, ChannelRecord.createReadRecord(channelName, valueType), listener);
    }

    public ChannelListener getChannelListener() {
        return listener;
    }
}
