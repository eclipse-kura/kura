/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public interface Tab {

    public void setDirty(boolean flag);

    public boolean isDirty();

    public boolean isValid();

    public void refresh();

    public void clear();

    public class RefreshHandler implements ClickHandler {

        private final Tab target;
        private boolean initialized = false;
        private boolean alwaysRefresh = false;

        public RefreshHandler(Tab target) {
            this(target, false);
        }

        public RefreshHandler(Tab target, final boolean alwaysRefresh) {
            this.target = target;
            this.alwaysRefresh = alwaysRefresh;
        }

        @Override
        public void onClick(ClickEvent event) {
            if (target.isDirty() || !initialized || alwaysRefresh) {
                target.refresh();
                this.initialized = true;
            }
        }
    }
}
