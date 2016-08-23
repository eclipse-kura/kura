/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.cloud.factory.internal;

import java.util.Map;

public final class Properties {
    private Properties() {
    }

    public static String asString(Map<String, ?> properties, String propertyName) {
        return asString(properties.get(propertyName));
    }
    
    public static String asString(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public static Integer asInteger(Map<String, ?> properties, String propertyName) {
        return asInteger(properties.get(propertyName));
    }

    public static Integer asInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}
