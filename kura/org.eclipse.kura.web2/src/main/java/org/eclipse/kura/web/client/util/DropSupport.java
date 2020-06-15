/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.web.client.util;

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

        public native String getFileName()
        /*-{
            var files = this.dataTransfer.files;
            return files[0].name;
        }-*/;

        public native boolean isFile()
        /*-{
            var files = this.dataTransfer.files;
            var result = true;
            if(files.length == 0) {
                return false;
            }
            return result;
        }-*/;

        public native void handleFile(FileUploadHandler fileUploadHandler)
        /*-{
            var file = this.dataTransfer.files[0];
            var reader = new FileReader();
            reader.onload = function(e) {
                fileUploadHandler.@org.eclipse.kura.web.client.util.FileUploadHandler::handleFileContent(Ljava/lang/String;Ljava/lang/String;)(file.name, e.target.result);
            }
            reader.readAsBinaryString(file);
        }-*/;

    }
}
