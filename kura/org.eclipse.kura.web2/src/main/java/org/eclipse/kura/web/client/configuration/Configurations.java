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
package org.eclipse.kura.web.client.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.kura.web.client.ui.drivers.assets.AssetModel;
import org.eclipse.kura.web.client.ui.drivers.assets.AssetModelImpl;
import org.eclipse.kura.web.client.util.ValidationUtil;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;

public class Configurations {

    private static final String WIRE_ASSET_PID = "org.eclipse.kura.wire.WireAsset";

    private final Set<String> allActivePids = new HashSet<>();
    private final Map<String, GwtConfigComponent> componentDefinitions = new HashMap<>();
    private final Map<String, HasConfiguration> currentConfigurations = new HashMap<>();
    private final Map<String, GwtConfigComponent> channelDescriptors = new HashMap<>();
    private GwtConfigComponent baseChannelDescriptor = null;

    public GwtConfigComponent getFactoryDefinition(String factoryPid) {
        return this.componentDefinitions.get(factoryPid);
    }

    public HasConfiguration getConfiguration(String pid) {
        return this.currentConfigurations.get(pid);
    }

    private GwtConfigComponent createConfigurationFromDefinition(String pid, String factoryPid,
            GwtConfigComponent definition) {
        final GwtConfigComponent cloned = new GwtConfigComponent(definition);
        cloned.setComponentId(pid);
        cloned.setFactoryPid(factoryPid);
        for (final GwtConfigParameter param : cloned.getParameters()) {
            if (param.getValue() == null && param.isRequired()) {
                param.setValue(param.getDefault());
            }
        }
        return cloned;
    }

    public HasConfiguration createConfiguration(String pid, String factoryPid) {
        final GwtConfigComponent definition = this.componentDefinitions.get(factoryPid);
        GwtConfigComponent newConfiguration = null;
        if (definition != null) {
            newConfiguration = createConfigurationFromDefinition(pid, factoryPid, definition);
        }
        if (newConfiguration == null) {
            newConfiguration = new GwtConfigComponent();
            newConfiguration.setComponentId(pid);
            newConfiguration.setFactoryPid(factoryPid);
        }
        return new ConfigurationWrapper(newConfiguration);
    }

    public HasConfiguration createAndRegisterConfiguration(String pid, String factoryPid) {
        final HasConfiguration result = createConfiguration(pid, factoryPid);
        this.currentConfigurations.put(pid, result);
        this.allActivePids.add(pid);
        return result;
    }

    public void setConfiguration(GwtConfigComponent gwtConfig) {
        final String pid = gwtConfig.getComponentId();
        this.allActivePids.add(pid);
        this.currentConfigurations.put(pid, new ConfigurationWrapper(gwtConfig));
    }

    public void setComponentDefinitions(List<GwtConfigComponent> factoryDefinitions) {
        this.componentDefinitions.clear();
        for (GwtConfigComponent factoryDefinition : factoryDefinitions) {
            this.componentDefinitions.put(factoryDefinition.getComponentId(), factoryDefinition);
        }
    }

    public void setComponentConfigurations(List<GwtConfigComponent> configurations) {
        this.currentConfigurations.clear();
        for (GwtConfigComponent configuration : configurations) {
            this.currentConfigurations.put(configuration.getComponentId(), new ConfigurationWrapper(configuration));
        }
    }

    public Collection<HasConfiguration> getConfigurations() {
        return this.currentConfigurations.values();
    }

    public void clear() {
        this.componentDefinitions.clear();
        this.currentConfigurations.clear();
        this.channelDescriptors.clear();
        this.baseChannelDescriptor = null;
    }

    public void setConfiguration(HasConfiguration configuration) {
        this.currentConfigurations.put(configuration.getComponentId(), configuration);
    }

    public void deleteConfiguration(String pid) {
        this.allActivePids.remove(pid);

        if (!WIRE_ASSET_PID.equals(this.currentConfigurations.get(pid).getConfiguration().getFactoryId())) {
            this.currentConfigurations.remove(pid);
        }
    }

    public void setChannelDescriptiors(List<GwtConfigComponent> descriptors) {
        this.channelDescriptors.clear();
        for (GwtConfigComponent descriptor : descriptors) {
            this.channelDescriptors.put(descriptor.getComponentId(), descriptor);
        }
    }

    public GwtConfigComponent getChannelDescriptor(String pid) {
        return this.channelDescriptors.get(pid);
    }

    public GwtConfigComponent getBaseChannelDescriptor() {
        return this.baseChannelDescriptor;
    }

    public void setBaseChannelDescriptor(GwtConfigComponent baseChannelDescriptor) {
        this.baseChannelDescriptor = baseChannelDescriptor;
    }

    public boolean isValid() {
        for (Entry<String, HasConfiguration> entry : this.currentConfigurations.entrySet()) {
            if (!entry.getValue().isValid()) {
                return false;
            }
        }
        return true;
    }

    public List<String> getInvalidConfigurationPids() {
        final List<String> result = new ArrayList<>();
        for (Entry<String, HasConfiguration> entry : this.currentConfigurations.entrySet()) {
            // Check if the item is invalid and if it is active (not queued for deletion).
            if (!entry.getValue().isValid() && this.allActivePids.contains(entry.getKey())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public boolean isDirty() {
        for (Entry<String, HasConfiguration> entry : this.currentConfigurations.entrySet()) {
            if (entry.getValue().isDirty()) {
                return true;
            }
        }
        return false;
    }

    public void clearDirtyState() {
        for (Entry<String, HasConfiguration> entry : this.currentConfigurations.entrySet()) {
            entry.getValue().clearDirtyState();
        }
    }

    public List<String> getDriverFactoryPids() {
        final ArrayList<String> result = new ArrayList<>();
        for (Entry<String, GwtConfigComponent> entry : this.componentDefinitions.entrySet()) {
            if (entry.getValue().isDriver()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public List<String> getDriverPids() {
        final ArrayList<String> result = new ArrayList<>();
        for (Entry<String, HasConfiguration> entry : this.currentConfigurations.entrySet()) {
            if (entry.getValue().getConfiguration().isDriver()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public List<String> getFactoryInstancesPids(String factoryPid) {
        final ArrayList<String> result = new ArrayList<>();
        for (Entry<String, HasConfiguration> entry : this.currentConfigurations.entrySet()) {
            final GwtConfigComponent config = entry.getValue().getConfiguration();
            if (factoryPid.equals(config.getFactoryId())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public void setChannelDescriptor(String pid, GwtConfigComponent descriptor) {
        this.channelDescriptors.put(pid, descriptor);
    }

    public void setAllActivePids(Collection<String> allActivePids) {
        this.allActivePids.clear();
        this.allActivePids.addAll(allActivePids);
    }

    public boolean isPidExisting(String pid) {
        return this.currentConfigurations.containsKey(pid) || this.allActivePids.contains(pid);
    }

    public void invalidateConfiguration(String pid) {
        this.currentConfigurations.computeIfPresent(pid,
                (p, configuration) -> new InvalidConfigurationWrapper(configuration));
    }

    private class ConfigurationWrapper implements HasConfiguration {

        private boolean isDirty;
        private final GwtConfigComponent configuration;

        public ConfigurationWrapper(GwtConfigComponent configuration) {
            this.configuration = configuration;
        }

        @Override
        public GwtConfigComponent getConfiguration() {
            return this.configuration;
        }

        @Override
        public void clearDirtyState() {
            this.isDirty = false;
        }

        @Override
        public boolean isValid() {
            if (this.configuration == null || !this.configuration.isValid()) {
                return false;
            }

            if (!WIRE_ASSET_PID.equals(this.configuration.getFactoryId())) {
                return ValidationUtil.validateParameters(configuration);
            } else {
                return validateAssetConfiguration();
            }
        }

        private boolean validateAssetConfiguration() {
            final String driverPid = this.configuration.getParameterValue(AssetConstants.ASSET_DRIVER_PROP.value());

            if (driverPid == null) {
                return false;
            }

            final GwtConfigComponent driverDescriptor = channelDescriptors.get(driverPid);

            if (driverDescriptor == null) {
                return false;
            }

            final AssetModel assetModelImpl = new AssetModelImpl(configuration, driverDescriptor,
                    baseChannelDescriptor);

            return assetModelImpl.isValid();
        }

        @Override
        public boolean isDirty() {
            return this.isDirty;
        }

        @Override
        public void markAsDirty() {
            this.isDirty = true;
        }

        @Override
        public void setListener(Listener listener) {
            // Not needed
        }

        @Override
        public String getComponentId() {
            return configuration != null ? configuration.getComponentId() : null;
        }
    }

    private class InvalidConfigurationWrapper implements HasConfiguration {

        final HasConfiguration wrapped;

        public InvalidConfigurationWrapper(HasConfiguration wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public GwtConfigComponent getConfiguration() {
            return this.wrapped.getConfiguration();
        }

        @Override
        public void clearDirtyState() {
            this.wrapped.clearDirtyState();
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public boolean isDirty() {
            return this.wrapped.isDirty();
        }

        @Override
        public void markAsDirty() {
            this.wrapped.markAsDirty();
        }

        @Override
        public void setListener(Listener listener) {
            this.wrapped.setListener(listener);
        }

        @Override
        public String getComponentId() {
            return wrapped.getComponentId();
        }
    }
}
