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
package org.eclipse.kura.core.data.transport.mqtt;

import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.kura.data.DataTransportListener;
import org.eclipse.kura.data.DataTransportToken;

class DataTransportListeners implements DataTransportListener {
	
	private static final Logger s_logger = LoggerFactory.getLogger(DataTransportListeners.class);
	
	private ServiceTracker<DataTransportListener, DataTransportListener> m_listenersTracker;
	
	public DataTransportListeners(ServiceTracker<DataTransportListener, DataTransportListener> listenersTracker) {
		super();
		this.m_listenersTracker = listenersTracker;
	}
	
	public synchronized void close() {
		if (m_listenersTracker.getTrackingCount() != -1) {
			m_listenersTracker.close();
		}
	}

	@Override
	public void onConnectionEstablished(boolean newSession) {
		openOnce();
		
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataTransportListener) listener).onConnectionEstablished(newSession);
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
					((DataTransportListener) listener).onDisconnecting();
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
					((DataTransportListener) listener).onDisconnected();
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		} else {
			s_logger.warn("No registered services. Missing onDisconnected()");
		}
	}

	@Override
	public void onConfigurationUpdating(boolean wasConnected) {
		openOnce();
		
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataTransportListener) listener).onConfigurationUpdating(wasConnected);
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		} else {
			s_logger.warn("No registered services. Missing onConfigurationUpdating()");
		}
	}

	@Override
	public void onConfigurationUpdated(boolean wasConnected) {
		openOnce();
		
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataTransportListener) listener).onConfigurationUpdated(wasConnected);
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		} else {
			s_logger.warn("No registered services. Missing onConfigurationUpdated()");
		}
	}

	@Override
	public void onConnectionLost(Throwable cause) {
		openOnce();
		
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataTransportListener) listener).onConnectionLost(cause);
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
					((DataTransportListener) listener).onMessageArrived(topic,
							payload, qos, retained);
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		} else {
			s_logger.warn("No registered services. Missing onMessageArrived()");
		}
	}

	@Override
	public void onMessageConfirmed(DataTransportToken token) {
		openOnce();
				
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((DataTransportListener) listener).onMessageConfirmed(token);
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
