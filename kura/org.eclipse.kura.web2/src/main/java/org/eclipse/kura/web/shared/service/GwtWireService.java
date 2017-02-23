/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
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
import java.util.Map;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtChannelInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtWiresConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * This is essentially used by GWT to interact with the OSGi WireService for
 * retrieving and managing the Wire Graphs.
 */
@RemoteServiceRelativePath("wires")
public interface GwtWireService extends RemoteService {

    /**
     * Retrieves all the registered driver instances.
     *
     * @param xsrfToken
     *            the XSRF token
     * @return the list of driver instances
     * @throws GwtKuraException
     *             if GWT encounters exception while retrieving the driver
     *             instances
     */
    public List<String> getDriverInstances(GwtXSRFToken xsrfToken) throws GwtKuraException;

    /**
     * Gets the base channel descriptor.
     *
     * @param xsrfToken
     *            the xsrf token
     * @return the gwt base channel descriptor
     * @throws GwtKuraException
     *             the gwt kura exception
     */
    public GwtConfigComponent getGwtBaseChannelDescriptor(GwtXSRFToken xsrfToken) throws GwtKuraException;

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
     * Gets the channels.
     *
     * @param xsrfToken
     *            the xsrf token
     * @param descriptor
     *            the descriptor
     * @param asset
     *            the asset
     * @return the gwt channels
     * @throws GwtKuraException
     *             the gwt kura exception
     */
    public List<GwtChannelInfo> getGwtChannels(GwtXSRFToken xsrfToken, GwtConfigComponent descriptor,
            GwtConfigComponent asset) throws GwtKuraException;

    /**
     * Returns the {@link GwtWiresConfiguration} instance associated.
     *
     * @param xsrfToken
     *            the XSRF token
     * @return the {@link GwtWiresConfiguration} instance
     * @throws GwtKuraException
     *             if the associated instance is not retrieved
     */
    public GwtWiresConfiguration getWiresConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException;

    /**
     * Updates the {@link GwtWiresConfiguration} instance with the provided
     * configuration.
     *
     * @param xsrfToken
     *            the XSRF token
     * @param newJsonConfiguration
     *            the new configuration to update
     * @param configurations
     *            the configurations
     * @throws GwtKuraException
     *             if the associated instance is not updated
     */
    public void updateWireConfiguration(GwtXSRFToken xsrfToken, String newJsonConfiguration,
            Map<String, GwtConfigComponent> configurations) throws GwtKuraException;

    /**
     * Returns the Driver Pid property as specified in {@link org.eclipse.kura.asset.AssetConstants}
     * 
     * @return String representing the {@link org.eclipse.kura.asset.AssetConstants.ASSET_DRIVER_PROP}
     */
    public String getDriverPidProp();
}
