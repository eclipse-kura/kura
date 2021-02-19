/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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

import java.util.Set;

import org.eclipse.kura.web.server.Audit;
import org.eclipse.kura.web.server.RequiredPermissions;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("users")
@RequiredPermissions(KuraPermission.ADMIN)
public interface GwtUserService extends RemoteService {

    @Audit(componentName = "UI User", description = "Create identity")
    public void createUser(GwtXSRFToken token, final String userName) throws GwtKuraException;

    @Audit(componentName = "UI User", description = "Delete identity")
    public void deleteUser(GwtXSRFToken token, final String userName) throws GwtKuraException;

    @Audit(componentName = "UI User", description = "Set identity password")
    public void setUserPassword(GwtXSRFToken token, final String userName, final String password)
            throws GwtKuraException;

    public Set<String> getDefinedPermissions(GwtXSRFToken token) throws GwtKuraException;

    @Audit(componentName = "UI User", description = "Get identities configuration")
    public Set<GwtUserConfig> getUserConfig(GwtXSRFToken token) throws GwtKuraException;

    @Audit(componentName = "UI User", description = "Update identities configuration")
    public void setUserConfig(GwtXSRFToken token, Set<GwtUserConfig> userConfig) throws GwtKuraException;
}