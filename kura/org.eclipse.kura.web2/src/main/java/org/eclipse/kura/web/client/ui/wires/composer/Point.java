package org.eclipse.kura.web.client.ui.wires.composer;

import com.google.gwt.core.client.JavaScriptObject;

public final class Point extends JavaScriptObject {

    protected Point() {
    }

    public static native Point create(double x, double y)
    /*-{
        return {x: x, y: y}
    }-*/;

    public native double getX()
    /*-{
        return this.x
    }-*/;

    public native int getY()
    /*-{
        return this.y
    }-*/;

    public native void setX(double x)
    /*-{
        this.x = x;
    }-*/;

    public native int getY(double y)
    /*-{
        this.y = y
    }-*/;
}
