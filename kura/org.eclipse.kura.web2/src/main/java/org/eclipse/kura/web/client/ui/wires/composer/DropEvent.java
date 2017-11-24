package org.eclipse.kura.web.client.ui.wires.composer;

import com.google.gwt.core.client.JavaScriptObject;

public final class DropEvent extends JavaScriptObject {

    protected DropEvent() {
    }

    public native String getAttachment()
    /*-{
        return this.getAttachment()
    }-*/;

    public native void complete(WireComponent component)
    /*-{
        this.complete(component)
    }-*/;

    public native void cancel()
    /*-{
        this.cancel()
    }-*/;
}
