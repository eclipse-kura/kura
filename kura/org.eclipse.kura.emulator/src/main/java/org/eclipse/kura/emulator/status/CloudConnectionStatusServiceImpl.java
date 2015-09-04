package org.eclipse.kura.emulator.status;

import java.util.HashSet;

import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudConnectionStatusServiceImpl implements CloudConnectionStatusService, CloudConnectionStatusComponent {

	private static final Logger s_logger = LoggerFactory.getLogger(CloudConnectionStatusServiceImpl.class);
	private static HashSet<CloudConnectionStatusComponent> registered = new HashSet<CloudConnectionStatusComponent>();
	private static CloudConnectionStatusEnum m_status = CloudConnectionStatusEnum.OFF;
	
	//StatusDisplayComponent
	private CloudConnectionStatusEnum local_status = CloudConnectionStatusEnum.OFF;

	protected void activate(ComponentContext componentContext) {
		s_logger.debug("activating emulated StatudDisplayNotification");
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("deactivating emulated StatudDisplayNotification");
	}

	@Override
	public void register(CloudConnectionStatusComponent component) {
		registered.add(component);
	}

	@Override
	public void unregister(CloudConnectionStatusComponent component) {
		registered.remove(component);
	}

	@Override
	public boolean updateStatus(CloudConnectionStatusComponent component, CloudConnectionStatusEnum status) {
		component.setNotificationStatus(status);
		notifyAndChangeStatus();
		return true;
	}

	@Override
	public int getNotificationPriority() {
		return CloudConnectionStatusService.PRIORITY_MIN;
	}

	private int getMaxPriority() {
		int maxPriority = Integer.MIN_VALUE;
		for (CloudConnectionStatusComponent c : registered) {
			maxPriority = Math.max(maxPriority, c.getNotificationPriority());
		}
		return maxPriority;
	}

	private String getStatusString(CloudConnectionStatusEnum status) {
		switch (status) {
		case ON:
			return "Notification LED on";
		case SLOW_BLINKING:
			return "Notification LED slow blinking";
		case FAST_BLINKING:
			return "Notification LED fast blinking";
		case HEARTBEAT:
			return "Notification LED heartbeating";
		default:
			return "Notification LED off";
		}
	}
	
	private void notifyAndChangeStatus(){
		int maxPriority = getMaxPriority();
		for(CloudConnectionStatusComponent c : registered){
			if(c.getNotificationPriority() == maxPriority){
				m_status = c.getNotificationStatus();
				s_logger.info("Emulated status notification: {}",getStatusString(m_status));				
			}
		}
	}

	@Override
	public CloudConnectionStatusEnum getNotificationStatus() {
		return local_status;
	}

	@Override
	public void setNotificationStatus(CloudConnectionStatusEnum status) {
		local_status = status;
	}
}
