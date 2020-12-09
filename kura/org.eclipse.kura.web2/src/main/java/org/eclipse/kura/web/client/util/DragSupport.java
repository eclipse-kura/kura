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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

public class DragSupport {

    private Listener listener;

    private DragSupport(Element e) {
        attachNativeEventHandlers(e);
    }

    private static native boolean isDragSupported(Element element)
    /*-{
        return element['ondragstart'] !== undefined
    }-*/;

    public static DragSupport addIfSupported(Widget w) {
        Element domElement = w.getElement();
        if (!isDragSupported(domElement)) {
            return null;
        }
        return new DragSupport(domElement);
    }

    private void dispatchDragStart(JavaScriptObject nativeObject) {
        if (this.listener == null) {
            return;
        }
        this.listener.onDragStart(new DragEvent(nativeObject));
    }

    private native void attachNativeEventHandlers(Element element)
    /*-{
        element.setAttribute('draggable', 'true')
        var self = this
        element.addEventListener('dragstart', function (event) {
        self.@org.eclipse.kura.web.client.util.DragSupport::dispatchDragStart(Lcom/google/gwt/core/client/JavaScriptObject;)(event.dataTransfer)
        })
    }-*/;

    public interface Listener {

        public void onDragStart(DragEvent event);

    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public static class DragEvent {

        private final JavaScriptObject nativeDataTransfer;

        private DragEvent(JavaScriptObject nativeDataTransfer) {
            this.nativeDataTransfer = nativeDataTransfer;
        }

        private native void setDataNative(JavaScriptObject nativeObject, String type, String data)
        /*-{
            nativeObject.setData(type, data)
        }-*/;

        public void setTextData(String data) {
            setDataNative(this.nativeDataTransfer, "Text", data);
        }
    }
}
