/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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
