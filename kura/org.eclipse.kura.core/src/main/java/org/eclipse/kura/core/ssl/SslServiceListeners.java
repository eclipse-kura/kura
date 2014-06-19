package org.eclipse.kura.core.ssl;

import org.eclipse.kura.ssl.SslServiceListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SslServiceListeners implements SslServiceListener {
	
	private static final Logger s_logger = LoggerFactory.getLogger(SslServiceListeners.class);
	
	private ServiceTracker<SslServiceListener, SslServiceListener> m_listenersTracker;

	public SslServiceListeners(ServiceTracker<SslServiceListener, SslServiceListener> listenersTracker) {
		super();
		this.m_listenersTracker = listenersTracker;
	}
	
	@Override
	public void onConfigurationUpdated() {
		openOnce();
		
		Object[] listeners = m_listenersTracker.getServices();
		if (listeners != null && listeners.length != 0) {
			for (Object listener : listeners) {
				try {
					((SslServiceListener) listener).onConfigurationUpdated();
				} catch (Throwable t) {
					s_logger.error("Unexpected Throwable", t);
				}
			}
		}	
	}
	
	public synchronized void close() {
		if (m_listenersTracker.getTrackingCount() != -1) {
			m_listenersTracker.close();
		}
	}
	
	private synchronized void openOnce() {
		if (m_listenersTracker.getTrackingCount() == -1) {
			m_listenersTracker.open();
		}
	}

}
