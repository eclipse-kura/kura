/*******************************************************************************
 * Copyright (c) 2016, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.cloudconnection;

import org.eclipse.kura.web.client.ui.ServicesUi;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionService;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class CloudConnectionConfigurationUi extends ServicesUi {

    private static final GwtCloudConnectionServiceAsync gwtCloudService = GWT.create(GwtCloudConnectionService.class);

    public CloudConnectionConfigurationUi(GwtConfigComponent addedItem) {
        super(addedItem);
        this.setDeleteButtonVisible(false);
        this.setBackend(new Backend() {

            @Override
            public void updateComponentConfiguration(GwtXSRFToken token, GwtConfigComponent component,
                    AsyncCallback<Void> callback) {
                gwtCloudService.updateStackComponentConfiguration(token, component, callback);

            }

            @Override
            public void deleteFactoryConfiguration(GwtXSRFToken token, String pid, AsyncCallback<Void> callback) {
                // no need
            }
        });
    }

}
