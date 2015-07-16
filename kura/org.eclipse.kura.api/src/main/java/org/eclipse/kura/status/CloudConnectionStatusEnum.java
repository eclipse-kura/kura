package org.eclipse.kura.status;

public enum CloudConnectionStatusEnum {
	OFF,
	FAST_BLINKING,
	SLOW_BLINKING,
	HEARTBEAT,
	ON;
	
	public static final int FAST_BLINKING_ON_TIME = 100;
	public static final int FAST_BLINKING_OFF_TIME = 100;

	public static final int SLOW_BLINKING_ON_TIME = 300;
	public static final int SLOW_BLINKING_OFF_TIME = 300;

	public static final int HEARTBEAT_SYSTOLE_DURATION = 150;
	public static final int HEARTBEAT_DIASTOLE_DURATION = 150;
	public static final int HEARTBEAT_PAUSE_DURATION = 600;
	
	public static final int PERIODIC_STATUS_CHECK_DELAY = 5000;
}
