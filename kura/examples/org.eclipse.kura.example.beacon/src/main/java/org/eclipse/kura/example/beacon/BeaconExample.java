package org.eclipse.kura.example.beacon;

import java.util.Map;

import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothBeaconCommandListener;
import org.eclipse.kura.bluetooth.BluetoothService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeaconExample implements ConfigurableComponent, BluetoothBeaconCommandListener{

	private static final Logger s_logger = LoggerFactory.getLogger(BeaconExample.class);

	private String PROPERTY_ENABLE        = "enableAdvertising";
	private String PROPERTY_MIN_INTERVAL  = "minBeaconInterval";
	private String PROPERTY_MAX_INTERVAL  = "maxBeaconInterval";
	private String PROPERTY_UUID          = "uuid";
	private String PROPERTY_MAJOR         = "major";
	private String PROPERTY_MINOR         = "minor";
	private String PROPERTY_COMPANY       = "companyCode";
	private String PROPERTY_TX_POWER      = "txPower";
	private String PROPERTY_LIMITED       = "LELimited";
	private String PROPERTY_BR_SUPPORTED  = "BR_EDRSupported";
	private String PROPERTY_BR_CONTROLLER = "LE_BRController";
	private String PROPERTY_BR_HOST       = "LE_BRHost";
	private String PROPERTY_INAME         = "iname";

	private BluetoothService        m_bluetoothService;
	private BluetoothAdapter        m_bluetoothAdapter;
	
	private boolean m_enable;
	private Integer m_minInterval;
	private Integer m_maxInterval;
	private String  m_uuid;
	private Integer m_major;
	private Integer m_minor;
	private String  m_companyCode;
	private Integer m_txPower;
	private boolean m_LELimited;
	private boolean m_BRSupported;
	private boolean m_BRController;
	private boolean m_BRHost;
	private String  m_iname = "hci0";

	public void setBluetoothService(BluetoothService bluetoothService) {
		m_bluetoothService = bluetoothService;
	}

	public void unsetBluetoothService(BluetoothService bluetoothService) {
		m_bluetoothService = null;
	}

	// --------------------------------------------------------------------
	//
	//  Activation APIs
	//
	// --------------------------------------------------------------------
	protected void activate(ComponentContext context, Map<String,Object> properties) {
		s_logger.info("Activating Bluetooth Beacon example...");
		
		if(properties!=null){
			if(properties.get(PROPERTY_ENABLE)!=null)
				m_enable = (Boolean) properties.get(PROPERTY_ENABLE);
			if(properties.get(PROPERTY_MIN_INTERVAL)!=null)
				m_minInterval = (int) ((Integer) properties.get(PROPERTY_MIN_INTERVAL) / 0.625);
			if(properties.get(PROPERTY_MAX_INTERVAL)!=null)
				m_maxInterval = (int) ((Integer) properties.get(PROPERTY_MAX_INTERVAL) / 0.625);
			if(properties.get(PROPERTY_UUID)!=null) {
				if(((String)properties.get(PROPERTY_UUID)).trim().replace("-", "").length() == 32)
					m_uuid = ((String) properties.get(PROPERTY_UUID)).replace("-", "");
				else
					s_logger.warn("UUID is too short!");
			}
			if(properties.get(PROPERTY_MAJOR)!=null)
				m_major = (Integer) properties.get(PROPERTY_MAJOR);
			if(properties.get(PROPERTY_MINOR)!=null)
				m_minor = (Integer) properties.get(PROPERTY_MINOR);			
			if(properties.get(PROPERTY_COMPANY)!=null)
				m_companyCode = (String) properties.get(PROPERTY_COMPANY);
			if(properties.get(PROPERTY_TX_POWER)!=null)
				m_txPower = (Integer) properties.get(PROPERTY_TX_POWER);
			if(properties.get(PROPERTY_LIMITED)!=null)
				m_LELimited = (Boolean) properties.get(PROPERTY_LIMITED);
			if(properties.get(PROPERTY_BR_SUPPORTED)!=null)
				m_BRSupported = (Boolean) properties.get(PROPERTY_BR_SUPPORTED);
			if(properties.get(PROPERTY_BR_CONTROLLER)!=null)
				m_BRController = (Boolean) properties.get(PROPERTY_BR_CONTROLLER);
			if(properties.get(PROPERTY_BR_HOST)!=null)
				m_BRHost = (Boolean) properties.get(PROPERTY_BR_HOST);
			if(properties.get(PROPERTY_INAME)!=null)
				m_iname = (String) properties.get(PROPERTY_INAME);
		}
		

		// Get Bluetooth adapter with Beacon capabilities and ensure it is enabled
		m_bluetoothAdapter = m_bluetoothService.getBluetoothAdapter(m_iname, this);
		if (m_bluetoothAdapter != null) {
			s_logger.info("Bluetooth adapter interface => " + m_iname);
			s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
			s_logger.info("Bluetooth adapter le enabled => " + m_bluetoothAdapter.isLeReady());

			if (!m_bluetoothAdapter.isEnabled()) {
				s_logger.info("Enabling bluetooth adapter...");
				m_bluetoothAdapter.enable();
				s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
			}

			configureBeacon();
				
		}
		else 
			s_logger.warn("No Bluetooth adapter found ...");

	}

	protected void deactivate(ComponentContext context) {

		s_logger.debug("Deactivating Beacon Example...");
		
		// Stop the advertising
		m_bluetoothAdapter.stopBeaconAdvertising();

		// cancel bluetoothAdapter
		m_bluetoothAdapter = null;
		
		s_logger.debug("Deactivating Beacon Example... Done.");
	}

	protected void updated(Map<String,Object> properties) {

		if(properties!=null){
			if(properties.get(PROPERTY_ENABLE)!=null)
				m_enable = (Boolean) properties.get(PROPERTY_ENABLE);
			if(properties.get(PROPERTY_MIN_INTERVAL)!=null)
				m_minInterval = (int) ((Integer) properties.get(PROPERTY_MIN_INTERVAL) / 0.625);
			if(properties.get(PROPERTY_MAX_INTERVAL)!=null)
				m_maxInterval = (int) ((Integer) properties.get(PROPERTY_MAX_INTERVAL) / 0.625);
			if(properties.get(PROPERTY_UUID)!=null) {
				if(((String)properties.get(PROPERTY_UUID)).trim().replace("-", "").length() == 32)
					m_uuid = ((String) properties.get(PROPERTY_UUID)).replace("-", "");
				else
					s_logger.warn("UUID is too short!");
			}
			if(properties.get(PROPERTY_MAJOR)!=null)
				m_major = (Integer) properties.get(PROPERTY_MAJOR);
			if(properties.get(PROPERTY_MINOR)!=null)
				m_minor = (Integer) properties.get(PROPERTY_MINOR);			
			if(properties.get(PROPERTY_COMPANY)!=null)
				m_companyCode = (String) properties.get(PROPERTY_COMPANY);	
			if(properties.get(PROPERTY_TX_POWER)!=null)
				m_txPower = (Integer) properties.get(PROPERTY_TX_POWER);
			if(properties.get(PROPERTY_LIMITED)!=null)
				m_LELimited = (Boolean) properties.get(PROPERTY_LIMITED);
			if(properties.get(PROPERTY_BR_SUPPORTED)!=null)
				m_BRSupported = (Boolean) properties.get(PROPERTY_BR_SUPPORTED);
			if(properties.get(PROPERTY_BR_CONTROLLER)!=null)
				m_BRController = (Boolean) properties.get(PROPERTY_BR_CONTROLLER);
			if(properties.get(PROPERTY_BR_HOST)!=null)
				m_BRHost = (Boolean) properties.get(PROPERTY_BR_HOST);
			if(properties.get(PROPERTY_INAME)!=null)
				m_iname = (String) properties.get(PROPERTY_INAME);
		}
		
		// Stop the advertising
		m_bluetoothAdapter.stopBeaconAdvertising();
		
		// cancel bluetoothAdapter
		m_bluetoothAdapter = null;
		
		// Get Bluetooth adapter and ensure it is enabled
		m_bluetoothAdapter = m_bluetoothService.getBluetoothAdapter(m_iname, this);
		if (m_bluetoothAdapter != null) {
			s_logger.info("Bluetooth adapter interface => " + m_iname);
			s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
			s_logger.info("Bluetooth adapter le enabled => " + m_bluetoothAdapter.isLeReady());

			if (!m_bluetoothAdapter.isEnabled()) {
				s_logger.info("Enabling bluetooth adapter...");
				m_bluetoothAdapter.enable();
				s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
			}

			configureBeacon();
				
		}
		else 
			s_logger.warn("No Bluetooth adapter found ...");
		
		s_logger.debug("Updating Beacon Example... Done.");
	}

	// --------------------------------------------------------------------
	//
	//  Private methods
	//
	// --------------------------------------------------------------------

	private void configureBeacon() {

		if (m_enable) {
			
			if (m_minInterval != null && m_maxInterval != null) {
				m_bluetoothAdapter.setBeaconAdvertisingInterval(m_minInterval, m_maxInterval);
			}

			m_bluetoothAdapter.startBeaconAdvertising();
			
			if (m_uuid != null && m_major != null && m_minor != null && m_companyCode != null && m_txPower != null) {
				m_bluetoothAdapter.setBeaconAdvertisingData(m_uuid, m_major, m_minor, m_companyCode, m_txPower, m_LELimited, (m_LELimited) ? false : true, 
						m_BRSupported, m_BRController, m_BRHost);
			}
			
		}
		else
			m_bluetoothAdapter.stopBeaconAdvertising();
	}

	// --------------------------------------------------------------------
	//
	//  BluetoothBeaconCommandListener APIs
	//
	// --------------------------------------------------------------------
	@Override
	public void onCommandFailed(String errorCode) {
		s_logger.warn("Error in executing command. Error Code: " + errorCode);
	}

	@Override
	public void onCommandResults(String results) {
		s_logger.info("Command results : " + results);
	}
	
}
