/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;

public class AssetModelImpl implements AssetModel {

    public static final LabelComparator<LegacyChannelModel> CHANNEL_LABEL_COMPARATOR = new LabelComparator<>();

    private GwtConfigComponent assetConfiguration;
    private GwtConfigComponent channelDescriptor;

    private Set<String> channelNames = new HashSet<>();
    private Map<String, Integer> paramIndexes = new HashMap<>();
    private List<ChannelModel> channelModels = new ArrayList<>();

    public AssetModelImpl(GwtConfigComponent assetConfiguration, GwtConfigComponent channelDescriptor,
            GwtConfigComponent baseChannelDescriptor) {
        this.assetConfiguration = assetConfiguration;

        this.channelDescriptor = new GwtConfigComponent();
        int i = 0;
        for (final GwtConfigParameter param : baseChannelDescriptor.getParameters()) {
            this.channelDescriptor.getParameters().add(param);
            this.paramIndexes.put(param.getId(), i);
            i++;
        }
        for (final GwtConfigParameter param : channelDescriptor.getParameters()) {
            this.channelDescriptor.getParameters().add(param);
            this.paramIndexes.put(param.getId(), i);
            i++;
        }

        findChannelNames();
        loadChannelModels();
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

    private void findChannelNames() {
        final Set<String> result = new HashSet<>();
        for (GwtConfigParameter param : assetConfiguration.getParameters()) {
            final String name = getChannelName(param.getId());
            if (name != null && !result.contains(name)) {
                result.add(name);
            }
        }
        this.channelNames = result;
    }

    private void loadChannelModels() {

        final HashMap<String, Integer> channelIndexes = new HashMap<>();
        int i = 0;
        for (GwtConfigParameter param : channelDescriptor.getParameters()) {
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
        final LegacyChannelModel result = new LegacyChannelModel(channelName, channelDescriptor.getParameters().size());
        int i = 0;
        for (GwtConfigParameter param : channelDescriptor.getParameters()) {
            final GwtConfigParameter cloned = new GwtConfigParameter(param);
            final String paramId = channelName + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value() + param.getId();
            cloned.setId(paramId);
            cloned.setName(paramId);
            cloned.setValue(cloned.getDefault());
            assetConfiguration.getParameters().add(cloned);
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
        return channelDescriptor;
    }

    @Override
    public void deleteChannel(String channelName) {
        this.channelNames.remove(channelName);
        final Iterator<ChannelModel> iter = channelModels.iterator();
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
            return channelName;
        }

        @Override
        public GwtConfigParameter getParameter(int index) {
            return this.parameters[index];
        }

        @Override
        public void setValue(String id, String value) {
            final Integer index = paramIndexes.get(id);
            if (index == null) {
                return;
            }
            parameters[index].setValue(value);
        }

        @Override
        public String getValue(String id) {
            final Integer index = paramIndexes.get(id);
            if (index == null) {
                return null;
            }
            return parameters[index].getValue();
        }

        private void remove() {
            final Iterator<GwtConfigParameter> iterator = assetConfiguration.getParameters().iterator();
            while (iterator.hasNext()) {
                final GwtConfigParameter param = iterator.next();
                for (int i = 0; i < this.parameters.length; i++) {
                    if (this.parameters[i] == param) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    @Override
    public GwtConfigComponent getConfiguration() {
        return assetConfiguration;
    }

    @Override
    public String getAssetPid() {
        return assetConfiguration.getComponentId();
    }

}
