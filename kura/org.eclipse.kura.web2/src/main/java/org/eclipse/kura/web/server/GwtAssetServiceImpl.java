/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.server.util.ServiceLocator.ServiceConsumer;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtChannelOperationResult;
import org.eclipse.kura.web.shared.model.GwtChannelRecord;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtAssetService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtAssetServiceImpl extends OsgiRemoteServiceServlet implements GwtAssetService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private static final long serialVersionUID = 8627173534436639487L;

    private static final Decoder BASE64_DECODER = Base64.getDecoder();
    private static final Encoder BASE64_ENCODER = Base64.getEncoder();

    @Override
    public GwtChannelOperationResult readAllChannels(GwtXSRFToken xsrfToken, String assetPid) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        try {
            List<GwtChannelRecord> result = new ArrayList<>();

            withAsset(assetPid, asset -> {
                List<ChannelRecord> assetData = asset.readAllChannels();
                for (ChannelRecord channelRecord : assetData) {
                    result.add(toGwt(channelRecord));
                }
            });
            auditLogger.info("UI Asset - Success - Successfully read all channels for user: {}, session: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());

            return new GwtChannelOperationResult(result);
        } catch (Exception e) {
            auditLogger.warn("UI Asset - Failure - Failed to read all channels for user: {}, session: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            return getFailureResult(e);
        }
    }

    @Override
    public GwtConfigComponent getUploadedCsvConfig(final GwtXSRFToken xsrfToken, final String assetPid)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpSession session = getThreadLocalRequest().getSession(false);
        final String key = "kura.csv.config." + assetPid;

        final GwtConfigComponent result = (GwtConfigComponent) session.getAttribute(key);

        if (result == null) {
            throw new GwtKuraException("Uploaded configuration not available");
        }

        session.removeAttribute(key);

        return result;
    }

    @Override
    public GwtChannelOperationResult write(GwtXSRFToken xsrfToken, String assetPid,
            List<GwtChannelRecord> gwtChannelRecords) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        try {
            final Map<String, GwtChannelRecord> groupedRecords = new HashMap<>(gwtChannelRecords.size());

            for (final GwtChannelRecord record : gwtChannelRecords) {
                record.setUnescaped(true);
                groupedRecords.put(record.getName(), record);
            }

            final List<ChannelRecord> channelRecords = new ArrayList<>();

            for (GwtChannelRecord gwtChannelData : gwtChannelRecords) {
                String channelName = gwtChannelData.getName();
                String typedValue = gwtChannelData.getValueType();
                String value = gwtChannelData.getValue();
                try {
                    channelRecords
                            .add(ChannelRecord.createWriteRecord(channelName, parseTypedValue(value, typedValue)));
                } catch (Exception e) {
                    gwtChannelData.setValue(null);
                    gwtChannelData.setExceptionMessage(e.getMessage());
                    gwtChannelData.setExceptionStackTrace(e.getStackTrace());
                }
            }

            withAsset(assetPid, asset -> asset.write(channelRecords));

            for (final ChannelRecord record : channelRecords) {
                final GwtChannelRecord gwtChannelRecord = groupedRecords.get(record.getChannelName());
                final ChannelStatus status = record.getChannelStatus();

                if (status.getChannelFlag() == ChannelFlag.FAILURE) {
                    fillErrorData(status, gwtChannelRecord);
                }
            }

            auditLogger.info("UI Asset - Success - Successful write for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());

            return new GwtChannelOperationResult(gwtChannelRecords);
        } catch (Exception e) {
            auditLogger.warn("UI Asset - Failure - Write failure for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            return getFailureResult(e);
        }
    }

    private GwtChannelOperationResult getFailureResult(final Throwable e) {
        final Throwable rootCause = e.getCause();
        if (rootCause != null) {
            // e is likely a not so interesting KuraException thrown by BaseAsset.
            // Since the FailureHandler widget is not able to display the
            // stack traces of the whole exception stack, forward the root
            // exception generated by the Driver if available.
            // TODO modify FailureHandler to display the whole exception stack
            return new GwtChannelOperationResult(rootCause);
        }
        return new GwtChannelOperationResult(e);
    }

    private void withAsset(final String kuraServicePid, final ServiceConsumer<Asset> consumer) throws Exception {
        final BundleContext ctx = FrameworkUtil.getBundle(ServiceLocator.class).getBundleContext();

        final String filter = format("(%s=%s)", KURA_SERVICE_PID, kuraServicePid);
        final Collection<ServiceReference<Asset>> refs = ctx.getServiceReferences(Asset.class, filter);

        if (refs == null || refs.isEmpty()) {
            return;
        }

        final ServiceReference<Asset> assetRef = refs.iterator().next();

        try {
            consumer.consume(ctx.getService(assetRef));
        } finally {
            ctx.ungetService(assetRef);
        }
    }

    private static GwtChannelRecord toGwt(final ChannelRecord channelRecord) {
        GwtChannelRecord channelData = new GwtChannelRecord();
        channelData.setName(channelRecord.getChannelName());
        channelData.setValueType(channelRecord.getValueType().toString());

        final ChannelStatus status = channelRecord.getChannelStatus();

        if (ChannelFlag.SUCCESS.equals(status.getChannelFlag())) {
            channelData.setValue(typedValueToString(channelRecord.getValue()));
        } else {
            fillErrorData(status, channelData);
        }
        return channelData;
    }

    private static void fillErrorData(final ChannelStatus status, final GwtChannelRecord record) {
        record.setValue(null);

        final Exception exception = status.getException();

        final String userMessage = status.getExceptionMessage();
        final String exceptionMessage = exception != null ? exception.getMessage() : null;

        final StringBuilder exceptionMessageBuilder = new StringBuilder();

        if (userMessage != null) {
            exceptionMessageBuilder.append(userMessage);
        }

        if (exceptionMessage != null && !exceptionMessage.equals(userMessage)) {
            if (userMessage != null) {
                exceptionMessageBuilder.append(" - ");
            }
            exceptionMessageBuilder.append(exceptionMessage);
        }

        record.setExceptionMessage(exceptionMessageBuilder.toString());

        if (exception != null) {
            record.setExceptionStackTrace(exception.getStackTrace());
        }
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

    private static String typedValueToString(TypedValue<?> typedValue) {
        if (typedValue.getType() == DataType.BYTE_ARRAY) {
            return BASE64_ENCODER.encodeToString((byte[]) typedValue.getValue());
        }
        return typedValue.getValue().toString();
    }
}