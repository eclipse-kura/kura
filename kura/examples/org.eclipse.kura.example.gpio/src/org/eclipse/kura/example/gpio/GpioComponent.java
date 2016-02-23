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
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.gpio.PinStatusListener;
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

	private GPIOService m_GPIOService;
	
	private Map<String, Object> m_properties;

	private static ArrayList<KuraGPIOPin> m_pins = new ArrayList<KuraGPIOPin>();
	
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
	
	public void setGPIOService(GPIOService gpioService){
		m_GPIOService = gpioService;
	}
	
	public void unsetGPIOService(GPIOService gpioService){
		m_GPIOService = null;
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
		
		Iterator<KuraGPIOPin> pins_it = m_pins.iterator();
		while (pins_it.hasNext()) {
			try {
				KuraGPIOPin p = pins_it.next();				
				s_logger.warn("Closing GPIO pin {}", p);
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

		Iterator<KuraGPIOPin> pins_it = m_pins.iterator();
		while (pins_it.hasNext()) {
			try {
				KuraGPIOPin p = pins_it.next();				
				s_logger.warn("Closing GPIO pin {}", p);
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
		s_logger.info("______________________________");
		s_logger.info("Available GPIOs on the system:");
		Map<Integer, String> gpios = m_GPIOService.getAvailablePins();
		for(Entry<Integer, String> e: gpios.entrySet()){
			s_logger.info("#{} - [{}]", e.getKey(), e.getValue());
		}
		s_logger.info("______________________________");
		for (int i = 0; i < pins.length; i++) {
			try {
				final int pinNum = pins[i];
				s_logger.info("Acquiring GPIO pin {} with params:",pins[i]);
				s_logger.info("   Direction....: {}",directions[i]);
				s_logger.info("   Mode.........: {}",modes[i]);
				s_logger.info("   Trigger......: {}",triggers[i]);
				KuraGPIOPin p = m_GPIOService.getPinByTerminal(
						pins[i], 
						getPinDirection(directions[i]), 
						getPinMode(modes[i]), 
						getPinTrigger(triggers[i]));
				p.open();
				s_logger.info("GPIO pin {} acquired", pins[i]);
				m_pins.add(p);
				if(p.getDirection() == KuraGPIODirection.OUTPUT){
					final int final_index = i;
					m_blinker = m_blinker_executor.scheduleAtFixedRate(new Runnable(){
						@Override
						public void run() {
							try {
								boolean value = !m_pins.get(final_index).getValue();
								s_logger.info("Setting GPIO pin {} to {}", m_pins.get(final_index), value);							
								m_pins.get(final_index).setValue(value);
								//s_logger.info("Trigger = "+m_pins.get(final_index).getTrigger());
							} catch (KuraUnavailableDeviceException e) {
								s_logger.warn("GPIO pin {} is not available for export.", final_index);
							} catch (KuraClosedDeviceException e) {
								s_logger.warn("GPIO pin {} has been closed.", final_index);
							} catch (IOException e) {
								s_logger.error("I/O Error occurred!");					
								e.printStackTrace();
							}
						}						
					}, 0, 2, TimeUnit.SECONDS);
				}else{
					s_logger.info("Attaching Pin Listener to GPIO pin {}", pins[i]);
					PinStatusListener listener = new PinStatusListener() {
						private int pinNumber = pinNum;
						@Override
						public void pinStatusChange(boolean value) {
							s_logger.info("Pin status for GPIO pin {} changed to {}", pinNumber, value);
						}					
					};
					p.addPinStatusListener(listener);
				}				
			} catch (IOException e) {
				s_logger.error("I/O Error occurred!");
				e.printStackTrace();
			} catch (Exception e) {
				s_logger.error(e.getLocalizedMessage());
			}			
		}
	}
	
	private KuraGPIODirection getPinDirection(int direction){
		switch(direction){
		case 0:
		case 2:
			return KuraGPIODirection.INPUT;
		case 1:
		case 3:
			return KuraGPIODirection.OUTPUT;
		}
		return KuraGPIODirection.OUTPUT;
	}

	private KuraGPIOMode getPinMode(int mode){
		switch(mode){
		case 2:
			return KuraGPIOMode.INPUT_PULL_DOWN;
		case 1:
			return KuraGPIOMode.INPUT_PULL_UP;
		case 8:
			return KuraGPIOMode.OUTPUT_OPEN_DRAIN;
		case 4:
			return KuraGPIOMode.OUTPUT_PUSH_PULL;
		}
		return KuraGPIOMode.OUTPUT_OPEN_DRAIN;
	}
	
	private KuraGPIOTrigger getPinTrigger(int trigger){
		switch(trigger){
		case 0:
			return KuraGPIOTrigger.NONE;
		case 2:
			return KuraGPIOTrigger.RAISING_EDGE;
		case 3:
			return KuraGPIOTrigger.BOTH_EDGES;
		case 1:
			return KuraGPIOTrigger.FALLING_EDGE;
		default:
			return KuraGPIOTrigger.NONE;
		}
	}
}
