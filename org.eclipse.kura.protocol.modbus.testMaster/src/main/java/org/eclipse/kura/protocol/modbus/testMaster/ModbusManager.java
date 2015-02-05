/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.protocol.modbus.testMaster;


import java.lang.Thread.State;
import java.util.Calendar;
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

	private boolean readWrite=true;

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
							//initializeLeds();
							initLeds=true;
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
		int[] digitalInputs;

		if(readWrite){
			s_logger.info("ReadHoldingRegisters:");
			digitalInputs = m_protocolDevice.readHoldingRegisters(slaveAddr, 1, 15);

			int powerOut= digitalInputs[0];
			int rechargeTime= digitalInputs[1];
			int energyOut= digitalInputs[2];
			int powerPV= digitalInputs[3];
			int faultFlag= digitalInputs[4] & 0x01;
			int rechargeAvailable= (digitalInputs[4] >> 1) & 0x01;
			int rechargeInProgress= (digitalInputs[4] >> 2) & 0x01;
			int pVSystemActive= (digitalInputs[4] >> 3) & 0x01;
			int auxChargerActive= (digitalInputs[4] >> 4) & 0x01;
			int storageBatteryContactorStatus= (digitalInputs[4] >> 5) & 0x01;
			int converterContactorStatus= (digitalInputs[4] >> 6) & 0x01;
			int faultString1= digitalInputs[5];
			int faultString2= digitalInputs[6];
			int iGBTTemp= digitalInputs[7];
			int storeBattTemp= digitalInputs[8];
			int storBatterySOC= digitalInputs[9];
			int vOut= digitalInputs[10];
			int storageBatteryV= digitalInputs[11];
			int pVSystemV= digitalInputs[12];
			int iOut= digitalInputs[13];
			int storageBatteryI= digitalInputs[14];
			s_logger.info("Slave-> power out: " + powerOut +
					" rechargeTime: " + rechargeTime + 
					" energyOut: " + energyOut +
					" powerPV: " + powerPV +
					" faultFlag: " + faultFlag +
					" rechargeAvailable: " + rechargeAvailable +
					" rechargeInProgress: " + rechargeInProgress +
					" pVSystemActive: " + pVSystemActive +
					" auxChargerActive: " + auxChargerActive +
					" storageBatteryContactorStatus: " + storageBatteryContactorStatus +
					" converterContactorStatus: " + converterContactorStatus +
					" faultString1: " + faultString1 +
					" faultString2: " + faultString2 +
					" iGBTTemp: " + iGBTTemp +
					" storeBattTemp: " + storeBattTemp +
					" storBatterySOC: " + storBatterySOC +
					" vOut: " + vOut +
					" storageBatteryV: " + storageBatteryV +
					" pVSystemV: " + pVSystemV +
					" iOut: " + iOut +
					" storageBatteryI: " + storageBatteryI);
			readWrite=false;
		}else{
			int[] data=new int[7];
			
			int startRecharge= 1; //start recharge [0,1]
			int isBooked= (0 << 1); //Recharge is booked? [0-No;1-Yes]
			int solarIrradiation= (2 << 2); //Next Day Solar Radiation Level [0-Low; 1-Medium; 2-High]
			
			data[0] = (startRecharge + isBooked + solarIrradiation) << 8; //Panel PC Start/Stop/Booking/Next day weather forecast
			data[1] = (25 << 8) + (12); //Booking Time
			data[2] = (2 << 8) + 22 ; //Booking month and day
			data[3] = 2014; //Booking year
			
			Calendar cal = Calendar.getInstance();
			data[4] = (cal.get(Calendar.MINUTE) << 8) + cal.get(Calendar.HOUR_OF_DAY); //Current date -> minutes and hours
			data[5] = (cal.get(Calendar.MONTH) << 8) + cal.get(Calendar.DAY_OF_MONTH);  //Current Date -> month and day
			data[6] = cal.get(Calendar.YEAR); //Current date -> year

			s_logger.info("WriteMultipleRegister");
			m_protocolDevice.writeMultipleRegister(slaveAddr, 1, data);
			s_logger.info("After write");
			readWrite= true;
		}


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
	
	private int buildShort(byte high, byte low){
		return ((0xFF & (int) high) << 8) + ((0xFF & (int) low));
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
