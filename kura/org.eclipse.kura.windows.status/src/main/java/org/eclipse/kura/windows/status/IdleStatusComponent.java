package org.eclipse.kura.windows.status;

import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;

public class IdleStatusComponent implements CloudConnectionStatusComponent {

	@Override
	public int getNotificationPriority() {
		return CloudConnectionStatusService.PRIORITY_MIN;
	}

	@Override
	public CloudConnectionStatusEnum getNotificationStatus() {
		return CloudConnectionStatusEnum.OFF;
	}

	@Override
	public void setNotificationStatus(CloudConnectionStatusEnum status) {
		//We need a always present minimum priority status of OFF, se we don't want
		//the default notification status to be changed.
		
		//Do nothing
	}

}
