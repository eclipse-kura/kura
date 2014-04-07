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
package org.eclipse.kura.core.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraNotConnectedException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.KuraTimeoutException;
import org.eclipse.kura.KuraTooManyInflightMessagesException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.data.store.DbDataStore;
import org.eclipse.kura.core.util.ExecutorUtil;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataServiceListener;
import org.eclipse.kura.data.DataTransportListener;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.db.DbService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataServiceImpl implements DataService, DataTransportListener, ConfigurableComponent {
	
	private static final Logger s_logger = LoggerFactory
			.getLogger(DataServiceImpl.class);
	
	private static final int TRANSPORT_TASK_TIMEOUT = 1; // In seconds
	
	private static final String AUTOCONNECT_PROP_NAME = "connect.auto-on-startup";
	private static final String CONNECT_DELAY_PROP_NAME = "connect.retry-interval";
	private static final String DISCONNECT_DELAY_PROP_NAME = "disconnect.quiesce-timeout";
	private static final String STORE_HOUSEKEEPER_INTERVAL_PROP_NAME = "store.housekeeper-interval";
	private static final String STORE_PURGE_AGE_PROP_NAME = "store.purge-age";
	private static final String STORE_CAPACITY_PROP_NAME = "store.capacity";
	private static final String REPUBLISH_IN_FLIGHT_MSGS_PROP_NAME = "in-flight-messages.republish-on-new-session";
	private static final String MAX_IN_FLIGHT_MSGS_PROP_NAME = "in-flight-messages.max-number";
	private static final String IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_PROP_NAME = "in-flight-messages.congestion-timeout";
	
	private Map<String, Object> m_properties = new HashMap<String, Object>();
	
	private DataTransportService m_dataTransportService;
	private DbService m_dbService;

	private ServiceTracker<DataServiceListener, DataServiceListener> m_listenersTracker;

	private ScheduledFuture<?> m_reconnectFuture;
	
	// A dedicated executor for the publishing task
	private ScheduledThreadPoolExecutor m_publisherExecutor;
	
	private DataStore m_store;
	
	private Map<DataTransportToken, Integer> m_inFlightMsgIds;
	
	private ScheduledFuture<?> m_congestionFuture;
	
	private ScheduledThreadPoolExecutor m_congestionExecutor;
	
	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext,
			Map<String, Object> properties) {
		s_logger.info("Activating...");
		
		m_publisherExecutor = new ScheduledThreadPoolExecutor(1);
		m_congestionExecutor = new ScheduledThreadPoolExecutor(1);
						
		m_properties.putAll(properties);
		
		m_store = new DbDataStore();
		
		try {
			m_store.start(m_dbService,
					(Integer) m_properties.get(STORE_HOUSEKEEPER_INTERVAL_PROP_NAME),
					(Integer) m_properties.get(STORE_PURGE_AGE_PROP_NAME),
					(Integer) m_properties.get(STORE_CAPACITY_PROP_NAME));
			
			// The initial list of in-flight messages
			List<DataMessage> inFlightMsgs = m_store.allInFlightMessagesNoPayload();
			
			// The map associating a DataTransportToken with a message ID
			m_inFlightMsgIds = new ConcurrentHashMap<DataTransportToken, Integer>();
			
			if (inFlightMsgs != null) {
				for (DataMessage message : inFlightMsgs) {
					
					DataTransportToken token = new DataTransportToken(message.getPublishedMessageId(), message.getSessionId());
					m_inFlightMsgIds.put(token, message.getId());
					
					s_logger.debug("Restored in-fligh messages from store. Topic: {}, ID: {}, MQTT message ID: {}",
							new Object[] {message.getTopic(), message.getId(), message.getPublishedMessageId()});
				}
			}
		} catch (KuraStoreException e) {
			s_logger.error("Failed to start store", e);
			throw new ComponentException("Failed to start store", e);
		}
		
		m_listenersTracker = new ServiceTracker<DataServiceListener, DataServiceListener>(
				componentContext.getBundleContext(),
				DataServiceListener.class, null);
		
		m_listenersTracker.open();
		
		startReconnectTask();
	}
	
	public void updated(Map<String, Object> properties) {
		s_logger.info("Updating...");
		
		stopReconnectTask();
		
		m_properties.clear();
		m_properties.putAll(properties);
		
		m_store.update((Integer) m_properties.get(STORE_HOUSEKEEPER_INTERVAL_PROP_NAME),
				(Integer) m_properties.get(STORE_PURGE_AGE_PROP_NAME),
				(Integer) m_properties.get(STORE_CAPACITY_PROP_NAME));
		
		if (!m_dataTransportService.isConnected()) {
			startReconnectTask();
		}
	}
	
	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Deactivating...");
		
		m_congestionExecutor.shutdownNow();
		
		// Await termination of the publisher executor tasks
		try {
			m_publisherExecutor.awaitTermination(TRANSPORT_TASK_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			s_logger.info("Interrupted", e);
		}
		m_publisherExecutor.shutdownNow();
		
		stopReconnectTask();
		
		disconnect();
				
		m_listenersTracker.close();
				
		m_store.stop();
	}
		
	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------
	
	public void setDataTransportService(DataTransportService dataTransportService) {
		this.m_dataTransportService = dataTransportService;
	}

	public void unsetDataTransportService(DataTransportService dataTransportService) {
		this.m_dataTransportService = null;
	}

	public void setDbService(DbService dbService) {
		this.m_dbService = dbService;
	}

	public void unsetDbService(DbService dbService) {
		this.m_dbService = null;
	}
	
	@Override
	public void onConnectionEstablished(boolean newSession) {
		
		s_logger.info("Notified connected");
		
		// On a new session all messages the were in-flight in the previous session
		// would be lost and never confirmed by the DataPublisherService.
		//
		// If the DataPublisherService is configured with Clean Start flag set to true,
		// then the session and connection boundaries are the same.
		// Otherwise, a session spans multiple connections as far as the client connects
		// to the same broker instance with the same client ID.
		//
		// We have two options here:
		// Forget them.
		// Unpublish them so they will be republished on the new session.
		//
		// The latter has the potential drawback that duplicates can be generated with any QoS.
		// This can occur for example if the DataPublisherService is connecting with a different client ID
		// or to a different broker URL resolved to the same broker instance.
		//
		// Also note that unpublished messages will be republished accordingly to their
		// original priority. Thus a message reordering may occur too.
		// Even if we artificially upgraded the priority of unpublished messages to -1 so to
		// republish them first, their relative order would not necessarily match the order
		// in the DataPublisherService persistence.
		
		if (newSession) {
			Boolean unpublishInFlightMsgs = (Boolean) m_properties.get(REPUBLISH_IN_FLIGHT_MSGS_PROP_NAME);
			
			if (unpublishInFlightMsgs) {
				s_logger.info("New session established. Unpublishing all in-flight messages. Disregarding the QoS level, this may cause duplicate messages.");
				try {
					m_store.unpublishAllInFlighMessages();
					m_inFlightMsgIds.clear();
				} catch (KuraStoreException e) {
					s_logger.error("Failed to unpublish in-flight messages", e);
				}
			} else {
				s_logger.info("New session established. Dropping all in-flight messages.");
				try {
					m_store.dropAllInFlightMessages();
					m_inFlightMsgIds.clear();
				} catch (KuraStoreException e) {
					s_logger.error("Failed to drop in-flight messages", e);
				}
			}
		}
		
		// Notify the listeners
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataServiceListener) listener).onConnectionEstablished();
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		}
		
		// Schedule execution of a publisher task
		submitPublishingWork();
	}

	@Override
	public void onDisconnecting() 
	{		
		s_logger.info("Notified disconnecting");
		
		// Notify the listeners
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataServiceListener) listener).onDisconnecting();
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		}
		
		// Schedule execution of a publisher task waiting until done or timeout.
		Future<?> future = submitPublishingWork();
		try {
			future.get(TRANSPORT_TASK_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			s_logger.info("Interrupted while waiting for the publishing work to complete");
		} catch (ExecutionException e) {
			s_logger.warn("ExecutionException while waiting for the publishing work to complete", e);
		} catch (TimeoutException e) {
			s_logger.warn("Timeout while waiting for the publishing work to complete");
		}
	}

	@Override
	public void onDisconnected() 
	{		
		s_logger.info("Notified disconnected");
		
		// Notify the listeners
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataServiceListener) listener).onDisconnected();
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		}
	}

	@Override
	public void onConfigurationUpdating(boolean wasConnected) {
		s_logger.info("Notified DataTransportService configuration updating...");
		stopReconnectTask();
		disconnect(0);
	}

	@Override
	public void onConfigurationUpdated(boolean wasConnected) 
	{
		s_logger.info("Notified DataTransportService configuration updated.");
		boolean autoConnect = startReconnectTask();
		if (!autoConnect && wasConnected) {			
			try {
				connect();
			} catch (KuraConnectException e) {
				s_logger.error("Error during re-connect after configuration update.", e);
			}
		}
	}


	@Override
	public void onConnectionLost(Throwable cause) 
	{		
		s_logger.info("connectionLost");
		
		stopReconnectTask(); // Just in case...
		startReconnectTask();
		
		// Notify the listeners
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataServiceListener) listener).onConnectionLost(cause);
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		}
	}

	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos,
			boolean retained) {
		
		s_logger.debug("Message arrived on topic: {}", topic);
		
		// Notify the listeners
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataServiceListener) listener).onMessageArrived(topic, payload, qos, retained);
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		} else {
			s_logger.error("No registered services. Dropping arrived message");
		}
		
		submitPublishingWork();
	}

	@Override
	// It's very important that the publishInternal and messageConfirmed methods are synchronized
	public synchronized void onMessageConfirmed(DataTransportToken token) {
		
		s_logger.debug("Confirmed message with MQTT message ID: {} on session ID: {}",
				token.getMessageId(), token.getSessionId());
		
		Integer messageId = m_inFlightMsgIds.remove(token);
		if (messageId == null) {
			s_logger.info("Confirmed message published with MQTT message ID: {} not tracked in the map of in-flight messages", token.getMessageId());
		} else {

			DataMessage confirmedMessage = null; 
			try {
				s_logger.info("Confirmed message ID: {} to store", messageId);
				m_store.confirmed(messageId);
				confirmedMessage = m_store.get(messageId);
			} catch (KuraStoreException e) {
				s_logger.error("Cannot confirm message to store", e);
			}
			
			// Notify the listeners
			if (confirmedMessage != null) {
				String topic = confirmedMessage.getTopic();
				Object[] listeners = m_listenersTracker.getServices();
				if (listeners != null && listeners.length != 0) {
					for (Object listener : listeners) {
						try {
							((DataServiceListener) listener).onMessageConfirmed(messageId, topic);
						} catch (Throwable t) {
							s_logger.error("Unexpected Throwable", t);
						}
					}
				} 
				else {
					s_logger.error("No registered services. Dropping message confirm");
				}
			}
			else {
				s_logger.error("Confirmed Message with ID {} could not be loaded from the DataStore.", messageId);
			}
		}
		
		if (m_inFlightMsgIds.size() < (Integer) m_properties.get(MAX_IN_FLIGHT_MSGS_PROP_NAME)) {
			handleInFlightDecongestion();
		}
		
		submitPublishingWork();
	}

	@Override
	public void connect() throws KuraConnectException {
		stopReconnectTask();
		if (!m_dataTransportService.isConnected()) {
			m_dataTransportService.connect();
		}
	}

	@Override
	public boolean isConnected() {
		return m_dataTransportService.isConnected();
	}
	
	@Override
	public boolean isAutoConnectEnabled() {
		return (Boolean)m_properties.get(AUTOCONNECT_PROP_NAME);
	}
	
	@Override
	public int getRetryInterval() {
		return (Integer) m_properties.get(CONNECT_DELAY_PROP_NAME);
	}

	@Override
	public void disconnect(long quiesceTimeout) {
		stopReconnectTask();
		m_dataTransportService.disconnect(quiesceTimeout);
	}

	@Override
	public void subscribe(String topic, int qos) throws KuraTimeoutException, KuraException, KuraNotConnectedException {
		m_dataTransportService.subscribe(topic, qos);
	}

	@Override
	public void unsubscribe(String topic) throws KuraTimeoutException, KuraException, KuraNotConnectedException {
		m_dataTransportService.unsubscribe(topic);
	}

	@Override
	public int publish(String topic, byte[] payload, int qos, boolean retain,
			int priority) throws KuraStoreException {
		
		s_logger.info("Storing message on topic :{}, priority: {}", topic, priority);

		DataMessage dataMsg = m_store.store(topic, payload, qos, retain, priority);		
		s_logger.info("Stored message on topic :{}, priority: {}", topic, priority);		
		
		submitPublishingWork();
		
		return dataMsg.getId();
	}
	
	@Override
	public List<Integer> getUnpublishedMessageIds(String topicRegex) throws KuraStoreException {
		List<DataMessage> messages = m_store.allUnpublishedMessagesNoPayload();
		return buildMessageIds(messages, topicRegex);
	}
	
	@Override
	public List<Integer> getInFlightMessageIds(String topicRegex) throws KuraStoreException {
		List<DataMessage> messages = m_store.allInFlightMessagesNoPayload();
		return buildMessageIds(messages, topicRegex);
	}
	
	@Override
	public List<Integer> getDroppedInFlightMessageIds(String topicRegex) throws KuraStoreException {
		List<DataMessage> messages = m_store.allDroppedInFlightMessagesNoPayload();
		return buildMessageIds(messages, topicRegex);
	}
	
	private boolean startReconnectTask() 
	{
		if (m_reconnectFuture != null && !m_reconnectFuture.isDone()) {
			s_logger.error("Reconnect task already running");
			throw new IllegalStateException("Reconnect task already running");
		}
		
		//
    	// Establish a reconnect Thread based on the reconnect interval
		boolean autoConnect = (Boolean) m_properties.get(AUTOCONNECT_PROP_NAME);
    	int reconnectInterval = (Integer) m_properties.get(CONNECT_DELAY_PROP_NAME);    			
		if (autoConnect) {

			// add a delay on the reconnect
			int maxDelay = reconnectInterval/5;
			maxDelay = maxDelay > 0 ? maxDelay : 1;
			int initialDelay = (new Random()).nextInt(maxDelay);
			
			s_logger.info("Starting reconnect task with initial delay {}", initialDelay);
			ScheduledThreadPoolExecutor stpe = ExecutorUtil.getInstance();
			m_reconnectFuture = stpe.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					Thread.currentThread().setName("DataServiceImpl:ReconnectTask");
					try {
						s_logger.info("Connecting...");
						if (m_dataTransportService.isConnected()) {
							s_logger.info("Already connected. Stopping reconnect task.");
						}
						else {
							m_dataTransportService.connect();
							s_logger.info("Connected. Stopping reconnect task");
						}												

						m_reconnectFuture.cancel(false);
					} 
					catch (KuraConnectException e) {
						// It's not apparent from the API but the connect() can be interrupted.
						// Anyway the InterruptedException is (deeply) nested in the KuraConnectException.
						// I'd prefer to have the publish method throwing explicitly the InterruptedException.
						if (!m_reconnectFuture.isCancelled()) {
							s_logger.info("Connect failed", e);
						}
					} catch (RuntimeException e) {
						// There's nothing we can do here but log an exception.
						s_logger.error("Unexpected RuntimeException. Thread will be terminated", e);
						throw e;
					} catch (Error e) {
						// There's nothing we can do here but log an exception.
						s_logger.error("Unexpected Error. Thread will be terminated", e);
						throw e;
					}
				}
			},
			initialDelay, 		// initial delay
			reconnectInterval, // repeat every reconnect interval until we stopped. 
			TimeUnit.SECONDS);
		}
		return autoConnect;
	}
	
	private void stopReconnectTask() {
		if (m_reconnectFuture != null && !m_reconnectFuture.isDone()) {

			s_logger.info("Reconnect task running. Stopping it");

			m_reconnectFuture.cancel(true);
		}
	}
	
	private void disconnect() {
		long millis = (Integer) m_properties.get(DISCONNECT_DELAY_PROP_NAME) * 1000L;
		m_dataTransportService.disconnect(millis);
	}
	
	// Submit a new publishing work if any
	// TODO: only one instance of the Runnable is needed
	private Future<?> submitPublishingWork() 
	{
		return m_publisherExecutor.submit(new Runnable() {		
			@Override
			public void run() {
				Thread.currentThread().setName("DataServiceImpl:Submit");
				if (!m_dataTransportService.isConnected()) {
					s_logger.info("DataPublisherService not connected");
					return;
				}
				try {
					
					// Compared with getting all unpublished messages, getting one message at a time
					// is a little bit inefficient (we query the underlying DB every time)
					// but improves responsiveness to high priority message.
					// TODO: add a getUnpublishedMessages with a limit argument?
					// getNextMessage is a special case with limit = 1.
					DataMessage message = null;
					while ((message = m_store.getNextMessage()) != null) {
						
						// Further limit the maximum number of in-flight messages
						if (message.getQos() > 0) {
							if (m_inFlightMsgIds.size() >= (Integer) m_properties.get(MAX_IN_FLIGHT_MSGS_PROP_NAME)) {
								s_logger.warn("The configured maximum number of in-flight messages has been reached");
								handleInFlightCongestion();
								break;
							}
						}
						
						publishInternal(message);
						
						// TODO: add a 'message throttle' configuration parameter to
						// slow down publish rate?
						
						// Notify the listeners
						String topic = message.getTopic();
						Object[] listeners = m_listenersTracker.getServices();
						if (listeners != null && listeners.length != 0) {
							for (Object listener : listeners) {
								try {
									((DataServiceListener) listener).onMessagePublished(message.getId(), topic);
								} catch (Throwable t) {
									s_logger.error("Unexpected Throwable", t);
								}
							}
						} else {
							s_logger.error("No registered services. Ignoring message confirm");
						}
					}
				} catch (KuraConnectException e) {
					s_logger.info("DataPublisherService is not connected", e);
				} catch (KuraTooManyInflightMessagesException e) {
					s_logger.info("Too many in-flight messages", e);
					handleInFlightCongestion();
				} catch (Exception e) {
					s_logger.error("Probably an unrecoverable exception", e);
				}
			}
		});
	}
	
	// It's very important that the publishInternal and messageConfirmed methods are synchronized
	private synchronized void publishInternal(DataMessage message)
			throws KuraConnectException, KuraTooManyInflightMessagesException, KuraStoreException, KuraException {
		
		String topic = message.getTopic();
		byte[] payload = message.getPayload();
		int qos = message.getQos();
		boolean retain = message.isRetain();
		int msgId = message.getId();
		
		s_logger.debug("Publishing message with ID: {} on topic: {}, priority: {}",
				new Object[] {msgId, topic, message.getPriority()});		
		
		DataTransportToken token = m_dataTransportService.publish(topic, payload, qos, retain);
		
		if (token == null) {
			m_store.published(msgId);
			s_logger.debug("Published message with ID: {}", msgId);
		} else {
			
			// Check if the token is already tracked in the map (in which case we are in trouble)
			Integer trackedMsgId = m_inFlightMsgIds.get(token);
			if (trackedMsgId != null) {
				s_logger.error("Token already tracked: "+token.getSessionId()+"-"+token.getMessageId());
			}
			
			m_inFlightMsgIds.put(token, msgId);
			m_store.published(msgId, token.getMessageId(), token.getSessionId());
			s_logger.debug("Published message with ID: {} and MQTT message ID: {}", msgId, token.getMessageId());
		}
	}
	
	private List<Integer> buildMessageIds(List<DataMessage> messages, String topicRegex) {
		Pattern topicPattern = Pattern.compile(topicRegex);
		List<Integer> ids = new ArrayList<Integer>();
		
		if (messages != null) {
			for (DataMessage message : messages) {
				String topic = message.getTopic();
				if (topicPattern.matcher(topic).matches()) {
					ids.add(message.getId());
				}
			}
		}
		
		return ids;
	}
	
	private void handleInFlightCongestion() {
		int timeout = (Integer) m_properties.get(IN_FLIGHT_MSGS_CONGESTION_TIMEOUT_PROP_NAME);
		
		// Do not schedule more that one task at a time
		if (timeout != 0 && (m_congestionFuture == null || m_congestionFuture.isDone())) {
			s_logger.warn("In-flight message congestion timeout started");
			m_congestionFuture = m_congestionExecutor.schedule(new Runnable() {
				@Override
				public void run() {
					Thread.currentThread().setName("DataServiceImpl:InFlightCongestion");
					s_logger.warn("In-flight message congestion timeout elapsed. Disconnecting and reconnecting again");
					disconnect();
					startReconnectTask();
				}
			},
			timeout,
			TimeUnit.SECONDS);
		}
	}
	
	private void handleInFlightDecongestion() {
		if (m_congestionFuture != null && !m_congestionFuture.isDone()) {
			m_congestionFuture.cancel(true);
		}
	}
}
