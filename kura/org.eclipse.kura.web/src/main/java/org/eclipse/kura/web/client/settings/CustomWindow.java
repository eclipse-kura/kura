package org.eclipse.kura.web.client.settings;

import com.google.gwt.core.client.JavaScriptObject;

public class CustomWindow extends JavaScriptObject {
	// All types that extend JavaScriptObject must have a protected,
	// no-args constructor. 
	protected CustomWindow() {}

	public static native CustomWindow open(String url, String target, String options) /*-{
	    return $wnd.open(url, target, options);
	  }-*/;

	public native void close() /*-{
	    this.close();
	  }-*/;

	public native void setUrl(String url) /*-{
	    if (this.location) {
	      this.location = url;
	    }
	  }-*/;
}
