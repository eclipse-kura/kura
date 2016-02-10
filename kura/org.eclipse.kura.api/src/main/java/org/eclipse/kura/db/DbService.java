/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DbService offers APIs to acquire and use a JDBC Connection to the embedded SQL database running in the framework. 
 * The configuration of the DbService will determine the configuration of the embedded SQL database.
 * The usage of API is typical for JDBC Connections; the connection is first acquired with getConnection(),
 * and it must be released when the operation is completed with close(). The implementation of the
 * DbService and the returned JdbcConnection will manage the concurrent access into the database appropriately. 
 */
public interface DbService 
{
	/**
	 * Returns the JDBC Connection to be used to communicate with the embedded SQL database.
	 * For each acquired connection, the DbService.close() method must be called. 
	 * @return Connection to be used
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException;

	/**
	 * Releases a previously acquired JDCB connection to the DbService.
	 * @param conn to be released
	 */
	public void close(Connection conn);

	/**
	 * Utility method to silently rollback a JDBC Connection without throwing SQLExcepton.
	 * The method will catch any SQLExcepton thrown and log it.
	 */	
	public void rollback(Connection conn);
		
	/**
	 * Utility method to silently close a JDBC ResultSet without throwing SQLExcepton.
	 * The method will catch any SQLExcepton thrown and log it.
	 */	
	public void close(ResultSet... rss);
	
	/**
	 * Utility method to silently close a JDBC Statement without throwing SQLExcepton.
	 * The method will catch any SQLExcepton thrown and log it.
	 */	
	public void close(Statement... stmts);
}
