package org.eclipse.kura.web.client.ui.wires.composer;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtWireComponentConfiguration;

import com.google.gwt.core.client.JavaScriptObject;

public final class WireComponent extends JavaScriptObject {

    protected WireComponent() {
    }

    public static native WireComponent create()
    /*-{
        return {
            renderingProperties: {}
        }
    }-*/;

    public native String getPid()
    /*-{
        return this.pid
    }-*/;

    public native String getFactoryPid()
    /*-{
        return this.factoryPid
    }-*/;

    public native int getInputPortCount()
    /*-{
        return this.inputPortCount
    }-*/;

    public native int getOutputPortCount()
    /*-{
        return this.outputPortCount
    }-*/;

    public native WireComponentRenderingProperties getRenderingProperties()
    /*-{
        return this.renderingProperties
    }-*/;

    public native String setPid(String pid)
    /*-{
        this.pid = pid
    }-*/;

    public native String setFactoryPid(String pid)
    /*-{
        this.factoryPid = pid
    }-*/;

    public native void setInputPortCount(int inputPortCount)
    /*-{
        this.inputPortCount = inputPortCount
    }-*/;

    public native void setOutputPortCount(int outputPortCount)
    /*-{
        this.outputPortCount = outputPortCount
    }-*/;

    public native void setRenderingProperties(WireComponentRenderingProperties properties)
    /*-{
        return this.renderingProperties = properties
    }-*/;

    public static WireComponent fromGwt(GwtWireComponentConfiguration config) {
        final GwtConfigComponent configuration = config.getConfiguration();

        final WireComponent result = WireComponent.create();

        result.setPid(configuration.getComponentId());
        result.setFactoryPid(configuration.getFactoryId());
        result.setInputPortCount(config.getInputPortCount());
        result.setOutputPortCount(config.getOutputPortCount());
        result.getRenderingProperties().setPosition(Point.create(config.getPositionX(), config.getPositionY()));

        return result;
    }

    public GwtWireComponentConfiguration toGwt(GwtConfigComponent configuration) {
        final GwtWireComponentConfiguration result = new GwtWireComponentConfiguration();
        result.setInputPortCount(getInputPortCount());
        result.setOutputPortCount(getOutputPortCount());
        result.setPositionX(getRenderingProperties().getPosition().getX());
        result.setPositionY(getRenderingProperties().getPosition().getY());
        result.setConfiguration(configuration);

        return result;
    }
}
