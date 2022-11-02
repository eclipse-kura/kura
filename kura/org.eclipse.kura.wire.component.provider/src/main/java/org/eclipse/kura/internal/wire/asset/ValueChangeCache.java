/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.asset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.type.TypedValue;

public class ValueChangeCache {

    private final Map<String, TypedValue<?>> cache = new HashMap<>();

    private boolean update(final ChannelRecord channelRecord) {

        final String key = channelRecord.getChannelName();

        if (channelRecord.getChannelStatus().getChannelFlag() != ChannelFlag.SUCCESS) {
            // Always process record in case of failure

            cache.remove(key);
            return true;
        }

        final TypedValue<?> value = channelRecord.getValue();

        if (Objects.equals(cache.get(key), value)) {
            return false;
        }

        cache.put(key, value);

        return true;

    }

    public synchronized List<ChannelRecord> filterRecords(final List<ChannelRecord> channelRecords) {

        return channelRecords.stream().filter(this::update).collect(Collectors.toList());
    }

}
