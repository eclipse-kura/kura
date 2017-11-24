package org.eclipse.kura.web.client.ui.wires.composer;

import com.google.gwt.core.client.JavaScriptObject;

public final class BlinkEffect extends JavaScriptObject {

    protected BlinkEffect() {
    }

    public static native BlinkEffect create(WireComposer composer)
    /*-{
        return new parent.window.BlinkEffect(composer)
    }-*/;

    public native void setEnabled(boolean enabled)
    /*-{
        this.setEnabled(enabled)
    }-*/;
}
