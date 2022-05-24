/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.h2db.common;

import static java.util.Objects.requireNonNull;

import java.sql.SQLException;

import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.wire.basedb.common.BaseDbServiceHelper;

/**
 * The Class DbServiceHelper is responsible for providing {@link H2DbService}
 * instance dependent helper methods for quick database related operations
 */
public final class H2DbServiceHelper extends BaseDbServiceHelper {

    /**
     * Instantiates a new DB Service Helper.
     *
     * @param dbService
     *            the DB service
     * @throws NullPointerException
     *             if argument is null
     */
    protected H2DbServiceHelper(final H2DbService dbService) {
        super(dbService);
        requireNonNull(dbService, "DB Service cannot be null");
    }

    public <T> T withConnection(final H2DbService.ConnectionCallable<T> callable) throws SQLException {
        return ((H2DbService) this.dbService).withConnection(callable);
    }

}
