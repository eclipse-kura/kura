/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server;

import static java.lang.String.format;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.server.util.ServiceLocator.ServiceConsumer;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtChannelRecord;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtAssetService;

public class GwtAssetServiceImpl extends OsgiRemoteServiceServlet implements GwtAssetService {

    private static final long serialVersionUID = 8627173534436639487L;

    private static final Decoder BASE64_DECODER = Base64.getDecoder();
    private static final Encoder BASE64_ENCODER = Base64.getEncoder();

    @Override
    public List<GwtChannelRecord> read(GwtXSRFToken xsrfToken, String assetPid, final Set<String> channelNames)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        List<GwtChannelRecord> result = new ArrayList<>();
        final String filter = format("(%s=%s)", KURA_SERVICE_PID, assetPid);
        ServiceLocator.withAllServices(Asset.class, filter, new ServiceConsumer<Asset>() {

            @Override
            public void consume(final Asset asset) throws Exception {
                List<ChannelRecord> assetData = asset.read(channelNames);
                for (ChannelRecord channelRecord : assetData) {
                    GwtChannelRecord channelData = new GwtChannelRecord();
                    channelData.setName(channelRecord.getChannelName());
                    channelData.setValue(typedValueToString(channelRecord.getValue()));
                    result.add(channelData);
                }
            }
        });
        return result;
    }

    @Override
    public List<GwtChannelRecord> readAllChannels(GwtXSRFToken xsrfToken, String assetPid) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        List<GwtChannelRecord> result = new ArrayList<>();
        final String filter = format("(%s=%s)", KURA_SERVICE_PID, assetPid);
        ServiceLocator.withAllServices(Asset.class, filter, new ServiceConsumer<Asset>() {

            @Override
            public void consume(final Asset asset) throws Exception {
                List<ChannelRecord> assetData = asset.readAllChannels();
                for (ChannelRecord channelRecord : assetData) {
                    GwtChannelRecord channelData = new GwtChannelRecord();
                    channelData.setName(channelRecord.getChannelName());
                    channelData.setValueType(channelRecord.getValueType().name());
                    if (ChannelFlag.SUCCESS.equals(channelRecord.getChannelStatus().getChannelFlag())) {
                        channelData.setValue(typedValueToString(channelRecord.getValue()));
                    }
                    result.add(channelData);
                }
            }
        });
        return result;
    }

    @Override
    public void write(GwtXSRFToken xsrfToken, String assetPid, List<GwtChannelRecord> gwtChannelRecords)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        List<ChannelRecord> channelRecords = new ArrayList<>();

        for (GwtChannelRecord gwtChannelData : gwtChannelRecords) {
            String channelName = gwtChannelData.getName();
            String typedValue = gwtChannelData.getValueType();
            String value = gwtChannelData.getValue();
            try {
                channelRecords.add(ChannelRecord.createWriteRecord(channelName, parseTypedValue(value, typedValue)));
            } catch (IllegalArgumentException e) {
                throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
            }
        }

        final String filter = format("(%s=%s)", KURA_SERVICE_PID, assetPid);
        ServiceLocator.withAllServices(Asset.class, filter, new ServiceConsumer<Asset>() {

            @Override
            public void consume(final Asset asset) throws Exception {
                asset.write(channelRecords);
            }
        });
    }

    private static TypedValue<?> parseTypedValue(final String userValue, final String userType) {
        final DataType dataType = DataType.getDataType(userType);

        if (DataType.INTEGER == dataType) {
            return TypedValues.newIntegerValue(Integer.parseInt(userValue));
        }
        if (DataType.BOOLEAN == dataType) {
            return TypedValues.newBooleanValue(Boolean.parseBoolean(userValue));
        }
        if (DataType.FLOAT == dataType) {
            return TypedValues.newFloatValue(Float.parseFloat(userValue));
        }
        if (DataType.DOUBLE == dataType) {
            return TypedValues.newDoubleValue(Double.parseDouble(userValue));
        }
        if (DataType.LONG == dataType) {
            return TypedValues.newLongValue(Long.parseLong(userValue));
        }
        if (DataType.STRING == dataType) {
            return TypedValues.newStringValue(userValue);
        }
        if (DataType.BYTE_ARRAY == dataType) {
            return TypedValues.newByteArrayValue(BASE64_DECODER.decode(userValue));
        }

        throw new IllegalArgumentException();
    }

    private String typedValueToString(TypedValue<?> typedValue) {
        if (typedValue.getType() == DataType.BYTE_ARRAY) {
            return BASE64_ENCODER.encodeToString((byte[]) typedValue.getValue());
        }
        return typedValue.getValue().toString();
    }
}