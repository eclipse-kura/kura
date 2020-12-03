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

import org.eclipse.kura.web2.ext.WidgetFactory;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class WidgetFactoryAdapter implements Adapter<WidgetFactory> {

    private static final String BUILD_WIDGET = "buildWidget";

    @Override
    public JavaScriptObject adaptNonNull(final WidgetFactory value) {

        final JsObject object = JavaScriptObject.createObject().cast();

        object.set(BUILD_WIDGET,
                new SupplierAdapter<>(new WidgetAdapter()).adaptNullable(() -> new AttachWrapper(value.buildWidget())));

        return object;
    }

    private class AttachWrapper extends Composite {

        public AttachWrapper(final Widget child) {
            initWidget(child);

            onAttach();
        }

    }

    @Override
    public WidgetFactory adaptNonNull(final JavaScriptObject value) {

        return new WidgetFactory() {

            @Override
            public Widget buildWidget() {
                final JsObject obj = value.cast();

                final JavaScriptObject jsWidget = obj.call(BUILD_WIDGET);

                return new WidgetAdapter().adaptNullable(jsWidget);
            }
        };
    }

}
