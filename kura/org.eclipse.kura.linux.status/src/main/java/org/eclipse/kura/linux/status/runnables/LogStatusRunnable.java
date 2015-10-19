package org.eclipse.kura.linux.status.runnables;

import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogStatusRunnable implements Runnable {

	private static final Logger s_logger = LoggerFactory.getLogger(LogStatusRunnable.class);
	
	private final CloudConnectionStatusEnum m_status;
	
	public LogStatusRunnable(CloudConnectionStatusEnum status) {
		this.m_status = status;
	}

	@Override
	public void run() {
		switch (m_status) {
		case ON:
			s_logger.info("Notification LED on");
			break;
		case SLOW_BLINKING:
			s_logger.info("Notification LED slow blinking");
			break;
		case FAST_BLINKING:
			s_logger.info("Notification LED fast blinking");
			break;
		case HEARTBEAT:
			s_logger.info("Notification LED heartbeating");
			break;
		default:
			s_logger.info("Notification LED off");
		}
	}

}
