/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.wire.store;

import static org.eclipse.kura.Preconditions.checkNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.db.DbService;

import com.google.common.base.Throwables;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

/**
 * The Class DbServiceHelper is responsible for providing {@link DbService}
 * instance dependent helper methods for quick database related operations
 */
public final class DbServiceHelper {

	/**
	 * SQL Escaper Builder to escape characters to sanitize SQL table and column
	 * names.
	 */
	private static Escapers.Builder builder = Escapers.builder();

	/**
	 * Gets the single instance of DbUtils.
	 *
	 * @param dbService
	 *            the db service
	 * @return single instance of DbUtils
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	public static DbServiceHelper getInstance(final DbService dbService) {
		return new DbServiceHelper(dbService);
	}

	/** The dependent DB service instance. */
	public DbService s_DbService;

	/**
	 * Instantiates a new DB utils.
	 *
	 * @param dbService
	 *            the DB service
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	public DbServiceHelper(final DbService dbService) {
		checkNull(dbService, "Db Service cannot be null");
		this.s_DbService = dbService;
	}

	/**
	 * Close the connection instance.
	 *
	 * @param conn
	 *            the connection instance to be closed
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	public void close(final Connection conn) {
		checkNull(conn, "Connection instance cannnot be null");
		this.s_DbService.close(conn);
	}

	/**
	 * close the result sets.
	 *
	 * @param rss
	 *            the result sets
	 */
	public void close(final ResultSet... rss) {
		this.s_DbService.close(rss);
	}

	/**
	 * Close the SQL statements.
	 *
	 * @param stmts
	 *            the SQL statements
	 */
	public void close(final Statement... stmts) {
		this.s_DbService.close(stmts);
	}

	/**
	 * Executes the provided SQL query.
	 *
	 * @param sql
	 *            the SQL query to execute
	 * @param params
	 *            the params extra parameters needed for the query
	 * @throws SQLException
	 *             the SQL exception
	 * @throws KuraRuntimeException
	 *             if SQL query argument is null
	 */
	public synchronized void execute(final String sql, final Integer... params) throws SQLException {
		checkNull(sql, "SQL query cannot be null");
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = this.getConnection();
			stmt = conn.prepareStatement(sql);
			for (int i = 0; i < params.length; i++) {
				stmt.setInt(1 + i, params[i]);
			}
			stmt.execute();
			conn.commit();
		} catch (final SQLException e) {
			this.rollback(conn);
			Throwables.propagate(e);
		} finally {
			this.close(stmt);
			this.close(conn);
		}
	}

	/**
	 * Gets the connection.
	 *
	 * @return the connection instance
	 * @throws SQLException
	 *             the SQL exception
	 */
	public Connection getConnection() throws SQLException {
		return this.s_DbService.getConnection();
	}

	/**
	 * Rollback the connection.
	 *
	 * @param conn
	 *            the connection instance
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	public void rollback(final Connection conn) {
		checkNull(conn, "Connection instance cannnot be null");
		this.s_DbService.rollback(conn);
	}

	/**
	 * Perform basic SQL table name and column name validation on input string.
	 * This is to allow safe encoding of parameters that must contain quotes,
	 * while still protecting users from SQL injection on the table names and
	 * column names.
	 *
	 * (' --> '_') (" --> _) (\ --> (remove backslashes)) (. --> _) ( (space)
	 * --> _)
	 *
	 * @param string
	 *            the string to be filtered
	 * @return the escaped string
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	public String sanitizeSqlTableAndColumnName(final String string) {
		checkNull(string, "Provided string cannot be null");
		final Escaper escaper = builder.addEscape('\'', "_").addEscape('"', "_").addEscape('\\', "").addEscape('.', "_")
				.addEscape(' ', "_").build();
		return escaper.escape(string).toLowerCase();
	}
}
