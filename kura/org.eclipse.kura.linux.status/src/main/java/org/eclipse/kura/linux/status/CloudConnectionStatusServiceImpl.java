package org.eclipse.kura.linux.status;

import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;

import org.eclipse.kura.linux.status.runnables.BlinkStatusRunnable;
import org.eclipse.kura.linux.status.runnables.HeartbeatStatusRunnable;
import org.eclipse.kura.linux.status.runnables.LogStatusRunnable;
import org.eclipse.kura.linux.status.runnables.OnOffStatusRunnable;
import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudConnectionStatusServiceImpl implements CloudConnectionStatusService {

	private static final String STATUS_NOTIFICATION_URL	= "ccs.status.notification.url";
	
	private static final Logger s_logger = LoggerFactory.getLogger(CloudConnectionStatusServiceImpl.class);
	
	private SystemService m_systemService;
	
	private GPIOPin notificationLED;
	
	private ExecutorService notificationExecutor;
	private Future<?> notificationWorker;
	
	private IdleStatusComponent idleComponent;
	
	private static int currentNotificationType;
	private static CloudConnectionStatusEnum currentStatus = null;
	
	private static final HashSet<CloudConnectionStatusComponent> componentRegistry = new HashSet<CloudConnectionStatusComponent>();
	
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------
	public CloudConnectionStatusServiceImpl() {
		super();
		notificationExecutor = Executors.newSingleThreadExecutor();
		idleComponent = new IdleStatusComponent();
	}	
	
	public void setSystemService(SystemService systemService){
		this.m_systemService = systemService;
	}
	
	public void unsetSystemService(SystemService systemService){
		this.m_systemService = null;
	}

	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	
	protected void activate(ComponentContext componentContext) 
	{		
		s_logger.info("Activating CloudConnectionStatus service...");
		
		
		String urlFromConfig = m_systemService.getProperties().getProperty(STATUS_NOTIFICATION_URL, "");
		
		Properties props = CloudConnectionStatusURL.parseURL(urlFromConfig);
		
		try{
		 int notificationType = (Integer) props.get("notification_type");
		 
		 switch (notificationType){
		 case CloudConnectionStatusURL.TYPE_LED:
			 currentNotificationType = CloudConnectionStatusURL.TYPE_LED;
			 
			 GPIOPinConfig config = new GPIOPinConfig(DeviceConfig.DEFAULT,						
						(Integer) props.get("led"), 
						GPIOPinConfig.DIR_OUTPUT_ONLY, 
						GPIOPinConfig.MODE_OUTPUT_OPEN_DRAIN, 
						GPIOPinConfig.TRIGGER_NONE, false);
			 
			 notificationLED = DeviceManager.open(GPIOPin.class, config);
			 
			 s_logger.info("CloudConnectionStatus active on LED {}.", props.get("led"));
			 break;
		 case CloudConnectionStatusURL.TYPE_LOG:
			 currentNotificationType = CloudConnectionStatusURL.TYPE_LOG;
			 
			 s_logger.info("CloudConnectionStatus active on log.");			 
			 break;
		 case CloudConnectionStatusURL.TYPE_NONE:
			 currentNotificationType = CloudConnectionStatusURL.TYPE_NONE;

			 s_logger.info("Cloud Connection Status notification disabled");
			 break;
		 }
		}catch(Exception ex){
			s_logger.error("Error activating Cloud Connection Status!");
		}
		
		register(idleComponent);
	}
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("Deactivating CloudConnectionStatus service...");
		
		unregister(idleComponent);
	}

	// ----------------------------------------------------------------
	//
	//   Cloud Connection Status APIs
	//
	// ----------------------------------------------------------------
	
	@Override
	public void register(CloudConnectionStatusComponent component) {
		componentRegistry.add(component);
		internalUpdateStatus();
	}

	@Override
	public void unregister(CloudConnectionStatusComponent component) {
		componentRegistry.remove(component);
		internalUpdateStatus();
	}

	@Override
	public boolean updateStatus(CloudConnectionStatusComponent component, CloudConnectionStatusEnum status) {
		try{
			component.setNotificationStatus(status);
			internalUpdateStatus();
		}catch(Exception ex){
			return false;
		}
		return true;
	}

	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------

	private void internalUpdateStatus(){
		
		CloudConnectionStatusComponent maxPriorityComponent = idleComponent;
		
		for(CloudConnectionStatusComponent c : componentRegistry){
			if(c.getNotificationPriority() > maxPriorityComponent.getNotificationPriority()){
				maxPriorityComponent = c;
			}
		}
		
		if(currentStatus == null || currentStatus != maxPriorityComponent.getNotificationStatus()){
			currentStatus = maxPriorityComponent.getNotificationStatus();
			
			if(notificationWorker != null){
				notificationWorker.cancel(true);
				notificationWorker = null;
			}
			
			notificationWorker = notificationExecutor.submit(this.getWorker(currentStatus));
		}
	}
	
	private Runnable getWorker(CloudConnectionStatusEnum status){
		if(currentNotificationType == CloudConnectionStatusURL.TYPE_LED){
			switch(status){
			case ON:
				return new OnOffStatusRunnable(notificationLED, true);
			case OFF:
				return new OnOffStatusRunnable(notificationLED, false);
			case SLOW_BLINKING:
				return new BlinkStatusRunnable(notificationLED, CloudConnectionStatusEnum.SLOW_BLINKING_ON_TIME, CloudConnectionStatusEnum.SLOW_BLINKING_OFF_TIME);
			case FAST_BLINKING:
				return new BlinkStatusRunnable(notificationLED, CloudConnectionStatusEnum.FAST_BLINKING_ON_TIME, CloudConnectionStatusEnum.FAST_BLINKING_OFF_TIME);
			case HEARTBEAT:
				return new HeartbeatStatusRunnable(notificationLED);
			}
		}else if(currentNotificationType == CloudConnectionStatusURL.TYPE_LOG){
			return new LogStatusRunnable(status);
		}
		
		return new Runnable(){
			@Override
			public void run() {
				s_logger.error("Error getting worker for Cloud Connection Status");
			}};
	}
}
