package org.eclipse.kura.protocol.can.test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.kura.protocol.can.CanConnectionService;
import org.eclipse.kura.protocol.can.CanMessage;


public class CanSocketTest extends TestCase implements ConfigurableComponent, CloudClientListener {
	private static final Logger s_logger = LoggerFactory.getLogger(CanSocketTest.class);

	// Cloud Application identifier
	private static final String APP_ID = "CanSocketTest";


	// Default MQTT Quality of Service
	private static final int DFLT_QOS = 0;

	// Default MQTT retain flag
	private static final boolean DFLT_RETAIN = false;	

	// Cloud Application semantic topics
	private static final String SEMANTIC_TOPIC_DEFAULT = "default";
	private static final String SEMANTIC_TOPIC_TIME_SERIES = "series";
	private static final String SEMANTIC_TOPIC_IO = "io";
	private static final String SEMANTIC_TOPIC_STATUS = "status";
	private static final String SEMANTIC_TOPIC_ERROR = "err";


	// Metric names for compressor status
	private static final String METRIC_NAME_COMPRESSOR_PRESSURE = "p_c";
	private static final String METRIC_NAME_COMPRESSOR_AVG_PRESSURE = "pavg_c";
	private static final String METRIC_NAME_COMPRESSOR_TEMPERATURE = "T_c";
	private static final String METRIC_NAME_COMPRESSOR_AVG_TEMPERATURE = "Tavg_c";
	private static final String METRIC_NAME_DRYER_TEMPERATURE = "T_d";
	private static final String METRIC_NAME_DRYER_AVG_TEMPERATURE = "Tavg_d";
	private static final String METRIC_NAME_DRYER_STATUS = "st_d";
	private static final String METRIC_NAME_INPUT_BITMAP = "in";
	private static final String METRIC_NAME_OUTPUT_BITMAP = "out";
	private static final String METRIC_NAME_COMPRESSOR_STATUS = "st_c";
	private static final String METRIC_NAME_COMPRESSOR_LOAD = "ld_c";
	private static final String METRIC_NAME_TOTAL_HOURS = "toth";
	private static final String METRIC_NAME_LOAD_HOURS = "ldh";
	private static final String METRIC_NAME_CURRENT_ERROR_STATUS = "err";
	private static final String METRIC_NAME_PRESSURE_UNIT = "pu";
	private static final String METRIC_NAME_TEMPERATURE_UNIT = "Tu";
	private static final String METRIC_NAME_SERIAL_NUMBER = "ser";
	private static final String METRIC_NAME_PRODUCTION_WEEK = "prodw";
	private static final String METRIC_NAME_PRODUCTION_YEAR = "prody";
	private static final String METRIC_NAME_PRODUCT_CODE = "prodc";
	private static final String METRIC_NAME_NEXT_SERVICE = "nsrv";
	private static final String METRIC_NAME_AIR_FILTER_REMAINING_TIME = "afrt";
	private static final String METRIC_NAME_OIL_FILTER_REMAINING_TIME = "ofrt";
	private static final String METRIC_NAME_OIL_REMAINING_TIME = "ort";
	private static final String METRIC_NAME_DISOIL_FILTER_REMAINING_TIME = "dort";
	private static final String METRIC_NAME_DRYER_FILTER_REMAINING_TIME = "dfrt";
	private static final String METRIC_NAME_MAX_PRESSURE = "pmax";
	private static final String METRIC_NAME_MIN_PRESSURE = "pmin";


	// Configuration property names for compressor status
	private static final String CONF_NAME_COMPRESSOR_PRESSURE_ENABLE = "compressor.pressure.enable";
	private static final String CONF_NAME_COMPRESSOR_PRESSURE_CHANGE = "compressor.pressure.change";
	private static final String CONF_NAME_COMPRESSOR_TEMPERATURE_ENABLE = "compressor.temperature.enable";
	private static final String CONF_NAME_COMPRESSOR_TEMPERATURE_CHANGE = "compressor.temperature.change";
	private static final String CONF_NAME_DRYER_TEMPERATURE_ENABLE = "dryer.temperature.enable";
	private static final String CONF_NAME_DRYER_TEMPERATURE_CHANGE = "dryer.temperature.change";
	private static final String CONF_NAME_DRYER_STATUS_ENABLE = "dryer.status.enable";
	private static final String CONF_NAME_DRYER_STATUS_CHANGE = "dryer.status.change";
	private static final String CONF_NAME_INPUT_BITMAP_ENABLE = "input.enable";
	private static final String CONF_NAME_INPUT_BITMAP_CHANGE = "input.change";
	private static final String CONF_NAME_OUTPUT_BITMAP_ENABLE = "output.enable";
	private static final String CONF_NAME_OUTPUT_BITMAP_CHANGE = "output.change";
	private static final String CONF_NAME_COMPRESSOR_STATUS_ENABLE = "compressor.status.enable";
	private static final String CONF_NAME_COMPRESSOR_STATUS_CHANGE = "compressor.status.change";
	private static final String CONF_NAME_COMPRESSOR_LOAD_ENABLE = "compressor.load.enable";
	private static final String CONF_NAME_COMPRESSOR_LOAD_CHANGE = "compressor.load.change";
	private static final String CONF_NAME_TOTAL_HOURS_ENABLE = "total.hours.enable";
	private static final String CONF_NAME_TOTAL_HOURS_CHANGE = "total.hours.change";
	private static final String CONF_NAME_LOAD_HOURS_ENABLE = "load.hours.enable";
	private static final String CONF_NAME_LOAD_HOURS_CHANGE = "load.hours.change";
	private static final String CONF_NAME_CURRENT_ERROR_STATUS_ENABLE = "error.enable";
	private static final String CONF_NAME_CURRENT_ERROR_STATUS_CHANGE = "error.change";
	private static final String CONF_NAME_PRESSURE_UNIT_ENABLE = "pressure.unit.enable";
	private static final String CONF_NAME_TEMPERATURE_UNIT_ENABLE = "temperature.unit.enable";
	private static final String CONF_NAME_SERIAL_NUMBER_ENABLE = "serial.number.enable";
	private static final String CONF_NAME_PRODUCTION_WEEK_ENABLE = "production.week.enable";
	private static final String CONF_NAME_PRODUCTION_YEAR_ENABLE = "production.year.enable";
	private static final String CONF_NAME_PRODUCT_CODE_ENABLE = "product.code.enable";
	private static final String CONF_NAME_NEXT_SERVICE_ENABLE = "next.service.enable";
	private static final String CONF_NAME_AIR_FILTER_REMAINING_TIME_ENABLE = "air.filter.remaining.time.enable";
	private static final String CONF_NAME_AIR_FILTER_REMAINING_TIME_CHANGE = "air.filter.remaining.time.change";
	private static final String CONF_NAME_OIL_FILTER_REMAINING_TIME_ENABLE = "oil.filter.remaining.time.enable";
	private static final String CONF_NAME_OIL_FILTER_REMAINING_TIME_CHANGE = "oil.filter.remaining.time.change";
	private static final String CONF_NAME_OIL_REMAINING_TIME_ENABLE = "oil.remaining.time.enable";
	private static final String CONF_NAME_OIL_REMAINING_TIME_CHANGE = "oil.remaining.time.change";
	private static final String CONF_NAME_DISOIL_FILTER_REMAINING_TIME_ENABLE = "disoil.filter.remaining.time.enable";
	private static final String CONF_NAME_DISOIL_FILTER_REMAINING_TIME_CHANGE = "disoil.filter.remaining.time.change";
	private static final String CONF_NAME_DRYER_FILTER_REMAINING_TIME_ENABLE = "dryer.filter.remaining.time.enable";
	private static final String CONF_NAME_DRYER_FILTER_REMAINING_TIME_CHANGE = "dryer.filter.remaining.time.change";
	private static final String CONF_NAME_MAX_PRESSURE_ENABLE = "pressure.max.enable";
	private static final String CONF_NAME_MIN_PRESSURE_ENABLE = "pressure.min.enable";

	// Configuration property names for compressor error history
	private static final String CONF_NAME_ERROR_HISTORY_ENABLE = "error.history.enable";
	private static final String CONF_NAME_ERROR_HISTORY_CHANGE = "error.history.change";

	// Configuration property names for various timings
	private static final String CONF_NAME_PUBLISH_ON_CHANGE = "publish.on.change";
	private static final String CONF_NAME_POLL_INTERVAL = "poll.interval";	
	private static final String CONF_NAME_AVERAGE_INTERVAL = "average.time";

	private CanConnectionService 	m_canConnection;
	private CloudService            m_cloudService;
	private CloudClient 			m_cloudAppClient=null;
	private Map<String,Object>   	m_properties;
	private Thread 					m_pollThread;
	private boolean 				thread_done = false;
	private String					m_ifName;
	private int 					m_canId;
	private int						m_orig;
	private int	nbFrame = 0;
	private int	nbdecod = 0;
	private byte indice = 0;
	private int  idToReceive;
	private int  idReceived;
	private long start;
	private CompressorStatus status = new CompressorStatus();
	private short maxPressure = -0x7FFF;
	private short minPressure = 0x7FFF;

	private CompressorStatus m_lastCompressorStatus;
	private CompressorError[] m_lastErrorHistory;

	private int m_compressorSumOfTemperatures;
	private int m_compressorSumOfPressures;
	private int m_dryerSumOfTemperatures;
	private int m_numOfPoints;

	private short m_compressorAvgTemperature;
	private short m_compressorAvgPressure;
	private short m_dryerAvgTemperature;
	private int m_pollInterval=1000;

	public void setCloudService(CloudService cloudService) {
		this.m_cloudService = cloudService;
	}

	public void unsetCloudService(CloudService cloudService) {
		this.m_cloudService = null;
	}

	public void setCanConnectionService(CanConnectionService canConnection) {
		this.m_canConnection = canConnection;
	}

	public void unsetCanConnectionService(CanConnectionService canConnection) {
		this.m_canConnection = null;
	}

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) {
		m_properties = properties;
		s_logger.info("activating can test");
		 for (Map.Entry<String,Object> m: ((Map<String,Object>) m_properties).entrySet())
	     	 System.out.println(m.getKey() + " = " + m.getValue().toString());

		m_ifName="can0";
		m_canId=-1;
		m_orig =-1;
		
		m_compressorSumOfPressures = 0;
		m_compressorSumOfTemperatures = 0;
		m_dryerSumOfTemperatures = 0;
		m_numOfPoints = 0;
		
		m_lastErrorHistory = null;
		m_lastCompressorStatus = null;
		
		start = System.currentTimeMillis();
		if(m_properties!=null){
			if(m_properties.get("can.name") != null) 
				m_ifName = (String) m_properties.get("can.name");
			if(m_properties.get("can.identifier") != null) 
				m_canId = (Integer) m_properties.get("can.identifier");
		}

		if(m_cloudAppClient==null) {
			// Acquire a Cloud Application Client for this Application 
			s_logger.info("Getting CloudApplicationClient for {}...", APP_ID);
			try {
				m_cloudAppClient = m_cloudService.newCloudClient(APP_ID);
			} catch (KuraException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			m_cloudAppClient.addCloudClientListener(this);
		}

		if(m_pollThread!=null){
			m_pollThread.interrupt();
			try {
				m_pollThread.join(100);
			} catch (InterruptedException e) {
				// Ignore
			}
			m_pollThread=null;
		}

		testSendImpl(m_ifName,m_canId,0);
		m_pollThread = new Thread(new Runnable() {		
			@Override
			public void run() {
				if(m_canConnection!=null){
					while(!thread_done){
						thread_done=doCanTest();
					}
				}
			}
		});
		m_pollThread.start();	
	}


	protected void deactivate(ComponentContext componentContext) {
		if(m_pollThread!=null){
			m_pollThread.interrupt();
			try {
				m_pollThread.join(100);
			} catch (InterruptedException e) {
				// Ignore
			}
		}
		m_pollThread=null;
		// Releasing the CloudApplicationClient
		s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
		m_cloudAppClient.release();
	}

	public void updated(Map<String,Object> properties)
	{
		s_logger.debug("updated...");		
		m_properties = properties;
		 for (Map.Entry<String,Object> m: ((Map<String,Object>) m_properties).entrySet())
	     	 System.out.println(m.getKey() + " = " + m.getValue().toString());
		if(m_properties!=null){
			if(m_properties.get("can.name") != null) 
				m_ifName = (String) m_properties.get("can.name");
			if(m_properties.get("can.identifier") != null) 
				m_canId = (Integer) m_properties.get("can.identifier");
			if(m_properties.get(CONF_NAME_POLL_INTERVAL) != null) {
				m_pollInterval=(Integer)m_properties.get(CONF_NAME_POLL_INTERVAL)*1000;
				s_logger.debug("sleep for "+m_pollInterval);
			}
			if(m_pollThread!=null){
				m_pollThread.interrupt();
				try {
					m_pollThread.join(100);
				} catch (InterruptedException e) {
					// Ignore
				}
				m_pollThread=null;
			}

			testSendImpl(m_ifName,m_canId,0);
			m_pollThread = new Thread(new Runnable() {		
				@Override
				public void run() {
					if(m_canConnection!=null){
						while(!thread_done){
							thread_done=doCanTest();
						}
					}
				}
			});
			m_pollThread.start();	
		}
	}
	
	@Test
	public void testDummy() {
		assertTrue(true);
	}

	public boolean doCanTest() {
		byte[] b; 
		CanMessage cm;
		int index=0;
		if(m_orig>=0){
			try {
				testSendImpl(m_ifName,m_canId,m_orig);
				cm = m_canConnection.receiveCanMessage(idToReceive,0x7FF);
			} catch (Exception e) {
				s_logger.warn("CanConnection Crash!");			
				e.printStackTrace();
				return true;
			}
		}
		else{
			try {
				long elapsed = System.currentTimeMillis()-start;
				if(elapsed>1000){
					s_logger.debug("timeout m_orig="+m_orig);
					testSendImpl(m_ifName,m_canId,m_orig);
					start = System.currentTimeMillis();
				}
				cm = m_canConnection.receiveCanMessage(-1,0xF);
			} catch (Exception e) {
				s_logger.warn("CanConnection Crash!");			
				e.printStackTrace();
				return true;
			}
		}

		b = cm.getData();
		if(b!=null){
			nbFrame++;
			StringBuilder sb = new StringBuilder("received : ");
			for(int i=0; i<b.length; i++){
				sb.append(b[i]);
				sb.append(";");
			}
			int id = cm.getCanId();
			idReceived = id;
			int code = (id >> 8);
			m_orig = (id >> 4)& 0xF;
			int dest = (id & 0xF);
			//s_logger.debug("id="+String.format("%03x", idToReceive)+"H"+" : "+sb);
			if(code==5){
				//s_logger.debug("id="+String.format("%03x", cm.getCanId())+"H"+" : "+sb);
				if(b.length==6){
					start = System.currentTimeMillis();
					nbdecod++;
					index = (int)b[0]+((int)b[1]<<8);
					int val1 = (int)b[2]+((int)b[3]<<8);
					int val2 = (int)b[4]+((int)b[5]<<8);
					//						s_logger.debug("sendCanMessage on "+m_ifName+" canId = "+String.format("%03x", (0x500 + (m_canId << 4) + m_orig))+"H  (code="+indice+")");
					//						s_logger.debug("Total nb frames = "+nbFrame+" decoded="+nbdecod+" id="+String.format("%03x", cm.getCanId())+"H"+" : "+sb);
					//						s_logger.debug("indice="+index+"  data1="+val1+"  data2="+val2);
					switch(index){
					case 0:
						status.setCompressorPressure(ProtocolUtils.getSigned16(b, 2));
						status.setCompressorTemperature(ProtocolUtils.getSigned16(b, 4));
						s_logger.debug("Pressure = "+status.getCompressorPressure()+"  Temperature = "+status.getCompressorTemperature());
						break;
					case 1:
						status.setDryerTemperature(ProtocolUtils.getSigned16(b, 2));
						status.setDryerStatus(ProtocolUtils.getUnsigned8(b, 4));
						s_logger.debug("Dryer temp = "+status.getDryerTemperature()+"  dryer status = "+status.getDryerStatus());
						break;
					case 2:
						status.setInputBitmap(ProtocolUtils.getUnsigned16(b, 2));
						status.setOutputBitmap(ProtocolUtils.getUnsigned8(b, 4));
						s_logger.debug("Inp bitmap = "+status.getInputBitmap()+"  outp bitmap = "+status.getOutputBitmap());
						break;
					case 3:
						status.setCompressorStatus(ProtocolUtils.getUnsigned8(b, 2));
						status.setCompressorLoad(ProtocolUtils.getUnsigned8(b, 4));
						s_logger.debug("Comp status = "+status.getCompressorStatus()+"  comp load = "+status.getCompressorLoad());
						break;
					case 4:
						status.setTotalHours(ProtocolUtils.getUnsigned16(b, 2));
						status.setLoadHours(ProtocolUtils.getUnsigned16(b, 4));
						s_logger.debug("Total hr = "+status.getTotalHours()+"  load hr = "+status.getLoadHours());
						break;
					case 5:
						status.setCurrentErrorStatus(ProtocolUtils.getUnsigned8(b, 3));
						s_logger.debug("Error status = "+status.getCurrentErrorStatus());
						break;
					case 6:
						status.setPressureUnit(ProtocolUtils.getUnsigned8(b, 2));
						status.setTemperatureUnit(ProtocolUtils.getUnsigned8(b, 4));
						s_logger.debug("Unit P = "+status.getPressureUnit()+"  unit T = "+status.getTemperatureUnit());
						break;
					case 7:
						status.setSerialNumber(ProtocolUtils.getUnsigned16(b, 2));
						status.setProductionWeek(ProtocolUtils.getUnsigned8(b, 4));
						s_logger.debug("Serial num = "+status.getSerialNumber()+"  Prod week = "+status.getProductionWeek());
						break;
					case 8:
						status.setProductionYear(ProtocolUtils.getUnsigned8(b, 2));
						status.setProductCode(ProtocolUtils.getUnsigned8(b, 4));
						s_logger.debug("Year prod = "+status.getProductionYear()+"  Product code = "+status.getProductCode());
						break;
					case 12:
						status.setNextService(ProtocolUtils.getUnsigned8(b, 2));
						status.setAirFilterRemainingTime(ProtocolUtils.getUnsigned16(b, 4));
						s_logger.debug("Next service = "+status.getNextService()+"  Air filter time = "+status.getAirFilterRemainingTime());
						break;
					case 13:
						status.setOilFilterRemainingTime(ProtocolUtils.getUnsigned16(b, 2));
						status.setOilRemainingTime(ProtocolUtils.getUnsigned16(b, 4));
						s_logger.debug("Oil Filter time = "+status.getOilFilterRemainingTime()+"  oil time = "+status.getOilRemainingTime());
						break;
					case 14:
						status.setDisoilFilterRemainingTime(ProtocolUtils.getUnsigned16(b, 2));
						status.setDryerFilterRemainingTime(ProtocolUtils.getUnsigned16(b, 4));
						s_logger.debug("Disoil Filter time = "+status.getDisoilFilterRemainingTime()+"  dryer filter time = "+status.getDryerFilterRemainingTime());
						break;
					case 15:
						status.setMaxPressure(ProtocolUtils.getSigned16(b, 2));
						status.setMinPressure(ProtocolUtils.getSigned16(b, 4));
						s_logger.debug("MinPress = "+status.getMinPressure()+"  MaxPress = "+status.getMaxPressure());
						break;
					}
				}
			}
			else{
				long elapsed = System.currentTimeMillis()-start;
				if(elapsed>10000){
					s_logger.debug("id="+String.format("%03x", cm.getCanId())+"H"+" : "+sb);
					start = System.currentTimeMillis();
				}
			}
			if(indice>14){ 
				indice=0;
				handleCompressorData(status, null, false);
				try {
					Thread.sleep(m_pollInterval);
				} catch (InterruptedException e) {
					//e.printStackTrace();
				}
			}
		}
		else{
			System.out.println("receive=null");
		}			
		return false;
	}



	public void testSendImpl(String ifName, int orig, int dest) {
		try {
			if((m_canConnection==null)||(orig<0)) 
				return;
			int id = 0x500 + (orig << 4) + dest;
			idToReceive = 0x500 + (dest << 4) + orig;
			byte btest[] = new byte[2];
			btest[0]=indice;
			btest[1]=0;
			m_canConnection.sendCanMessage(ifName, id, btest);
			long elapsed = System.currentTimeMillis()-start;
			//if(elapsed>10000)
			//s_logger.debug("sendCanMessage on "+ifName+" canId = "+String.format("%03x", id)+"H  (code="+indice+")");
			indice++;
			//if(indice>14) indice=0;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onControlMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionLost() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionEstablished() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {
		// TODO Auto-generated method stub

	}

	private synchronized void handleCompressorData(CompressorStatus status,
			CompressorError[] errorHistory,
			boolean averagingIntervalElapsed) {

		// Allocate a new payload
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());

		Boolean changed = false;
		Set<String> topics = new HashSet<String>();

		//
		// Process the compressor status
		s_logger.info(CONF_NAME_COMPRESSOR_PRESSURE_ENABLE+m_properties.get(CONF_NAME_COMPRESSOR_PRESSURE_ENABLE));
		Boolean enabled = (Boolean) m_properties.get(CONF_NAME_COMPRESSOR_PRESSURE_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_COMPRESSOR_PRESSURE, Integer.valueOf(status.getCompressorPressure()));

			Short confChange = (Short) m_properties.get(CONF_NAME_COMPRESSOR_PRESSURE_CHANGE);
			if (m_lastCompressorStatus == null || 
					Math.abs(m_lastCompressorStatus.getCompressorPressure() - status.getCompressorPressure()) > confChange) {
				changed = true;
			}
		}

		s_logger.info(CONF_NAME_COMPRESSOR_TEMPERATURE_ENABLE+m_properties.get(CONF_NAME_COMPRESSOR_TEMPERATURE_ENABLE));
		enabled = (Boolean) m_properties.get(CONF_NAME_COMPRESSOR_TEMPERATURE_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_COMPRESSOR_TEMPERATURE, Integer.valueOf(status.getCompressorTemperature()));

			Short confChange = (Short) m_properties.get(CONF_NAME_COMPRESSOR_TEMPERATURE_CHANGE);
			if (m_lastCompressorStatus == null || 
					Math.abs(m_lastCompressorStatus.getCompressorTemperature() - status.getCompressorTemperature()) > confChange) {
				changed = true;
			}

		}

		enabled = (Boolean) m_properties.get(CONF_NAME_DRYER_TEMPERATURE_ENABLE);
		if (enabled) {			
			payload.addMetric(METRIC_NAME_DRYER_TEMPERATURE, Integer.valueOf(status.getDryerTemperature()));

			Short confChange = (Short) m_properties.get(CONF_NAME_DRYER_TEMPERATURE_CHANGE);
			if (m_lastCompressorStatus == null || 
					Math.abs(m_lastCompressorStatus.getDryerTemperature() - status.getDryerTemperature()) > confChange) {
				changed = true;
			}
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_DRYER_STATUS_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_DRYER_STATUS, Integer.valueOf(status.getDryerStatus()));

			Boolean confChange = (Boolean)m_properties.get(CONF_NAME_DRYER_STATUS_CHANGE);
			if (confChange) {
				if (m_lastCompressorStatus == null ||
						m_lastCompressorStatus.getDryerStatus() != status.getDryerStatus()) {
					topics.add(SEMANTIC_TOPIC_STATUS);
					changed = true;
				}
			}
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_INPUT_BITMAP_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_INPUT_BITMAP, status.getInputBitmap());

			Boolean confChange = (Boolean)m_properties.get(CONF_NAME_INPUT_BITMAP_CHANGE);
			if (confChange) {
				if (m_lastCompressorStatus == null ||
						m_lastCompressorStatus.getInputBitmap() != status.getInputBitmap()) {
					topics.add(SEMANTIC_TOPIC_IO);
					changed = true;
				}
			}			
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_OUTPUT_BITMAP_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_OUTPUT_BITMAP, Integer.valueOf(status.getOutputBitmap()));

			Boolean confChange = (Boolean)m_properties.get(CONF_NAME_OUTPUT_BITMAP_CHANGE);
			if (confChange) {
				if (m_lastCompressorStatus == null ||
						m_lastCompressorStatus.getOutputBitmap() != status.getOutputBitmap()) {
					topics.add(SEMANTIC_TOPIC_IO);
					changed = true;
				}
			}
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_COMPRESSOR_STATUS_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_COMPRESSOR_STATUS, Integer.valueOf(status.getCompressorStatus()));

			Boolean confChange = (Boolean)m_properties.get(CONF_NAME_COMPRESSOR_STATUS_CHANGE);
			if (confChange) {
				if (m_lastCompressorStatus == null ||
						m_lastCompressorStatus.getCompressorStatus() != status.getCompressorStatus()) {
					topics.add(SEMANTIC_TOPIC_STATUS);
					changed = true;
				}
			}
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_COMPRESSOR_LOAD_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_COMPRESSOR_LOAD, Integer.valueOf(status.getCompressorLoad()));

			Short confChange = (Short) m_properties.get(CONF_NAME_COMPRESSOR_LOAD_CHANGE);
			if (m_lastCompressorStatus == null || 
					Math.abs(m_lastCompressorStatus.getCompressorLoad() - status.getCompressorLoad()) > confChange) {
				changed = true;
			}

		}

		enabled = (Boolean) m_properties.get(CONF_NAME_TOTAL_HOURS_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_TOTAL_HOURS, status.getTotalHours());

			Long confChange = (Long) m_properties.get(CONF_NAME_TOTAL_HOURS_CHANGE);
			if (m_lastCompressorStatus == null || 
					Math.abs(m_lastCompressorStatus.getTotalHours() - status.getTotalHours()) > confChange) {
				changed = true;
			}
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_LOAD_HOURS_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_LOAD_HOURS, status.getLoadHours());

			Long confChange = (Long) m_properties.get(CONF_NAME_LOAD_HOURS_CHANGE);
			if (m_lastCompressorStatus == null || 
					Math.abs(m_lastCompressorStatus.getLoadHours() - status.getLoadHours()) > confChange) {
				changed = true;
			}
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_CURRENT_ERROR_STATUS_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_CURRENT_ERROR_STATUS, Integer.valueOf(status.getCurrentErrorStatus()));

			Boolean confChange = (Boolean)m_properties.get(CONF_NAME_CURRENT_ERROR_STATUS_CHANGE);
			if (confChange) {
				if (m_lastCompressorStatus == null ||
						m_lastCompressorStatus.getCurrentErrorStatus() != status.getCurrentErrorStatus()) {
					topics.add(SEMANTIC_TOPIC_ERROR);
					changed = true;
				}
			}
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_PRESSURE_UNIT_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_PRESSURE_UNIT, Integer.valueOf(status.getPressureUnit()));
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_TEMPERATURE_UNIT_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_TEMPERATURE_UNIT, Integer.valueOf(status.getTemperatureUnit()));
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_SERIAL_NUMBER_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_SERIAL_NUMBER, status.getSerialNumber());
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_PRODUCTION_WEEK_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_PRODUCTION_WEEK, Integer.valueOf(status.getProductionWeek()));
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_PRODUCTION_YEAR_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_PRODUCTION_YEAR, Integer.valueOf(status.getProductionYear()));
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_PRODUCT_CODE_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_PRODUCT_CODE, Integer.valueOf(status.getProductCode()));
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_NEXT_SERVICE_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_NEXT_SERVICE, Integer.valueOf(status.getNextService()));
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_AIR_FILTER_REMAINING_TIME_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_AIR_FILTER_REMAINING_TIME, status.getAirFilterRemainingTime());

			Integer confChange = (Integer) m_properties.get(CONF_NAME_AIR_FILTER_REMAINING_TIME_CHANGE);
			if (m_lastCompressorStatus == null || 
					Math.abs(m_lastCompressorStatus.getAirFilterRemainingTime() - status.getAirFilterRemainingTime()) > confChange) {
				changed = true;
			}
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_OIL_FILTER_REMAINING_TIME_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_OIL_FILTER_REMAINING_TIME, status.getOilFilterRemainingTime());

			Integer confChange = (Integer) m_properties.get(CONF_NAME_OIL_FILTER_REMAINING_TIME_CHANGE);
			if (m_lastCompressorStatus == null || 
					Math.abs(m_lastCompressorStatus.getOilFilterRemainingTime() - status.getOilFilterRemainingTime()) > confChange) {
				changed = true;
			}

		}

		enabled = (Boolean) m_properties.get(CONF_NAME_OIL_REMAINING_TIME_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_OIL_REMAINING_TIME, status.getOilRemainingTime());

			Integer confChange = (Integer) m_properties.get(CONF_NAME_OIL_REMAINING_TIME_CHANGE);
			if (m_lastCompressorStatus == null || 
					Math.abs(m_lastCompressorStatus.getOilRemainingTime() - status.getOilRemainingTime()) > confChange) {
				changed = true;
			}
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_DISOIL_FILTER_REMAINING_TIME_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_DISOIL_FILTER_REMAINING_TIME, status.getDisoilFilterRemainingTime());

			Integer confChange = (Integer) m_properties.get(CONF_NAME_DISOIL_FILTER_REMAINING_TIME_CHANGE);
			if (m_lastCompressorStatus == null || 
					Math.abs(m_lastCompressorStatus.getDisoilFilterRemainingTime() - status.getDisoilFilterRemainingTime()) > confChange) {
				changed = true;
			}
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_DRYER_FILTER_REMAINING_TIME_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_DRYER_FILTER_REMAINING_TIME, status.getDryerFilterRemainingTime());

			Integer confChange = (Integer) m_properties.get(CONF_NAME_DRYER_FILTER_REMAINING_TIME_CHANGE);
			if (m_lastCompressorStatus == null || 
					Math.abs(m_lastCompressorStatus.getDryerFilterRemainingTime() - status.getDryerFilterRemainingTime()) > confChange) {
				changed = true;
			}
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_MAX_PRESSURE_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_MAX_PRESSURE, Integer.valueOf(status.getMaxPressure()));
		}

		enabled = (Boolean) m_properties.get(CONF_NAME_MIN_PRESSURE_ENABLE);
		if (enabled) {
			payload.addMetric(METRIC_NAME_MIN_PRESSURE, Integer.valueOf(status.getMinPressure()));
		}

		//
		// Process the compressor error history
		enabled = (Boolean) m_properties.get(CONF_NAME_ERROR_HISTORY_ENABLE);
		if ((enabled)&&(errorHistory!=null)) {
			Boolean confChange = (Boolean) m_properties.get(CONF_NAME_ERROR_HISTORY_CHANGE);
			if (confChange) {
				if (m_lastErrorHistory == null) {
					changed = true;
				} else {
					for (int i = 0; i < errorHistory.length; i++) {
						if (m_lastErrorHistory[i].getCode() != errorHistory[i].getCode() ||
								m_lastErrorHistory[i].getDay() != errorHistory[i].getDay() ||
								m_lastErrorHistory[i].getMonth() != errorHistory[i].getMonth() ||
								m_lastErrorHistory[i].getYear() != errorHistory[i].getYear()) {

							s_logger.info("Error history changed {}", Arrays.toString(errorHistory));

							topics.add(SEMANTIC_TOPIC_ERROR);
							changed = true;

							// TODO what metrics to publish?

							break;
						}
					}
				}
			}
		}

		// Publish by default
		boolean doPublish = true;

		Boolean publishOnChange = (Boolean) m_properties.get(CONF_NAME_PUBLISH_ON_CHANGE);
		if (publishOnChange) {
			doPublish = changed;
		}

		Integer averageInterval = (Integer) m_properties.get(CONF_NAME_AVERAGE_INTERVAL);
		if (averageInterval != 0) {
			if (averagingIntervalElapsed && m_numOfPoints > 0) {
				s_logger.info("Average interval elapsed");

				m_compressorAvgPressure =(short) ((double) m_compressorSumOfPressures / (double) m_numOfPoints);
				m_compressorAvgTemperature =(short) ((double) m_compressorSumOfTemperatures / (double) m_numOfPoints);
				m_dryerAvgTemperature =(short) ((double) m_dryerSumOfTemperatures / (double) m_numOfPoints);

				s_logger.info("Average commpressor pressure {}", m_compressorAvgPressure);
				s_logger.info("Average commpressor temperature {}", m_compressorAvgTemperature);
				s_logger.info("Average dryer temperature {}", m_dryerAvgTemperature);


				m_compressorSumOfPressures = 0;
				m_compressorSumOfTemperatures = 0;
				m_dryerSumOfTemperatures = 0;
				m_numOfPoints = 0;

				topics.add(SEMANTIC_TOPIC_TIME_SERIES);

				doPublish = true;
			} else {
				m_compressorSumOfPressures += status.getCompressorPressure();
				m_compressorSumOfTemperatures += status.getCompressorTemperature();
				m_dryerSumOfTemperatures += status.getDryerTemperature();
				m_numOfPoints++;				
			}

			enabled = (Boolean) m_properties.get(CONF_NAME_COMPRESSOR_PRESSURE_ENABLE);
			if (enabled) {
				payload.addMetric(METRIC_NAME_COMPRESSOR_AVG_PRESSURE, Integer.valueOf(m_compressorAvgPressure));
			}

			enabled = (Boolean) m_properties.get(CONF_NAME_COMPRESSOR_TEMPERATURE_ENABLE);
			if (enabled) {
				payload.addMetric(METRIC_NAME_COMPRESSOR_AVG_TEMPERATURE, Integer.valueOf(m_compressorAvgTemperature));
			}

			enabled = (Boolean) m_properties.get(CONF_NAME_DRYER_TEMPERATURE_ENABLE);
			if (enabled) {
				payload.addMetric(METRIC_NAME_DRYER_AVG_TEMPERATURE, Integer.valueOf(m_dryerAvgTemperature));
			}
		}

		if (doPublish) {

			if (topics.isEmpty()) {
				topics.add(SEMANTIC_TOPIC_DEFAULT);
			}

			for (String topic : topics) {
				s_logger.info("Publishing on topic: {}", topic);

				// Try to publish the message
				try {
					m_cloudAppClient.publish(topic, payload, DFLT_QOS, DFLT_RETAIN);
				} catch (Throwable t) {
					s_logger.error("Cannot publish topic: {}", topic);
				}
			}
		}

		m_lastCompressorStatus = status;
		m_lastErrorHistory = errorHistory;
	}
}
