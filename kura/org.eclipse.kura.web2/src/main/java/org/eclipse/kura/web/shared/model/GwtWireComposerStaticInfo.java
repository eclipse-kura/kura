package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.List;

public final class GwtWireComposerStaticInfo extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = -2068309846272914698L;

    private List<GwtConfigComponent> componentDefinitions;

    private List<GwtWireComponentDescriptor> wireComponentDescriptors;

    private List<GwtConfigComponent> driverDescriptors;

    private GwtConfigComponent baseChannelDescriptor;

    public List<GwtConfigComponent> getComponentDefinitions() {
        return this.componentDefinitions;
    }

    public void setComponentDefinitions(List<GwtConfigComponent> componentDefinitions) {
        this.componentDefinitions = componentDefinitions;
    }

    public List<GwtWireComponentDescriptor> getWireComponentDescriptors() {
        return this.wireComponentDescriptors;
    }

    public void setWireComponentDescriptors(List<GwtWireComponentDescriptor> wireComponentDescriptors) {
        this.wireComponentDescriptors = wireComponentDescriptors;
    }

    public List<GwtConfigComponent> getDriverDescriptors() {
        return this.driverDescriptors;
    }

    public void setDriverDescriptors(List<GwtConfigComponent> driverDescriptors) {
        this.driverDescriptors = driverDescriptors;
    }

    public GwtConfigComponent getBaseChannelDescriptor() {
        return this.baseChannelDescriptor;
    }

    public void setBaseChannelDescriptor(GwtConfigComponent baseChannelDescriptor) {
        this.baseChannelDescriptor = baseChannelDescriptor;
    }
}
