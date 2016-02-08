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
package org.eclipse.kura.core.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.eclipse.kura.core.db.pool.KuraJDBCConnectionPool;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HsqlDbServiceImpl implements DbService 
{
	private static Logger s_logger = LoggerFactory.getLogger(HsqlDbServiceImpl.class);	
    static {

        // load the driver
        // Use this way of loading the driver as it is required for OSGi
        // Just loading the class with Class.forName is not sufficient.
		try {
			DriverManager.registerDriver( new org.hsqldb.jdbcDriver());
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
    }	

	private static final String s_username  = "sa";
	private static final String s_password  = "";
	private static final Object s_init_lock = "init lock";
	private static boolean      s_inited    = false;

	@SuppressWarnings("unused")
	private ComponentContext m_ctx;
	private SystemService    m_systemService;
    private KuraJDBCConnectionPool         m_connPool;
    
    
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public void setSystemService(SystemService systemService) {
		this.m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		this.m_systemService = null;
	}


	
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	
	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("activate...");

		//
		// save the bundle context
		m_ctx = componentContext;

		synchronized (s_init_lock) {
			if (!s_inited) {

				// get a connection for this Message Store
				// If this is the first connection, this will result into starting up the database
				Connection conn = null;
				try {									
					conn = getConnection();
				}
				catch (SQLException e) {
					rollback(conn);
					s_logger.error("Error during HsqdbService startup", e);
					throw new ComponentException(e);
				}
				finally {
					close(conn);
				}
		
				// init the database
				// get a connection for this Message Store
				// If this is the first connection, this will result into starting up the database
				try {
					init();
					s_inited = true;
				}
				catch (SQLException e) {
					s_logger.error("Error during HsqdbService init", e);
					throw new ComponentException(e);
				}
			}			
		}
	}
	
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("deactivate...");				
		try {
			execute("SHUTDOWN");
			s_inited = false;
		}			
		catch (SQLException e) {
			s_logger.error("Error during HsqlDbService shutdown", e);
			throw new ComponentException(e);
		}
		
		try {
			if (m_connPool != null) {
				m_connPool.close(0); // no wait
			    m_connPool = null;
			}
		}			
		catch (SQLException e) {
			s_logger.error("Error during HsqlDbService connection close", e);
			throw new ComponentException(e);
		}
	}
	
	

	// ----------------------------------------------------------------
	//
	//   Service APIs
	//
	// ----------------------------------------------------------------
    
	public synchronized Connection getConnection() throws SQLException 
	{	    
	    if (m_connPool == null) {

	    	String url = m_systemService.getProperties().getProperty(SystemService.DB_URL_PROPNAME);	
			s_logger.info("Opening database with url: "+url);			
			    
		    //m_connPool = new JDBCPool();
			m_connPool = new KuraJDBCConnectionPool();
		    m_connPool.setUrl(url);
		    m_connPool.setUser(s_username);
		    m_connPool.setPassword(s_password);
	    }
	    
        Connection conn = null;
	    try {
		    conn = m_connPool.getConnection();
		}
		catch (SQLException e) {
		    s_logger.error("Error getting connection", e);
		    closeSilently();
		    throw e;
		}
		return conn;
	}
	
	public void rollback(Connection conn) {
		try {
			if (conn != null) {
				conn.rollback();
			}
		}
		catch (SQLException e) {
			s_logger.error("Error during Connection rollback.", e);
		}
	}
	
	public void close(ResultSet... rss) {
		if (rss != null) {
			for (ResultSet rs : rss) {
				try {
					if (rs != null) {
						rs.close();
					}
				}
				catch (SQLException e) {
					s_logger.error("Error during ResultSet closing", e);
				}					
			}
		}
	}

	public void close(Statement... stmts) {
		if (stmts != null) {
			for (Statement stmt : stmts) {
				try {
					if (stmt != null) {
						stmt.close();
					}
				}
				catch (SQLException e) {
					s_logger.error("Error during Statement closing", e);
				}					
			}
		}
	}

	public void close(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		}
		catch (SQLException e) {
			s_logger.error("Error during Connection closing", e);
		}
	}

	public boolean isLogDataEnabled() {
		boolean isLogDataEnabled = true;
		String sIsLogDataEnabled = m_systemService.getProperties().getProperty(SystemService.DB_LOG_DATA_PROPNAME);
		
		if (sIsLogDataEnabled != null && !sIsLogDataEnabled.isEmpty()) { 
			isLogDataEnabled = new Boolean(sIsLogDataEnabled);
		}
		
		return isLogDataEnabled;
	}
	
	// ----------------------------------------------------------------
	//
	//   Private methods
	//
	// ----------------------------------------------------------------

	private void init() 
		throws SQLException 
	{
		// concurrency control
		// Switching from concurrency control MVCC to LOCKS (2PL)
		// 2PL will lock the whole table on a write but it makes count(*) extremely fast.
		// As we serialize database access in the DbDataStore, 2PL is a better choice than MVCC.
		execute("SET DATABASE TRANSACTION CONTROL LOCKS");

		// Transaction Level
		execute("SET TRANSACTION READ WRITE, ISOLATION LEVEL READ COMMITTED");

		// set auto-commit
		execute("SET AUTOCOMMIT FALSE");			

		// Sets the write delay_millies property, delay in milliseconds.
		String writeDelayMillies = m_systemService.getProperties().getProperty(SystemService.DB_WRITE_DELAY_MILLIES_PROPNAME);
		if (writeDelayMillies == null || writeDelayMillies.isEmpty()) {
			writeDelayMillies = "500";
		}
		execute("SET FILES WRITE DELAY "+writeDelayMillies+" MILLIS");


		// use cache tables by default as they load only part of the data in mem
		execute("SET DATABASE DEFAULT TABLE TYPE CACHED");

		String cacheRows = m_systemService.getProperties().getProperty(SystemService.DB_CACHE_ROWS_PROPNAME);
		if (cacheRows != null && !cacheRows.isEmpty()) {
			execute("SET FILES CACHE ROWS "+cacheRows);
		}

		String lobScale = m_systemService.getProperties().getProperty(SystemService.DB_LOB_FILE_PROPNAME);
		if (lobScale != null && !lobScale.isEmpty()) {
			execute("SET FILES LOB SCALE "+lobScale);
		}
		
		String defragLimit = m_systemService.getProperties().getProperty(SystemService.DB_DEFRAG_LIMIT_PROPNAME);
		if (defragLimit != null && !defragLimit.isEmpty()) {
			execute("SET FILES DEFRAG "+defragLimit);
		}
	
		String logData = m_systemService.getProperties().getProperty(SystemService.DB_LOG_DATA_PROPNAME);
		if (logData != null && !logData.isEmpty()) {
			execute("SET FILES LOG "+logData.toUpperCase());
		}
		
		String logSize = m_systemService.getProperties().getProperty(SystemService.DB_LOG_SIZE_PROPNAME);
		if (logSize != null && !logSize.isEmpty()) {
			execute("SET FILES LOG SIZE "+logSize);
		}
		
		String useNio = m_systemService.getProperties().getProperty(SystemService.DB_NIO_PROPNAME);
		if (useNio != null && !useNio.isEmpty()) {
			execute("SET FILES NIO "+useNio.toUpperCase());
		}
		
		// Note: an automatic checkpoint is performed every time the DB is started.
		
		// TODO: defrag?
	    			
		// for encryption
//				ResultSet rs = stmt.executeQuery("select CRYPT_KEY('AES', null) from some_table");  
//				String key = rs.next().getString(1);  
//				Store the key in a secure place. Now you can create an encrypted DB like so: 
//				DriverManager.getConnection("jdbc:hsqldb:file:_some_encrypted_db;crypt_key="+key+";crypt_type=AES", "SA", "")
	}
	

	private void execute(String sql) throws SQLException 
    {
		Connection conn = null;
		Statement stmt = null;
		try {			
			conn = getConnection();
			stmt = conn.createStatement();
			stmt.execute(sql);
			conn.commit();
		}
		catch (SQLException e) {
			rollback(conn);
			throw e;
		}
		finally {
			close(stmt);
			close(conn);
		}
    }
	
	
	private void closeSilently()
	{
	    try {
	        if (m_connPool != null) {
	            m_connPool.close(0);
	        }
        }
	    catch (Exception e) {
            s_logger.warn("Error during HsqlDbService connection close", e);	        
	    }
	    finally {
            m_connPool = null;
	    }
	}
}
