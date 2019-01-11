/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.web.client.util.request;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface RequestContext {

    public <T> AsyncCallback<T> callback();

    public <T> AsyncCallback<T> callback(SuccessCallback<T> callback);

    public <T> AsyncCallback<T> callback(AsyncCallback<T> callback);

    public void defer(int delayMs, Runnable action);
}
