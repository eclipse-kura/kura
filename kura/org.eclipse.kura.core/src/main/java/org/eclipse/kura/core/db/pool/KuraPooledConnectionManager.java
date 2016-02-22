/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.db.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.PooledConnection;

import org.hsqldb.jdbc.JDBCConnection;
import org.hsqldb.jdbc.pool.JDBCPooledConnection;

public class KuraPooledConnectionManager {
 
	public static PooledConnection getPooledConnection(String url, Properties props) throws SQLException{
		
		Connection connection = DriverManager.getConnection(url, props);
		
		if(connection instanceof JDBCConnection){
			return new JDBCPooledConnection((JDBCConnection)connection);
		}
		
		throw new SQLException();
	}
	
	public static void releaseConnection(PooledConnection connection){
		if(connection instanceof JDBCPooledConnection){
			((JDBCPooledConnection)connection).release();
		}
	}
	
}
