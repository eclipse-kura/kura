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
package org.eclipse.kura.example.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.clock.ClockService;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleComponent implements CloudClientListener, EventHandler {

	private static final Logger s_logger = LoggerFactory.getLogger(ExampleComponent.class);

	// Cloud Application identifier
	private static final String APP_ID = "EXAMPLE_COMPONENT";
	private static final int POLL_DELAY_SEC = 10;
	
	private CloudService m_cloudService;
	private PositionService m_positionService;
	private ConfigurationService m_configurationService;
	private ClockService m_clockService;
	private CloudClient m_cloudClient;
	private NetworkService m_networkService;
	private UsbService m_usbService;
	
	private ConnectionFactory m_connectionFactory;
	private ScheduledThreadPoolExecutor m_worker;
	private ScheduledFuture<?> m_handle;
	private ScheduledExecutorService m_gpsWorker;
	private ScheduledFuture<?> m_gpsHandle;
	private ScheduledThreadPoolExecutor m_systemPropsWorker;
	private ScheduledFuture<?> m_systemPropsHandle;
	private Thread 	m_serialThread;
	private int counter;
	private StringBuilder m_serialSb;
	
	InputStream in;
	OutputStream out;
	CommConnection conn=null;
	
	public void setCloudService(CloudService cloudService) {
		m_cloudService = cloudService;
	}

	public void unsetCloudService(CloudService cloudService) {
		m_cloudService = null;
	}
	
	public void setPositionService(PositionService positionService) {
		m_positionService = positionService;
	}

	public void unsetPositionService(PositionService positionService) {
		m_positionService = null;
	}
	
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.m_connectionFactory = connectionFactory;
	}
	
	public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
		this.m_connectionFactory = null;
	}
	
	public void setConfigurationService(ConfigurationService configurationService) {
		m_configurationService = configurationService;
	}

	public void unsetConfigurationService(ConfigurationService configurationService) {
		m_configurationService = null;
	}
	
	public void setClockService(ClockService clockService) {
		m_clockService = clockService;
	}

	public void unsetClockService(ClockService clockService) {
		m_clockService = null;
	}
	
	public void setNetworkService(NetworkService networkService) {
		m_networkService = networkService;
	}

	public void unsetNetworkService(NetworkService networkService) {
		m_networkService = null;
	}
	
	public void setUsbService(UsbService usbService) {
		m_usbService = usbService;
	}

	public void unsetUsbService(UsbService usbService) {
		m_usbService = null;
	}

	private boolean clockIsSynced = false;
	
	protected void activate(ComponentContext componentContext) {
		s_logger.debug("Activating ExampleComponent");
		
		List<UsbTtyDevice> ttyDevices = m_usbService.getUsbTtyDevices();
		if(ttyDevices != null && !ttyDevices.isEmpty()) {
			for(UsbTtyDevice device : ttyDevices) {
				System.out.println("Device: " + device.getVendorId() + ":" + device.getProductId());
				System.out.println("\t" + device.getDeviceNode());
				System.out.println("\t" + device.getManufacturerName());
				System.out.println("\t" + device.getProductName());
				System.out.println("\t" + device.getUsbPort());
			}
		}
		
		/*
		m_worker = new ScheduledThreadPoolExecutor(1);
		
		m_worker.schedule(new Runnable() {

			@Override
			public void run() {
				try {
					System.out.println("m_networkService.getState(): " + m_networkService.getState());
					
					List<String> interfaceNames = m_networkService.getAllNetworkInterfaceNames();
					if(interfaceNames != null && interfaceNames.size() > 0) {
						for(String interfaceName : interfaceNames) {
							System.out.println("Interface Name: " + interfaceName + " with State: " + m_networkService.getState(interfaceName));
						}
					}
					
					List<NetInterface<? extends NetInterfaceAddress>> activeNetworkInterfaces = m_networkService.getActiveNetworkInterfaces();
					if(activeNetworkInterfaces != null && activeNetworkInterfaces.size() > 0) {
						for(NetInterface<? extends NetInterfaceAddress> activeNetworkInterface : activeNetworkInterfaces) {
							System.out.println("ActiveNetworkInterface: " + activeNetworkInterface);
						}
					}
					
					List<NetInterface<? extends NetInterfaceAddress>> networkInterfaces = m_networkService.getNetworkInterfaces();
					if(networkInterfaces != null && networkInterfaces.size() > 0) {
						for(NetInterface<? extends NetInterfaceAddress> networkInterface : networkInterfaces) {
							System.out.println("NetworkInterface: " + networkInterface);
						}
					}
					
					List<WifiAccessPoint> wifiAccessPoints = m_networkService.getAllWifiAccessPoints();
					if(wifiAccessPoints != null && wifiAccessPoints.size() > 0) {
						for(WifiAccessPoint wifiAccessPoint : wifiAccessPoints) {
							System.out.println("WifiAccessPoint: " + wifiAccessPoint);
						}
					}
					
					List<WifiAccessPoint> wlan0wifiAccessPoints = m_networkService.getAllWifiAccessPoints();
					if(wlan0wifiAccessPoints != null && wlan0wifiAccessPoints.size() > 0) {
						for(WifiAccessPoint wifiAccessPoint : wlan0wifiAccessPoints) {
							System.out.println("wlan0 WifiAccessPoint: " + wifiAccessPoint);
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			
		}, 0, TimeUnit.SECONDS);
		*/
		
		doGpsUpdate();
		
		/*
		// install event listener for serial ports and specific topics of interest
		Dictionary props = new Hashtable<String, String>();
		props.put(EventConstants.EVENT_TOPIC, "CLOCK_SERVICE_EVENT");
		BundleContext bc = componentContext.getBundleContext();
		bc.registerService(EventHandler.class.getName(), this, props);
		
		try {
			if(m_clockService.getLastSync() != null) {
				clockIsSynced = true;
			}
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			List<ComponentConfiguration> configs = m_configurationService.getComponentConfigurations();
			for(ComponentConfiguration config : configs) {
				System.out.println(config.getPid());
			}
		} catch (KuraException e) {
			e.printStackTrace();
		}*/
		
		//doGpsUpdate();
		
		/*
		m_systemPropsWorker = new ScheduledThreadPoolExecutor(1);		
		m_systemPropsHandle = m_systemPropsWorker.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {

					String[] values = {"Zero", "One", "Two", "Three", "Four"};

					for(int i=0; i<5; i++) { 
						Map<String, Object> map = new Hashtable<String, Object>();
						System.out.println("SETTING TO " + values[i]);
						map.put("0", values[i]);
						m_systemPropertiesService.setValues(map);
						if(m_systemPropertiesService.getValue("0").equals(values[i])) {
							System.out.println("SUCCESS... " + m_systemPropertiesService.getValue("0"));
						} else {
							System.out.println("FAILURE!!! " + m_systemPropertiesService.getValue("0"));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}    
		}, 10, (Integer) 10, TimeUnit.SECONDS);
		*/

		/*
		// get the mqtt client for this application
		try  {
			s_logger.info("Getting CloudApplicationClient for {}...", APP_ID);
			m_cloudClient = m_cloudService.getCloudApplicationClient(APP_ID);
			m_cloudClient.addCloudCallbackHandler(this);

			// initialize a COM port
			Properties props = new Properties();
			props.setProperty("port", "/dev/ttyUSB0");
			props.setProperty("baudRate", "9600");
			props.setProperty("stopBits", "1");
			props.setProperty("parity", "0");
			props.setProperty("bitsPerWord", "8");
			try {
				initSerialCom(props);
			} catch (ProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			m_serialSb = new StringBuilder();
			if(conn!=null){
				m_serialThread = new Thread(new Runnable() {		
					@Override
					public void run() {
						while(conn!=null){
							doSerial();
						}
					}
				});
				m_serialThread.start();
			}
			counter = 0;
			doUpdate();
			doGpsUpdate();
		} catch (KuraException e) {
			s_logger.error("Cannot activate", e);
			throw new ComponentException(e);
		}*/
	}
	
	private boolean serialPortExists(String portName){
		if(portName!=null){						
			File f = new File(portName);
			if(f.exists())
				return true;
//		List<UsbTtyDevice> utd=m_usbService.getUsbTtyDevices();	
//			if(utd!=null){
//				for (UsbTtyDevice u : utd) {
//					if(portName.contains(u.getDeviceNode()))
//						return true;
//				}
//			}
		}
		return false;
	}
	
	private void initSerialCom(Properties connectionConfig) throws KuraException {
		String sPort;
		String sBaud;
		String sStop;
		String sParity;
		String sBits;				

		if (((sPort = connectionConfig.getProperty("port")) == null)
				|| ((sBaud = connectionConfig.getProperty("baudRate")) == null)
				|| ((sStop = connectionConfig.getProperty("stopBits")) == null)
				|| ((sParity = connectionConfig.getProperty("parity")) == null)
				|| ((sBits = connectionConfig.getProperty("bitsPerWord")) == null))
			throw new KuraException(KuraErrorCode.SERIAL_PORT_INVALID_CONFIGURATION);
		
		int baud = Integer.valueOf(sBaud).intValue();
		int stop = Integer.valueOf(sStop).intValue();
		int parity = Integer.valueOf(sParity).intValue();
		int bits = Integer.valueOf(sBits).intValue();
		if(!serialPortExists(sPort)){
			throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING);
		}
		
		String uri = new CommURI.Builder(sPort)
								.withBaudRate(baud)
								.withDataBits(bits)
								.withStopBits(stop)
								.withParity(parity)
								.withTimeout(2000)
								.build().toString();

		try {
			conn = (CommConnection) m_connectionFactory.createConnection(uri, 1, false);
			s_logger.info(sPort+" initialized");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// get the streams
		try {
			in = conn.openInputStream();
			out = conn.openOutputStream();
			byte[] array = "Port opened \r\n".getBytes();
			out.write(array);
			out.flush();
			} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	private void doSerial() {
		synchronized (in) {
			try {
				if(in.available()==0) {								
					try {
						Thread.sleep(10);	// avoid a high cpu load
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return;
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			int c=0;
			try {
				c = in.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// on reception of CR, publish the received sentence
			if(c==13){
				s_logger.debug("Received : "+m_serialSb.toString());
				KuraPayload payload = new KuraPayload();
				payload.addMetric("sentence", m_serialSb.toString());
				try {
					m_cloudClient.publish("message", payload, 0, false);
				} catch (KuraException e) {
					e.printStackTrace();
				}
				m_serialSb = new StringBuilder();
			}
			else if(c!=10){
				m_serialSb.append((char)c);
			}			
		}
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("Deactivating ExampleComponent");
		if (conn!=null) {
			try {
				conn.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			conn = null;
		}
	}
	
	public void updated(Map<String,Object> properties)
	{
		s_logger.info("updated...");		
//		m_properties = properties;
//		modbusProperties = getModbusProperties();
//		m_serialPortExist=serialPortExists(); // check if /dev/ttyxxx exists
//		configured=false;
	}	
	
	private void doUpdate() {
		if (m_handle != null) {
			m_handle.cancel(true);
		}
		
		m_handle = m_worker.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				doPublish();
			}
		}, 0, (Integer) POLL_DELAY_SEC, TimeUnit.SECONDS);
	}
	
	private void doGpsUpdate() {
		if (m_gpsHandle != null) {
			m_gpsHandle.cancel(true);
		}
		
		m_gpsWorker = Executors.newSingleThreadScheduledExecutor();
		m_gpsHandle = m_gpsWorker.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				Position position = m_positionService.getPosition();
				s_logger.debug("Latitude: " + position.getLatitude());
				s_logger.debug("Longitude: " + position.getLongitude());
				s_logger.debug("Altitude: " + position.getAltitude());
				s_logger.debug("Speed: " + position.getSpeed());
				s_logger.debug("Track: " + position.getTrack());
				s_logger.debug("Time: " + m_positionService.getNmeaTime());
				s_logger.debug("Date: " + m_positionService.getNmeaDate());
				s_logger.debug("Last Sentence: " + m_positionService.getLastSentence());
			}
		}, 0, (Integer) POLL_DELAY_SEC, TimeUnit.SECONDS);
	}
	
	public void doPublish() {

		try {
			if(m_cloudClient != null){
				KuraPayload payload = new KuraPayload();
				payload.addMetric("counter", counter);
				m_cloudClient.publish("sensor", payload, 0, false);
				counter++;
				if(counter==4){
					if (conn!=null) {
						try {
							conn.close();
							s_logger.info("conn closed");
						} catch (IOException e) {
							e.printStackTrace();
						}
						conn = null;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
		s_logger.debug("control arrived for " + deviceId + " on topic " + appTopic);
	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
		s_logger.debug("publish arrived for " + deviceId + " on topic " + appTopic);
	}

	@Override
	public void onConnectionLost() {
		s_logger.debug("connection lost");
	}

	@Override
	public void onConnectionEstablished() {
		s_logger.debug("connection restored");
	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {
		s_logger.debug("published: " + messageId);
	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {
		s_logger.debug("published: " + messageId);
	}

	@Override
	public void handleEvent(Event event) {
		System.out.println("Got clock event: " + event);
		clockIsSynced = true;
	}
}
