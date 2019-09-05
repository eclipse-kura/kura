/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web2.ext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.kura.web2.ext.internal.ExtensionAdapter;
import org.eclipse.kura.web2.ext.internal.JsExtensionRegistry;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class ExtensionRegistry {

    private final JsExtensionRegistry jsExtensionRegistry;

    private static final ExtensionAdapter EXTENSION_ADAPTER = new ExtensionAdapter();

    private ExtensionRegistry(final JsExtensionRegistry jsExtensionRegistry) {
        this.jsExtensionRegistry = jsExtensionRegistry;
    }

    public static ExtensionRegistry get() {
        return new ExtensionRegistry(JsExtensionRegistry.get());
    }

    public void registerExtension(final Extension extension) {
        this.jsExtensionRegistry.registerExtension(EXTENSION_ADAPTER.adaptNullable(extension));
    }

    public void unregisterExtension(final Extension extension) {
        this.jsExtensionRegistry.unregisterExtension(EXTENSION_ADAPTER.adaptNullable(extension));
    }

    public void addExtensionConsumer(final Consumer<Extension> consumer) {
        this.jsExtensionRegistry.addExtensionConsumer(e -> consumer.accept(EXTENSION_ADAPTER.adaptNullable(e)));
    }

    public List<Extension> getExtensions() {
        final JsArray<JavaScriptObject> extensions = this.jsExtensionRegistry.getExtensions();

        final List<Extension> result = new ArrayList<>();

        for (int i = 0; i < extensions.length(); i++) {
            result.add(EXTENSION_ADAPTER.adaptNullable(extensions.get(i)));
        }

        return result;
    }
}
