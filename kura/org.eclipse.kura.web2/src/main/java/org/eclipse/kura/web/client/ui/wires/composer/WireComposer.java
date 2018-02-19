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

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

public final class WireComposer extends JavaScriptObject {

    protected WireComposer() {
    }

    public static native WireComposer create(Element target)
    /*-{
        var self = new parent.window.WireComposer(target)
        self.dispatchWireCreated = function (wire) {
            this.isDirty = true
            if (!this.listener) {
                return
            }
            this.listener.@org.eclipse.kura.web.client.ui.wires.composer.WireComposer$Listener::onWireCreated(Lorg/eclipse/kura/web/client/ui/wires/composer/Wire;)(wire)
        }
        self.dispatchWireChanged = function (wire) {
            this.isDirty = true
            if (!this.listener) {
                return
            }
            this.listener.@org.eclipse.kura.web.client.ui.wires.composer.WireComposer$Listener::onWireChanged(Lorg/eclipse/kura/web/client/ui/wires/composer/Wire;)(wire)
        }
        self.dispatchWireDeleted = function (wire) {
            this.isDirty = true
            if (!this.listener) {
                return
            }
            this.listener.@org.eclipse.kura.web.client.ui.wires.composer.WireComposer$Listener::onWireDeleted(Lorg/eclipse/kura/web/client/ui/wires/composer/Wire;)(wire)
        }
        self.dispatchWireComponentSelected = function (component) {
            if (!this.listener) {
                return
            }
            this.listener.@org.eclipse.kura.web.client.ui.wires.composer.WireComposer$Listener::onWireComponentSelected(Lorg/eclipse/kura/web/client/ui/wires/composer/WireComponent;)(component)
        }
        self.dispatchWireComponentDeselected = function (component) {
            if (!this.listener) {
                return
            }
            this.listener.@org.eclipse.kura.web.client.ui.wires.composer.WireComposer$Listener::onWireComponentDeselected(Lorg/eclipse/kura/web/client/ui/wires/composer/WireComponent;)(component)
        }
        self.dispatchWireComponentCreated = function (component) {
            this.isDirty = true
            if (!this.listener) {
                return
            }
            this.listener.@org.eclipse.kura.web.client.ui.wires.composer.WireComposer$Listener::onWireComponentCreated(Lorg/eclipse/kura/web/client/ui/wires/composer/WireComponent;)(component)
        }
        self.dispatchWireComponentChanged = function (component) {
            this.isDirty = true
            if (!this.listener) {
                return
            }
            this.listener.@org.eclipse.kura.web.client.ui.wires.composer.WireComposer$Listener::onWireComponentChanged(Lorg/eclipse/kura/web/client/ui/wires/composer/WireComponent;)(component)
        }
        self.dispatchWireComponentDeleted = function (component) {
            this.isDirty = true
            if (!this.listener) {
                return
            }
            this.listener.@org.eclipse.kura.web.client.ui.wires.composer.WireComposer$Listener::onWireComponentDeleted(Lorg/eclipse/kura/web/client/ui/wires/composer/WireComponent;)(component)
        }
        self.dispatchDrop = function (event) {
            if (!this.listener) {
                return
            }
            this.listener.@org.eclipse.kura.web.client.ui.wires.composer.WireComposer$Listener::onDrop(Lorg/eclipse/kura/web/client/ui/wires/composer/DropEvent;)(event)
        }
        return self
    }-*/;

    public native void addWireComponent(WireComponent component)
    /*-{
        this.addWireComponent(component)
    }-*/;

    public native WireComponent getSelectedWireComponent()
    /*-{
        return this.getSelectedWireComponent()
    }-*/;

    public native void deselectWireCompoent()
    /*-{
        return this.deselectWireComponent()
    }-*/;

    public native void deleteWireComponent(WireComponent component)
    /*-{
        this.deleteWireComponent(component)
    }-*/;

    public native void addWire(Wire wire)
    /*-{
        this.addWire(wire)
    }-*/;

    public native WireComponent getWireComponent(String pid)
    /*-{
        var result = null
        this.forEachWireComponent(function (component) {
            if (pid === component.pid) {
                result = component
            }
        })
        return result
    }-*/;

    public native List<WireComponent> getWireComponents()
    /*-{
        var result = @java.util.ArrayList::new()()
        this.forEachWireComponent(function (component) {
            result.@java.util.ArrayList::add(Ljava/lang/Object;)(component)
        })
        return result
    }-*/;

    public native List<Wire> getWires()
    /*-{
        var result = @java.util.ArrayList::new()()
        this.forEachWire(function (wire) {
            result.@java.util.ArrayList::add(Ljava/lang/Object;)(wire)
        })
        return result
    }-*/;

    public native int getWireComponentCount()
    /*-{
        return this.getWireComponentCount()
    }-*/;

    public native int getWireCount()
    /*-{
        return this.getWireCount()
    }-*/;

    public native void zoomIn()
    /*-{
        this.zoomIn()
    }-*/;

    public native void zoomOut()
    /*-{
        this.zoomOut()
    }-*/;

    public native void clear()
    /*-{
        this.clear()
    }-*/;

    public native boolean isDirty()
    /*-{
        return this.isDirty
    }-*/;

    public native void clearDirtyState()
    /*-{
        this.isDirty = false;
    }-*/;

    public native void fitContent(boolean transition)
    /*-{
        this.fitContent(transition)
    }-*/;

    public native void setListener(Listener listener)
    /*-{
        this.listener = listener
    }-*/;

    public interface Listener {

        public void onWireCreated(Wire wire);

        public void onWireDeleted(Wire wire);

        public void onWireChanged(Wire wire);

        public void onWireComponentSelected(WireComponent component);

        public void onWireComponentDeselected(WireComponent component);

        public void onWireComponentChanged(WireComponent component);

        public void onWireComponentCreated(WireComponent component);

        public void onWireComponentDeleted(WireComponent component);

        public void onDrop(DropEvent event);
    }
}
