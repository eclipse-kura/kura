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

import org.eclipse.kura.web2.ext.Context;
import org.eclipse.kura.web2.ext.Extension;

import com.google.gwt.user.client.ui.Label;

public class ExampleViewExtension implements Extension {

    @Override
    public void onLoad(final Context context) {
        context.addSidenavComponent("Example Sidenav Extension", "PUZZLE_PIECE",
                () -> new Label("test string from extension in sidenav"));
        context.addSettingsComponent("Extension", () -> new Label("test string from extension in settings"));
    }

}
