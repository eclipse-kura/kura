/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.configuration;

import org.eclipse.kura.web.shared.model.GwtConfigComponent;

public interface HasConfiguration {

    public GwtConfigComponent getConfiguration();

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
