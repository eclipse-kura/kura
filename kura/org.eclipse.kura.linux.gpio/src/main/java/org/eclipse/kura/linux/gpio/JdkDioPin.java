package org.eclipse.kura.linux.gpio;

import java.io.IOException;

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

import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.gpio.PinStatusListener;

public class JdkDioPin implements KuraGPIOPin {

	private GPIOPin thePin;
	private int pinIndex;
	private String pinName = null;

	private KuraGPIODirection direction = null;
	private KuraGPIOMode mode = null;
	private KuraGPIOTrigger trigger = null;

	PinStatusListener localListener;

	public JdkDioPin(int pinIndex) {
		super();
		this.pinIndex = pinIndex;
	}

	public JdkDioPin(String pinName) {
		super();
		this.pinName = pinName;
	}

	public JdkDioPin(int pinIndex, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger) {
		super();
		this.pinIndex = pinIndex;
		this.direction = direction;
		this.mode = mode;
		this.trigger = trigger;
	}

	public JdkDioPin(String pinName, KuraGPIODirection direction, KuraGPIOMode mode, KuraGPIOTrigger trigger) {
		super();
		this.pinName = pinName;
		this.direction = direction;
		this.mode = mode;
		this.trigger = trigger;
	}

	@Override
	public void changeValue(boolean active) throws KuraClosedDeviceException, KuraUnavailableDeviceException, IOException {
		try {
			thePin.setValue(active);
		} catch (UnavailableDeviceException ex) {
			throw new KuraUnavailableDeviceException(ex, pinName != null ? pinName : pinIndex);
		} catch (ClosedDeviceException ex) {
			throw new KuraClosedDeviceException(ex, pinName != null ? pinName : pinIndex);
		}
	}

	@Override
	public boolean getValue() throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException {
		try {
			return thePin.getValue();
		} catch (UnavailableDeviceException e) {
			throw new KuraUnavailableDeviceException(e, pinName != null ? pinName : pinIndex);
		} catch (ClosedDeviceException e) {
			throw new KuraClosedDeviceException(e, pinName != null ? pinName : pinIndex);
		}
	}

	@Override
	public void addPinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException {
		localListener = listener;
		try {
			thePin.setInputListener(privateListener);
		} catch (ClosedDeviceException e) {
			throw new KuraClosedDeviceException(e, pinName != null ? pinName : pinIndex);
		}
	}

	@Override
	public void removePinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException {
		localListener = null;

		try {
			thePin.setInputListener(null);
		} catch (ClosedDeviceException e) {
			throw new KuraClosedDeviceException(e, pinName != null ? pinName : pinIndex);
		}
	}

	@Override
	public void open() throws KuraGPIODeviceException, KuraUnavailableDeviceException, IOException {
		if(direction != null){
			GPIOPinConfig config = new GPIOPinConfig(
					DeviceConfig.DEFAULT,						
					getPinIndex(), 
					getDirectionInternal(), 
					getModeInternal(), 
					getTriggerInternal(), 
					false);
			try {
				thePin = DeviceManager.open(GPIOPin.class, config);
			} catch (InvalidDeviceConfigException e) {
				throw new KuraGPIODeviceException(e, getPinIndex());
			} catch (UnsupportedDeviceTypeException e) {
				throw new KuraGPIODeviceException(e, getPinIndex());
			} catch (DeviceNotFoundException e) {
				throw new KuraGPIODeviceException(e, getPinIndex());
			} catch (UnavailableDeviceException e) {
				throw new KuraUnavailableDeviceException(e, getPinIndex());
			} 
		}else{
			try{
				thePin = DeviceManager.open(getPinIndex());
			} catch (InvalidDeviceConfigException e) {
				throw new KuraGPIODeviceException(e, getPinIndex());
			} catch (UnsupportedDeviceTypeException e) {
				throw new KuraGPIODeviceException(e, getPinIndex());
			} catch (DeviceNotFoundException e) {
				throw new KuraGPIODeviceException(e, getPinIndex());
			} catch (UnavailableDeviceException e) {
				throw new KuraUnavailableDeviceException(e, getPinIndex());
			} 
		}
	}

	@Override
	public void close() throws IOException {
		if (localListener != null) {
			try {
				removePinStatusListener(localListener);
			} catch (Exception ex) {
				// Do nothing
			}
		}
		thePin.close();
	}

	private PinListener privateListener = new PinListener() {

		@Override
		public void valueChanged(PinEvent pinEvent) {

			if (localListener != null) {
				localListener.pinStatusChange(pinEvent.getValue());
			}
		}
	};

	private int getPinIndex() {
		if(pinName != null){
			return -1;
		}else{
			return pinIndex;
		}
	}

	private int getDirectionInternal() {
		switch (direction) {
		case INPUT:
			return GPIOPinConfig.DIR_INPUT_ONLY;
		case OUTPUT:
			return GPIOPinConfig.DIR_OUTPUT_ONLY;
		}
		return -1;
	}

	private int getModeInternal() {
		switch (mode) {
		case INPUT_PULL_DOWN:
			return GPIOPinConfig.MODE_INPUT_PULL_DOWN;
		case INPUT_PULL_UP:
			return GPIOPinConfig.MODE_INPUT_PULL_UP;
		case OUTPUT_OPEN_DRAIN:
			return GPIOPinConfig.MODE_OUTPUT_OPEN_DRAIN;
		case OUTPUT_PUSH_PULL:
			return GPIOPinConfig.MODE_OUTPUT_PUSH_PULL;
		}
		return -1;
	}

	private int getTriggerInternal() {
		switch (trigger) {
		case BOTH_EDGES:
			return GPIOPinConfig.TRIGGER_BOTH_EDGES;
		case BOTH_LEVELS:
			return GPIOPinConfig.TRIGGER_BOTH_LEVELS;
		case FALLING_EDGE:
			return GPIOPinConfig.TRIGGER_FALLING_EDGE;
		case HIGH_LEVEL:
			return GPIOPinConfig.TRIGGER_HIGH_LEVEL;
		case LOW_LEVEL:
			return GPIOPinConfig.TRIGGER_LOW_LEVEL;
		case NONE:
			return GPIOPinConfig.TRIGGER_NONE;
		case RAISING_EDGE:
			return GPIOPinConfig.TRIGGER_RISING_EDGE;
		}
		return -1;
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
		return thePin != null ? thePin.isOpen() : false;
	}
}
