package org.eclipse.kura.core.db.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.sql.StatementEvent;
import javax.sql.StatementEventListener;

import org.hsqldb.jdbc.JDBCUtil;
import org.hsqldb.jdbc.pool.JDBCPooledConnection;

public class KuraJDBCConnectionPool implements ConnectionEventListener, StatementEventListener {

	interface RefState {
		int empty = 0;
		int available = 1;
		int allocated = 2;
	}

	AtomicIntegerArray states;
	JDBCPooledConnection[] connections;
	KuraJDBCPooledDatasource source;
	volatile boolean closed;

	public KuraJDBCConnectionPool() {
		this(10);
	}

	public KuraJDBCConnectionPool(int size) {
		source = new KuraJDBCPooledDatasource();
		connections = new JDBCPooledConnection[size];
		states = new AtomicIntegerArray(size);
	}

	public Connection getConnection() throws SQLException {

		int retries = 300;

		if (source.getLoginTimeout() != 0) {
			retries = source.getLoginTimeout() * 10;
		}

		if (closed) {
			throw new SQLException("connection pool is closed");
		}

		for (int count = 0; count < retries; count++) {
			for (int i = 0; i < states.length(); i++) {
				if (states.compareAndSet(i, RefState.available, RefState.allocated)) {
					return connections[i].getConnection();
				}

				if (states.compareAndSet(i, RefState.empty, RefState.allocated)) {
					try {
						JDBCPooledConnection connection = (JDBCPooledConnection) source.getPooledConnection();

						connection.addConnectionEventListener(this);
						connection.addStatementEventListener(this);
						connections[i] = connection;

						return connections[i].getConnection();
					} catch (SQLException e) {
						states.set(i, RefState.empty);
					}
				}
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		throw JDBCUtil.invalidArgument();
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return source.getPooledConnection(username, password).getConnection();
	}

	@Override
	public void connectionClosed(ConnectionEvent event) {
		PooledConnection connection = (PooledConnection) event.getSource();

		for (int i = 0; i < connections.length; i++) {
			if (connections[i] == connection) {
				states.set(i, RefState.available);

				break;
			}
		}
	}

	@Override
	public void connectionErrorOccurred(ConnectionEvent event) {
		PooledConnection connection = (PooledConnection) event.getSource();

		for (int i = 0; i < connections.length; i++) {
			if (connections[i] == connection) {
				states.set(i, RefState.allocated);
				connections[i] = null;
				states.set(i, RefState.empty);
				break;
			}
		}
	}

	@Override
	public void statementClosed(StatementEvent event) {
	}

	@Override
	public void statementErrorOccurred(StatementEvent event) {
	}

	public String getUrl() {
		return source.getUrl();
	}

	public String getUser() {
		return source.getUser();
	}

	public void setUrl(String url) {
		source.setUrl(url);
	}

	public void setPassword(String password) {
		source.setPassword(password);
	}

	public void setUser(String user) {
		source.setUser(user);
	}

	public void close(int wait) throws SQLException {

		if (wait < 0 || wait > 60) {
			throw JDBCUtil.outOfRangeArgument();
		}
		if (closed) {
			return;
		}

		closed = true;

		try {
			Thread.sleep(1000 * wait);
		} catch (Throwable t) {
		}

		for (int i = 0; i < connections.length; i++) {
			if (connections[i] != null) {
				connections[i].release();
			}
		}

		for (int i = 0; i < connections.length; i++) {
			connections[i] = null;
		}

	}

}
