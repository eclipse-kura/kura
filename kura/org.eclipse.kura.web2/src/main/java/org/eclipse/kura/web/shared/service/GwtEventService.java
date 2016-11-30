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

import org.eclipse.kura.web.shared.model.GwtEventInfo;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("event")
public interface GwtEventService extends RemoteService {

    public static final int POLL_TIMEOUT_SECONDS = 30;

    public List<GwtEventInfo> getNextEvents(String fromTimestamp);

    public String getLastEventTimestamp();

}
