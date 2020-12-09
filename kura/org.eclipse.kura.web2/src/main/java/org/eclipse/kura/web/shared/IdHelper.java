/*******************************************************************************
 * Copyright (c) 2018, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.web.shared;

public final class IdHelper {

    private IdHelper() {
    }

    /**
     * Get the last component of an OSGi ID.
     * 
     * @param pid
     *            The ID, may be {@code null}.
     * @return The last component or {@code null} if the input was {@code null}.
     */
    public static String getLastIdComponent(final String pid) {
        if (pid == null)
            return null;

        final String[] toks = pid.split("\\.");
        if (toks.length > 1) {
            return toks[toks.length - 1];
        } else {
            return toks[0];
        }
    }

}
