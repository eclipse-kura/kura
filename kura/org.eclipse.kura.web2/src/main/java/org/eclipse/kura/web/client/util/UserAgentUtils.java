/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

public class UserAgentUtils {

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
