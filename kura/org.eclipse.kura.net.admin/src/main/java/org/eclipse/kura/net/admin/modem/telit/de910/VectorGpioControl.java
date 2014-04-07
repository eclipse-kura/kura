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

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines GPIO control for Vector board
 * 
 * @author ilya.binshtok
 *
 */
public class VectorGpioControl implements IVectorJ21GpioService {

	private static final Logger s_logger = LoggerFactory.getLogger(VectorGpioControl.class);
	
	private static final int GPIOCMD_PIN_STATUS = 0;
	private static final int GPIOCMD_PIN_OUT = 1;
	private static final int GPIOCMD_PIN_DIRECTION = 2;
	
	private static VectorGpioControl s_vectorGpioControl = null;
	
	//private static final int [] J21Pins = {3,5,7,9,13,15,17,19,2,4,6,8,10,14,16,18,20,22};
	
	private ICatalystSmBusService catalystSmBusService = null;
	
	// Slave address for TCA8418 U47 J21
	private static int TCA8418_ADDRESS = 0x34;
	
	private VectorJ21 vectorJ21 = null;
	
	private VectorGpioControl() {
		this.catalystSmBusService = SmBus.getInstance();
		
		this.vectorJ21 = new VectorJ21();
		
		for (int i = 0; i < VectorJ21.getJ21pins().length; i++) {
			try {
				this.setPinDirection(VectorJ21.getJ21pins()[i], this.vectorJ21
						.getPinDirection(VectorJ21.getJ21pins()[i]));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static VectorGpioControl getInstance() {
		if(s_vectorGpioControl == null) {
			s_vectorGpioControl = new VectorGpioControl();
		}
		return s_vectorGpioControl;
	}
	
//	/**
//	 * Binds services and allocates resources
//	 * 
//	 * @param catalystSmBusService - Catalyst SmBus service as {@link ICatalystSmBusService}
//	 * @param kuraLoggerService - Kura logger service as {@link IKuraLoggerService}
//	 */
//	public void bind(ICatalystSmBusService catalystSmBusService,
//			IKuraLoggerService kuraLoggerService) {
//		
//		this.catalystSmBusService = catalystSmBusService;
//		this.kuraLoggerService = kuraLoggerService;
//		
//		this.vectorJ21 = new VectorJ21();
//		
//		for (int i = 0; i < VectorJ21.getJ21pins().length; i++) {
//			try {
//				this.setPinDirection(VectorJ21.getJ21pins()[i], this.vectorJ21
//						.getPinDirection(VectorJ21.getJ21pins()[i]));
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}
//	
//	/**
//	 * Releases resources
//	 */
//	public void unbind () {
//		
//		this.vectorJ21 = null;
//		this.catalystSmBusService = null;
//		this.kuraLoggerService = null;
//	}
	
	public synchronized int j21pinGetDirection (int j21pin) throws Exception {
		
		return this.getPinDirection(j21pin);
	}
	
	public synchronized void j21pinSetDirection (int j21pin, int direction) throws Exception {
		
		this.setPinDirection(j21pin, direction);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.kura.sbc.vector.gpio.service.IVectorGpioService#j21pinIsOn(int)
	 */
	public synchronized boolean j21pinIsOn(int j21pin) throws Exception {

		byte pinState = this.catalystSmBusService.readByte(
				this.formSlaveReadAddress(),
				this.formCommandByte(GPIOCMD_PIN_STATUS,
						this.vectorJ21.getJ21PinIndex(j21pin)));

		return ((pinState & (1 << (this.vectorJ21.getJ21PinIndex(j21pin) % 8))) != 0) ? true
				: false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.kura.sbc.vector.j21.gpio.service.IVectorJ21GpioService#j21pinGetStatus(int)
	 */
	public synchronized String j21pinGetStatus(int j21pin) throws Exception {

		byte pinState = this.catalystSmBusService.readByte(
				this.formSlaveReadAddress(),
				this.formCommandByte(GPIOCMD_PIN_STATUS,
						this.vectorJ21.getJ21PinIndex(j21pin)));

		return ((pinState & (1 << (this.vectorJ21.getJ21PinIndex(j21pin) % 8))) != 0) ? "ON" : "OFF";
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.kura.sbc.vector.gpio.service.IVectorGpioService#j21pinTurnOn(int)
	 */
	public synchronized void j21pinTurnOn(int j21pin) throws Exception {

		s_logger.debug("Turning J21 GPIO Pin " + j21pin + " ON");
		byte pinStatus = this.getPinStatus(j21pin);
		byte[] mask = { (byte) (pinStatus | (1 << (this.vectorJ21.getJ21PinIndex(j21pin) % 8))) };
		this.catalystSmBusService.write(
				this.formSlaveWriteAddress(),
				this.formCommandByte(GPIOCMD_PIN_OUT,
						this.vectorJ21.getJ21PinIndex(j21pin)), mask);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.kura.sbc.vector.gpio.service.IVectorGpioService#j21pinTurnOff(int)
	 */
	public synchronized void j21pinTurnOff(int j21pin) throws Exception {
		
		s_logger.debug("Turning J21 GPIO Pin " + j21pin + " OFF");
		byte pinStatus = this.getPinStatus(j21pin);
		byte [] mask = { (byte)(pinStatus & ~(1 << (this.vectorJ21.getJ21PinIndex(j21pin) % 8))) };
		this.catalystSmBusService.write(
				this.formSlaveWriteAddress(),
				this.formCommandByte(GPIOCMD_PIN_OUT,
						this.vectorJ21.getJ21PinIndex(j21pin)), mask);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.kura.sbc.vector.j21.gpio.service.IVectorJ21GpioService#j21pinToggle(int)
	 */
	public void j21pinToggle(int j21pin) throws Exception {
	
		if (this.j21pinIsOn(j21pin)) {
			s_logger.debug("Turning J21 GPIO Pin " + j21pin + " ON/OFF");
			this.j21pinTurnOff(j21pin);
		} else {
			s_logger.debug("Turning J21 GPIO Pin " + j21pin + " OFF/ON");
			this.j21pinTurnOn(j21pin);
		}
	}
	
	/*
	 * This method reports status of J21 pin specified
	 */
	private byte getPinStatus (int j21pin) throws Exception {
		
		byte pinStatus = this.catalystSmBusService.readByte(
				this.formSlaveReadAddress(),
				this.formCommandByte(GPIOCMD_PIN_STATUS,
						this.vectorJ21.getJ21PinIndex(j21pin)));
		
		return pinStatus;
	}
	
	/*
	 * This method set a direction of J21 pin
	 */
	private void setPinDirection (int j21pin, int direction) throws Exception {
		
		byte pinDirection = this.getPinDirection(j21pin);
		byte [] mask = new byte [1]; 
		switch (direction) {
		case J21PIN_DIRECTION_IN:
			mask [0] = (byte)(pinDirection & ~(1 << (this.vectorJ21.getJ21PinIndex(j21pin) % 8)));
			break;
		case J21PIN_DIRECTION_OUT:
			mask [0] = (byte)(pinDirection | (1 << (this.vectorJ21.getJ21PinIndex(j21pin) % 8)));
			break;
		default:
//			throw new J21GpioException ("Invalid PIN direction");
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Invalid PIN direction");
		}
		
		this.catalystSmBusService.write(
				this.formSlaveWriteAddress(),
				this.formCommandByte(GPIOCMD_PIN_DIRECTION,
						this.vectorJ21.getJ21PinIndex(j21pin)), mask);
	}
	
	/*
	 * This method reports direction of J21 pin specified
	 */
	private byte getPinDirection (int j21pin) throws Exception {
		
		byte pinDirection = this.catalystSmBusService.readByte(
				this.formSlaveReadAddress(),
				this.formCommandByte(GPIOCMD_PIN_DIRECTION,
						this.vectorJ21.getJ21PinIndex(j21pin)));
		
		return pinDirection;
	}
	
	/*
	 * This method forms slave write address
	 */
	private byte formSlaveWriteAddress() {
		
		return (byte)(2 * TCA8418_ADDRESS);
	}
	
	/*
	 * This method forms slave read address
	 */
	private byte formSlaveReadAddress() {
		
		return (byte)(2 * TCA8418_ADDRESS + 1);
	}
	
	/*
	 * This method forms SmBus command byte based on supplied command 
	 * type (pin status, pin output, and pin direction) and J21 pin index
	 */
	private byte formCommandByte (int cmdType, int j21PinInd) throws Exception {
		
		byte cmd = 0;
		switch (cmdType) {
		case GPIOCMD_PIN_STATUS:
			cmd =  (byte)(0x14 + j21PinInd/8);
			break;
		case GPIOCMD_PIN_OUT:
			cmd =  (byte)(0x17 + j21PinInd/8);
			break;
		case GPIOCMD_PIN_DIRECTION:
			cmd =  (byte)(0x23 + j21PinInd/8);
			break;
		default:
//			throw new J21GpioException ("Invalid GPIO PIN command");
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Invalid GPIO PIN command");
		}
		
		return cmd;
	}
}
