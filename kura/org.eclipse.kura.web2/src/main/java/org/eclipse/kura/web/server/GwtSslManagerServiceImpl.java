/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server;

import java.util.List;
import java.util.Set;

import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.web.server.util.GwtComponentServiceInternal;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtComponentInstanceInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSslManagerService;

public class GwtSslManagerServiceImpl extends OsgiRemoteServiceServlet implements GwtSslManagerService {

    /**
     * 
     */
    private static final long serialVersionUID = -1480613592267828475L;

    @Override
    public Set<String> listSslManagerServiceFactoryPids() throws GwtKuraException {
        return GwtServerUtil.getServiceProviderFactoryPids(SslManagerService.class);
    }

    @Override
    public List<GwtComponentInstanceInfo> listSslManagerServiceInstances() throws GwtKuraException {
        return GwtServerUtil.getComponentInstances(SslManagerService.class);
    }

    @Override
    public void createSslManagerServiceFactoryConfiguration(GwtXSRFToken token, String pid, String factoryPid)
            throws GwtKuraException {
        checkXSRFToken(token);

        requireIsSslManagerServiceFactory(factoryPid);

        GwtComponentServiceInternal.createFactoryComponent(factoryPid, pid);

    }

    @Override
    public GwtConfigComponent getSslManagerServiceConfiguration(GwtXSRFToken token, String pid)
            throws GwtKuraException {

        checkXSRFToken(token);

        requireIsSslManagerService(pid);

        return GwtComponentServiceInternal.findFilteredComponentConfiguration(pid).get(0);
    }

    @Override
    public void updateSslManagerServiceConfiguration(GwtXSRFToken token, GwtConfigComponent component)
            throws GwtKuraException {
        checkXSRFToken(token);

        requireIsSslManagerService(component.getComponentId());

        GwtComponentServiceInternal.updateComponentConfiguration(component);
    }

    @Override
    public void deleteSslManagerService(GwtXSRFToken token, String pid) throws GwtKuraException {
        checkXSRFToken(token);

        requireIsSslManagerService(pid);

        GwtComponentServiceInternal.deleteFactoryConfiguration(pid, true);

    }

    private static void requireIsSslManagerServiceFactory(String factoryPid) throws GwtKuraException {
        if (!(GwtServerUtil.isFactoryOfAnyService(factoryPid, SslManagerService.class))) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }
    }

    private static void requireIsSslManagerService(String kuraServicePid) throws GwtKuraException {
        if (!(GwtServerUtil.providesService(kuraServicePid, SslManagerService.class))) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }
    }

}
