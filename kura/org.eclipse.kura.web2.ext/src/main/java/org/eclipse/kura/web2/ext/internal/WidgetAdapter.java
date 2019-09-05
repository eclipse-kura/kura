/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
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
