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

import java.util.List;

import org.eclipse.kura.web.server.Audit;
import org.eclipse.kura.web.server.RequiredPermissions;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("device")
@RequiredPermissions(KuraPermission.DEVICE)
public interface GwtDeviceService extends RemoteService {

    public List<GwtGroupedNVPair> findDeviceConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public List<GwtGroupedNVPair> findBundles(GwtXSRFToken xsrfToken) throws GwtKuraException;

    @Audit(componentName = "UI Device", description = "Start bundle")
    public void startBundle(GwtXSRFToken xsrfToken, String bundleId) throws GwtKuraException;

    @Audit(componentName = "UI Device", description = "Stop bundle")
    public void stopBundle(GwtXSRFToken xsrfToken, String bundleId) throws GwtKuraException;

    public List<GwtGroupedNVPair> findThreads(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public List<GwtGroupedNVPair> findSystemPackages(GwtXSRFToken xsrfToken) throws GwtKuraException;

    @RequiredPermissions({})
    public List<GwtGroupedNVPair> findSystemProperties(GwtXSRFToken xsrfToken) throws GwtKuraException;

    @Audit(componentName = "UI Device", description = "Execute command")
    public String executeCommand(GwtXSRFToken xsrfToken, String cmd, String pwd) throws GwtKuraException;
}
