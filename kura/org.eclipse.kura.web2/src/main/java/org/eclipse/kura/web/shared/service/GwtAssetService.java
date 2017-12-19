/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.shared.service;

import java.util.List;
import java.util.Set;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtChannelRecord;
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

    public List<GwtChannelRecord> read(GwtXSRFToken xsrfToken, String assetPid, Set<String> channelNames)
            throws GwtKuraException;

    public List<GwtChannelRecord> readAllChannels(GwtXSRFToken xsrfToken, String assetPid) throws GwtKuraException;

    public void write(GwtXSRFToken xsrfToken, String assetPid, List<GwtChannelRecord> channelRecords)
            throws GwtKuraException;

}