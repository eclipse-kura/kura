/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
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
