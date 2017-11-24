package org.eclipse.kura.web.client.ui.wires;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.web.client.ui.wires.composer.WireComponent;
import org.eclipse.kura.web.shared.model.GwtWireComponentDescriptor;

public class WireComponentDescriptors {

    private Map<String, GwtWireComponentDescriptor> descriptors = new HashMap<>();

    public void setDescriptors(List<GwtWireComponentDescriptor> descriptors) {
        this.descriptors.clear();
        for (GwtWireComponentDescriptor desc : descriptors) {
            log(desc);
            this.descriptors.put(desc.getFactoryPid(), desc);
        }
    }

    public GwtWireComponentDescriptor getDescriptor(String factoryPid) {
        return descriptors.get(factoryPid);
    }

    public Map<String, GwtWireComponentDescriptor> getDescriptors() {
        return descriptors;
    }

    private native void log(Object o)
    /*-{
        console.log(o)
    }-*/;

    public WireComponent createNewComponent(String pid, String factoryPid) {
        log(pid);
        log(factoryPid);
        log(descriptors);
        final GwtWireComponentDescriptor descriptor = descriptors.get(factoryPid);
        if (descriptor == null) {
            return null;
        }

        final WireComponent result = WireComponent.create();

        result.setPid(pid);
        result.setFactoryPid(factoryPid);
        result.setInputPortCount(descriptor.getMinInputPorts());
        result.setOutputPortCount(descriptor.getMinOutputPorts());

        return result;
    }
}
