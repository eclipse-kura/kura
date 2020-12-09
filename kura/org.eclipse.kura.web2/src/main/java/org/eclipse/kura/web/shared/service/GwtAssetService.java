/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.web.shared.GwtKuraException;
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
@RemoteServiceRelativePath("assetservices")
public interface GwtAssetService extends RemoteService {

    public GwtChannelOperationResult readAllChannels(GwtXSRFToken xsrfToken, String assetPid) throws GwtKuraException;

    public GwtChannelOperationResult write(GwtXSRFToken xsrfToken, String assetPid,
            List<GwtChannelRecord> channelRecords) throws GwtKuraException;

    public GwtConfigComponent getUploadedCsvConfig(GwtXSRFToken xsrfToken, String assetPid) throws GwtKuraException;

    public int convertToCsv(GwtXSRFToken token, String driverPid, GwtConfigComponent assetConfig)
            throws GwtKuraException, IOException;

}