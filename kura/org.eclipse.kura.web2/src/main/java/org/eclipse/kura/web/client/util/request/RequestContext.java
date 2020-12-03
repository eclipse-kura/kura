/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.util.request;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface RequestContext {

    public <T> AsyncCallback<T> callback();

    public <T> AsyncCallback<T> callback(SuccessCallback<T> callback);

    public <T> AsyncCallback<T> callback(AsyncCallback<T> callback);

    public void defer(int delayMs, Runnable action);
}
