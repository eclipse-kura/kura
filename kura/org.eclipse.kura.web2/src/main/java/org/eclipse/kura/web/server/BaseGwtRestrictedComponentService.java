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
import java.util.function.Predicate;

import org.eclipse.kura.web.server.util.GwtComponentServiceInternal;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtComponentInstanceInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtRestrictedComponentService;

public class BaseGwtRestrictedComponentService extends OsgiRemoteServiceServlet
        implements GwtRestrictedComponentService {

    private final Predicate<Set<String>> providedInterfacesFilter;

    public BaseGwtRestrictedComponentService(final Predicate<Set<String>> providedInterfacesFilter) {
        super();
        this.providedInterfacesFilter = providedInterfacesFilter;
    }

    @Override
    public Set<String> listFactoryPids() throws GwtKuraException {
        return GwtServerUtil.getServiceProviderFactoryPids(providedInterfacesFilter);
    }

    @Override
    public List<GwtComponentInstanceInfo> listServiceInstances() throws GwtKuraException {
        return GwtServerUtil.getComponentInstances(providedInterfacesFilter);
    }

    @Override
    public void createFactoryConfiguration(GwtXSRFToken token, String pid, String factoryPid) throws GwtKuraException {
        checkXSRFToken(token);

        requireIsManagedServiceFactory(factoryPid);

        GwtComponentServiceInternal.createFactoryComponent(factoryPid, pid);
    }

    @Override
    public GwtConfigComponent getConfiguration(GwtXSRFToken token, String pid) throws GwtKuraException {
        checkXSRFToken(token);

        requireIsManagedService(pid);

        return GwtComponentServiceInternal.findFilteredComponentConfiguration(pid).get(0);
    }

    @Override
    public void updateConfiguration(GwtXSRFToken token, GwtConfigComponent component) throws GwtKuraException {
        checkXSRFToken(token);

        requireIsManagedService(component.getComponentId());

        GwtComponentServiceInternal.updateComponentConfiguration(component);
    }

    @Override
    public void deleteFactoryConfiguration(GwtXSRFToken token, String pid) throws GwtKuraException {
        checkXSRFToken(token);

        requireIsManagedService(pid);

        GwtComponentServiceInternal.deleteFactoryConfiguration(pid, true);

    }

    private void requireIsManagedServiceFactory(String factoryPid) throws GwtKuraException {
        if (!(GwtServerUtil.isFactoryOf(factoryPid, providedInterfacesFilter))) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }
    }

    private void requireIsManagedService(String kuraServicePid) throws GwtKuraException {
        if (!(GwtServerUtil.providesService(kuraServicePid, providedInterfacesFilter))) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }
    }
}
