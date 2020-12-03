/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

import java.util.ArrayList;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("device")
public interface GwtDeviceService extends RemoteService {

    public ArrayList<GwtGroupedNVPair> findDeviceConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public ArrayList<GwtGroupedNVPair> findBundles(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public void startBundle(GwtXSRFToken xsrfToken, String bundleId) throws GwtKuraException;

    public void stopBundle(GwtXSRFToken xsrfToken, String bundleId) throws GwtKuraException;

    public ArrayList<GwtGroupedNVPair> findThreads(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public ArrayList<GwtGroupedNVPair> findSystemProperties(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public String executeCommand(GwtXSRFToken xsrfToken, String cmd, String pwd) throws GwtKuraException;
}
