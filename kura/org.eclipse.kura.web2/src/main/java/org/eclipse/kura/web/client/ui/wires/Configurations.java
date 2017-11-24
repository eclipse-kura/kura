package org.eclipse.kura.web.client.ui.wires;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;

public class Configurations {

    private final Map<String, GwtConfigComponent> componentDefinitions = new HashMap<>();
    private final Map<String, HasConfiguration> currentConfigurations = new HashMap<>();
    private final Map<String, GwtConfigComponent> channelDescriptors = new HashMap<>();
    private GwtConfigComponent baseChannelDescriptor = null;

    public GwtConfigComponent getFactoryDefinition(String factoryPid) {
        return componentDefinitions.get(factoryPid);
    }

    public HasConfiguration getConfiguration(String pid) {
        return currentConfigurations.get(pid);
    }

    private native void log(Object o)
    /*-{
        console.log(o)
    }-*/;

    private GwtConfigComponent createConfigurationFromDefinition(String pid, String factoryPid,
            GwtConfigComponent definition) {
        final GwtConfigComponent cloned = new GwtConfigComponent(definition);
        cloned.setComponentId(pid);
        cloned.setFactoryPid(factoryPid);
        for (GwtConfigParameter param : cloned.getParameters()) {
            log(param.getName());
            log(param.getDefault());
            log(param.getValue());
            param.setValue(param.getDefault());
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
        final HasConfiguration result = new ConfigurationWrapper(newConfiguration);
        this.currentConfigurations.put(pid, result);
        return result;
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

    public List<HasConfiguration> getModifiedConfigurations() {
        final ArrayList<HasConfiguration> result = new ArrayList<>();
        for (Entry<String, HasConfiguration> config : this.currentConfigurations.entrySet()) {
            if (!(config.getValue() instanceof ConfigurationWrapper)) {
                result.add(config.getValue());
            }
        }
        return result;
    }

    public void clear() {
        this.componentDefinitions.clear();
        this.currentConfigurations.clear();
        this.channelDescriptors.clear();
        this.baseChannelDescriptor = null;
    }

    public void updateConfiguration(HasConfiguration configuration) {
        this.currentConfigurations.put(configuration.getConfiguration().getComponentId(), configuration);
    }

    public void deleteConfiguration(String pid) {
        this.currentConfigurations.remove(pid);
    }

    private class ConfigurationWrapper implements HasConfiguration {

        private GwtConfigComponent configuration;

        public ConfigurationWrapper(GwtConfigComponent configuration) {
            this.configuration = configuration;
        }

        @Override
        public GwtConfigComponent getConfiguration() {
            return configuration;
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
        return baseChannelDescriptor;
    }

    public void setBaseChannelDescriptor(GwtConfigComponent baseChannelDescriptor) {
        this.baseChannelDescriptor = baseChannelDescriptor;
    }

}
