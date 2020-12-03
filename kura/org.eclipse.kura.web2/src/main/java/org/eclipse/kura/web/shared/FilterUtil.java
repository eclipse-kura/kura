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
package org.eclipse.kura.web.shared;

import java.util.Iterator;

public final class FilterUtil {

    private FilterUtil() {
    }

    public static String getPidFilter(final Iterator<String> pids) {
        if (!pids.hasNext()) {
            throw new IllegalArgumentException("pids list must be non empty");
        }
        final StringBuilder builder = new StringBuilder();
        builder.append("(|");
        while (pids.hasNext()) {
            final String pid = pids.next();
            builder.append("(kura.service.pid=");
            builder.append(pid);
            builder.append(")");
        }
        builder.append(")");
        return builder.toString();
    }
}
