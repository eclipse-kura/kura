/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.protocol.modbus.test;


import java.lang.Thread.State;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.protocol.modbus.ModbusProtocolDeviceService;
import org.eclipse.kura.protocol.modbus.ModbusProtocolErrorCode;
import org.eclipse.kura.protocol.modbus.ModbusProtocolException;
import org.eclipse.kura.protocol.modbus.ModbusTransmissionMode;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.watchdog.CriticalComponent;
import org.eclipse.kura.watchdog.WatchdogService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusManager implements ConfigurableComponent, CriticalComponent, CloudClientListener{

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusManager.class);

	private Thread 						m_thread;
	private boolean						m_threadShouldStop;
	private Map<String,Object>    		m_properties;
	private CloudService                m_cloudService;
	private SystemService 		        m_systemService;	
	private ModbusProtocolDeviceService m_protocolDevice;
	private CloudClient 				m_cloudAppClient=null;
	private WatchdogService		   		m_watchdogService;

	private int slaveAddr;
	private int pollInterval;	//milliseconds
	private int publishInterval;	//seconds
	private boolean initLeds;
	private boolean clientIsConnected = false;
	private boolean configured;
	private boolean metricsChanged;
	private static boolean[] lastDigitalInputs = new boolean[8];
	private static boolean[] lastDigitalOutputs = new boolean[6];
	private static int[] lastAnalogInputs = new int[8];
	private static boolean iJustConnected = false;
	private long publishTime = 0L;	

	private static boolean doConnection = true;
	
	private static Properties modbusProperties;
	private static boolean wdConfigured = false;
	private static boolean connectionFailed = false;

	public void setModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
		this.m_protocolDevice = modbusService;
	}
	
	public void unsetModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
		this.m_protocolDevice = null;
	}

	public void setCloudService(CloudService cloudService) {
		this.m_cloudService = cloudService;
	}		// install event listener for serial ports

	public void unsetCloudService(CloudService cloudService) {
		this.m_cloudService = null;
	}

	public void setSystemService(SystemService sms) {
		this.m_systemService = sms;
	}	
	
	public void unsetSystemService(SystemService sms) {
		this.m_systemService = null;
	}
	
	public void setWatchdogService(WatchdogService watchdogService) {
		this.m_watchdogService = watchdogService;
	}
	
	public void unsetWatchdogService(WatchdogService watchdogService) {
		this.m_watchdogService = null;
	}

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) {
		m_properties = properties;
		configured = false;
		
		modbusProperties = getModbusProperties();
		pollInterval = Integer.valueOf(modbusProperties.getProperty("pollInterval")).intValue();
		publishInterval = Integer.valueOf(modbusProperties.getProperty("publishInterval")).intValue();
		slaveAddr = Integer.valueOf(modbusProperties.getProperty("slaveAddr")).intValue();
		
		m_threadShouldStop=false;
		m_thread = new Thread(new Runnable() {		
			@Override
			public void run() {
				Thread.currentThread().setName(getClass().getSimpleName());
				doModbusPollWork();
			}
		});
		m_thread.start();
		
		s_logger.info("ModbusManager activated");
	}
	
	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Modbus deactivate");
		m_watchdogService.unregisterCriticalService(this);
		m_threadShouldStop=true;
		while(m_thread.getState()!=State.TERMINATED){
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		s_logger.info("Modbus polling thread killed");
		
		if(m_protocolDevice!=null)
			try {
				m_protocolDevice.disconnect();
			} catch (ModbusProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		doConnection=true;
		configured = false;
	}
	
	public void updated(Map<String,Object> properties)
	{
		s_logger.info("updated...");		
		m_properties = properties;
		modbusProperties = getModbusProperties();
		pollInterval = Integer.valueOf(modbusProperties.getProperty("pollInterval")).intValue();
		publishInterval = Integer.valueOf(modbusProperties.getProperty("publishInterval")).intValue();
		slaveAddr = Integer.valueOf(modbusProperties.getProperty("slaveAddr")).intValue();
		configured=false;
	}	
	
	private boolean doConnectionWork() {
		String topic = null;
		try {
			// wait for a valid topic configured
			if(modbusProperties==null){
				return true;
			}

			topic = modbusProperties.getProperty("controlTopic");
			if(topic==null){
				return true;
			}
			
			if(m_cloudAppClient==null) {
				// Attempt to get Master Client reference
				s_logger.debug("Getting Cloud Client");
				try {
					m_cloudAppClient = m_cloudService.newCloudClient("ModbusManager");
					m_cloudAppClient.addCloudClientListener(this);
				} catch (KuraException e) {
					s_logger.debug("Cannot get a Cloud Client");
					e.printStackTrace();
				}
				
			}

			//s_logger.debug("Checking for an active MQtt connection...");
			clientIsConnected = m_cloudAppClient.isConnected(); 

			if(!clientIsConnected) {
				s_logger.debug("Waiting for Cloud Client to connect");
				return true;
			}

		} catch (Exception e) {
			s_logger.debug("Cloud client is not yet available..");
			return true;
		}

		// Successfully connected - kill the thread
		s_logger.info("Successfully connected the Cloud Client");
		try {
			iJustConnected = true;
			m_cloudAppClient.controlSubscribe(topic + "/led/#", 0);
			m_cloudAppClient.controlSubscribe(topic + "/resetcnt/#", 0);
			m_cloudAppClient.controlSubscribe(topic + "/alarm", 0);
			
			String assetIdEth0 = m_systemService.getPrimaryMacAddress();
			m_cloudAppClient.subscribe("RulesAssistant/"+ topic + "/led/#",0);
			m_cloudAppClient.subscribe("RulesAssistant/"+ topic + "/resetcnt/#",0);
			m_cloudAppClient.subscribe("RulesAssistant/"+ topic + "/alarm",0);
			s_logger.info("Successfully subscribed with assetIdEth0="+assetIdEth0);
		} catch (KuraException e) {
			s_logger.debug("Error issuing MQTT subscription");
			e.printStackTrace();
			return true;
		}
		
		return false;
	}

	private void doModbusPollWork() {
		while(!m_threadShouldStop){
			if(modbusProperties!=null){
				// Establish the MQTT connection		
				if(doConnection)
					doConnection=doConnectionWork();

				if(!configured) {
					initLeds = false;
					try {
						if(!connectionFailed)
							s_logger.debug("configureDevice");
						configureDevice();
						connectionFailed=false;
					} catch(ModbusProtocolException e) {
						if(!connectionFailed)
							s_logger.warn("The modbus port is not yet available");
						connectionFailed=true;
					}
				}

				if(configured) {
					try {
						if (!initLeds) {
							initializeLeds();
						}
						if(initLeds){
							if (!wdConfigured){
								if(m_watchdogService!=null){
									m_watchdogService.registerCriticalService(this);
									//m_watchdogService.startWatchdog();
									wdConfigured=true;
								}
							}

							performPoll();
						}
					} catch (ModbusProtocolException e) {
						if(e.getCode()==ModbusProtocolErrorCode.NOT_CONNECTED){
							s_logger.error("Error on modbus polling, closing connection ");
							configured=false;
						}
						else
							s_logger.error("Error on modbus polling : "+e.getCode());
					}
				}
			}
			try {
				Thread.sleep(pollInterval);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void configureDevice() throws ModbusProtocolException {
		if(m_protocolDevice!=null){
			m_protocolDevice.disconnect();

			m_protocolDevice.configureConnection(modbusProperties);

			configured = true;
		}
	}

	
	private Properties getModbusProperties() {
		Properties prop = new Properties();

		if(m_properties!=null){
			String portName = null;
			String serialMode = null;
			String baudRate = null;
			String bitsPerWord = null;
			String stopBits = null;
			String parity = null;
			String ptopic = null;
			String ctopic = null;
			String ipAddress = null;
			String pollInt= null;
			String pubInt= null;
			String Slave= null;
			String Mode= null;
			String timeout= null;
			if(m_properties.get("slaveAddr") != null) Slave 		= (String) m_properties.get("slaveAddr");
			if(m_properties.get("transmissionMode") != null) Mode	= (String) m_properties.get("transmissionMode");
			if(m_properties.get("respTimeout") != null) timeout		= (String) m_properties.get("respTimeout");
			if(m_properties.get("port") != null) portName 			= (String) m_properties.get("port");
			if(m_properties.get("serialMode") != null) serialMode 	= (String) m_properties.get("serialMode");
			if(m_properties.get("baudRate") != null) baudRate 		= (String) m_properties.get("baudRate");
			if(m_properties.get("bitsPerWord") != null) bitsPerWord = (String) m_properties.get("bitsPerWord");
			if(m_properties.get("stopBits") != null) stopBits 		= (String) m_properties.get("stopBits");
			if(m_properties.get("parity") != null) parity 			= (String) m_properties.get("parity");
			if(m_properties.get("publishTopic") != null) ptopic		= (String) m_properties.get("publishTopic");
			if(m_properties.get("controlTopic") != null) ctopic		= (String) m_properties.get("controlTopic");
			if(m_properties.get("ipAddress") != null) ipAddress		= (String) m_properties.get("ipAddress");
			if(m_properties.get("pollInterval") != null) pollInt	= (String) m_properties.get("pollInterval");
			if(m_properties.get("publishInterval") != null) pubInt	= (String) m_properties.get("publishInterval");
			
			if(portName==null) //portName="/dev/ttyUSB0";
				return null;		
			if(baudRate==null) baudRate="9600";
			if(stopBits==null) stopBits="1";
			if(parity==null) parity="0";
			if(bitsPerWord==null) bitsPerWord="8";
			if(Slave==null) Slave="1";
			if(Mode==null) Mode="RTU";
			if(timeout==null) timeout="1000";
			if(ptopic==null) ptopic="eurotech/demo";
			if(ctopic==null) ctopic="eurotech/demo";
			if(pollInt==null) pollInt="500";
			if(pubInt==null) pubInt="180";
			
			if(serialMode!=null) {
				if(serialMode.equalsIgnoreCase("RS232") || serialMode.equalsIgnoreCase("RS485")) {
					prop.setProperty("connectionType", "SERIAL");
					prop.setProperty("serialMode", serialMode);
					prop.setProperty("port", portName);
					prop.setProperty("exclusive", "false");
					prop.setProperty("mode", "0");
					prop.setProperty("baudRate", baudRate);
					prop.setProperty("stopBits", stopBits);
					prop.setProperty("parity", parity);
					prop.setProperty("bitsPerWord", bitsPerWord);
				} else {
					prop.setProperty("connectionType", "ETHERTCP");
					prop.setProperty("ipAddress", ipAddress);
					prop.setProperty("port", portName);
				}
			}
			prop.setProperty("slaveAddr", Slave);
			prop.setProperty("transmissionMode", Mode);
			prop.setProperty("respTimeout", timeout);
			prop.setProperty("publishTopic", ptopic);
			prop.setProperty("controlTopic", ctopic);
			prop.setProperty("pollInterval", pollInt);
			prop.setProperty("publishInterval", pubInt);

			return prop;
		} else {
			return null;
		}
	}

	private Properties getProtocolProperties() {
		Properties prop = new Properties();

		prop.setProperty("unitName", "Demo");
		prop.setProperty("unitAddress", "1");
		prop.setProperty("txMode", ModbusTransmissionMode.RTU);
		prop.setProperty("respTimeout", "1000");

		return prop;
	}

	public int bcd2Dec(int bcdVal) {
		byte bcd = (byte)bcdVal;
	    int decimal =	(bcd & 0x000F) + 
	    				(((int)bcd & 0x000F0) >> 4)*10 +
	    				(((int)bcd & 0x00F00) >> 8)*100 +
	    				(((int)bcd & 0x0F000) >> 12)*1000 +
	    				(((int)bcd & 0xF0000) >> 16)*10000;
    				
	    return decimal;
	}

	private void performPoll()throws ModbusProtocolException {
		KuraPayload payload = new KuraPayload();

		metricsChanged=false;

		int it4=0;
		int it5=0;
		int it6=0;
		boolean[] digitalInputs;

		digitalInputs = m_protocolDevice.readDiscreteInputs(slaveAddr, 2048, 8);

		payload.addMetric("t3", Boolean.valueOf(digitalInputs[2]));
		if((digitalInputs[2] != lastDigitalInputs[2]) || iJustConnected){
			metricsChanged=true;
			s_logger.info("t3=" + digitalInputs[2]);
		}
		lastDigitalInputs[2] = digitalInputs[2];

		payload.addMetric("t4", Boolean.valueOf(digitalInputs[3]));
		if(digitalInputs[3])it4=1; 
		payload.addMetric("it4", Integer.valueOf(it4));
		if((digitalInputs[3] != lastDigitalInputs[3]) || iJustConnected){
			s_logger.info("t4=" + digitalInputs[3]);
			metricsChanged=true;
		}
		lastDigitalInputs[3] = digitalInputs[3];

		payload.addMetric("t5", Boolean.valueOf(digitalInputs[4]));
		if(digitalInputs[4])it5=1; 
		payload.addMetric("it5", Integer.valueOf(it5));
		if((digitalInputs[4] != lastDigitalInputs[4]) || iJustConnected){
			s_logger.info("t5=" + digitalInputs[4]);
			metricsChanged=true;
		}
		lastDigitalInputs[4] = digitalInputs[4];

		payload.addMetric("t6", Boolean.valueOf(digitalInputs[5]));
		if(digitalInputs[5])it6=1; 
		payload.addMetric("it6", Integer.valueOf(it6));
		if((digitalInputs[5] != lastDigitalInputs[5]) || iJustConnected){
			s_logger.info("t6=" + digitalInputs[5]);
			metricsChanged=true;
		}
		lastDigitalInputs[5] = digitalInputs[5];

		int[] analogInputs = m_protocolDevice.readInputRegisters(slaveAddr, 512, 8);

		int c3 = bcd2Dec(analogInputs[2]);
		payload.addMetric("c3", Integer.valueOf(c3));
		if((c3 != lastAnalogInputs[2]) || iJustConnected){
			s_logger.info("c3=" + c3);
			metricsChanged=true;
		}
		lastAnalogInputs[2] = c3;

		int qc = bcd2Dec(analogInputs[7]);
		payload.addMetric("qc", Integer.valueOf(qc));
		if((qc != lastAnalogInputs[7]) || iJustConnected){
			s_logger.info("qc=" + qc);
			metricsChanged=true;
		}
		lastAnalogInputs[7] = qc;

		// LEDs 
		boolean[] digitalOutputs = m_protocolDevice.readCoils(slaveAddr, 2048, 6); 

		payload.addMetric("LED1", Boolean.valueOf(digitalOutputs[0])); 
		if((digitalOutputs[0] != lastDigitalOutputs[0]) || iJustConnected){
			s_logger.info("LED1=" + digitalOutputs[0]); 
			metricsChanged=true; 
		}
		lastDigitalOutputs[0] = digitalOutputs[0]; 

		payload.addMetric("LED2", Boolean.valueOf(digitalOutputs[1])); 
		if((digitalOutputs[1] != lastDigitalOutputs[1]) || iJustConnected){
			s_logger.info("LED2=" + digitalOutputs[1]); 
			metricsChanged=true; 
		}
		lastDigitalOutputs[1] = digitalOutputs[1]; 

		payload.addMetric("LED3", Boolean.valueOf(digitalOutputs[2])); 
		if((digitalOutputs[2] != lastDigitalOutputs[2]) || iJustConnected){
			s_logger.info("LED3=" + digitalOutputs[2]); 
			metricsChanged=true; 
		}
		lastDigitalOutputs[2] = digitalOutputs[2]; 

		payload.addMetric("LED4red", Boolean.valueOf(digitalOutputs[3])); 
		if((digitalOutputs[3] != lastDigitalOutputs[3]) || iJustConnected){
			s_logger.info("LED4red=" + digitalOutputs[3]); 
			metricsChanged=true; 
		}
		lastDigitalOutputs[3] = digitalOutputs[3]; 

		payload.addMetric("LED4green", Boolean.valueOf(digitalOutputs[4])); 
		if((digitalOutputs[4] != lastDigitalOutputs[4]) || iJustConnected){
			s_logger.info("LED4green=" + digitalOutputs[4]); 
			metricsChanged=true; 
		}
		lastDigitalOutputs[4] = digitalOutputs[4]; 

		payload.addMetric("LED4blue", Boolean.valueOf(digitalOutputs[5])); 
		if((digitalOutputs[5] != lastDigitalOutputs[5]) || iJustConnected){
			s_logger.info("LED4blue=" + digitalOutputs[5]); 
			metricsChanged=true; 
		}
		lastDigitalOutputs[5] = digitalOutputs[5];

		// refresh Watchdog 
		if (wdConfigured){
			if(m_watchdogService!=null){
				m_watchdogService.checkin(this);
			}
		}


		if (clientIsConnected) {
			iJustConnected = false;
			// publish data
			long now = System.currentTimeMillis();
			if(metricsChanged || ((now - publishTime) > (publishInterval*1000L)) ){
				try {
					String topic = modbusProperties.getProperty("publishTopic");
					if (metricsChanged) {
						s_logger.info("One of the metrics changed");
					}
					Date pubDate = new Date();
					payload.setTimestamp(pubDate);

					s_logger.info("Publishing on topic: " + topic);
					if(m_cloudAppClient!=null)
						m_cloudAppClient.publish(topic, payload, 0, false);
					publishTime = System.currentTimeMillis();

				} catch (Exception e) {
					e.printStackTrace();
				}
				metricsChanged=false;
			}
		}
	}
	
	private void initializeLeds() throws ModbusProtocolException {
		s_logger.debug("Initializing LEDs");	// once on startup, turn on each light
		for( int led = 1; led <= 6; led++)
		{
			TurnOnLED( led, true);
			try {
				Thread.sleep(200);
			} catch (Exception e) {
				e.printStackTrace();
			}
			TurnOnLED( led, false);
		}			
		initLeds = true;
	}

	
	public void TurnOnLED(int LED, boolean On)throws ModbusProtocolException
	{
		boolean TurnON = true;
		boolean TurnOFF = false;
		try
		{
			switch(LED) {
			case 1:		s_logger.debug("Setting LED" + LED + " to " + On);
						break;
			case 2:		s_logger.debug("Setting LED" + LED + " to " + On);
						break;
			case 3:		s_logger.debug("Setting LED" + LED + " to " + On);
						break;
			case 4:		s_logger.debug("Setting LED4 red to " + On);
						break;
			case 5:		s_logger.debug("Setting LED4 green to " + On);
						break;
			case 6:		s_logger.debug("Setting LED4 blue to " + On);
						break;
			default:	s_logger.warn("Error in TurnOnLED - LED " + LED + " is not valid.");
						break;
			}
			m_protocolDevice.writeSingleCoil(slaveAddr, 2047 + LED, On?TurnON:TurnOFF);
		}
		catch (ModbusProtocolException e) {
			throw(e);
		}
	}
	
	
	@Override
	public void onControlMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		s_logger.debug("EDC control message received on topic: " + appTopic);
		ProcessKuraPayload(deviceId, appTopic, msg, qos, retain);
	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		s_logger.debug("EDC message received on topic: " + appTopic);
		ProcessKuraPayload(deviceId, appTopic, msg, qos, retain) ;
	}

	@Override
	public void onConnectionLost() {
		s_logger.debug("Connection Lost");
	}

	@Override
	public void onConnectionEstablished() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {
		// TODO Auto-generated method stub		
	}

	
	public void ProcessKuraPayload(String assetId, String topic, KuraPayload msg, int qos, boolean retained) 
	{
		Object[] Names = msg.metricNames().toArray();
		Properties props = new Properties();
		
		for( int index = 0; index < Names.length; index++)
		{
			String key = (String)Names[index];
			String value = (String)msg.getMetric(key);
			props.put(key, value);
		}
		ProcessPayload(props, topic);
	}

	public void ProcessBytePayload(String assetId, String topic, byte[] payload, int qos, boolean retain) {
		String pload = new String(payload);
		StringTokenizer tok = new StringTokenizer(pload, ":");
		Properties props = new Properties();

		while(tok.hasMoreElements()) {
			String key = tok.nextToken();
			String value = tok.nextToken();
			props.put(key, value);
		}
		ProcessPayload(props, topic);
	}

	
	// main routine to process various control topics from EDC or byte payload format
	public void ProcessPayload(Properties props, String topic) {
		String controlTopic = modbusProperties.getProperty("controlTopic");

		if (topic.contains(controlTopic + "/led")) {
			s_logger.debug("topic contains '" + controlTopic + "/led' ");
			ProcessLEDMessage( topic.substring(topic.indexOf("led/") + 4), props);

		} else if(topic.contains(controlTopic + "/resetcnt")) {
			s_logger.debug("topic contains '" + controlTopic + "/resetcnt' ");
			ProcessResetMessage( topic.substring(topic.indexOf("resetcnt/") + 9), props);
			
		} else if(topic.contains(controlTopic + "/alarm")) {
			s_logger.warn("topic contains '" + controlTopic + "/alarm' ");
			ProcessAlarmMessage( props );
		}
	}

	// handle LED controls
	public void ProcessLEDMessage( String LED, Properties props)
	{
		try {
			if (LED.equals("1")) {
				TurnOnLED( 1, Boolean.parseBoolean((String)props.get("light")));
			} else if (LED.equals("2")) {
				TurnOnLED( 2, Boolean.parseBoolean((String)props.get("light")));
			} else if (LED.equals("3")) {
				TurnOnLED( 3, Boolean.parseBoolean((String)props.get("light")));

			} else if (LED.equals("4")) {
				Object value = null;
				if((value = props.get("red")) != null) 
					TurnOnLED( 4, Boolean.parseBoolean((String)value));
				if((value = props.get("green")) != null)
					TurnOnLED( 5, Boolean.parseBoolean((String)value));
				if((value = props.get("blue")) != null)
					TurnOnLED( 6, Boolean.parseBoolean((String)value));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// reset counter values
	public void ProcessResetMessage( String counter, Properties props)
	{
		try {
			if(counter.equals("c3"))
				clearCounter( 3, Boolean.parseBoolean((String)props.get("value")));
			if(counter.equals("c4"))
				clearCounter( 4, Boolean.parseBoolean((String)props.get("value")));
			if(counter.equals("c5"))
				clearCounter( 5, Boolean.parseBoolean((String)props.get("value")));
			if(counter.equals("c6"))
				clearCounter( 6, Boolean.parseBoolean((String)props.get("value")));
			if(counter.equals("qc"))
				clearCounter( 12, Boolean.parseBoolean((String)props.get("value")));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void clearCounter(int counter, boolean On)
	{
		boolean TurnON = true;
		boolean TurnOFF = false;
		try
		{
			switch(counter) {
			case 3:		s_logger.debug("Counter c3 reset " + On);
						break;
			case 4:		s_logger.debug("Counter c4 reset " + On);
						break;
			case 5:		s_logger.debug("Counter c5 reset " + On);
						break;
			case 6:		s_logger.debug("Counter c6 reset " + On);
						break;
			case 12:	s_logger.debug("Counter qc reset " + On);
						break;
			default:	s_logger.warn("Error in clearCounter - Counter " + counter + " is not valid.");
						break;
			}
			m_protocolDevice.writeSingleCoil(slaveAddr, 3072 + counter, On?TurnON:TurnOFF);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void ProcessAlarmMessage( Properties props)
	{
//		String sirenStr = (String)props.get("siren");
//		if(sirenStr.equals("true")) {
//			siren.startSiren();
//		} else {
//		}
	}

	@Override
	public String getCriticalComponentName() {
		return "ModbusManager";
	}

	@Override
	public int getCriticalComponentTimeout() {
		return pollInterval*2; // return double of the pollInterval
	}
	
}
