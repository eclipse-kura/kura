/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.driver.ble.xdk;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Xdk {
	
	private static final Logger logger = LoggerFactory.getLogger(Xdk.class);
	
	private static final String DEVINFO = "devinfo";
	private static final String SENSOR = "sensor";
	private static final String HIGH_PRIORITY_ARRAY = "high priority array";
	private static final String LOW_PRIORITY_ARRAY = "low priority array";
	
	
	private static final String ACCELERATION_X = "acceletetion x";
	private static final String ACCELERATION_Y = "acceletetion y";
	private static final String ACCELERATION_Z = "acceletetion z";
	private static final String GYROSCOPE_X = "gyroscope x";
	private static final String GYROSCOPE_Y = "gyroscope y";
	private static final String GYROSCOPE_Z = "gyroscope z";
	private static final String LIGHT = "light";
	private static final String NOISE = "noise";
	private static final String PRESSURE = "pressure";
	private static final String TEMPERATURE = "temperature";
    private static final String HUMIDITY = "humidity";
    private static final String SD_CARD_DETECT_STATUS= "sd card detect status";
    private static final String BUTTON_STATUS = "button statud";
    private static final String MAGNETIC_X = "magnetic x";
    private static final String MAGNETIC_Y = "magnetic x";
    private static final String MAGNETIC_Z = "magnetic x";
    private static final String MAGNETOMETER_RESISTANCE = "magnetometer resistence";
    private static final String LED_STATUS = "led statud";
    private static final String VOLTAGE_LEM = "voltage lem";
    private static final String OPTO = "opto";
    private static final String KEYS = "keys";
    private static final String IO = "io";
	
	private static final int SERVICE_TIMEOUT = 1000000;
	private static final byte m1 = 0x01;
	private static final byte m2 = 0x02;
	
	private BluetoothLeDevice device;
	private Map<String, XdkGattResources> gattResources;
	
	public Xdk(BluetoothLeDevice bluetoothLeDevice) {
		this.device = bluetoothLeDevice;
        this.gattResources = new HashMap<>();
	}
	
    public BluetoothLeDevice getBluetoothLeDevice() {
        return this.device;
    }

    public void setBluetoothLeDevice(BluetoothLeDevice device) {
        this.device = device;
    }

	public boolean isConnected() {
		return this.device.isConnected();
	}

	public void connect() throws ConnectionException{
		try {
            this.device.connect();
            // Wait a bit to ensure that the device is really connected and services discovered
            Long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < SERVICE_TIMEOUT) {
                if (this.device.isServicesResolved()) {
                    break;
                }
                XdkDriver.waitFor(1000);
            }
            if (!isConnected() || !this.device.isServicesResolved()) {
                throw new ConnectionException("Connection failed");
            }
        } catch (KuraBluetoothConnectionException e) {
            throw new ConnectionException(e);
        }
		
	}

	public void init() throws ConnectionException {
        if (isConnected() && this.gattResources.size() != 8) {
            getGattResources();
        }
	}


	public void disconnect() throws ConnectionException {
		if (isHighNotifying()) {
			disableHighNotifications();
		}
        if (isLowNotifying()) {
            disableLowNotifications();
        }
        try {
            this.device.disconnect();
            if (isConnected()) {
                throw new ConnectionException("Disconnection failed");
            }
        } catch (KuraBluetoothConnectionException e) {
            throw new ConnectionException(e);
        }
        // Wait a while after disconnection
        XdkDriver.waitFor(1000);
    }
	
	public String getFirmareRevision() {
        String firmware = "";
        try {
            BluetoothLeGattCharacteristic devinfo = this.gattResources.get(DEVINFO).getGattService()
                    .findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_GET_FIRMWARE_VERSION);
            firmware = new String(devinfo.readValue(), "UTF-8");
        } catch (KuraException | UnsupportedEncodingException e) {
            logger.error("Firmware revision read failed", e);
        }
        return firmware;
    }
	
    /*
     * Discover services
     */
    public Map<String, BluetoothLeGattService> discoverServices() {
        Map<String, BluetoothLeGattService> services = new HashMap<>();
        for (XdkGattResources resources : this.gattResources.values()) {
            services.put(resources.getName(), resources.getGattService());
        }
        return services;
    }

    public List<BluetoothLeGattCharacteristic> getCharacteristics() {
        List<BluetoothLeGattCharacteristic> characteristics = new ArrayList<>();
        for (Entry<String, XdkGattResources> entry : this.gattResources.entrySet()) {
            try {
                characteristics.addAll(entry.getValue().getGattService().findCharacteristics());
            } catch (KuraException e) {
                logger.error("Failed to get characteristic", e);
            }
        }
        return characteristics;
    }
    
    //-------------------------------------------------------------------------------------
    //
    // Sensor
    //
    //-------------------------------------------------------------------------------------
    /*
     * Enable Sensor
     */
	public void startSensor() { 
		// Write "00" to enable sensor
        byte[] value = { 0x00 };
        try {
            BluetoothLeGattService x = this.gattResources.get(SENSOR).getGattService();
                 
            BluetoothLeGattCharacteristic y = x.findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION);
            y.writeValue(value);
        } catch (KuraException e) {
            logger.error("Sensor start failed", e);
        }
    }
	
	/*
     * Disable sensor ????????????????????????????????????????????????????????????????????????
     */
	
	
	/*
     * Read High Data sensor
     */
	public Object[] readHighData() {
		Object[] hightData = new Object[6];
		try {
            hightData = calculateHighData(
                    this.gattResources.get(HIGH_PRIORITY_ARRAY).getGattValueCharacteristic().readValue());
        } catch (KuraException e) {
            logger.error("High Data read failed", e);
        }
        return hightData;
	}
	/*
     * Read Low Data sensor
     */
    public Object[] readLowData(byte ID) {
		
		Object[] lowData = new Object[7];
		byte[] data;
		
		try {
			data = this.gattResources.get(LOW_PRIORITY_ARRAY).getGattValueCharacteristic().readValue();
			while (data[0] != ID) {
			   data = this.gattResources.get(LOW_PRIORITY_ARRAY).getGattValueCharacteristic().readValue();
			}
			
            lowData = calculateLowData(data, ID);
        } catch (KuraException e) {
            logger.error("Low Data read failed", e);
        }
        return lowData;
	}
    
    /*
     * Enable High Notifications
     */
    public void enableHighTWONotifications(Consumer<int[]> callback) {
    	Consumer<byte[]> callbackHigh = new Consumer<byte[]>() {
			@Override
			public void accept(byte[] t) {
				callback.accept(calculateHighTWOData(t));
			}
    	};
    	try {
        	this.gattResources.get(HIGH_PRIORITY_ARRAY).getGattService().findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY).enableValueNotifications(callbackHigh);
    	} catch (KuraException e) {
            logger.error("Notification enable failed", e);
        }		
    }
    public void enableHighNotifications(Consumer<double[]> callback, int index) {
        try {
        	this.gattResources.get(HIGH_PRIORITY_ARRAY).getGattService().findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY).enableValueNotifications(new Consumer<byte[]>() {
        		
				@Override
				public void accept(byte[] t) {
					
					switch(index) {
					case 0: 
						int ax = (int) calculateHighData(t)[index]; 
						logger.info("Accelerometer X-Axis received notification: {}", ax);
						break;
					case 1:
						int ay = (int) calculateHighData(t)[index]; 
						logger.info("Accelerometer Y-Axis received notification: {}", ay);
						break;
					case 2:
						int az = (int) calculateHighData(t)[index]; 
						logger.info("Accelerometer Z-Axis received notification: {}", az);
						break;
					case 3:
						int gx = (int) calculateHighData(t)[index]; 
						logger.info("Gyro X-Axis received notification: {}", gx);
						break;
					case 4:
						int gy = (int) calculateHighData(t)[index]; 
						logger.info("Gyro Y-Axis received notification: {}", gy);
						break;
					case 5:
						int gz = (int) calculateHighData(t)[index]; 
						logger.info("Gyro Z-Axis received notification: {}", gz);
						break;
				     default:
					}
					//logger.info("received notification: {}", calculateHighData(t));
				}
			});
        } catch (KuraException e) {
            logger.error("Notification enable failed", e);
        }
    }
    
    public void enableOneLowNotifications(Consumer<double[]> callback, int index) {
        try {
        	this.gattResources.get(LOW_PRIORITY_ARRAY).getGattService().findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY).enableValueNotifications(new Consumer<byte[]>() {
				
				@Override
				public void accept(byte[] t) {
					
					if(t[0] == m1) {
						switch(index) {
						case 0: 
							Integer lux = (Integer) calculateLowData(t, m1)[index]; 
							logger.info("Light received notification: {}", lux);
							break;
						case 1:
							Integer noise = (Integer) calculateLowData(t, m1)[index]; 
							logger.info("Noise received notification: {}", noise);
							break;
						case 2:
							Integer pres = (Integer) calculateLowData(t, m1)[index]; 
							logger.info("Pressure received notification: {}", pres);
							break;
						case 3:
							double temp = (double) calculateLowData(t, m1)[index]; 
							logger.info("Temperature received notification: {}", temp);
							break;
						case 4:
							Integer hum = (Integer) calculateLowData(t, m1)[index]; 
							logger.info("Humidity received notification: {}", hum);
							break;
						case 5:
							byte sd = (byte) calculateLowData(t, m1)[index]; 
							logger.info("SD-Card Detection Status received notification: {}", sd);
							break;
						case 6:	
							byte button = (byte) calculateLowData(t, m1)[index]; 
							logger.info("Button Status received notification: {}", button);
							break;
							default:
						}
					} 
				}
			});
        } catch (KuraException e) {
            logger.error("notification enable failed", e);
        }
    }
    
    public void enableTwoLowNotifications(Consumer<double[]> callback, int index) {
        try {
        	this.gattResources.get(LOW_PRIORITY_ARRAY).getGattService().findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY).enableValueNotifications(new Consumer<byte[]>() {
				
				@Override
				public void accept(byte[] t) {
					
					if(t[0] == m2) {
							switch(index) {
							case 0: 
								int mx = (int) calculateLowData(t, m2)[index]; 
								logger.info("Maghetometer X-Axis received notification: {}", mx);
								break;
							case 1:
								int my = (int) calculateLowData(t, m2)[index]; 
								logger.info("Maghetometer Y-Axis received notification: {}", my);
								break;
							case 2:
								int mz = (int) calculateLowData(t, m2)[index]; 
								logger.info("Maghetometer Z-Axis received notification: {}", mz);
								break;
							case 3:
								int mr = (int) calculateLowData(t, m2)[index]; 
								logger.info("Maghetometer Resistence received notification: {}", mr);
								break;
							case 4:
								byte led = (byte) calculateLowData(t, m2)[index]; 
								logger.info("Led Status received notification: {}", led);
								break;
							case 5:
								double volt = (double) calculateLowData(t, m2)[index]; 
								logger.info("RMS voltage of LEM sensor received notification: {}", volt);
								break;
							case 6:	
								logger.info("received notification: PN");
								break;
						     default:
							}
					} 
				}
			});
        } catch (KuraException e) {
            logger.error("Notification enable failed", e);
        }
    }

    /*
     * Disable temperature notifications
     */
    
    public void disableHighNotifications() {
        try {
            this.gattResources.get(HIGH_PRIORITY_ARRAY).getGattService().findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY).disableValueNotifications();
        } catch (KuraException e) {
            logger.error("Notification disable failed", e);
        }
    }
    
    public void disableLowNotifications() {
        try {
            this.gattResources.get(LOW_PRIORITY_ARRAY).getGattService().findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY).disableValueNotifications();
        } catch (KuraException e) {
            logger.error("Notification disable failed", e);
        }
    }
    
    /*
     * convert in byte
     */

    /*
     * Set sampling period
     */
    public void setSamplingRate(int rate) {
        byte[] b = ByteBuffer.allocate(4).putInt(rate).array();
        byte[] rateBytes = new byte[4];
        rateBytes[0] = b[3];
        rateBytes[1] = b[2];
        rateBytes[2] = b[1];
        rateBytes[3] = b[1];
        try {
            this.gattResources.get(SENSOR).getGattService()
                    .findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_CHANGE_SENSOR_SAMPLING_RATA).writeValue(rateBytes);
        } catch (KuraException e) {
            logger.error("Measurement period set failed", e);
        }
	}


    public boolean isHighNotifying() {
        return isNotifying(HIGH_PRIORITY_ARRAY); 
    }
    
    public boolean isLowNotifying() {
        return isNotifying(LOW_PRIORITY_ARRAY); 
    }
    
    /*
     * Calculate High Data
     */
	private Object[] calculateHighData(byte[] valueByte) {
		
		logger.info("Received High Data value: {}", byteArrayToHexString(valueByte));
		
		Object[] highData = new Object[6];
		
		int Ax = shortSignedAtOffset(valueByte, 0);
		int Ay = shortSignedAtOffset(valueByte, 2); 
		int Az = shortSignedAtOffset(valueByte, 4);
		
		int Gx = shortSignedAtOffset(valueByte, 6);
		int Gy = shortSignedAtOffset(valueByte, 8);
		int Gz = shortSignedAtOffset(valueByte, 10);
	    
		highData[0] = Ax;
		highData[1] = Ay;
		highData[2] = Az;
		highData[3] = Gx;
		highData[4] = Gy;
		highData[5] = Gz;
		
		return highData;
	}
	
	/*
     * Calculate High Data
     */
	private int[] calculateHighTWOData(byte[] valueByte) {
		
		logger.info("Received High Data value: {}", byteArrayToHexString(valueByte));
		
		int[] highData = new int[6];
		
		int Ax = shortSignedAtOffset(valueByte, 0);
		int Ay = shortSignedAtOffset(valueByte, 2); 
		int Az = shortSignedAtOffset(valueByte, 4);
		
		int Gx = shortSignedAtOffset(valueByte, 6);
		int Gy = shortSignedAtOffset(valueByte, 8);
		int Gz = shortSignedAtOffset(valueByte, 10);
	    
		highData[0] = Ax;
		highData[1] = Ay;
		highData[2] = Az;
		highData[3] = Gx;
		highData[4] = Gy;
		highData[5] = Gz;
		
		return highData;
	}
	
	/*
     * Calculate Low Data
     */
	
	 private Object[] calculateLowData(byte[] valueByte, byte ID) {
		
		 logger.info("Received Low Data value: {}", byteArrayToHexString(valueByte));
			
		 Object[] LowData = new Object[7];
		 
		 if(ID == 0x01) {
			 
			 Integer lux = thirtyTwoBitUnsignedAtOffset(valueByte, 1)/1000;
			 Integer noise = eightBitUnsignedAtOffset(valueByte, 5);
			 Integer pressure = thirtyTwoBitUnsignedAtOffset(valueByte, 6);
			 double temperature = thirtyTwoBitShortSignedAtOffset(valueByte, 10)/1000;
			 Integer humidity = thirtyTwoBitUnsignedAtOffset(valueByte, 14);
			 byte sd_card = valueByte[18];
			 byte button = valueByte[19];
			 
			 LowData[0] = lux;
			 LowData[1] = noise;
			 LowData[2] = pressure;
			 LowData[3] = temperature;
		     LowData[4] = humidity;
			 LowData[5] = sd_card;
			 LowData[6] = button;
			 
			 
		 } else {
			 
			 int Mx = shortSignedAtOffset(valueByte, 1);
			 int My = shortSignedAtOffset(valueByte, 3);
			 int Mz = shortSignedAtOffset(valueByte, 5);
			 int M_res = shortSignedAtOffset(valueByte, 7);
			 byte led = valueByte[9];
			 double voltage = shortSignedAtOffset(valueByte, 10)/1000;
			 byte none = 0x00;
			 
			 LowData[0] = Mx;
			 LowData[1] = My;
			 LowData[2] = Mz;
			 LowData[3] = M_res;
		     LowData[4] = led;
			 LowData[5] = voltage;
			 LowData[6] = none;
			 
		 }
		 
		 return LowData;
		 
	}

	// ---------------------------------------------------------------------------------------------
    //
    // Auxiliary methods
    //
    // ---------------------------------------------------------------------------------------------

    private String byteArrayToHexString(byte[] value) {
        final char[] hexadecimals = "0123456789ABCDEF".toCharArray();
        char[] hexValue = new char[value.length * 2];
        for (int j = 0; j < value.length; j++) {
            int v = value[j] & 0xFF;
            hexValue[j * 2] = hexadecimals[v >>> 4];
            hexValue[j * 2 + 1] = hexadecimals[v & 0x0F];
        }
        return new String(hexValue);
    }

	private int shortSignedAtOffset(byte[] c, int offset) {
		Integer lowerByte = c[offset] & 0xFF;
        Integer upperByte = (int) c[offset + 1];
        return (upperByte << 8) + lowerByte;
	}
	
	private int thirtyTwoBitShortSignedAtOffset(byte[] c, int offset) {
		Integer lowerByte = c[offset] & 0xFF;
		Integer mediumByteA = c[offset + 1] & 0xFF;
		Integer mediumByteB = c[offset + 2] & 0xFF;
		Integer upperByte = (int)c[offset + 3];
		return (upperByte << 32) + (mediumByteB << 16) + (mediumByteA << 8) + lowerByte;
	}
	
	private Integer eightBitUnsignedAtOffset(byte[] c, int offset) {
		Integer lowerByte = c[offset] & 0xFF;
		return lowerByte;
	}
	
	private Integer thirtyTwoBitUnsignedAtOffset(byte[] c, int offset) {
		Integer lowerByte = c[offset] & 0xFF;
		Integer mediumByteA = c[offset + 1] & 0xFF;
		Integer mediumByteB = c[offset + 2] & 0xFF;
		Integer upperByte = c[offset + 3] & 0xFF;
		return (upperByte << 32) + (mediumByteB << 16) + (mediumByteA << 8) + lowerByte;
	}
	
	private void getGattResources() throws ConnectionException {
        try {
        	    BluetoothLeGattService ControlService = this.device.findService(XdkGatt.UUID_XDK_CONTROL_SERVICE);
        	    BluetoothLeGattService DataService = this.device.findService(XdkGatt.UUID_XDK_HIGH_DATA_RATE);
        	   
        	    //BluetoothLeGattCharacteristic SensorCharacteristic = ControlService.findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION);
        	    BluetoothLeGattCharacteristic SensorCharacteristic = ControlService.findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_CHANGE_SENSOR_SAMPLING_RATA);
        	    //
        	    BluetoothLeGattCharacteristic HighDataCharacteristic = DataService.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY);
        	    BluetoothLeGattCharacteristic LowDataCharacteristic = DataService.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY);
        	    
        	    //XdkGattResources SensorGattResources =  new XdkGattResources(SENSOR, ControlService, null);
        	    XdkGattResources SensorGattResources =  new XdkGattResources(SENSOR, ControlService, SensorCharacteristic);
        	    //
        	    XdkGattResources HighDataGattResources = new XdkGattResources(HIGH_PRIORITY_ARRAY, DataService, HighDataCharacteristic);
        	    XdkGattResources LowDataGattResources = new XdkGattResources(LOW_PRIORITY_ARRAY, DataService,LowDataCharacteristic);
        	    
                gattResources.put(SENSOR, SensorGattResources);
                
                gattResources.put(HIGH_PRIORITY_ARRAY, HighDataGattResources);
                
                gattResources.put(LOW_PRIORITY_ARRAY, LowDataGattResources);

            
        } catch (KuraBluetoothResourceNotFoundException e) {
            logger.error("Failed to get GATT service", e);
            disconnect();
        }
    }
	
	 
	 private boolean isNotifying(String resourceName) {
	        XdkGattResources resource = this.gattResources.get(resourceName);
	        if (resource != null) {
	            return resource.getGattValueCharacteristic().isNotifying();
	        } else {
	            return false;
	        }
	    }

}
