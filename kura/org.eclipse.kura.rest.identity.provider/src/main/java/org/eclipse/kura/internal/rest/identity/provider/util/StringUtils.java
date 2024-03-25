/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.identity.provider.util;

public class StringUtils {

    private StringUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void requireNotEmpty(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
