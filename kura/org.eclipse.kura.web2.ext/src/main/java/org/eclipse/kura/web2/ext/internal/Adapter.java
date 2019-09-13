/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web2.ext.internal;

import com.google.gwt.core.client.JavaScriptObject;

public interface Adapter<T> {

    public default JavaScriptObject adaptNullable(T value) {
        return value == null ? null : adaptNonNull(value);
    }

    public default T adaptNullable(final JavaScriptObject value) {
        return value == null ? null : adaptNonNull(value);
    }

    public JavaScriptObject adaptNonNull(T value);

    public T adaptNonNull(final JavaScriptObject value);

}
