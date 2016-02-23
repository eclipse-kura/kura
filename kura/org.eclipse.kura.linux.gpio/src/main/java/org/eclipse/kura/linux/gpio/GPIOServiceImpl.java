/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.linux.gpio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
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

	//private static final HashMap<Integer, String> pins = new HashMap<Integer, String>();

	private static final HashSet<JdkDioPin> pins = new HashSet<JdkDioPin>();

	private SystemService m_SystemService;

	public void setSystemService(SystemService systemService) {
		m_SystemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		m_SystemService = null;
	}

	protected void activate(ComponentContext componentContext) {
		s_logger.debug("activating jdk.dio GPIOService");

		File dioPropsFile= null;
		FileReader fr= null;
		try {
			String configFile = System.getProperty("jdk.dio.registry");
			if(configFile == null){
				//Emulator?
				configFile = m_SystemService.getProperties().getProperty("kura.configuration").replace("kura.properties", "jdk.dio.properties");
			}else{
				configFile = "file:"+configFile;
			}
			
			dioPropsFile = new File(new URL(configFile).toURI());
			if (dioPropsFile.exists()) {
				Properties dioDefaults = new Properties();
				fr= new FileReader(dioPropsFile);
				dioDefaults.load(fr);

				pins.clear();

				for (Map.Entry<Object, Object> entry : dioDefaults.entrySet()) {
					Object k = entry.getKey();
					//s_logger.info("{} -> {}", k, dioDefaults.get(k));
					String line = (String) entry.getValue();

					JdkDioPin p = JdkDioPin.parseFromProperty(k, line);
					if(p != null){
						pins.add(p);
					}
				}
				s_logger.info("Loaded File jdk.dio.properties: " + dioPropsFile);
			} else {
				s_logger.warn("File does not exist: " + dioPropsFile);
			}
		} catch (FileNotFoundException e) {
			s_logger.error("Exception while accessing resource!", e);
		} catch (IOException e) {
			s_logger.error("Exception while accessing resource!", e);
		} catch (URISyntaxException e) {
			s_logger.error("Exception while accessing resource!", e);
		} finally {
			if (fr != null){
				try {
					fr.close();
				} catch (IOException e) {
					s_logger.error("Exception while releasing resource!", e);
				}
			}
		}

		s_logger.debug("GPIOService activated.");
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("deactivating jdk.dio GPIOService");
	}

	@Override
	public KuraGPIOPin getPinByName(String pinName) {
		for(JdkDioPin p : pins){
			if(p.getName().equals(pinName)){
				return p;
			}
		}
		return null;
	}

	@Override
	public KuraGPIOPin getPinByName(String pinName, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger) {
		for(JdkDioPin p : pins){
			if(p.getName().equals(pinName)){
				if((p.getDirection() != direction) ||
						(p.getMode() != mode) ||
						(p.getTrigger() != trigger)){
					if(p.isOpen()){
						try {
							p.close();	
						} catch (IOException e) {
							s_logger.warn("Cannot close GPIO Pin {}", pinName);
							return p;
						}
					}
					int index = p.getIndex();
					pins.remove(p);
					JdkDioPin newPin = new JdkDioPin(index, pinName, direction, mode, trigger);
					pins.add(newPin);
					return newPin;
				}
				return p;
			}
		}
		return null;
	}

	@Override
	public KuraGPIOPin getPinByTerminal(int terminal) {
		for(JdkDioPin p : pins){
			if(p.getIndex() == terminal){
				return p;
			}
		}
		JdkDioPin newPin = new JdkDioPin(terminal);
		pins.add(newPin);
		return newPin;
	}

	@Override
	public KuraGPIOPin getPinByTerminal(int terminal, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger) {
		for(JdkDioPin p : pins){
			if(p.getIndex() == terminal){
				if((p.getDirection() != direction) ||
						(p.getMode() != mode) ||
						(p.getTrigger() != trigger)){
					if(p.isOpen()){
						try {
							p.close();	
						} catch (IOException e) {
							s_logger.warn("Cannot close GPIO Pin {}", terminal);
							return p;
						}
					}
					String pinName = p.getName();
					pins.remove(p);
					JdkDioPin newPin = new JdkDioPin(terminal, pinName, direction, mode, trigger);
					pins.add(newPin);
					return newPin;
				}
				return p;
			}
		}
		JdkDioPin newPin = new JdkDioPin(terminal, null, direction, mode, trigger);
		pins.add(newPin);
		return newPin;
	}

	@Override
	public Map<Integer, String> getAvailablePins() {
		HashMap<Integer, String> result = new HashMap<Integer, String>();
		for(JdkDioPin p : pins){
			result.put(p.getIndex(), p.getName());
		}

		return result;
	}

}
