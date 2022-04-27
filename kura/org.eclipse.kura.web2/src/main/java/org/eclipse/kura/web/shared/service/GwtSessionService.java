/*******************************************************************************
 * Copyright (c) 2019, 2022 Eurotech and/or its affiliates and others
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
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("session")
public interface GwtSessionService extends RemoteService {

    @Audit(componentName = "UI Session", description = "Logout")
    public void logout(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public GwtConsoleUserOptions getUserOptions(GwtXSRFToken token) throws GwtKuraException;

    public GwtUserConfig getUserConfig(GwtXSRFToken token) throws GwtKuraException;

    @Audit(componentName = "UI Session", description = "Password update")
    public void updatePassword(GwtXSRFToken token, final String oldPassword, final String newPassword)
            throws GwtKuraException;
}
