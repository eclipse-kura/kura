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
import java.util.Arrays;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.WireMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

/**
 * The Class DbServiceHelper is responsible for providing {@link DbService}
 * instance dependent helper methods for quick database related operations
 */
final class DbServiceHelper {

	/** Escaper Builder to sanitize SQL table and column names. */
	private static Escapers.Builder s_builder = Escapers.builder();

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DbServiceHelper.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The dependent DB service instance. */
	private final DbService m_dbService;

	/**
	 * Instantiates a new DB Service Helper.
	 *
	 * @param dbService
	 *            the DB service
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	private DbServiceHelper(final DbService dbService) {
		checkNull(dbService, s_message.dbServiceNonNull());
		this.m_dbService = dbService;
	}

	/**
	 * Close the connection instance.
	 *
	 * @param conn
	 *            the connection instance to be closed
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	void close(final Connection conn) {
		checkNull(conn, s_message.connectionNonNull());
		s_logger.debug(s_message.closingConnection() + conn);
		this.m_dbService.close(conn);
		s_logger.debug(s_message.closingConnectionDone());
	}

	/**
	 * close the result sets.
	 *
	 * @param rss
	 *            the result sets
	 */
	void close(final ResultSet... rss) {
		s_logger.debug(s_message.closingResultSet() + Arrays.toString(rss));
		this.m_dbService.close(rss);
		s_logger.debug(s_message.closingResultSetDone());
	}

	/**
	 * Close the SQL statements.
	 *
	 * @param stmts
	 *            the SQL statements
	 */
	void close(final Statement... stmts) {
		s_logger.debug(s_message.closingStatement() + Arrays.toString(stmts));
		this.m_dbService.close(stmts);
		s_logger.debug(s_message.closingStatementDone());
	}

	/**
	 * Executes the provided SQL query.
	 *
	 * @param sql
	 *            the SQL query to execute
	 * @param params
	 *            the extra parameters needed for the query
	 * @throws SQLException
	 *             the SQL exception
	 * @throws KuraRuntimeException
	 *             if SQL query argument is null
	 */
	synchronized void execute(final String sql, final Integer... params) throws SQLException {
		checkNull(sql, s_message.sqlQueryNonNull());
		s_logger.debug(s_message.execSql() + sql);
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
		s_logger.debug(s_message.execSqlDone());
	}

	/**
	 * Gets the connection.
	 *
	 * @return the connection instance
	 * @throws SQLException
	 *             the SQL exception
	 */
	Connection getConnection() throws SQLException {
		return this.m_dbService.getConnection();
	}

	/**
	 * Rollback the connection.
	 *
	 * @param conn
	 *            the connection instance
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	void rollback(final Connection conn) {
		checkNull(conn, s_message.connectionNonNull());
		s_logger.debug(s_message.rollback() + conn);
		this.m_dbService.rollback(conn);
		s_logger.debug(s_message.rollbackDone());
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
	 *            the string to be sanitized
	 * @return the escaped string
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	String sanitizeSqlTableAndColumnName(final String string) {
		checkNull(string, s_message.stringNonNull());
		s_logger.debug(s_message.sanitize() + string);
		final Escaper escaper = s_builder.addEscape('\'', "_").addEscape('"', "_").addEscape('\\', "")
				.addEscape('.', "_").addEscape(' ', "_").build();
		return escaper.escape(string).toLowerCase();
	}
	
	/**
	 * Gets the single instance of DbUtils.
	 *
	 * @param dbService
	 *            the db service
	 * @return single instance of DbUtils
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	static DbServiceHelper getInstance(final DbService dbService) {
		return new DbServiceHelper(dbService);
	}
}
