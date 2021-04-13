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
package org.eclipse.kura.web.shared.service;

import java.util.List;
import java.util.Set;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtComponentInstanceInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;

public interface GwtRestrictedComponentService extends RemoteService {

    Set<String> listFactoryPids() throws GwtKuraException;

    List<GwtComponentInstanceInfo> listServiceInstances() throws GwtKuraException;

    void createFactoryConfiguration(GwtXSRFToken token, String pid, String factoryPid) throws GwtKuraException;

    GwtConfigComponent getConfiguration(GwtXSRFToken token, String pid) throws GwtKuraException;

    void updateConfiguration(GwtXSRFToken token, GwtConfigComponent component) throws GwtKuraException;

    void deleteFactoryConfiguration(GwtXSRFToken token, String pid) throws GwtKuraException;
}
