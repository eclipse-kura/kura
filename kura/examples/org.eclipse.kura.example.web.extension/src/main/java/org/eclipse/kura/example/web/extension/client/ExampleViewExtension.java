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
