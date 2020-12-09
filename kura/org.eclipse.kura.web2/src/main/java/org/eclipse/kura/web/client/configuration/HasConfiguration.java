/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.configuration;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;

public interface HasConfiguration {

    public GwtConfigComponent getConfiguration();

    public String getComponentId();

    public void clearDirtyState();

    public boolean isValid();

    public boolean isDirty();

    public void markAsDirty();

    public void setListener(Listener listener);

    public interface Listener {

        public void onConfigurationChanged(HasConfiguration hasConfiguration);

        public void onDirtyStateChanged(HasConfiguration hasConfiguration);
    }
}
