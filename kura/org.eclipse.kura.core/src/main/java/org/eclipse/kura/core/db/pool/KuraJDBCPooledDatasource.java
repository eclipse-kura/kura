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

import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.CommonDataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

public class KuraJDBCPooledDatasource implements ConnectionPoolDataSource, CommonDataSource {

    protected transient PrintWriter printWriter;
    protected String url;
    protected int loginTimeout;
    protected String user;
    protected char[] password;
    protected Properties connectionProps = new Properties();

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        return KuraPooledConnectionManager.getPooledConnection(this.url, this.connectionProps);
    }

    @Override
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {
        Properties props = new Properties();

        props.setProperty("user", user);
        props.setProperty("password", password);

        return KuraPooledConnectionManager.getPooledConnection(this.url, props);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.printWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.printWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return this.loginTimeout;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Not supported!");
    }

    public String getUrl() {
        return this.url;
    }

    public String getUser() {
        return this.user;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPassword(String password) {

        this.password = password.toCharArray();

        this.connectionProps.setProperty("password", password);
    }

    public void setUser(String user) {

        this.user = user;

        this.connectionProps.setProperty("user", user);
    }

    public void setProperties(Properties props) {

        this.connectionProps = props == null ? new Properties() : (Properties) props.clone();

        if (this.user != null) {
            props.setProperty("user", this.user);
        }

        if (this.password != null) {
            props.setProperty("password", new String(this.password));
        }

        if (this.loginTimeout != 0) {
            props.setProperty("loginTimeout", Integer.toString(this.loginTimeout));
        }
    }

}
