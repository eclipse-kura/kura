/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.web.client.util;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;

public class ResizableTableHeader extends SafeHtmlHeader {

    public ResizableTableHeader(final String value) {
        this(value, value);
    }

    public ResizableTableHeader(final String value, final String title) {
        super(getSafeHtml(value, title));
    }

    private static final SafeHtml getSafeHtml(final String value, final String title) {
        return new SafeHtmlBuilder() //
                .appendHtmlConstant("<div class=\"table-header-wrapper\" title=\"") //
                .appendEscaped(title) //
                .appendHtmlConstant("\">") //
                .appendEscaped(value) //
                .appendHtmlConstant("</div>") //
                .toSafeHtml();
    }

}
