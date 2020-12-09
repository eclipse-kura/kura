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

package org.eclipse.kura.web.client.util;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.DataTransfer;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

public final class DropSupport extends JavaScriptObject {

    protected DropSupport() {
    }

    public static native DropSupport addIfSupported(Element e)
    /*-{
        var self = parent.window.DropSupport.addIfSupported(e)
        if (!self) {
        return null
        }
        self.dragOverHandler = function (event) {
            if (!self.listener) {
                return false
            }
            return self.listener.@org.eclipse.kura.web.client.util.DropSupport$Listener::onDragOver(Lorg/eclipse/kura/web/client/util/DropSupport$DropEvent;)(event)
        }
        self.dragExitHandler = function (event) {
            if (!self.listener) {
                return
            }
            self.listener.@org.eclipse.kura.web.client.util.DropSupport$Listener::onDragExit(Lorg/eclipse/kura/web/client/util/DropSupport$DropEvent;)(event)
        }
        self.dropHandler = function (event) {
            if (!self.listener) {
                return false
            }
            return self.listener.@org.eclipse.kura.web.client.util.DropSupport$Listener::onDrop(Lorg/eclipse/kura/web/client/util/DropSupport$DropEvent;)(event)
        }
        return self
    }-*/;

    public static DropSupport addIfSupported(Widget w) {
        return addIfSupported(w.getElement());
    }

    public interface Listener {

        public boolean onDragOver(DropEvent event);

        public void onDragExit(DropEvent event);

        public boolean onDrop(DropEvent event);
    }

    public native void setListener(Listener listener)
    /*-{
        this.listener = listener;
    }-*/;

    public static final class DropEvent extends JavaScriptObject {

        protected DropEvent() {
        }

        public native DataTransfer getDataTransfer()
        /*-{
            return this.dataTransfer;
        }-*/;

        public native void preventDefault()
        /*-{
            this.preventDefault();
        }-*/;

        public native void stopPropagation()
        /*-{
            return this.stopPropagation();
        }-*/;

        public native String getAsText()
        /*-{
            return this.dataTransfer.getData('Text')
        }-*/;

        public native double getClientX()
        /*-{
            return this.clientX
        }-*/;

        public native double getClientY()
        /*-{
            return this.clientY
        }-*/;

        public native List<File> getFiles()
        /*-{
            var result = @java.util.ArrayList::new()()
            if (!this.dataTransfer) {
                return result
            }
            if (!this.dataTransfer.files) {
                return result
            }
            for (var i = 0; i < this.dataTransfer.files.length; i++) {
                result.@java.util.ArrayList::add(Ljava/lang/Object;)(this.dataTransfer.files[i])
            }
            return result
        }-*/;

    }
}
