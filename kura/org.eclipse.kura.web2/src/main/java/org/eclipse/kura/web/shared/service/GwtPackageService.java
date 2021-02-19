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
import org.eclipse.kura.web.shared.model.GwtDeploymentPackage;
import org.eclipse.kura.web.shared.model.GwtMarketplacePackageDescriptor;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("package")
@RequiredPermissions(KuraPermission.PACKAGES_ADMIN)
public interface GwtPackageService extends RemoteService {

    @Audit(componentName = "UI Packages", description = "List deployment packages")
    public List<GwtDeploymentPackage> findDeviceDeploymentPackages(GwtXSRFToken xsrfToken) throws GwtKuraException;

    @Audit(componentName = "UI Packages", description = "Uninstall deployment package")
    public void uninstallDeploymentPackage(GwtXSRFToken xsrfToken, String packageName) throws GwtKuraException;

    public GwtMarketplacePackageDescriptor getMarketplacePackageDescriptor(GwtXSRFToken xsrfToken, String nodeId)
            throws GwtKuraException;

    @Audit(componentName = "UI Packages", description = "Install deployment package from Eclipse Marketplace")
    public void installPackageFromMarketplace(GwtXSRFToken xsrfToken, GwtMarketplacePackageDescriptor descriptor)
            throws GwtKuraException;

}
