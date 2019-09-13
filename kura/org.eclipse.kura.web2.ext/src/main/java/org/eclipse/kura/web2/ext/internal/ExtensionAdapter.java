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

import org.eclipse.kura.web2.ext.Context;
import org.eclipse.kura.web2.ext.Extension;

import com.google.gwt.core.client.JavaScriptObject;

public class ExtensionAdapter implements Adapter<Extension> {

    private static final String ON_LOAD = "onLoad";

    @Override
    public JavaScriptObject adaptNonNull(final Extension extension) {
        final JsObject object = JavaScriptObject.createObject().cast();

        object.set(ON_LOAD, new ConsumerAdapter<>(new ContextAdapter()).adaptNullable(extension::onLoad));

        return object;
    }

    @Override
    public Extension adaptNonNull(final JavaScriptObject jsExtension) {
        return new Extension() {

            @Override
            public void onLoad(Context context) {
                final JsObject obj = jsExtension.cast();

                obj.call(ON_LOAD, new ContextAdapter().adaptNullable(context));
            }
        };
    }

}
