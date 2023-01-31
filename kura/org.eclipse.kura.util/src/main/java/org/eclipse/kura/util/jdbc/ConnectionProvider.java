/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.util.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.kura.KuraStoreException;

public interface ConnectionProvider {
    public <T> T withConnection(final SQLFunction<Connection, T> task) throws SQLException;

    public default <T> T withConnection(final SQLFunction<Connection, T> task, final String message)
            throws KuraStoreException {
        try {
            return this.withConnection(task);
        } catch (final Exception e) {
            throw new KuraStoreException(e, message);
        }
    }
}