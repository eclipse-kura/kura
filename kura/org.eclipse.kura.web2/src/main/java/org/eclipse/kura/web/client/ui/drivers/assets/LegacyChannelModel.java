/*******************************************************************************
 * Copyright (c) 2024, 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/

package org.eclipse.kura.web.client.ui.drivers.assets;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.kura.web.client.util.ValidationUtil;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.DataType;
import org.eclipse.kura.web.shared.ScaleOffsetType;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;

public class LegacyChannelModel implements AssetModel.ChannelModel {

    private final String channelName;
    private final GwtConfigParameter[] parameters;

    private GwtConfigParameterType subtype;

    public LegacyChannelModel(String channelName, GwtConfigParameter[] parameters) {
        this.channelName = channelName;
        this.parameters = parameters;

        // calculate the subtype for scale and offset parameters based on the scaleoffset.type and value type
        // properties.
        detectSubtype();

        setScaleOffsetSubtype();
    }

    private void setScaleOffsetSubtype() {
        for (GwtConfigParameter param : this.parameters) {
            if (param.getId().equals(getId(AssetConstants.VALUE_SCALE))
                    || param.getId().equals(getId(AssetConstants.VALUE_OFFSET))) {

                param.setSubtype(this.subtype);
            }
        }
    }

    private void updateScaleOffsetSubtypeForParameters() {
        for (GwtConfigParameter param : this.parameters) {
            if (param.getSubtype() != null && param.getSubtype() != this.subtype) {
                param.setSubtype(this.subtype);
            }
        }
    }

    private void detectSubtype() {

        GwtConfigParameterType subType = null;

        ScaleOffsetType scaleOffsetType = null;
        DataType valueType = null;

        for (GwtConfigParameter param : this.parameters) {
            if (param.getId().equals(getId(AssetConstants.SCALE_OFFSET_TYPE))) {
                String paramValue = param.getValue() != null ? param.getValue() : param.getDefault();
                scaleOffsetType = ScaleOffsetType.getScaleOffsetType(paramValue);
            }
            if (param.getId().equals(getId(AssetConstants.VALUE_TYPE))) {
                String paramValue = param.getValue() != null ? param.getValue() : param.getDefault();
                valueType = DataType.getDataType(paramValue);
            }
        }

        if (isScalarType(valueType) && scaleOffsetType != null && valueType != null) {
            subType = scaleOffsetType == ScaleOffsetType.DEFINED_BY_VALUE_TYPE
                    ? GwtConfigParameterType.valueOf(valueType.name())
                    : GwtConfigParameterType.valueOf(scaleOffsetType.name());
        }

        this.subtype = subType;
    }

    private boolean isScalarType(DataType valueType) {
        return valueType != null && valueType != DataType.BOOLEAN && valueType != DataType.STRING
                && valueType != DataType.BYTE_ARRAY;
    }

    private String getId(AssetConstants assetConstant) {
        return this.channelName + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value() + assetConstant.value();
    }

    @Override
    public String getChannelName() {
        return this.channelName;
    }

    @Override
    public GwtConfigParameter getParameter(int index) {
        return this.parameters[index];
    }

    @Override
    public void setValue(Integer index, String value) {
        if (index == null) {
            return;
        }
        GwtConfigParameter param = this.parameters[index];
        param.setValue(value);

        if (param.getId().equals(getId(AssetConstants.SCALE_OFFSET_TYPE))
                || param.getId().equals(getId(AssetConstants.VALUE_TYPE))) {

            detectSubtype();
            updateScaleOffsetSubtypeForParameters();
        }
    }

    @Override
    public boolean isValid(final Integer index) {
        if (index == null) {
            return false;
        }
        final GwtConfigParameter param = getParameter(index);

        return ValidationUtil.validateParameter(param, param.getValue());
    }

    @Override
    public String getValue(final Integer index) {
        if (index == null) {
            return null;
        }
        return this.parameters[index].getValue();
    }

    @Override
    public String toString() {
        return "LegacyChannelModel [channelName=" + this.channelName + ", parameters="
                + Arrays.toString(this.parameters) + "]";
    }

    public static LegacyChannelModelBuilder builder(String channelName, int parameterCount) {
        return new LegacyChannelModelBuilder(channelName, parameterCount);
    }

    public void removeParameters(List<GwtConfigParameter> parametersToRemove) {
        final Iterator<GwtConfigParameter> iterator = parametersToRemove.iterator();
        while (iterator.hasNext()) {
            final GwtConfigParameter param = iterator.next();
            for (GwtConfigParameter parameter : this.parameters) {
                if (parameter == param) {
                    iterator.remove();
                }
            }
        }
    }

    public static class LegacyChannelModelBuilder {

        private String channelName;
        private GwtConfigParameter[] parameters;

        public LegacyChannelModelBuilder(String channelName, int parameterCount) {
            this.channelName = channelName;
            this.parameters = new GwtConfigParameter[parameterCount];
        }

        public LegacyChannelModelBuilder addParameter(int index, GwtConfigParameter param) {
            this.parameters[index] = param;
            return this;
        }

        public LegacyChannelModel build() {
            return new LegacyChannelModel(this.channelName, this.parameters);
        }
    }

}
