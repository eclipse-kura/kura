package org.eclipse.kura.linux.bluetooth;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothServiceImpl implements BluetoothService, ConfigurableComponent {

	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothServiceImpl.class);
	
	private static ComponentContext s_context;
	
	private final String PROPERTY_INAME = "iname";
	
	private String m_name;
	
	// --------------------------------------------------------------------
	//
	//  Activation APIs
	//
	// --------------------------------------------------------------------
	protected void activate(ComponentContext context, Map<String,Object> properties) {
		s_logger.info("Activating Bluetooth Service...");
		s_context = context;
		m_name = (String) properties.get(PROPERTY_INAME);
	}
	
	protected void deactivate(ComponentContext context) {
		s_logger.debug("Deactivating Bluetooth Service...");
	}
	
	protected void updated(Map<String,Object> properties) {
		s_logger.debug("Updating Bluetooth Service...");
	}
	
	// --------------------------------------------------------------------
	//
	//  Service APIs
	//
	// --------------------------------------------------------------------
	@Override
	public BluetoothAdapter getBluetoothAdapter() {
		return getBluetoothAdapter(m_name);
	}
	
	@Override
	public BluetoothAdapter getBluetoothAdapter(String name) {
		try {
			BluetoothAdapterImpl ba = new BluetoothAdapterImpl(name);
			return ba;
		} catch (KuraException e) {
			s_logger.error("Could not get bluetooth adapter", e);
			return null;
		}
	}
	
	// --------------------------------------------------------------------
	//
	//  Local methods
	//
	// --------------------------------------------------------------------
	public static BundleContext getBundleContext() {
		return s_context.getBundleContext();
	}

}
