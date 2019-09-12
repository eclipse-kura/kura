/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.example.web.extension.client;

import org.eclipse.kura.web2.ext.ExtensionRegistry;

import com.google.gwt.core.client.EntryPoint;

public class ExampleEntryPoint implements EntryPoint {

    @Override
    public void onModuleLoad() {
        final ExtensionRegistry registry = ExtensionRegistry.get();

        registry.registerExtension(new ExampleViewExtension());
        registry.registerExtension(new ExampleAuthenticationHandler());
    }

}
