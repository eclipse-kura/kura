/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
