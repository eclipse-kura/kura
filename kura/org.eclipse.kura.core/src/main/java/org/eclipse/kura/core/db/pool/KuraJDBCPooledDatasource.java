package org.eclipse.kura.core.db.pool;

import java.sql.SQLException;
import java.util.Properties;

import javax.sql.CommonDataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.hsqldb.jdbc.JDBCCommonDataSource;
import org.hsqldb.jdbc.JDBCConnection;
import org.hsqldb.jdbc.JDBCDriver;
import org.hsqldb.jdbc.pool.JDBCPooledConnection;

@SuppressWarnings("serial")
public class KuraJDBCPooledDatasource extends JDBCCommonDataSource implements ConnectionPoolDataSource, CommonDataSource {

	@Override
	public PooledConnection getPooledConnection() throws SQLException {
		JDBCConnection connection = (JDBCConnection) JDBCDriver.getConnection(url, connectionProps);

		return new JDBCPooledConnection(connection);
	}

	@Override
	public PooledConnection getPooledConnection(String user, String password) throws SQLException {
		Properties props = new Properties();

		props.setProperty("user", user);
		props.setProperty("password", password);

		JDBCConnection connection = (JDBCConnection) JDBCDriver.getConnection(url, props);

		return new JDBCPooledConnection(connection);
	}

	
}
