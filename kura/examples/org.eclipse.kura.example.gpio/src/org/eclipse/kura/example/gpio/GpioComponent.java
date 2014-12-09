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
package org.eclipse.kura.example.gpio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceNotFoundException;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.UnsupportedDeviceTypeException;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;
import jdk.dio.gpio.PinEvent;
import jdk.dio.gpio.PinListener;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpioComponent implements ConfigurableComponent, PinListener {

	private static final Logger s_logger = LoggerFactory
			.getLogger(GpioComponent.class);

	// Cloud Application identifier
	private static final String APP_ID = "GPIO_COMPONENT";

	// Property Names
	private static final String PROP_NAME_GPIO_PINS = "gpio.pins";
	private static final String PROP_NAME_GPIO_DIRECTIONS = "gpio.directions";
	private static final String PROP_NAME_GPIO_MODES = "gpio.modes";
	private static final String PROP_NAME_GPIO_TRIGGERS = "gpio.triggers";

	private Map<String, Object> m_properties;

	private ArrayList<GPIOPin> m_pins = new ArrayList<GPIOPin>();

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext,
			Map<String, Object> properties) {
		s_logger.debug("Activating {}", APP_ID);

		m_properties = new HashMap<String, Object>();

		doUpdate(properties);

		s_logger.info("Activating {}... Done.", APP_ID);

	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("Deactivating {}", APP_ID);
	}

	public void updated(Map<String, Object> properties) {
		s_logger.info("updated...");

		doUpdate(properties);
	}

	// ----------------------------------------------------------------
	//
	// Private Methods
	//
	// ----------------------------------------------------------------
	
	/**
	 * Called after a new set of properties has been configured on the service
	 */
	private void doUpdate(Map<String, Object> properties) {
		for (String s : properties.keySet()) {
			s_logger.info("Update - " + s + ": " + properties.get(s));
		}

		m_properties.clear();
		m_properties.putAll(properties);

		Iterator<GPIOPin> pins_it = m_pins.iterator();
		while (pins_it.hasNext()) {
			try {
				GPIOPin p = pins_it.next();
				s_logger.warn("Closing {}", p.getDescriptor().getID());
				p.setInputListener(null);
				p.close();
			} catch (IOException e) {
				s_logger.warn("Cannot close pin!");
			}
		}

		m_pins.clear();

		Integer[] pins = (Integer[]) properties.get(PROP_NAME_GPIO_PINS);
		Integer[] directions = (Integer[]) properties
				.get(PROP_NAME_GPIO_DIRECTIONS);
		Integer[] modes = (Integer[]) properties.get(PROP_NAME_GPIO_MODES);
		Integer[] triggers = (Integer[]) properties
				.get(PROP_NAME_GPIO_TRIGGERS);
		for (int i = 0; i < pins.length; i++) {
			try {
				s_logger.info("Acquiring GPIO pin {} with params:",pins[i]);
				s_logger.info("   Direction....: {}",directions[i]);
				s_logger.info("   Mode.........: {}",modes[i]);
				s_logger.info("   Trigger......: {}",triggers[i]);
				GPIOPinConfig config = new GPIOPinConfig(DeviceConfig.DEFAULT,
						pins[i], directions[i], modes[i], triggers[i], false);
				GPIOPin p = DeviceManager.open(GPIOPin.class, config);
				s_logger.info("GPIO pin {} acquired", pins[i]);
				m_pins.add(p);
				if(p.getDirection() == GPIOPinConfig.DIR_OUTPUT_ONLY){
					s_logger.info("Activating GPIO pin {}", pins[i]);
				}else{
					s_logger.info("Attaching Pin Listener to GPIO pin {}", pins[i]);
				}
				
			} catch (InvalidDeviceConfigException e) {
				s_logger.warn("Invalid PIN configuration for GPIO pin {}", pins[i]);
			} catch (UnsupportedDeviceTypeException e) {
				s_logger.error("Unsupported device! Gpio pin {}", pins[i]);
			} catch (DeviceNotFoundException e) {
				s_logger.warn("Cannot find device GPIO {} on the device", pins[i]);
			} catch (UnavailableDeviceException e) {
				s_logger.warn("GPIO pin {} is not available for export.", pins[i]);
			} catch (IOException e) {
				s_logger.error("I/O Error occurred!");
				e.printStackTrace();
			} catch (Exception e) {
				s_logger.error(e.getLocalizedMessage());
			}
		}
	}

	@Override
	public void valueChanged(PinEvent pinEvent) {
		s_logger.info("Pin status for GPIO pin {} changed to {}", pinEvent.getDevice().getDescriptor().getID(), pinEvent.getValue());
	}
}
