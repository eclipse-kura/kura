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
package org.eclipse.kura.core.status;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.core.status.runnable.BlinkStatusRunnable;
import org.eclipse.kura.core.status.runnable.HeartbeatStatusRunnable;
import org.eclipse.kura.core.status.runnable.LogStatusRunnable;
import org.eclipse.kura.core.status.runnable.OnOffStatusRunnable;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudConnectionStatusServiceImpl implements CloudConnectionStatusService {

	private static final String STATUS_NOTIFICATION_URL = "ccs.status.notification.url";

	private static final Logger s_logger = LoggerFactory.getLogger(CloudConnectionStatusServiceImpl.class);

	private SystemService m_systemService;
	private GPIOService m_gpioService;

	private int m_ledIndex = -1;
	private KuraGPIOPin m_notificationLED;

	private ExecutorService m_notificationExecutor;
	private Future<?> m_notificationWorker;

	private IdleStatusComponent m_idleComponent;

	private static int m_currentNotificationType;
	private static CloudConnectionStatusEnum m_currentStatus;

	private static final HashSet<CloudConnectionStatusComponent> s_componentRegistry = new HashSet<CloudConnectionStatusComponent>();

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------
	public CloudConnectionStatusServiceImpl() {
		super();
		m_notificationExecutor = Executors.newSingleThreadExecutor();
		m_idleComponent = new IdleStatusComponent();
	}

	public void setSystemService(SystemService systemService) {
		this.m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		this.m_systemService = null;
	}

	public void setGPIOService(GPIOService GpioService) {
		s_logger.info("Binding GPIO Service...");
		this.m_gpioService = GpioService;
		// Check if LED can be already acquired
		if (m_ledIndex != -1) {
			m_notificationLED = m_gpioService.getPinByTerminal(m_ledIndex, KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN, KuraGPIOTrigger.NONE);

			try {
				m_notificationLED.open();
			} catch (Exception e) {
				s_logger.error("Could not open LED {}.", m_ledIndex);
			}
			s_logger.info("CloudConnectionStatus active on LED {}.", m_ledIndex);
		}
	}

	public void unsetGPIOService(GPIOService GpioService) {
		s_logger.info("Unbinding GPIO Service...");
		if (m_notificationLED != null) {
			try {
				m_notificationLED.close();
			} catch (IOException e) {
				s_logger.error("Error closing GPIO LED!");
			}
			m_notificationLED = null;
		}
		this.m_gpioService = null;
		m_currentStatus = null;
		internalUpdateStatus();
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext) {
		s_logger.info("Activating CloudConnectionStatus service...");

		String urlFromConfig = m_systemService.getProperties().getProperty(STATUS_NOTIFICATION_URL,
				CloudConnectionStatusURL.S_CCS + CloudConnectionStatusURL.S_NONE);

		Properties props = CloudConnectionStatusURL.parseURL(urlFromConfig);
		m_ledIndex = -1;

		try {
			int notificationType = (Integer) props.get("notification_type");

			switch (notificationType) {
			case CloudConnectionStatusURL.TYPE_LED:
				m_currentNotificationType = CloudConnectionStatusURL.TYPE_LED;

				m_ledIndex = (Integer) props.get("led");
				m_notificationLED = m_gpioService.getPinByTerminal(m_ledIndex, KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN, KuraGPIOTrigger.NONE);

				m_notificationLED.open();
				s_logger.info("CloudConnectionStatus active on LED {}.", m_ledIndex);
				break;
			case CloudConnectionStatusURL.TYPE_LOG:
				m_currentNotificationType = CloudConnectionStatusURL.TYPE_LOG;

				s_logger.info("CloudConnectionStatus active on log.");
				break;
			case CloudConnectionStatusURL.TYPE_NONE:
				m_currentNotificationType = CloudConnectionStatusURL.TYPE_NONE;

				s_logger.info("Cloud Connection Status notification disabled");
				break;
			}
		} catch (Exception ex) {
			s_logger.error("Error activating Cloud Connection Status!");
		}

		register(m_idleComponent);
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Deactivating CloudConnectionStatus service...");

		unregister(m_idleComponent);
	}

	// ----------------------------------------------------------------
	//
	// Cloud Connection Status APIs
	//
	// ----------------------------------------------------------------

	@Override
	public void register(CloudConnectionStatusComponent component) {
		s_componentRegistry.add(component);
		internalUpdateStatus();
	}

	@Override
	public void unregister(CloudConnectionStatusComponent component) {
		s_componentRegistry.remove(component);
		internalUpdateStatus();
	}

	@Override
	public boolean updateStatus(CloudConnectionStatusComponent component, CloudConnectionStatusEnum status) {
		try {
			component.setNotificationStatus(status);
			internalUpdateStatus();
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	// ----------------------------------------------------------------
	//
	// Private Methods
	//
	// ----------------------------------------------------------------

	private void internalUpdateStatus() {

		CloudConnectionStatusComponent maxPriorityComponent = m_idleComponent;

		for (CloudConnectionStatusComponent c : s_componentRegistry) {
			if (c.getNotificationPriority() > maxPriorityComponent.getNotificationPriority()) {
				maxPriorityComponent = c;
			}
		}

		if (m_currentStatus == null || m_currentStatus != maxPriorityComponent.getNotificationStatus()) {
			m_currentStatus = maxPriorityComponent.getNotificationStatus();

			if (m_notificationWorker != null) {
				m_notificationWorker.cancel(true);
				m_notificationWorker = null;
			}

			// Avoid NPE if CloudConnectionStatusComponent doesn't initialize
			// its internal status.
			// Defaults to OFF
			m_currentStatus = m_currentStatus == null ? CloudConnectionStatusEnum.OFF : m_currentStatus;

			m_notificationWorker = m_notificationExecutor.submit(this.getWorker(m_currentStatus));
		}
	}

	private Runnable getWorker(CloudConnectionStatusEnum status) {
		if (m_currentNotificationType == CloudConnectionStatusURL.TYPE_LED) {
			if (m_notificationLED != null && m_notificationLED.isOpen()) {
				switch (status) {
				case ON:
					return new OnOffStatusRunnable(m_notificationLED, true);
				case OFF:
					return new OnOffStatusRunnable(m_notificationLED, false);
				case SLOW_BLINKING:
					return new BlinkStatusRunnable(m_notificationLED, CloudConnectionStatusEnum.SLOW_BLINKING_ON_TIME,
							CloudConnectionStatusEnum.SLOW_BLINKING_OFF_TIME);
				case FAST_BLINKING:
					return new BlinkStatusRunnable(m_notificationLED, CloudConnectionStatusEnum.FAST_BLINKING_ON_TIME,
							CloudConnectionStatusEnum.FAST_BLINKING_OFF_TIME);
				case HEARTBEAT:
					return new HeartbeatStatusRunnable(m_notificationLED);
				}
			} else {
				// LED not available. Falling back to log notification.
				return new LogStatusRunnable(status);
			}
		} else if (m_currentNotificationType == CloudConnectionStatusURL.TYPE_LOG) {
			return new LogStatusRunnable(status);
		} else if (m_currentNotificationType == CloudConnectionStatusURL.TYPE_NONE) {
			return new Runnable() {
				@Override
				public void run() { /* Empty runnable */
				}
			};
		}

		return new Runnable() {
			@Override
			public void run() {
				s_logger.error("Error getting worker for Cloud Connection Status");
			}
		};
	}
}
