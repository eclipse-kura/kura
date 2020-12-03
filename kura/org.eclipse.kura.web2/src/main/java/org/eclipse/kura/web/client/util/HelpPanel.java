/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
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

import com.google.gwt.user.client.ui.RootPanel;

public final class HelpPanel {

    private HelpPanel() {
    }

    private static HelpPanelImpl helpPanelImpl;

    static {
        helpPanelImpl = new HelpPanelImpl();
        RootPanel.get().add(helpPanelImpl);
    }

    public static void show(String helpText) {
        setHelpText(helpText);
        show();
    }

    public static void show() {
        helpPanelImpl.show();
    }

    public static void setHelpText(String helpText) {
        helpPanelImpl.setContent(helpText);
    }
}
