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
package org.eclipse.kura.net.admin.modem.telit.de910;

/**
 * Defines GPIO service interface for the Vector board
 * 
 * @author ilya.binshtok
 *
 */
public interface IVectorJ21GpioService {

	/**
	 * service name of this interface for the bundle activator
	 */
	public static final String SERVICE_NAME = IVectorJ21GpioService.class.getName();
	
	public static final int J21PIN_DIRECTION_IN = 0;
	public static final int J21PIN_DIRECTION_OUT = 1;
	
	public static final int J21PIN_CELL_RESET = 6;
	public static final int J21PIN_CELL_ON_OFF = 8;
	public static final int J21PIN_CELL_PWR_EN = 10;
	public static final int J21PIN_USB_RESET = 13;
	public static final int J21PIN_5V_ENABLE = 15;
	public static final int J21PIN_ZIGBEE_PWR_EN = 18;
	public static final int J21PIN_PWR_MON = 17;
	public static final int J21PIN_USB_SER_PWR_EN = 19;
	public static final int J21PIN_GPS_PWR_EN = 14;
	public static final int J21PIN_SLOT_PWR_EN = 16;
	public static final int J21PIN_LAN_PWR_ON = 20;
	public static final int J21PIN_ADC_ON = 22;
	
	/**
	 * Reports direction of J21 pin.
	 * 
	 * @param j21pin - J21 pin number as {@link int}
	 * @return J21 pin direction as {@link int}
	 * @throws Exception
	 */
	public int j21pinGetDirection (int j21pin) throws Exception;
	
	/**
	 * Sets direction of J21 pin.
	 * 
	 * @param j21pin - J21 pin number as {@link int}
	 * @param direction - J21 pin direction as {@link int}
	 * @throws Exception
	 */
	public void j21pinSetDirection (int j21pin, int direction) throws Exception;
	
	
	/**
	 * Reports a state of specified J21 pin.
	 *  
	 * @param j21pin - J21 pin as {@link int}
	 * @return pin status as {@link boolean}
	 * @throws Exception
	 */
	public boolean j21pinIsOn (int j21pin) throws Exception;
	
	/**
	 * Reports a status of specified J21 pin.
	 * 
	 * @param j21pin - J21 pin as {@link int}
	 * @return state of specified J21 pin as {@link String}
	 * @throws Exception
	 */
	public String j21pinGetStatus (int j21pin) throws Exception;
	
	/**
	 * Turns specified J21 pin ON.
	 * 
	 * @param j21pin - J21 pin to turn ON as {@link int}
	 * @throws Exception
	 */
	public void j21pinTurnOn(int j21pin) throws Exception;
	
	/**
	 * Turns specified J21 pin OFF.
	 * 
	 * @param j21pin - J21 pin to turn OFF as {@link int}
	 * @throws Exception
	 */
	public void j21pinTurnOff(int j21pin) throws Exception;
	
	/**
	 * 
	 * @param j21pin
	 * @throws Exception
	 */
	public void j21pinToggle(int j21pin) throws Exception;
}
