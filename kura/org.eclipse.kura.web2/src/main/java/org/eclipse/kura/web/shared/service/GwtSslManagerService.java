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

import java.util.List;
import java.util.Set;

import org.eclipse.kura.web.server.Audit;
import org.eclipse.kura.web.server.RequiredPermissions;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtComponentInstanceInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RequiredPermissions(KuraPermission.ADMIN)
@RemoteServiceRelativePath("ssl")
public interface GwtSslManagerService extends RemoteService {

    public Set<String> listSslManagerServiceFactoryPids() throws GwtKuraException;

    public List<GwtComponentInstanceInfo> listSslManagerServiceInstances() throws GwtKuraException;

    @Audit(componentName = "UI Ssl", description = "Create SslManagerService configuration")
    public void createSslManagerServiceFactoryConfiguration(GwtXSRFToken token, String pid, String factoryPid)
            throws GwtKuraException;

    @Audit(componentName = "UI Ssl", description = "Get SslManagerService configuration")
    public GwtConfigComponent getSslManagerServiceConfiguration(GwtXSRFToken token, String pid) throws GwtKuraException;

    @Audit(componentName = "UI Ssl", description = "Update SslManagerService configuration")
    public void updateSslManagerServiceConfiguration(GwtXSRFToken token, GwtConfigComponent component)
            throws GwtKuraException;

    @Audit(componentName = "UI Ssl", description = "Remove SslManagerService instance")
    public void deleteSslManagerService(GwtXSRFToken token, String pid) throws GwtKuraException;
}
