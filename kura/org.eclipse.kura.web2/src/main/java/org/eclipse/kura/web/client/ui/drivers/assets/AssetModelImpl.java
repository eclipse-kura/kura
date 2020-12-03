/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.drivers.assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.kura.web.client.util.LabelComparator;
import org.eclipse.kura.web.client.util.ValidationUtil;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;

public class AssetModelImpl implements AssetModel {

    public static final LabelComparator<LegacyChannelModel> CHANNEL_LABEL_COMPARATOR = new LabelComparator<>();

    private final GwtConfigComponent assetConfiguration;
    private final GwtConfigComponent channelDescriptor;

    private Set<String> channelNames = new HashSet<>();
    private final Map<String, Integer> paramIndexes = new HashMap<>();
    private final List<ChannelModel> channelModels = new ArrayList<>();
    private final List<GwtConfigParameter> extraParameters = new ArrayList<>();

    public AssetModelImpl(GwtConfigComponent assetConfiguration, GwtConfigComponent channelDescriptor) {
        this.assetConfiguration = assetConfiguration;

        this.channelDescriptor = channelDescriptor;
        int i = 0;
        for (final GwtConfigParameter param : channelDescriptor.getParameters()) {
            this.paramIndexes.put(param.getId(), i);
            i++;
        }

        probeChannels();
        loadChannelModels();
    }

    public AssetModelImpl(GwtConfigComponent assetConfiguration, GwtConfigComponent channelDescriptor,
            GwtConfigComponent baseChannelDescriptor) {
        this(assetConfiguration, concat(baseChannelDescriptor, channelDescriptor));
    }

    private static GwtConfigComponent concat(final GwtConfigComponent first, final GwtConfigComponent second) {
        final GwtConfigComponent result = new GwtConfigComponent();

        for (final GwtConfigParameter c : first.getParameters()) {
            result.getParameters().add(c);
        }

        for (final GwtConfigParameter c : second.getParameters()) {
            result.getParameters().add(c);
        }

        return result;
    }

    private String getChannelName(String propertyName) {
        int separatorIndex = propertyName.indexOf(AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value());
        if (separatorIndex != -1) {
            return propertyName.substring(0, separatorIndex);
        }
        return null;
    }

    private String getChannelPropertyName(String propertyName) {
        int separatorIndex = propertyName.indexOf(AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value());
        if (separatorIndex != -1) {
            return propertyName.substring(separatorIndex + 1);
        }
        return null;
    }

    private void probeChannels() {
        final Set<String> result = new HashSet<>();
        for (GwtConfigParameter param : this.assetConfiguration.getParameters()) {
            final String name = getChannelName(param.getId());
            if (name != null) {
                result.add(name);
            } else {
                extraParameters.add(param);
            }
        }
        this.channelNames = result;
    }

    private void loadChannelModels() {

        final HashMap<String, Integer> channelIndexes = new HashMap<>();
        int i = 0;
        for (GwtConfigParameter param : this.channelDescriptor.getParameters()) {
            channelIndexes.put(param.getId(), i);
            i++;
        }

        final HashMap<String, LegacyChannelModel> models = new HashMap<>();

        for (GwtConfigParameter param : this.assetConfiguration.getParameters()) {
            final String channelName = getChannelName(param.getId());
            final String propertyName = getChannelPropertyName(param.getId());
            if (channelName == null || propertyName == null) {
                continue;
            }

            final int index = i;
            LegacyChannelModel model = models.computeIfAbsent(channelName, name -> new LegacyChannelModel(name, index));
            model.parameters[channelIndexes.get(propertyName)] = param;
        }
        ArrayList<Entry<String, LegacyChannelModel>> sortedModels = new ArrayList<>(models.entrySet());
        Collections.sort(sortedModels, CHANNEL_LABEL_COMPARATOR);
        List<LegacyChannelModel> sortedLegacyChannelModels = new ArrayList<>();
        for (Entry<String, LegacyChannelModel> entry : sortedModels) {
            sortedLegacyChannelModels.add(entry.getValue());
        }

        this.channelModels.clear();
        this.channelModels.addAll(sortedLegacyChannelModels);
    }

    @Override
    public List<ChannelModel> getChannels() {
        return this.channelModels;
    }

    @Override
    public ChannelModel createNewChannel(String channelName) {
        final LegacyChannelModel result = new LegacyChannelModel(channelName,
                this.channelDescriptor.getParameters().size());
        int i = 0;
        for (GwtConfigParameter param : this.channelDescriptor.getParameters()) {
            final GwtConfigParameter cloned = new GwtConfigParameter(param);
            final String paramId = channelName + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value() + param.getId();
            cloned.setId(paramId);
            cloned.setName(paramId);
            cloned.setValue(cloned.getDefault());
            this.assetConfiguration.getParameters().add(cloned);
            result.parameters[i] = cloned;
            i++;
        }
        result.setValue(AssetConstants.NAME.value(), channelName);
        this.channelNames.add(channelName);
        this.channelModels.add(result);
        return result;
    }

    @Override
    public Set<String> getChannelNames() {
        return this.channelNames;
    }

    @Override
    public GwtConfigComponent getChannelDescriptor() {
        return this.channelDescriptor;
    }

    @Override
    public void deleteChannel(String channelName) {
        this.channelNames.remove(channelName);
        final Iterator<ChannelModel> iter = this.channelModels.iterator();
        while (iter.hasNext()) {
            final ChannelModel model = iter.next();
            if (model.getChannelName().equals(channelName)) {
                iter.remove();
                ((LegacyChannelModel) model).remove();
                return;
            }
        }
    }

    private class LegacyChannelModel implements AssetModel.ChannelModel {

        String channelName;
        GwtConfigParameter[] parameters;

        public LegacyChannelModel(String channelName, int parameterCount) {
            this.channelName = channelName;
            this.parameters = new GwtConfigParameter[parameterCount];
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
        public void setValue(String id, String value) {
            final Integer index = AssetModelImpl.this.paramIndexes.get(id);
            if (index == null) {
                return;
            }
            this.parameters[index].setValue(value);
        }

        @Override
        public boolean isValid(final String id) {
            final Integer index = AssetModelImpl.this.paramIndexes.get(id);
            if (index == null) {
                return false;
            }
            final GwtConfigParameter param = getParameter(index);

            return ValidationUtil.validateParameter(param, param.getValue());
        }

        @Override
        public String getValue(String id) {
            final Integer index = AssetModelImpl.this.paramIndexes.get(id);
            if (index == null) {
                return null;
            }
            return this.parameters[index].getValue();
        }

        private void remove() {
            final Iterator<GwtConfigParameter> iterator = AssetModelImpl.this.assetConfiguration.getParameters()
                    .iterator();
            while (iterator.hasNext()) {
                final GwtConfigParameter param = iterator.next();
                for (GwtConfigParameter parameter : this.parameters) {
                    if (parameter == param) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    @Override
    public GwtConfigComponent getConfiguration() {
        return this.assetConfiguration;
    }

    @Override
    public String getAssetPid() {
        return this.assetConfiguration.getComponentId();
    }

    @Override
    public boolean isValid() {
        for (final ChannelModel model : this.channelModels) {
            for (final String param : paramIndexes.keySet()) {
                if (!model.isValid(param)) {
                    return false;
                }
            }
        }

        for (final GwtConfigParameter extraParam : extraParameters) {

            if (!ValidationUtil.validateParameter(extraParam, extraParam.getValue())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void addAllChannels(final AssetModel other) {
        for (final ChannelModel model : other.getChannels()) {
            final ChannelModel channel = channelModels.stream()
                    .filter(c -> c.getChannelName().contentEquals(model.getChannelName())).findAny()
                    .orElseGet(() -> createNewChannel(model.getChannelName()));
            for (final String param : paramIndexes.keySet()) {
                channel.setValue(param, model.getValue(param));
            }
        }
    }

    @Override
    public void replaceChannels(final AssetModel other) {
        while (!channelNames.isEmpty()) {
            deleteChannel(channelNames.iterator().next());
        }
        addAllChannels(other);
    }

}
