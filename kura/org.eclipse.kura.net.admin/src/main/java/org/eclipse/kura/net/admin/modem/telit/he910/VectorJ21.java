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
package org.eclipse.kura.net.admin.modem.telit.he910;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class VectorJ21 {

	private static final int [] J21Pins = {3,5,7,9,13,15,17,19,2,4,6,8,10,14,16,18,20,22};
	
	private int [] j21PinDirections = null;

	/**
	 * VectorJ21 constructor
	 */
	public VectorJ21 () {
		
		this.j21PinDirections = new int [J21Pins.length];
		
		try {
			Properties props = new Properties();
			InputStream is = this.getClass().getResourceAsStream(
					"/resources/j21pin.direction.config");
			props.load(is);
			Enumeration keys = props.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				int j21Pin = this.getJ21Pin(key);
				int j21PinInd = this.getJ21PinIndex(j21Pin);
				String sPinDir = props.getProperty(key);
				if (sPinDir.compareToIgnoreCase("IN") == 0) {
					this.j21PinDirections[j21PinInd] = IVectorJ21GpioService.J21PIN_DIRECTION_IN;
				} else if (sPinDir.compareToIgnoreCase("OUT") == 0) {
					this.j21PinDirections[j21PinInd] = IVectorJ21GpioService.J21PIN_DIRECTION_OUT;
				} else {
					this.j21PinDirections[j21PinInd] = IVectorJ21GpioService.J21PIN_DIRECTION_OUT;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			for (int i = 0; i < this.j21PinDirections.length; i++) {
				this.j21PinDirections[i] = IVectorJ21GpioService.J21PIN_DIRECTION_OUT;
			}
		}
	}
	
	
	/**
	 * Reports J21 pins array
	 * 
	 * @return J21 pins array as {@link int []}
	 */
	public static int[] getJ21pins() {
		return J21Pins;
	}



	/**
	 * Reports J21 pin direction
	 * 
	 * @param j21pin - J21 pin number as {@link int}
	 * @return J21 pin direction as {@link int}
	 * @throws Exception
	 */
	public int getPinDirection (int j21pin) throws Exception {

		return this.j21PinDirections[this.getJ21PinIndex(j21pin)];
	}
	
	/**
	 * Reports index of J21 pin
	 * 
	 * @param j21pin - J21 pin number as {@link int}
	 * @return index of J21 pin as {@link int}
	 * @throws Exception
	 */
	public int getJ21PinIndex (int j21pin) throws Exception {
		
		int j21PinInd = -1;
		
		for (int i = 0; i < J21Pins.length; i++) {
			if (j21pin == J21Pins[i]) {
				j21PinInd = i;
				break;
			}
		}
		if (j21PinInd == -1) {
//			throw new J21GpioException ("Invalid J21 pin");
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Invalid J21 pin");
		}
		return j21PinInd;
	}
	
	/*
	 * This method reports a value (J21 pin number) for given property name
	 */
	private int getJ21Pin (String propName) throws Exception {
		
		int ind = propName.lastIndexOf(".");
		if (ind < 0) {
//			throw new J21GpioException ("Invalid property name: " + propName);
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Invalid property name: " + propName);
		}
		
		try {
			int pinNo = Integer.parseInt(propName.substring(ind+1));
			return pinNo;
		} catch (Exception e) {
//			throw new J21GpioException ("Invalid property name: " + propName, e);
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Invalid property name: " + propName, e);
		}
	}
}
