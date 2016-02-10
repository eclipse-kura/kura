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
package org.eclipse.kura.core.data;

import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.kura.data.DataServiceListener;

class DataServiceListeners implements DataServiceListener {

	private static final Logger s_logger = LoggerFactory.getLogger(DataServiceListeners.class);
	
	private ServiceTracker<DataServiceListener, DataServiceListener> m_listenersTracker;

	public DataServiceListeners(ServiceTracker<DataServiceListener, DataServiceListener> listenersTracker) {
		m_listenersTracker = listenersTracker;
	}
	
	public synchronized void close() {
		if (m_listenersTracker.getTrackingCount() != -1) {
			m_listenersTracker.close();
		}
	}

	@Override
	public void onConnectionEstablished() {
		openOnce();
		
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataServiceListener) listener).onConnectionEstablished();
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		} else {
			s_logger.warn("No registered services. Missing onConnectionEstablished()");
		}
	}

	@Override
	public void onDisconnecting() {
		openOnce();
		
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataServiceListener) listener).onDisconnecting();
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		} else {
			s_logger.warn("No registered services. Missing onDisconnecting()");
		}
	}

	@Override
	public void onDisconnected() {
		openOnce();
		
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataServiceListener) listener).onDisconnected();
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		} else {
			s_logger.warn("No registered services. Missing onDisconnected()");
		}
	}

	@Override
	public void onConnectionLost(Throwable cause) {
		openOnce();
		
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataServiceListener) listener).onConnectionLost(cause);
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		} else {
			s_logger.warn("No registered services. Missing onConnectionLost()");
		}
	}

	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos,
			boolean retained) {
		openOnce();
		
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
			s_logger.warn("No registered services. Missing onMessageArrived()");
		}
	}

	@Override
	public void onMessagePublished(int messageId, String topic) {
		openOnce();
		
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataServiceListener) listener).onMessagePublished(messageId, topic);
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		} else {
			s_logger.warn("No registered services. Missing onMessagePublished()");
		}
	}

	@Override
	public void onMessageConfirmed(int messageId, String topic) {
		openOnce();
		
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataServiceListener) listener).onMessageConfirmed(messageId, topic);
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		} else {
			s_logger.warn("No registered services. Missing onMessageConfirmed()");
		}
	}
	
	private synchronized void openOnce() {
		if (m_listenersTracker.getTrackingCount() == -1) {
			m_listenersTracker.open();
		}
	}
}
