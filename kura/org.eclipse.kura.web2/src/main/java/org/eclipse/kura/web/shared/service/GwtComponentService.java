/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared.service;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("component")
public interface GwtComponentService extends RemoteService {

    public void createFactoryComponent(GwtXSRFToken xsrfToken, String factoryPid, String pid) throws GwtKuraException;

    public void deleteFactoryConfiguration(GwtXSRFToken xsrfToken, String pid, boolean takeSnapshot)
            throws GwtKuraException;

    public List<GwtConfigComponent> findComponentConfiguration(GwtXSRFToken xsrfToken, String pid)
            throws GwtKuraException;

    public List<GwtConfigComponent> findComponentConfigurations(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public List<String> findFactoryComponents(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public List<GwtConfigComponent> findFilteredComponentConfiguration(GwtXSRFToken xsrfToken, String pid)
            throws GwtKuraException;

    public List<GwtConfigComponent> findFilteredComponentConfigurations(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public List<GwtConfigComponent> findServicesConfigurations(GwtXSRFToken xsrfToken) throws GwtKuraException;

    public GwtConfigComponent findWireComponentConfigurationFromPid(GwtXSRFToken xsrfToken, String pid,
            String factoryPid, Map<String, Object> extraProps) throws GwtKuraException;

    public void updateComponentConfiguration(GwtXSRFToken xsrfToken, GwtConfigComponent configComponent)
            throws GwtKuraException;

}
