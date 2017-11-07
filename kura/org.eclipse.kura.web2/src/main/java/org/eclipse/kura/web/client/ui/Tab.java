/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public interface Tab {

    public void setDirty(boolean flag);

    public boolean isDirty();

    public boolean isValid();

    public void refresh();

    public class RefreshHandler implements ClickHandler {

        private final Tab target;
        private boolean initialized = false;

        public RefreshHandler(Tab target) {
            this.target = target;
        }

        @Override
        public void onClick(ClickEvent event) {
            if (target.isDirty() || !initialized) {
                target.refresh();
                this.initialized = true;
            }
        }
    }
}
