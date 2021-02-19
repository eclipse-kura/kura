/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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

import java.io.IOException;
import java.util.List;

import org.eclipse.kura.web.server.Audit;
import org.eclipse.kura.web.server.RequiredPermissions;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtChannelOperationResult;
import org.eclipse.kura.web.shared.model.GwtChannelRecord;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * This interface provide a set of methods to manage assets from the web UI.
 * In particular, it provides a way to read, write and list asset instances.
 *
 */

@RequiredPermissions(KuraPermission.WIRES_ADMIN)
@RemoteServiceRelativePath("assetservices")
public interface GwtDriverAndAssetService extends RemoteService {

    @Audit(componentName = "UI Asset", description = "Read all channels")
    public GwtChannelOperationResult readAllChannels(GwtXSRFToken xsrfToken, String assetPid) throws GwtKuraException;

    @Audit(componentName = "UI Asset", description = "Write")
    public GwtChannelOperationResult write(GwtXSRFToken xsrfToken, String assetPid,
            List<GwtChannelRecord> channelRecords) throws GwtKuraException;

    public GwtConfigComponent getUploadedCsvConfig(GwtXSRFToken xsrfToken, String assetPid) throws GwtKuraException;

    public int convertToCsv(GwtXSRFToken token, String driverPid, GwtConfigComponent assetConfig)
            throws GwtKuraException, IOException;

    @Audit(componentName = "UI Asset", description = "Create driver or asset configuration")
    public void createDriverOrAssetConfiguration(GwtXSRFToken token, String factoryPid, String pid)
            throws GwtKuraException;

    @Audit(componentName = "UI Asset", description = "Create driver or asset configuration")
    public void createDriverOrAssetConfiguration(GwtXSRFToken token, String factoryPid, String pid,
            GwtConfigComponent config) throws GwtKuraException;

    @Audit(componentName = "UI Asset", description = "Update driver or asset configuration")
    public void updateDriverOrAssetConfiguration(GwtXSRFToken token, GwtConfigComponent config) throws GwtKuraException;

    @Audit(componentName = "UI Asset", description = "Delete driver or asset configuration")
    public void deleteDriverOrAssetConfiguration(GwtXSRFToken token, String pid, boolean takeSnapshot)
            throws GwtKuraException;
}