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
package org.eclipse.kura.core.data.store;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraStoreCapacityReachedException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.core.data.DataMessage;
import org.eclipse.kura.core.data.DataStore;
import org.eclipse.kura.core.db.HsqlDbServiceImpl;
import org.eclipse.kura.db.DbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the DataStore which stores messages into an embedded HSQLDB instance.
 * FIXME: reset identity (see below, not working) on sequence limit exceed exception.
 */
public class DbDataStore implements DataStore
{
	private static final Logger s_logger = LoggerFactory.getLogger(DbDataStore.class);
	
    private DbService            m_dbService;
    private Calendar             m_utcCalendar;
    private ScheduledExecutorService m_houseKeeperExecutor;
    private ScheduledFuture<?>   m_houseKeeperTask;
    private int m_capacity;
    
    private String m_table;
    
    private String m_sqlCreateTable;
    private String m_sqlDropIndex;
    private String m_sqlCreateIndex;
    private String m_sqlMessageCount;
    private String m_sqlResetId;
    private String m_sqlStore;
    private String m_sqlGetMessage;
    private String m_sqlGetNextMessage;
    private String m_sqlSetPublished;
    private String m_sqlSetPublished2;
    private String m_sqlSetConfirmed;
    private String m_sqlAllUnpublishedMessages;
    private String m_sqlAllInFlightMessages;
    private String m_sqlAllDroppedInFlightMessages;
    private String m_sqlUnpublishAllInFlightMessages;
    private String m_sqlDropAllInFlightMessages;
    private String m_sqlDeleteDroppedMessages;
    private String m_sqlDeleteDroppedMessages2;
    private String m_sqlDeleteConfirmedMessages;
    private String m_sqlDeleteConfirmedMessages2;
    private String m_sqlDeletePublishedMessages;
    private String m_sqlDeletePublishedMessages2;
    private String m_sqlDuplicateCount;
    private String m_sqlDropPrimaryKey;
    private String m_sqlDeleteDuplicates;
    private String m_sqlCreatePrimaryKey;
    
    // package level constructor to be invoked only by the factory
    public DbDataStore(String table) {
    	// do not make this static as it may not be thread safe
    	m_utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    	
        m_table = table;
        
        m_sqlCreateTable = "CREATE TABLE IF NOT EXISTS "+m_table+" (id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, topic VARCHAR(32767 CHARACTERS), qos INTEGER, retain BOOLEAN, createdOn TIMESTAMP, publishedOn TIMESTAMP, publishedMessageId INTEGER, confirmedOn TIMESTAMP, payload VARBINARY(16777216), priority INTEGER, sessionId VARCHAR(32767 CHARACTERS), droppedOn TIMESTAMP);";
        m_sqlDropIndex = "DROP INDEX IF EXISTS "+m_table+"_publishedOn;";
        m_sqlCreateIndex = "CREATE INDEX "+m_table+"_nextMsg ON "+m_table+" (priority ASC, createdOn ASC, publishedOn, qos);";
        m_sqlMessageCount = "SELECT COUNT(*) FROM "+m_table+";";
        m_sqlResetId = "ALTER TABLE "+m_table+" ALTER COLUMN id RESTART WITH 0;";
        m_sqlStore = "INSERT INTO "+m_table+" (topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, payload, priority, sessionId, droppedOn) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        m_sqlGetMessage = "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, payload, priority, sessionId, droppedOn FROM "+m_table+" WHERE id = ?";
        m_sqlGetNextMessage = "SELECT d.id, d.topic, d.qos, d.retain, d.createdOn, d.publishedOn, d.publishedMessageId, d.confirmedOn, d.payload, d.priority, d.sessionId, d.droppedOn FROM (SELECT id FROM "+m_table+" WHERE publishedOn IS NULL ORDER BY priority ASC, createdOn ASC LIMIT 1 USING INDEX) a, "+m_table+" d WHERE a.id = d.id;";
        m_sqlSetPublished = "UPDATE "+m_table+" SET publishedOn = ?, publishedMessageId = ?, sessionId = ? WHERE id = ?;";
        m_sqlSetPublished2 = "UPDATE "+m_table+" SET publishedOn = ? WHERE id = ?;";
        m_sqlSetConfirmed = "UPDATE "+m_table+" SET confirmedOn = ? WHERE id = ?;";
        m_sqlAllUnpublishedMessages = "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, priority, sessionId, droppedOn FROM "+m_table+" WHERE publishedOn IS NULL ORDER BY priority ASC, createdOn ASC;";
        m_sqlAllInFlightMessages = "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, priority, sessionId, droppedOn FROM "+m_table+" WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL AND droppedOn IS NULL ORDER BY priority ASC, createdOn ASC;";
        m_sqlAllDroppedInFlightMessages = "SELECT id, topic, qos, retain, createdOn, publishedOn, publishedMessageId, confirmedOn, priority, sessionId, droppedOn FROM "+m_table+" WHERE droppedOn IS NOT NULL ORDER BY priority ASC, createdOn ASC;";
        m_sqlUnpublishAllInFlightMessages = "UPDATE "+m_table+" SET publishedOn = NULL WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;";
        m_sqlDropAllInFlightMessages = "UPDATE "+m_table+" SET droppedOn = ? WHERE publishedOn IS NOT NULL AND qos > 0 AND confirmedOn IS NULL;";
        m_sqlDeleteDroppedMessages = "DELETE FROM "+m_table+" WHERE DATEDIFF('ss', droppedOn, ?) > ? AND droppedOn IS NOT NULL;";
        m_sqlDeleteDroppedMessages2 = "DELETE FROM "+m_table+" WHERE DATEDIFF('yy', droppedOn, ?) > ? AND droppedOn IS NOT NULL;";
        m_sqlDeleteConfirmedMessages = "DELETE FROM "+m_table+" WHERE DATEDIFF('ss', confirmedOn, ?) > ? AND confirmedOn IS NOT NULL;";
        m_sqlDeleteConfirmedMessages2 = "DELETE FROM "+m_table+" WHERE DATEDIFF('yy', confirmedOn, ?) > ? AND confirmedOn IS NOT NULL;";
        m_sqlDeletePublishedMessages = "DELETE FROM "+m_table+" WHERE qos = 0 AND DATEDIFF('ss', publishedOn, ?) > ? AND publishedOn IS NOT NULL;";
        m_sqlDeletePublishedMessages2 = "DELETE FROM "+m_table+" WHERE qos = 0 AND DATEDIFF('yy', publishedOn, ?) > ? AND publishedOn IS NOT NULL;";
        m_sqlDuplicateCount = "SELECT count(*) FROM (SELECT id, COUNT(id) FROM "+m_table+" GROUP BY id HAVING (COUNT(id) > 1)) dups;";
        m_sqlDropPrimaryKey = "ALTER TABLE "+m_table+" DROP PRIMARY KEY;";
        m_sqlDeleteDuplicates = "DELETE FROM "+m_table+" WHERE id IN (SELECT id FROM "+m_table+" GROUP BY id HAVING COUNT(*) > 1);";
        m_sqlCreatePrimaryKey = "ALTER TABLE "+m_table+" ADD PRIMARY KEY (id);";
    }
    
    // ----------------------------------------------------------
    //
    //    Start/Stop, ServiceId  
    //
    // ----------------------------------------------------------
   
    public synchronized void start(DbService dbService, int houseKeeperInterval, int purgeAge, int capacity) throws KuraStoreException
    {
    	m_dbService = dbService;
    	
    	m_houseKeeperExecutor = Executors.newSingleThreadScheduledExecutor();
    	    	
    	//
    	// Set up the schema tables required by the DataStore
    	init(houseKeeperInterval, purgeAge, capacity);
    }

	private void init(int houseKeeperInterval, int purgeAge, int capacity)
		throws KuraStoreException 
	{
		// create the MESSAGES table
		// Note that the HSQLDB will throw an sequence limit exceeded exception when the sequence generator reaches the value 2147483647 + 1.
		execute(m_sqlCreateTable);


		// From version 2.0.4, the index ds_messages_publishedOn is replaced with ds_messages_nextMsg
		// So, drop it on startup if it exists.
		execute(m_sqlDropIndex);

		// Introduced in 2.0.4, create index for ds_messages
		try {
			execute(m_sqlCreateIndex);
		}
		catch (KuraStoreException e) {
			boolean handled = false;
			if (e.getCause() != null && e.getCause() instanceof SQLException) {
				SQLException sqle = (SQLException) e.getCause();
				if (sqle.getErrorCode() == -5504) {
					// Object already exist. We can ignore it
					handled = true;
				}
			}
			if (!handled) {
				throw e;
			}
		}

		// Test.
		// Initialize the sequence generator with 2147483647. This throws a sequence limit exceed exception on the second INSERT.
		//execute("CREATE TABLE IF NOT EXISTS ds_messages (id INTEGER GENERATED ALWAYS AS IDENTITY (START WITH 2147483647) PRIMARY KEY, topic VARCHAR(32767 CHARACTERS), qos INTEGER, retain BOOLEAN, createdOn TIMESTAMP, publishedOn TIMESTAMP, publishedMessageId INTEGER, confirmedOn TIMESTAMP, payload BLOB(256M), priority INTEGER, sessionId VARCHAR(32767 CHARACTERS), droppedOn TIMESTAMP);");
		
		// Test (note the 'BY DEFAULT' clause instead of 'ALWAYS').
		// Initialize the sequence generator with 2147483647. This throws a sequence limit exceed exception on the second INSERT.
		//execute("CREATE TABLE IF NOT EXISTS ds_messages (id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 2147483647) PRIMARY KEY, topic VARCHAR(32767 CHARACTERS), qos INTEGER, retain BOOLEAN, createdOn TIMESTAMP, publishedOn TIMESTAMP, publishedMessageId INTEGER, confirmedOn TIMESTAMP, payload BLOB(256M), priority INTEGER, sessionId VARCHAR(32767 CHARACTERS), droppedOn TIMESTAMP);");
	
		update(houseKeeperInterval, purgeAge, capacity);
	}
    
	public synchronized void stop()
    {
		s_logger.info("Canceling the Housekeeper Task...");
		if (m_houseKeeperTask != null) {
			m_houseKeeperTask.cancel(true);
		}
		m_houseKeeperExecutor.shutdownNow();
    }
	
	public synchronized void update(int houseKeeperInterval, int purgeAge, int capacity)
	{
		m_capacity = capacity;
		
		if (m_houseKeeperTask != null) {
			m_houseKeeperTask.cancel(true);
		}
		
		boolean doCheckpoint = !((HsqlDbServiceImpl) m_dbService).isLogDataEnabled();
		
		// Start the Housekeeper task
		m_houseKeeperTask = m_houseKeeperExecutor.scheduleAtFixedRate(new HouseKeeperTask(this, purgeAge, doCheckpoint),
										      1,  // start in one second
										      houseKeeperInterval, // repeat every retryInterval until we stopped. 
										      TimeUnit.SECONDS);
	}
    
    // ----------------------------------------------------------
    //
    //    Message APIs  
    //
    // ----------------------------------------------------------
	
	private synchronized int getMessageCount() throws KuraStoreException
	{
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement stmt = null;
		int count = -1;
		try {			
			
			conn = getConnection();
			stmt = conn.prepareStatement(m_sqlMessageCount);
			rs = stmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
		}
		catch (Exception e) {
			throw new KuraStoreException(e, "Cannot get message count");
		}
		finally {
			close(rs);
			close(stmt);
			close(conn);
		}
		
		return count;
	}
    
	private synchronized void resetIdentityGenerator() throws KuraStoreException
	{
		execute(m_sqlResetId);
	}
	
	public synchronized DataMessage store(String topic, byte[] payload, int qos, boolean retain, int priority) throws KuraStoreException
	{
		if (topic == null || topic.trim().length() == 0) {
			throw new IllegalArgumentException("topic");
		}
		
		// Priority 0 are used for life-cycle messages like birth and death certificates. 
		// Priority 1 are used for remove management by Cloudlet applications.  
		// For those messages, bypass the max message count check of the DB cache;
		// we want to publish those message even if the db is full, so allow their storage.
		if (priority != 0 && priority != 1) {
			int count = getMessageCount();
			s_logger.debug("Store message count: {}", count);
			if (count >= m_capacity) {
				s_logger.error("Store capacity exceeded");
				throw new KuraStoreCapacityReachedException("Store capacity exceeded");
			}
		}
		
		DataMessage message = null;
		try {
			message = storeInternal(topic, payload, qos, retain, priority);
		} catch (KuraStoreException e) {
			// Try to reset the sequence generator and store the message again.
			// FIXME: it doesn't work but if we restart Kura the sequence generator restarts from 0!
			Throwable cause = e.getCause();
			if (cause instanceof SQLException) {
				SQLException sqle = (SQLException) cause;
				int errorCode = sqle.getErrorCode();
				if (errorCode == -3416) {
					s_logger.warn("Identity generator limit exceeded. Resetting it...");
					resetIdentityGenerator();
					message = storeInternal(topic, payload, qos, retain, priority);
				} else {
					throw e;
				}
			} else {
				throw e;
			}
		}
		
		return message;
	}
	
    private synchronized DataMessage storeInternal(String topic, byte[] payload, int qos, boolean retain, int priority) throws KuraStoreException 
	{
		if (topic == null || topic.trim().length() == 0) {
			throw new IllegalArgumentException("topic");
		}

		Timestamp now = new Timestamp((new Date()).getTime());

		int messageId = -1;
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		PreparedStatement cstmt = null;
		try {			
			
			conn = getConnection();

			// store message
			pstmt = conn.prepareStatement(m_sqlStore);
			pstmt.setString   (1,  topic);				// topic
			pstmt.setInt      (2,  qos);				// qos
			pstmt.setBoolean  (3,  retain);				// retain
			pstmt.setTimestamp(4,  now, m_utcCalendar); // createdOn
			pstmt.setTimestamp(5,  null);				// publishedOn
            pstmt.setInt      (6,  -1);                 // publishedMessageId
			pstmt.setTimestamp(7,  null);				// confirmedOn
			pstmt.setBytes    (8,  payload);			// payload
			pstmt.setInt      (9, priority);            // priority
			pstmt.setString   (10, null);               // sessionId
			pstmt.setTimestamp(11, null);				// droppedOn
			pstmt.execute();			
			
			// retrieve message id
			cstmt = conn.prepareStatement("CALL IDENTITY();");
			rs = cstmt.executeQuery();
			if (rs != null && rs.next()) {
				messageId = rs.getInt(1);
			}
			
			conn.commit();
		}
		catch (SQLException e) {
			rollback(conn);
			s_logger.error("SQL error code: {}", e.getErrorCode());
			throw new KuraStoreException(e, "Cannot store message");
		}
		finally {
			close(rs);
			close(cstmt);
			close(pstmt);
			close(conn);
		}
		return get(messageId);
	}
        
    public synchronized DataMessage get(int msgId) throws KuraStoreException
    {
    	DataMessage msg = null;
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			
			conn = getConnection();
			stmt = conn.prepareStatement(m_sqlGetMessage);
			stmt.setInt(1, msgId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				msg  = buildDataMessage(rs);
			}
		}
		catch (Exception e) {
			throw new KuraStoreException(e, "Cannot get message by ID: " + msgId);
		}
		finally {
			close(rs);
			close(stmt);
			close(conn);
		}
		return msg;
    }
    
    public synchronized DataMessage getNextMessage() throws KuraStoreException
    {
    	DataMessage msg = null;
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			
			conn = getConnection();
			stmt = conn.prepareStatement(m_sqlGetNextMessage);
			rs = stmt.executeQuery();
			if (rs != null && rs.next()) {
				msg  = buildDataMessage(rs);
			}
		}
		catch (Exception e) {
			throw new KuraStoreException(e, "Cannot get message next message");
		}
		finally {
			close(rs);
			close(stmt);
			close(conn);
		}
		return msg;
    }
    
	public synchronized void published(int msgId, int publishedMsgId, String sessionId) throws KuraStoreException {
		Timestamp now = new Timestamp((new Date()).getTime());

		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			
			conn = getConnection();
			stmt = conn.prepareStatement(m_sqlSetPublished);
			stmt.setTimestamp(1, now, m_utcCalendar); // timestamp
			stmt.setInt      (2, publishedMsgId);
			stmt.setString   (3, sessionId);
			stmt.setInt      (4, msgId);
			
			stmt.execute();
			conn.commit();
		}
		catch (SQLException e) {
			rollback(conn);
			throw new KuraStoreException(e, "Cannot update timestamp");
		}
		finally {
			close(stmt);
			close(conn);
		}
	}
	
	public synchronized void published(int msgId) throws KuraStoreException {
		updateTimestamp(m_sqlSetPublished2, msgId);
	}

	public synchronized void confirmed(int msgId) throws KuraStoreException {
		updateTimestamp(m_sqlSetConfirmed, msgId);
	}
    
    public synchronized List<DataMessage> allUnpublishedMessagesNoPayload() throws KuraStoreException {    
    	// Order by priority, createdOn
    	return listMessages(m_sqlAllUnpublishedMessages);
    }
    
	public synchronized List<DataMessage> allInFlightMessagesNoPayload() throws KuraStoreException {
    	// Order by priority, createdOn
		return listMessages(m_sqlAllInFlightMessages);
	}
	
	public synchronized List<DataMessage> allDroppedInFlightMessagesNoPayload() throws KuraStoreException {
    	// Order by priority, createdOn
    	return listMessages(m_sqlAllDroppedInFlightMessages);		
	}
    
	public synchronized void unpublishAllInFlighMessages() throws KuraStoreException {
		execute(m_sqlUnpublishAllInFlightMessages);			
	}
	
	public synchronized void dropAllInFlightMessages()  throws KuraStoreException {
		updateTimestamp(m_sqlDropAllInFlightMessages);
	}
    
    public synchronized void deleteStaleMessages(int purgeAge) throws KuraStoreException {
    	final int INTERVAL_FIELD_OVERFLOW = -3435;
    	Timestamp now = new Timestamp((new Date()).getTime());
    	// Delete dropped messages (published with QoS > 0)
    	try {
    		execute(m_sqlDeleteDroppedMessages, now, purgeAge);
    	} catch (KuraStoreException e) {
    		// Interval field overflow
    		Throwable cause = e.getCause();
    		if (cause != null && cause instanceof SQLDataException && ((SQLDataException) cause).getErrorCode() == INTERVAL_FIELD_OVERFLOW) {
    			s_logger.info("Delete all dropped messages older than one year");
    			execute(m_sqlDeleteDroppedMessages2, now, 0);
    		} else {
    			throw e;
    		}
    	}
    	
    	// Delete stale confirmed messages (published with QoS > 0)
    	try {
    		execute(m_sqlDeleteConfirmedMessages, now, purgeAge);
    	} catch (KuraStoreException e) {
    		// Interval field overflow
    		Throwable cause = e.getCause();
    		if (cause != null && cause instanceof SQLDataException && ((SQLDataException) cause).getErrorCode() == INTERVAL_FIELD_OVERFLOW) {
    			s_logger.info("Delete all confirmed messages older than one year");
    			execute(m_sqlDeleteConfirmedMessages2, now, 0);
    		} else {
    			throw e;
    		}    			
    	}
    	
    	// Delete stale published messages with QoS == 0
    	try {
    		execute(m_sqlDeletePublishedMessages, now, purgeAge);
    	} catch (KuraStoreException e) {
    		// Interval field overflow
    		Throwable cause = e.getCause();
    		if (cause != null && cause instanceof SQLDataException && ((SQLDataException) cause).getErrorCode() == INTERVAL_FIELD_OVERFLOW) {
    			s_logger.info("Delete all published messages older than one year");
    			execute(m_sqlDeletePublishedMessages2, now, 0);
    		} else {
    			throw e;
    		}    		
    	}
	}
	
    public synchronized void defrag() throws KuraStoreException {
    	execute("CHECKPOINT DEFRAG"); // regains the disk space
    }
    
    public synchronized void checkpoint() throws KuraStoreException {
    	execute("CHECKPOINT");
    }
    
    public synchronized void repair() throws KuraStoreException {
    	// See:
    	// https://sourceforge.net/p/hsqldb/discussion/73674/thread/a08046eb/#7960
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		int count = -1;
		try {			
			
			conn = getConnection();
			// Get the count of IDs for which duplicates exist
			pstmt = conn.prepareStatement(m_sqlDuplicateCount);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
			
			if (count <= 0) {
				return;
			}
			
			s_logger.error("Found messages with duplicate ID. Count of IDs for which duplicates exist: {}. Attempting to repair...", count);
			
			stmt = conn.createStatement();
			
			stmt.execute(m_sqlDropPrimaryKey);
			s_logger.info("Primary key dropped");
			
			stmt.execute(m_sqlDeleteDuplicates);
			s_logger.info("Duplicate messages deleted");
			
			stmt.execute(m_sqlCreatePrimaryKey);
			s_logger.info("Primary key created");
			
			conn.commit();
			
			stmt.execute("CHECKPOINT DEFRAG");
			s_logger.info("Checkpoint defrag");
			conn.commit();
		}
		catch (SQLException e) {
			rollback(conn);
			throw new KuraStoreException(e, "Cannot repair database");
		}
		finally {
			close(rs);
			close(pstmt);
			close(stmt);
			close(conn);
		}
    }
    
	// ------------------------------------------------------------------
	//
	//      Private Methods  
	//
	// ------------------------------------------------------------------    
    
    
    private synchronized void updateTimestamp(String sql, Integer... msgIds) throws KuraStoreException 
    {
		Timestamp now = new Timestamp((new Date()).getTime());

		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			
			conn = getConnection();
			stmt = conn.prepareStatement(sql);
			stmt.setTimestamp(1, now, m_utcCalendar); // timestamp

			for (int i=0; i<msgIds.length; i++) {
			    stmt.setInt(2+i, msgIds[i]);  // messageId
			}
			stmt.execute();
			conn.commit();
		}
		catch (SQLException e) {
			rollback(conn);
			throw new KuraStoreException(e, "Cannot update timestamp");
		}
		finally {
			close(stmt);
			close(conn);
		}
    }
    
    private synchronized List<DataMessage> listMessages(String sql, Integer... params) throws KuraStoreException 
    {    	
    	List<DataMessage> msgs = new ArrayList<DataMessage>();
    	
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			
			conn = getConnection();
			stmt = conn.prepareStatement(sql);
			if (params != null) {
				for (int i=0; i<params.length; i++) {
					stmt.setInt(2+i, params[i]);  // timeInterval
				}
			}
			
			rs   = stmt.executeQuery();			
			msgs = buildDataMessagesNoPayload(rs);
		}
		catch (Exception e) {
			throw new KuraStoreException(e, "Cannot list messages");
		}
		finally {
			close(rs);
			close(stmt);
			close(conn);
		}
		
		return msgs;
    }

    private synchronized void execute(String sql, Integer... params) throws KuraStoreException 
    {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			
			conn = getConnection();
			stmt = conn.prepareStatement(sql);
			for (int i=0; i<params.length; i++) {
			    stmt.setInt(1+i, params[i]); 
			}
			stmt.execute();
			conn.commit();
		}
		catch (SQLException e) {
			rollback(conn);
			throw new KuraStoreException(e, "Cannot execute query");
		}
		finally {
			close(stmt);
			close(conn);
		}
    }
    
    private synchronized void execute(String sql, Timestamp timestamp, Integer... params) throws KuraStoreException 
    {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			
			conn = getConnection();
			stmt = conn.prepareStatement(sql);
			if (timestamp != null) {
				stmt.setTimestamp(1, timestamp, m_utcCalendar);
			}
			for (int i=0; i<params.length; i++) {
			    stmt.setInt(2+i, params[i]); 
			}
			stmt.execute();
			conn.commit();
		}
		catch (SQLException e) {
			rollback(conn);
			throw new KuraStoreException(e, "Cannot execute query");
		}
		finally {
			close(stmt);
			close(conn);
		}
    }

    
    // ------------------------------------------------------------------
	//
	//      Private Methods: Connection Management  
	//
	// ------------------------------------------------------------------    

    
    private List<DataMessage> buildDataMessagesNoPayload(ResultSet rs) 
    		throws SQLException, IOException
    {
    	List<DataMessage> messages = new ArrayList<DataMessage>();
    	while (rs.next()) {    		
    		messages.add(buildDataMessageNoPayload(rs));
    	}
    	return messages;
    }
    
	private DataMessage buildDataMessageNoPayload(ResultSet rs) 
		throws SQLException 
	{
		DataMessage.Builder builder = buildDataMessageBuilder(rs);
		return builder.build();		
	}
	
	private DataMessage buildDataMessage(ResultSet rs) 
		throws SQLException 
	{
		DataMessage.Builder builder = buildDataMessageBuilder(rs);
		builder = builder.withPayload(rs.getBytes("payload"));
		return builder.build();
	}

	private DataMessage.Builder buildDataMessageBuilder(ResultSet rs)
			throws SQLException 
	{
		DataMessage.Builder builder;
		builder = new DataMessage.Builder(rs.getInt("id"))
									    .withTopic(rs.getString("topic"))
									    .withQos(rs.getInt("qos"))
									    .withRetain(rs.getBoolean("retain"))
									    .withCreatedOn(rs.getTimestamp("createdOn", m_utcCalendar))
									    .withPublishedOn(rs.getTimestamp("publishedOn", m_utcCalendar))
                                        .withPublishedMessageId(rs.getInt("publishedMessageId"))
									    .withConfirmedOn(rs.getTimestamp("confirmedOn", m_utcCalendar))
									    .withPriority(rs.getInt("priority"))
									    .withSessionId(rs.getString("sessionId"))
									    .withDroppedOn(rs.getTimestamp("droppedOn"));
		return builder;
	}
    
	private Connection getConnection() throws SQLException {	    
		return m_dbService.getConnection();
	}
	
	private void rollback(Connection conn) {
		m_dbService.rollback(conn);
	}
	
	private void close(ResultSet... rss) {
		m_dbService.close(rss);
	}

	private void close(Statement... stmts) {
		m_dbService.close(stmts);
	}

	private void close(Connection conn) {
		m_dbService.close(conn);
	}
}
