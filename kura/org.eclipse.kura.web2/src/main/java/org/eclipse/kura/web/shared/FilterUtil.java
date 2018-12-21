/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared;

import java.util.Iterator;

public final class FilterUtil {

    private FilterUtil() {
    }

    public static String getPidFilter(final Iterator<String> pids) {
        if (!pids.hasNext()) {
            return "()";
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
