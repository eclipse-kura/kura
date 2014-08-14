/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.client.util;

public class UserAgentUtils
{
    public static boolean isIE() {
        return getUserAgent().contains("msie");
    }
    
    public static int getIEDocumentMode() {
    	return getIEUserAgentAgent();
    }
    
    public static native String getUserAgent() /*-{
        return navigator.userAgent.toLowerCase();
    }-*/;
    
    public static native int getIEUserAgentAgent() /*-{
    	var myNav = navigator.userAgent.toLowerCase();
    	var ieVersion = (myNav.indexOf('msie') != -1) ? parseInt(myNav.split('msie')[1]) : 0;
    	if (ieVersion > 0) {
    		return Math.min(ieVersion, document.documentMode);
    	}
    	return ieVersion;
    }-*/;
}
