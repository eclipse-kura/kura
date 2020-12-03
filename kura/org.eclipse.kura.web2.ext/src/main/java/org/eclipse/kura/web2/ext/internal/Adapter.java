/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
