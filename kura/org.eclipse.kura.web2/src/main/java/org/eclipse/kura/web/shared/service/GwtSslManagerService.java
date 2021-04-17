/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.service;

import org.eclipse.kura.web.server.Audit;
import org.eclipse.kura.web.server.RequiredPermissions;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RequiredPermissions(KuraPermission.ADMIN)
@RemoteServiceRelativePath("ssl")
public interface GwtSslManagerService extends GwtRestrictedComponentService {

    @Audit(componentName = "UI SslManagerService", description = "Create configuration")
    public void createFactoryConfiguration(GwtXSRFToken token, String pid, String factoryPid) throws GwtKuraException;

    @Audit(componentName = "UI SslManagerService", description = "Get configuration")
    public GwtConfigComponent getConfiguration(GwtXSRFToken token, String pid) throws GwtKuraException;

    @Audit(componentName = "UI SslManagerService", description = "Update configuration")
    public void updateConfiguration(GwtXSRFToken token, GwtConfigComponent component) throws GwtKuraException;

    @Audit(componentName = "UI SslManagerService", description = "Remove configuration")
    public void deleteFactoryConfiguration(GwtXSRFToken token, String pid) throws GwtKuraException;
}
