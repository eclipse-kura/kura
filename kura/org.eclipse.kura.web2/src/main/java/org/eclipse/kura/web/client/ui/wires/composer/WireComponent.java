/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.web.client.ui.wires.composer;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtWireComponentConfiguration;

import com.google.gwt.core.client.JavaScriptObject;

public final class WireComponent extends JavaScriptObject {

    protected WireComponent() {
    }

    public static native WireComponent create()
    /*-{
        return new parent.window.WireComponent()
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

    public native int getPortIndex(String portName, String direction)
    /*-{
        return this.getPortIndex(portName, direction)
    }-*/;

    public native String getPortName(int portIndex, String direction)
    /*-{
        return this.getPortName(portIndex, direction)
    }-*/;

    public native void setValid(boolean isValid)
    /*-{
        this.setValid(isValid)
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
