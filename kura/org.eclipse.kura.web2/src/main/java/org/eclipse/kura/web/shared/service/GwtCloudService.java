/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates
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
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("cloudservices")
public interface GwtCloudService extends RemoteService {

    public List<GwtCloudConnectionEntry> findCloudServices() throws GwtKuraException;

    public List<GwtGroupedNVPair> findCloudServiceFactories() throws GwtKuraException;

    public List<String> findStackPidsByFactory(String factoryPid, String cloudServicePid) throws GwtKuraException;

    public String getSuggestedCloudServicePid(String factoryPid) throws GwtKuraException;

    public String getCloudServicePidRegex(String factoryPid) throws GwtKuraException;

    public void createCloudServiceFromFactory(GwtXSRFToken xsrfToken, String factoryPid, String cloudServicePid)
            throws GwtKuraException;

    public void deleteCloudServiceFromFactory(GwtXSRFToken xsrfToken, String factoryPid, String cloudServicePid)
            throws GwtKuraException;

}
