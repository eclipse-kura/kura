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
