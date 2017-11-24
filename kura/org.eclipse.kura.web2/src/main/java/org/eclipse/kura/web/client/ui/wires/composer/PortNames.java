package org.eclipse.kura.web.client.ui.wires.composer;

import com.google.gwt.core.client.JavaScriptObject;

public final class PortNames extends JavaScriptObject {

    protected PortNames() {
    }

    public native String getPortName(int index)
    /*-{
        return this["" + index]
    }-*/;

    public native void setPortName(String name, int index)
    /*-{
        if (name) {
            this["" + index] = name
        } else {
            delete this["" + index]
        }
    }-*/;
}
