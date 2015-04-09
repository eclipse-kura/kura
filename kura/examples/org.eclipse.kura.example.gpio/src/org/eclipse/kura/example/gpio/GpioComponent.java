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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jdk.dio.ClosedDeviceException;
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

public class GpioComponent implements ConfigurableComponent {

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

	private static ArrayList<GPIOPin> m_pins = new ArrayList<GPIOPin>();
	
	private ScheduledFuture<?> m_blinker = null;
	private ScheduledExecutorService m_blinker_executor;

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	public GpioComponent() {
		super();
		m_blinker_executor = Executors.newSingleThreadScheduledExecutor();
	}
	
	protected void activate(ComponentContext componentContext,
			Map<String, Object> properties) {
		s_logger.debug("Activating {}", APP_ID);
		m_properties = new HashMap<String, Object>();

		doUpdate(properties);

		s_logger.info("Activating {}... Done.", APP_ID);

	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("Deactivating {}", APP_ID);
		
		if(m_blinker != null){
			m_blinker.cancel(true);
		}
		
		Iterator<GPIOPin> pins_it = m_pins.iterator();
		while (pins_it.hasNext()) {
			try {
				GPIOPin p = pins_it.next();				
				s_logger.warn("Closing GPIO pin {}", p.getDescriptor().getConfiguration().toString());
				p.close();
			} catch (IOException e) {
				s_logger.warn("Cannot close pin!");
			}
		}
	}

	public void updated(Map<String, Object> properties) {
		s_logger.info("updated...");
		
		if(m_blinker != null){
			m_blinker.cancel(true);
		}

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
//		for (String s : properties.keySet()) {
//			s_logger.info("Update - " + s + ": " + properties.get(s));
//		}

		m_properties.clear();
		m_properties.putAll(properties);

		Iterator<GPIOPin> pins_it = m_pins.iterator();
		while (pins_it.hasNext()) {
			try {
				GPIOPin p = pins_it.next();				
				s_logger.warn("Closing GPIO pin {}", p.getDescriptor().getConfiguration().toString());
				p.close();
			} catch (IOException e) {
				s_logger.warn("Cannot close pin!");
			}
		}

		m_pins.clear();

		Integer[] pins = (Integer[]) properties.get(PROP_NAME_GPIO_PINS);
		Integer[] directions = (Integer[]) properties.get(PROP_NAME_GPIO_DIRECTIONS);
		Integer[] modes = (Integer[]) properties.get(PROP_NAME_GPIO_MODES);
		Integer[] triggers = (Integer[]) properties.get(PROP_NAME_GPIO_TRIGGERS);
		for (int i = 0; i < pins.length; i++) {
			try {
				final int pinNum = pins[i];
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
					final int final_index = i;
					m_blinker = m_blinker_executor.scheduleAtFixedRate(new Runnable(){
						@Override
						public void run() {
							try {
								boolean value = !m_pins.get(final_index).getValue();
								s_logger.info("Setting GPIO pin {} to {}", m_pins.get(final_index).getDescriptor().toString(), value);							
								m_pins.get(final_index).setValue(value);
								//s_logger.info("Trigger = "+m_pins.get(final_index).getTrigger());
							} catch (UnavailableDeviceException e) {
								s_logger.warn("GPIO pin {} is not available for export.", final_index);
							} catch (ClosedDeviceException e) {
								s_logger.warn("GPIO pin {} has been closed.", final_index);
							} catch (IOException e) {
								s_logger.error("I/O Error occurred!");					
								e.printStackTrace();
							}
						}						
					}, 0, 2, TimeUnit.SECONDS);
				}else{
					s_logger.info("Attaching Pin Listener to GPIO pin {}", pins[i]);
					PinListener listener = new PinListener() {
						private int pinNumber = pinNum;
						@Override
						public void valueChanged(PinEvent event) {
							s_logger.info("Pin status for GPIO pin {} changed to {}", pinNumber, event.getValue());
						}					
					};
					p.setInputListener(listener);
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

}
