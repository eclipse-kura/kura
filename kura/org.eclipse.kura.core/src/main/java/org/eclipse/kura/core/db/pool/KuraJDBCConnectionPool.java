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
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.sql.StatementEvent;
import javax.sql.StatementEventListener;

public class KuraJDBCConnectionPool implements ConnectionEventListener, StatementEventListener {

    interface RefState {

        int empty = 0;
        int available = 1;
        int allocated = 2;
    }

    AtomicIntegerArray states;
    PooledConnection[] connections;
    KuraJDBCPooledDatasource source;
    volatile boolean closed;

    public KuraJDBCConnectionPool() {
        this(10);
    }

    public KuraJDBCConnectionPool(int size) {
        this.source = new KuraJDBCPooledDatasource();
        this.connections = new PooledConnection[size];
        this.states = new AtomicIntegerArray(size);
    }

    public Connection getConnection() throws SQLException {

        int retries = 300;

        if (this.source.getLoginTimeout() != 0) {
            retries = this.source.getLoginTimeout() * 10;
        }

        if (this.closed) {
            throw new SQLException("connection pool is closed");
        }

        for (int count = 0; count < retries; count++) {
            for (int i = 0; i < this.states.length(); i++) {
                if (this.states.compareAndSet(i, RefState.available, RefState.allocated)) {
                    return this.connections[i].getConnection();
                }

                if (this.states.compareAndSet(i, RefState.empty, RefState.allocated)) {
                    try {
                        PooledConnection connection = this.source.getPooledConnection();

                        connection.addConnectionEventListener(this);
                        connection.addStatementEventListener(this);
                        this.connections[i] = connection;

                        return this.connections[i].getConnection();
                    } catch (SQLException e) {
                        this.states.set(i, RefState.empty);
                    }
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        throw new SQLException("Invalid argument!");
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return this.source.getPooledConnection(username, password).getConnection();
    }

    @Override
    public void connectionClosed(ConnectionEvent event) {
        PooledConnection connection = (PooledConnection) event.getSource();

        for (int i = 0; i < this.connections.length; i++) {
            if (this.connections[i] == connection) {
                this.states.set(i, RefState.available);

                break;
            }
        }
    }

    @Override
    public void connectionErrorOccurred(ConnectionEvent event) {
        PooledConnection connection = (PooledConnection) event.getSource();

        for (int i = 0; i < this.connections.length; i++) {
            if (this.connections[i] == connection) {
                this.states.set(i, RefState.allocated);
                this.connections[i] = null;
                this.states.set(i, RefState.empty);
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
        return this.source.getUrl();
    }

    public String getUser() {
        return this.source.getUser();
    }

    public void setUrl(String url) {
        this.source.setUrl(url);
    }

    public void setPassword(String password) {
        this.source.setPassword(password);
    }

    public void setUser(String user) {
        this.source.setUser(user);
    }

    public void close(int wait) throws SQLException {

        if (wait < 0 || wait > 60) {
            throw new SQLException("Out of range!");
        }
        if (this.closed) {
            return;
        }

        this.closed = true;

        try {
            Thread.sleep(1000 * wait);
        } catch (Throwable t) {
        }

        for (PooledConnection connection : this.connections) {
            if (connection != null) {
                KuraPooledConnectionManager.releaseConnection(connection);
            }
        }

        for (int i = 0; i < this.connections.length; i++) {
            this.connections[i] = null;
        }

    }

}
