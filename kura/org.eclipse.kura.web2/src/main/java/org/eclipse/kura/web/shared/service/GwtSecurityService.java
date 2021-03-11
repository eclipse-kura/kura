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
package org.eclipse.kura.web.shared.service;

import org.eclipse.kura.web.server.Audit;
import org.eclipse.kura.web.server.RequiredPermissions;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtTamperStatus;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("security")
@RequiredPermissions(KuraPermission.ADMIN)
public interface GwtSecurityService extends RemoteService {

    @RequiredPermissions({})
    public Boolean isSecurityServiceAvailable();

    @RequiredPermissions({})
    public Boolean isDebugMode();

    @Audit(componentName = "UI Security", description = "Reload security policy fingerprint")
    public void reloadSecurityPolicyFingerprint(GwtXSRFToken xsrfToken) throws GwtKuraException;

    @Audit(componentName = "UI Security", description = "Reload command line fingerprint")
    public void reloadCommandLineFingerprint(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public boolean isThreatManagerAvailable();

    public boolean isTamperDetectionAvailable();

    public GwtTamperStatus getTamperStatus(GwtXSRFToken xsrfToken) throws GwtKuraException;

    @Audit(componentName = "UI Security", description = "Reset tamper status")
    public void resetTamperStatus(GwtXSRFToken xsrfToken) throws GwtKuraException;
}