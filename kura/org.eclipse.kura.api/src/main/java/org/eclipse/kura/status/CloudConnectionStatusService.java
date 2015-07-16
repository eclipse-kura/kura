package org.eclipse.kura.status;

public interface CloudConnectionStatusService {

	/**
	 * Maximum priority for the notification. There should be only one StatusDisplayComponent at the same time
	 * using this priority.
	 */
	public static final int PRIORITY_MAX 		= Integer.MAX_VALUE;
	
	/**
	 * Maximum priority for the notification. There should be only one StatusDisplayComponent at the same time
	 * using this priority.
	 */
	public static final int PRIORITY_CRITICAL 	= 400;
	
	public static final int PRIORITY_HIGH 		= 300;
	
	public static final int PRIORITY_MEDIUM 	= 200;
	
	public static final int PRIORITY_LOW 		= 100;
	
	/**
	 * Minimum priority for the notification. There should be at least and only one StatusDisplayComponent at the same time
	 * using this priority.
	 */
	public static final int PRIORITY_MIN 		= Integer.MIN_VALUE;
	
	public void register(CloudConnectionStatusComponent component);
	
	public void unregister(CloudConnectionStatusComponent component);
	
	public boolean updateStatus(CloudConnectionStatusComponent component, CloudConnectionStatusEnum status);
}
