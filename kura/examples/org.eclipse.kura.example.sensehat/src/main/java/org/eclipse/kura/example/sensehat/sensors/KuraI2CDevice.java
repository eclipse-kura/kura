package org.eclipse.kura.example.sensehat.sensors;

import java.io.IOException;
import java.nio.ByteBuffer;

import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

public class KuraI2CDevice {

	private I2CDevice m_device;
	
	public KuraI2CDevice(int controllerNumber, int address, int addressSize, int clockFrequency) throws IOException {
		
		I2CDeviceConfig config = new I2CDeviceConfig(controllerNumber, address, addressSize, clockFrequency);
		try {
			m_device = (I2CDevice)DeviceManager.open(I2CDevice.class, config);
		} catch(Exception ex) {
			throw new IOException(ex);
		}
		
	}
	
	public int read() throws IOException {
		return m_device.read();
	}

	public void write(int value) throws IOException {
		m_device.write(value);
	}

	public void write(int register, int size, ByteBuffer value) throws IOException {
		m_device.write(register, size, value);
	}
	
	public void beginTransaction() throws IOException {
		m_device.begin();
	}

	public void endTransaction() throws IOException {
		m_device.end();
	}

	public void close() throws IOException {
		m_device.close();
	}

	public boolean isOpen() {
		return m_device.isOpen();
	}

}
