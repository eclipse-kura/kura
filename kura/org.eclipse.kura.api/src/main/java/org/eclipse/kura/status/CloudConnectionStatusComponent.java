package org.eclipse.kura.status;

public interface CloudConnectionStatusComponent {
	
	public int getNotificationPriority();
	
	public CloudConnectionStatusEnum getNotificationStatus();
	public void setNotificationStatus(CloudConnectionStatusEnum status);
	
}
