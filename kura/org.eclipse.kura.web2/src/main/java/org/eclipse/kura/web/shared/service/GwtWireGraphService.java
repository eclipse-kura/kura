/*******************************************************************************
 * Copyright (c) 2016, 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared.service;

import java.util.List;

import org.eclipse.kura.web.server.Audit;
import org.eclipse.kura.web.server.RequiredPermissions;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.KuraPermission;
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
@RequiredPermissions(KuraPermission.WIRES_ADMIN)
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
    @Audit(componentName = "UI Wires", description = "Obtain Wire Graph configuration")
    public GwtWireGraphConfiguration getWiresConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException;

    @Audit(componentName = "UI Wires", description = "Update Wire Graph configuration")
    public void updateWireConfiguration(GwtXSRFToken xsrfToken, GwtWireGraphConfiguration configurations,
            List<GwtConfigComponent> additionalConfigs) throws GwtKuraException;

    public GwtWireComposerStaticInfo getWireComposerStaticInfo(GwtXSRFToken xsrfToken) throws GwtKuraException;

    @Audit(componentName = "UI Wires", description = "Obtain Wire Graph configuration")
    public GwtWireGraph getWireGraph(GwtXSRFToken xsrfToken) throws GwtKuraException;
}
