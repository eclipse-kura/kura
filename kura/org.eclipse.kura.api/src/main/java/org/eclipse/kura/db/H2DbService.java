/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A {@link H2DbService} instance provides an implementation of {@link BaseDbService} using the H2 database engine.
 *
 * The Kura core implementation of {@link H2DbService} provides the capability to perform periodic database
 * defragmentation.
 * Since H2 currently does not support online defragmentation, the database needs to be shut down to perform the
 * operation.
 *
 * Running the defragmentation will cause the existing connections obtained using the
 * {@link H2DbService#getConnection()} method to be closed, so applications must be prepared to reopen connections if
 * necessary.
 *
 * As an alternative, it is possible to use the {@link H2DbService#withConnection(ConnectionCallable)} method.
 *
 *
 * @since 1.3
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface H2DbService extends BaseDbService {

    public static final String DEFAULT_INSTANCE_PID = "org.eclipse.kura.db.H2DbService";

    /**
     * Executes the provided {@link ConnectionCallable} task on the current thread, and returns the result.
     * It is not necessary to close the {@link Connection} received as argument. If an exception is thrown by the task,
     * the connection will be rolled back automatically.
     *
     * This method guarantees that the execution of the provided task will not be affected by the defragmentation
     * process.
     * Performing long running operations in the provided tasks might delay the defragmentation
     * process.
     *
     * @param task
     *            the task to be executed.
     * @return the result of the executed task.
     * @throws SQLException
     *             if the provided task throws a {@link SQLException}.
     * @since 2.0
     */
    public <T> T withConnection(ConnectionCallable<T> task) throws SQLException;

    /**
     * Represents a task that can be executed using the {@link H2DbService#withConnection(ConnectionCallable)} method.
     *
     * @param <T>
     *            The return type of the task.
     * @since 2.0
     */
    @FunctionalInterface
    public interface ConnectionCallable<T> {

        public T call(Connection connection) throws SQLException;
    }
}
