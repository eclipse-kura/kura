/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web2.ext.internal;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

public class WidgetAdapter implements Adapter<Widget> {

    @Override
    public JavaScriptObject adaptNonNull(Widget value) {
        return value.getElement();
    }

    private class WidgetWrapper extends Widget {

        public WidgetWrapper(final Element element) {
            setElement(element);
        }
    }

    @Override
    public Widget adaptNonNull(JavaScriptObject value) {

        return new WidgetWrapper(value.cast());
    }

}
