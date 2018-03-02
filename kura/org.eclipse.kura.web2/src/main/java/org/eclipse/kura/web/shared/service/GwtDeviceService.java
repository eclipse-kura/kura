/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("device")
public interface GwtDeviceService extends RemoteService {

    public List<GwtGroupedNVPair> findDeviceConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public List<GwtGroupedNVPair> findBundles(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public void startBundle(GwtXSRFToken xsrfToken, String bundleId) throws GwtKuraException;

    public void stopBundle(GwtXSRFToken xsrfToken, String bundleId) throws GwtKuraException;

    public List<GwtGroupedNVPair> findThreads(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public List<GwtGroupedNVPair> findSystemProperties(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public String executeCommand(GwtXSRFToken xsrfToken, String cmd, String pwd) throws GwtKuraException;
}
