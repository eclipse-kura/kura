package org.eclipse.kura.emulator.gpio;

import java.io.IOException;

import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.gpio.PinStatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmulatedPin implements KuraGPIOPin {

	private static final Logger s_logger = LoggerFactory.getLogger(EmulatedPin.class);
	
	private boolean internalValue = false;
	String pinName = null;
	int pinIndex = -1;
	
	private KuraGPIODirection direction = KuraGPIODirection.OUTPUT;
	private KuraGPIOMode mode = KuraGPIOMode.OUTPUT_OPEN_DRAIN;
	private KuraGPIOTrigger trigger = KuraGPIOTrigger.NONE;
	
	public EmulatedPin(String pinName) {
		super();
		this.pinName = pinName;
	}

	public EmulatedPin(int pinIndex) {
		super();
		this.pinIndex = pinIndex;
	}
	
	public EmulatedPin(String pinName, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger) {
		super();
		this.pinName = pinName;
		this.direction = direction;
		this.mode = mode;
		this.trigger = trigger;
	}

	public EmulatedPin(int pinIndex, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger) {
		super();
		this.pinIndex = pinIndex;
		this.direction = direction;
		this.mode = mode;
		this.trigger = trigger;
	}

	@Override
	public void setValue(boolean active) throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException {
		internalValue = active;
		
		s_logger.debug("Emulated GPIO Pin {} changed to {}", pinName != null ? pinName : pinIndex, active == true ? "on" : "off");
	}

	@Override
	public boolean getValue() throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException {
		return internalValue;
	}

	@Override
	public void addPinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException {
	}

	@Override
	public void removePinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException {
	}

	@Override
	public void open() throws KuraGPIODeviceException, KuraUnavailableDeviceException, IOException {
		s_logger.info("Emulated GPIO Pin {} open.", pinName != null ? pinName : pinIndex);
	}

	@Override
	public void close() throws IOException {
		s_logger.info("Emulated GPIO Pin {} closed.", pinName != null ? pinName : pinIndex);
	}

	@Override
	public String toString() {
		return pinName != null ? "GPIO Pin: "+ pinName : "Gpio PIN #" + String.valueOf(pinIndex);
	}

	@Override
	public KuraGPIODirection getDirection() {
		return direction;
	}

	@Override
	public KuraGPIOMode getMode() {
		return mode;
	}

	@Override
	public KuraGPIOTrigger getTrigger() {
		return trigger;
	}

	@Override
	public String getName() {
		return pinName != null ? pinName : String.valueOf(pinIndex);
	}

	@Override
	public int getIndex() {
		return pinIndex;
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	
}
