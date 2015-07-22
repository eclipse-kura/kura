package org.eclipse.kura.linux.gpio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPIOServiceImpl implements GPIOService {
	private static final Logger s_logger = LoggerFactory.getLogger(GPIOServiceImpl.class);

	private static final HashMap<Integer, String> pins = new HashMap<Integer, String>();

	private SystemService m_SystemService;
	
	public void setSystemService(SystemService systemService){
		m_SystemService = systemService;
	}
	
	public void unsetSystemService(SystemService systemService){
		m_SystemService = null;
	}
	
	protected void activate(ComponentContext componentContext) {
		s_logger.debug("activating jdk.dio GPIOService");

		String prova = m_SystemService.getProperties().getProperty("jdk.dio.registry");
		prova = m_SystemService.getProperties().getProperty("kura.configuration");
		if (prova == null) {
			try {
				File dioPropsFile = new File(System.getProperty("jdk.dio.registry"));
				if (dioPropsFile.exists()) {
					Properties dioDefaults = new Properties();
					dioDefaults.load(new FileReader(dioPropsFile));
					s_logger.info("Loaded File jdk.dio.properties: " + dioPropsFile);
				} else {
					s_logger.warn("File does not exist: " + dioPropsFile);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		s_logger.debug("GPIOService activated.");
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("deactivating jdk.dio GPIOService");
	}

	@Override
	public KuraGPIOPin getPinByName(String pinName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public KuraGPIOPin getPinByName(String pinName, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public KuraGPIOPin getPinByTerminal(int terminal) {
		return new JdkDioPin(terminal);
	}

	@Override
	public KuraGPIOPin getPinByTerminal(int terminal, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger) {
		return new JdkDioPin(terminal, direction, mode, trigger);
	}

	@Override
	public Map<Integer, String> getAvailablePins() {
		// TODO Auto-generated method stub
		return null;
	}

}
