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
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

public class DropSupport {

    private Listener listener;

    private DropSupport(Element e) {
        attachNativeEventHandlers(e);
    }

    private static native boolean isDropSupported(Element element) /*-{
                                                                    return element['ondragenter'] !== undefined && element['ondrop'] !== undefined                 
                                                                    }-*/;

    public static DropSupport addIfSupported(Widget w) {
        Element domElement = w.getElement();
        if (!isDropSupported(domElement)) {
            return null;
        }
        return new DropSupport(domElement);
    }

    private boolean dispatchDragOver(JavaScriptObject nativeObject) {
        if (listener == null) {
            return false;
        }
        return listener.onDragOver(new DropEvent(nativeObject));
    }

    private boolean dispatchDrop(JavaScriptObject nativeObject) {
        if (listener == null) {
            return false;
        }
        return listener.onDrop(new DropEvent(nativeObject));
    }

    private native void attachNativeEventHandlers(Element element) /*-{
                                                                    var self = this
                                                                    element.addEventListener('dragover', function (event) {
                                                                        if (self.@org.eclipse.kura.web.client.util.DropSupport::dispatchDragOver(Lcom/google/gwt/core/client/JavaScriptObject;)(event.dataTransfer)) {
                                                                            event.preventDefault()
                                                                        }
                                                                    })
                                                                    element.addEventListener('drop', function (event) {
                                                                        if (self.@org.eclipse.kura.web.client.util.DropSupport::dispatchDrop(Lcom/google/gwt/core/client/JavaScriptObject;)(event.dataTransfer)) {
                                                                            event.preventDefault()
                                                                        }
                                                                    })
                                                                    }-*/;

    public interface Listener {

        public boolean onDragOver(DropEvent event);

        public boolean onDrop(DropEvent event);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public static class DropEvent {

        private JavaScriptObject nativeDataTransfer;

        private DropEvent(JavaScriptObject nativeDataTransfer) {
            this.nativeDataTransfer = nativeDataTransfer;
        }

        private native String getAsTextNative(JavaScriptObject nativeObject) /*-{
                                                                                  return nativeObject.getData('text')
                                                                              }-*/;

        private native boolean hasTypeNative(JavaScriptObject nativeObject, String type) /*-{
                                                                                               for (var i=0; i<nativeObject.types.length; i++) {
                                                                                                   if (nativeObject.types[i] === type) {
                                                                                                       return true
                                                                                                   }
                                                                                                   return false
                                                                                               }
                                                                                           }-*/;

        public boolean hasType(String type) {
            return hasTypeNative(nativeDataTransfer, type);
        }

        public String getAsText() {
            return getAsTextNative(nativeDataTransfer);
        }
    }
}
