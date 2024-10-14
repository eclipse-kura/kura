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

package org.eclipse.kura.internal.asset.provider.helper;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.asset.provider.AssetConstants;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ScaleOffsetType;
import org.eclipse.kura.type.DataType;

public class ChannelRecordHelper {

    private ChannelRecordHelper() {
        // Private constructor to prevent instantiation
    }

    public static ChannelRecord createModifiedChannelRecord(Channel channel) {

        DataType actualDataType = channel.getScaleOffsetType() == ScaleOffsetType.DEFINED_BY_VALUE_TYPE
                ? channel.getValueType()
                : toDataType(channel.getScaleOffsetType());

        ChannelRecord channelRecord = ChannelRecord.createReadRecord(channel.getName(), actualDataType,
                channel.getUnit());

        Map<String, Object> configMap = new HashMap<>(channel.getConfiguration());
        configMap.put(AssetConstants.VALUE_TYPE.value(), actualDataType.name());
        channelRecord.setChannelConfig(configMap);

        return channelRecord;
    }

    private static DataType toDataType(ScaleOffsetType scaleOffsetType) {
        switch (scaleOffsetType) {
        case DOUBLE:
            return DataType.DOUBLE;
        case LONG:
            return DataType.LONG;
        default:
            throw new IllegalArgumentException("Unsupported scale offset type: " + scaleOffsetType);
        }
    }
}
