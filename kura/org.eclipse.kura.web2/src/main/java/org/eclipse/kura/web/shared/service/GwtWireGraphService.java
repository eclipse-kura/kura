/*******************************************************************************
 * Copyright (c) 2016, 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared.service;

import java.util.List;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtWireComposerStaticInfo;
import org.eclipse.kura.web.shared.model.GwtWireGraph;
import org.eclipse.kura.web.shared.model.GwtWireGraphConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * This is essentially used by GWT to interact with the OSGi WireGraphService for
 * retrieving and managing the Wire Graphs.
 */
@RemoteServiceRelativePath("wires")
public interface GwtWireGraphService extends RemoteService {

    /**
     * Gets the channel descriptor.
     *
     * @param xsrfToken
     *            the xsrf token
     * @param driverPid
     *            the driver pid
     * @return the gwt channel descriptor
     * @throws GwtKuraException
     *             the gwt kura exception
     */
    public GwtConfigComponent getGwtChannelDescriptor(GwtXSRFToken xsrfToken, String driverPid) throws GwtKuraException;

    /**
     * Returns the {@link GwtWireGraphConfiguration} instance associated.
     *
     * @param xsrfToken
     *            the XSRF token
     * @return the {@link GwtWireGraphConfiguration} instance
     * @throws GwtKuraException
     *             if the associated instance is not retrieved
     */
    public GwtWireGraphConfiguration getWiresConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public void updateWireConfiguration(GwtXSRFToken xsrfToken, GwtWireGraphConfiguration configurations,
            List<GwtConfigComponent> additionalConfigs) throws GwtKuraException;

    public GwtWireComposerStaticInfo getWireComposerStaticInfo(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public GwtWireGraph getWireGraph(GwtXSRFToken xsrfToken) throws GwtKuraException;
}
