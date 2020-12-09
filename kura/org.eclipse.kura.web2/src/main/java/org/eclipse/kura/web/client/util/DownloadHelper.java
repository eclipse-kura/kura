/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.http.client.URL;

public final class DownloadHelper {

    private static DownloadHelper instance;
    private Element downloadIframe;

    private DownloadHelper() {
        initDownloadIframe();
    }

    private native void initDownloadIframe()
    /*-{
        var iframe = document.createElement('iframe');
        iframe.style.display = 'none'
        document.getElementsByTagName('body')[0].appendChild(iframe);
        this.@org.eclipse.kura.web.client.util.DownloadHelper::downloadIframe = iframe;
    }-*/;

    public native void startDownload(String url)
    /*-{
        var downloadIframe = this.@org.eclipse.kura.web.client.util.DownloadHelper::downloadIframe;
        downloadIframe.setAttribute('src', url);
    }-*/;

    public void startDownload(GwtXSRFToken token, String resource) {
        final StringBuilder sbUrl = new StringBuilder();

        sbUrl.append(Console.ADMIN_ROOT).append("/").append(GWT.getModuleName()).append(resource)
                .append(resource.indexOf('?') != -1 ? '&' : '?').append("xsrfToken=")
                .append(URL.encodeQueryString(token.getToken()));

        startDownload(sbUrl.toString());
    }

    public static DownloadHelper instance() {
        if (instance == null) {
            instance = new DownloadHelper();
        }
        return instance;
    }
}
