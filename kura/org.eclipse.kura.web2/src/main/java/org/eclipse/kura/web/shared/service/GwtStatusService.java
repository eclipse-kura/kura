/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

import java.util.ArrayList;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("status")
public interface GwtStatusService extends RemoteService {

    /**
     * Returns a list of name/value pairs describing status aspects of the device.
     *
     * @param xsrfToken
     *            - A GwtXSRFToken token necessary to prevent cross-site request forgery attacks.
     * @param hasNetAdmin
     *            - Boolean value indicating if system is running with the network management services enabled.
     * @return
     * @throws GwtKuraException
     */
    public ArrayList<GwtGroupedNVPair> getDeviceConfig(GwtXSRFToken xsrfToken, boolean hasNetAdmin)
            throws GwtKuraException;

    /**
     * Connects the local MQTT client to the specified broker.
     *
     * @param xsrfToken
     *            - A GwtXSRFToken token necessary to prevent cross-site request forgery attacks.
     * @throws GwtKuraException
     */
    public void connectDataService(GwtXSRFToken xsrfToken, String connectionId) throws GwtKuraException;

    /**
     * Disconnects the local MQTT client.
     *
     * @param xsrfToken
     *            - A GwtXSRFToken token necessary to prevent cross-site request forgery attacks.
     * @throws GwtKuraException
     */
    public void disconnectDataService(GwtXSRFToken xsrfToken, String connectionId) throws GwtKuraException;
    
    public boolean isConnected(GwtXSRFToken xsrfToken, String connectionId) throws GwtKuraException;
}
