/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.web.server.util.ServiceLocator;


public class AuthenticationManager 
{
	private static final AuthenticationManager s_instance = new AuthenticationManager();
	
	private boolean   m_inited;
	private DbService m_dbService;
	
	private AuthenticationManager() {
		m_inited = false;
	}

	/**
	 * Returns the singleton instance of AuthenticationManager. 
	 * @return AuthenticationManager
	 */
	public static AuthenticationManager getInstance() {
		return s_instance;
	}


	public synchronized void init(DbService dbService) 
		throws SQLException 
	{
		if (!s_instance.m_inited) {
			s_instance.m_dbService = dbService;
			s_instance.initUserStore();
			s_instance.m_inited = true;
		}
	}

	
	public boolean authenticate(String username, String password)
		throws SQLException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {			
			
			CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
			conn = m_dbService.getConnection();
			stmt = conn.prepareStatement("SELECT username FROM dn_user WHERE username = ? AND password = ?;");
			stmt.setString(1, username);
			stmt.setString(2, cryptoService.sha1Hash(password));
			
			rs = stmt.executeQuery();
			if (rs != null && rs.next()) {
				return true;
			}			
		}
		catch (SQLException e) {
			throw e;
		}
		catch (Exception e) {
			throw new SQLException(e);
		}
		finally {
			m_dbService.close(rs);
			m_dbService.close(stmt);
			m_dbService.close(conn);
		}
		
		return false;
	}
	
	
	public void changeAdminPassword(String newPassword)
		throws SQLException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
			conn = m_dbService.getConnection();
			stmt = conn.prepareStatement("UPDATE dn_user SET password = ? WHERE username = ?;");
			stmt.setString(1, cryptoService.sha1Hash(newPassword));
			stmt.setString(2, "admin");
			
			stmt.execute();
			conn.commit();
		}
		catch (SQLException e) {
			throw e;
		}
		catch (Exception e) {
			m_dbService.rollback(conn);
			throw new SQLException(e);
		}
		finally {
			m_dbService.close(stmt);
			m_dbService.close(conn);
		}
	}
	
	
	// -------------------------------------------------
	//
	//    Private methods
	//
	// -------------------------------------------------
	
	private synchronized void initUserStore() 
		throws SQLException
	{
		execute("CREATE TABLE IF NOT EXISTS dn_user (username VARCHAR(255) PRIMARY KEY, password  VARCHAR(255) NOT NULL);");
		checkAdminUser();
	}


    private synchronized void checkAdminUser() throws SQLException 
    {
    	boolean bAdminExists = false;
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {			
			
			conn = m_dbService.getConnection();
			stmt = conn.prepareStatement("SELECT username FROM dn_user WHERE username = ?;");
			stmt.setString(1, "admin");
			rs = stmt.executeQuery();

			if (rs != null && rs.next()) {
				bAdminExists = true;
			}
		}
		catch (SQLException e) {
			throw e;
		}
		finally {
			m_dbService.close(rs);
			m_dbService.close(stmt);
			m_dbService.close(conn);
		}		

		if (!bAdminExists) {
			createAdminUser();
		}
    }


    private synchronized void createAdminUser() 
    	throws SQLException 
    {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
			conn = m_dbService.getConnection();
			stmt = conn.prepareStatement("INSERT INTO dn_user (username, password) VALUES (?, ?);");
			stmt.setString(1, "admin");
			stmt.setString(2, cryptoService.sha1Hash("admin"));
			
			stmt.execute();
		}
		catch (SQLException e) {
			throw e;
		}
		catch (Exception e) {
			throw new SQLException(e);
		}
		finally {
			m_dbService.close(stmt);
			m_dbService.close(conn);
		}
    }
    
    
    private synchronized void execute(String sql) throws SQLException 
    {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			
			conn = m_dbService.getConnection();
			stmt = conn.prepareStatement(sql);
			stmt.execute();
			conn.commit();
		}
		catch (SQLException e) {
			m_dbService.rollback(conn);
			throw e;
		}
		finally {
			m_dbService.close(stmt);
			m_dbService.close(conn);
		}
    }
}
