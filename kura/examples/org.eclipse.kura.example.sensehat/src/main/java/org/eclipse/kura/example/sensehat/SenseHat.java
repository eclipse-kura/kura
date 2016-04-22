package org.eclipse.kura.example.sensehat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import jdk.dio.DeviceManager;
import jdk.dio.DeviceNotFoundException;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.UnsupportedDeviceTypeException;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.example.sensehat.joystick.Joystick;
import org.eclipse.kura.example.sensehat.joystick.JoystickEvent;
import org.eclipse.kura.example.sensehat.ledmatrix.Colors;
import org.eclipse.kura.example.sensehat.ledmatrix.FrameBuffer;
import org.eclipse.kura.example.sensehat.sensors.HTS221;
import org.eclipse.kura.example.sensehat.sensors.LPS25H;
import org.eclipse.kura.example.sensehat.sensors.LSM9DS1;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenseHat implements ConfigurableComponent, CloudClientListener {

	private static final Logger s_logger = LoggerFactory.getLogger(SenseHat.class);

	// Cloud Application identifier
	private static final String APP_ID = "SenseHat";

	private ComponentContext m_ctx;

	private static final int I2C_BUS = 1;
	private static final int I2C_ADDRESS_SIZE = 7;
	private static final int I2C_FREQUENCY = 400000;
	private static final int I2C_ACC_ADDRESS = 0x6A;
	private static final int I2C_MAG_ADDRESS = 0x1C;
	private static final int I2C_PRE_ADDRESS = 0x5C;
	private static final int I2C_HUM_ADDRESS = 0x5F;

	private static final String IMU_ACC_ENABLE = "imu.accelerometer.enable";
	private static final String IMU_GYRO_ENABLE = "imu.gyroscope.enable";
	private static final String IMU_COMP_ENABLE = "imu.compass.enable";
	private static final String PRE_ENABLE = "pressure.enable";
	private static final String HUM_ENABLE = "humidity.enable";
	private static final String LCD_ENABLE = "screen.enable";
	private static final String STICK_ENABLE = "stick.enable";
	private static final String SCREEN_MESSAGE = "screen.message";
	private static final String SCREEN_ROTATION = "screen.rotation";

	private boolean             m_imuAccEnable = false;
	private boolean             m_imuGyroEnable = false;
	private boolean             m_imuCompEnable = false;
	private boolean             m_preEnable = false;
	private boolean             m_humEnable = false;
	private boolean             m_lcdEnable = false;
	private boolean             m_stickEnable = false;
	private String              m_screenMessage = "";
	private int                 m_screenRotation = 0;

	private Joystick SenseHatJoystick;
	private JoystickEvent je;
	private boolean runThread;

	private FrameBuffer frameBuffer;

	private ScheduledExecutorService m_worker;
	private Future<?>           m_handle;

	private CloudService        m_cloudService;
	private CloudClient      	m_cloudClient;

	private LSM9DS1             m_imuSensor;          // Inertial Measurement Unit (Accelerometer, Gyroscope, Magnetometer)
	private LPS25H              m_pressureSensor;     // Atmospheric Pressure
	private HTS221              m_humiditySensor;     // Humidity
	private Map<String, Object> m_properties;

	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public SenseHat() 
	{
		super();
	}

	public void setCloudService(CloudService cloudService) {
		m_cloudService = cloudService;
	}

	public void unsetCloudService(CloudService cloudService) {
		m_cloudService = null;
	}

	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("Activating Sense Hat Application...");

		m_ctx = componentContext;

		m_properties = properties;
		if (m_properties.get(IMU_ACC_ENABLE) != null) 
			m_imuAccEnable = (Boolean) m_properties.get(IMU_ACC_ENABLE);
		if (m_properties.get(IMU_GYRO_ENABLE) != null) 
			m_imuGyroEnable = (Boolean) m_properties.get(IMU_GYRO_ENABLE);
		if (m_properties.get(IMU_COMP_ENABLE) != null) 
			m_imuCompEnable = (Boolean) m_properties.get(IMU_COMP_ENABLE);
		if (m_properties.get(PRE_ENABLE) != null)
			m_preEnable = (Boolean) m_properties.get(PRE_ENABLE);
		if (m_properties.get(HUM_ENABLE) != null)
			m_humEnable = (Boolean) m_properties.get(HUM_ENABLE);
		if (m_properties.get(LCD_ENABLE) != null)
			m_lcdEnable = (Boolean) m_properties.get(LCD_ENABLE);
		if (m_properties.get(STICK_ENABLE) != null)
			m_stickEnable = (Boolean) m_properties.get(STICK_ENABLE);
		if (m_properties.get(SCREEN_MESSAGE) != null)
			m_screenMessage = (String) m_properties.get(SCREEN_MESSAGE);		
		if (m_properties.get(SCREEN_ROTATION) != null)
			m_screenRotation = (Integer) m_properties.get(SCREEN_ROTATION);		

		//		// get the mqtt client for this application
		//		try  {
		//
		//			// Acquire a Cloud Application Client for this Application 
		//			s_logger.info("Getting CloudClient for {}...", APP_ID);
		//			m_cloudClient = m_cloudService.newCloudClient(APP_ID);
		//			m_cloudClient.addCloudClientListener(this);
		//
		//			// Don't subscribe because these are handled by the default 
		//			// subscriptions and we don't want to get messages twice			
		////			doUpdate(false);
		//		}
		//		catch (Exception e) {
		//			s_logger.error("Error during component activation", e);
		//			throw new ComponentException(e);
		//		}

		runThread = false;
		update();
		s_logger.info("Activating Sense Hat Application... Done.");
	}


	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.debug("Deactivating Sense Hat Application...");

		LPS25H.closeDevice();
		HTS221.closeDevice();
		LSM9DS1.closeDevice();

		if (m_handle != null)
			m_handle.cancel(true);
		if (m_worker != null)
			m_worker.shutdown();
		if (SenseHatJoystick != null)
			SenseHatJoystick.closeJoystick();


		if (frameBuffer != null) {
			frameBuffer.clearFrameBuffer();
			frameBuffer.closeFrameBuffer();
			frameBuffer = null;
		}

		// shutting down the worker and cleaning up the properties
		//		m_worker.shutdown();

		// Releasing the CloudApplicationClient
		s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
		//		m_cloudClient.release();

		s_logger.debug("Deactivating Sense Hat Application... Done.");
	}	


	public void updated(Map<String,Object> properties)
	{
		s_logger.info("Updated Sense Hat Application...");

		// store the properties received
		m_properties = properties;
		if (m_properties.get(IMU_ACC_ENABLE) != null) 
			m_imuAccEnable = (Boolean) m_properties.get(IMU_ACC_ENABLE);
		if (m_properties.get(IMU_GYRO_ENABLE) != null) 
			m_imuGyroEnable = (Boolean) m_properties.get(IMU_GYRO_ENABLE);
		if (m_properties.get(IMU_COMP_ENABLE) != null) 
			m_imuCompEnable = (Boolean) m_properties.get(IMU_COMP_ENABLE);
		if (m_properties.get(PRE_ENABLE) != null)
			m_preEnable = (Boolean) m_properties.get(PRE_ENABLE);
		if (m_properties.get(HUM_ENABLE) != null)
			m_humEnable = (Boolean) m_properties.get(HUM_ENABLE);
		if (m_properties.get(LCD_ENABLE) != null)
			m_lcdEnable = (Boolean) m_properties.get(LCD_ENABLE);
		if (m_properties.get(STICK_ENABLE) != null)
			m_stickEnable = (Boolean) m_properties.get(STICK_ENABLE);
		if (m_properties.get(SCREEN_MESSAGE) != null)
			m_screenMessage = (String) m_properties.get(SCREEN_MESSAGE);	
		if (m_properties.get(SCREEN_ROTATION) != null)
			m_screenRotation = (Integer) m_properties.get(SCREEN_ROTATION);		

		update();
		s_logger.info("Updated Sense Hat Application... Done.");
	}

	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------

	private void update() {
		if (m_imuAccEnable || m_imuGyroEnable || m_imuCompEnable) {

			m_imuSensor = LSM9DS1.getIMUSensor(I2C_BUS, I2C_ACC_ADDRESS, I2C_MAG_ADDRESS, I2C_ADDRESS_SIZE, I2C_FREQUENCY);
			boolean status = m_imuSensor.initDevice(m_imuAccEnable, m_imuGyroEnable, m_imuCompEnable);
			if (!status)
				s_logger.error("Unable to initialize IMU sensor.");
			else {
				if (m_imuAccEnable) {
					float[] acc = new float[3];
					for (int i = 0; i < 20; i++) {
						acc = m_imuSensor.getAccelerometerRaw();
					}
					s_logger.info("Acceleration X : " + acc[0] + " Y : " + acc[1] + " Z : " + acc[2]);
				}
				if (m_imuGyroEnable) {
					float[] gyro = new float[3];
					for (int i = 0; i < 20; i++) {
						gyro = m_imuSensor.getGyroscopeRaw();
					}
					s_logger.info("Orientation X : " + gyro[0] + " Y : " + gyro[1] + " Z : " + gyro[2]);
				}
				if (m_imuCompEnable) {
					float[] comp = new float[3];
					for (int i = 0; i < 20; i++) {
						comp = m_imuSensor.getCompassRaw(); 
					}
					s_logger.info("Compass X : " + comp[0] + " Y : " + comp[1] + " Z : " + comp[2]);
				}
			}
		}
		else {
			LSM9DS1.closeDevice();
		}

		if (m_preEnable) {

			m_pressureSensor = LPS25H.getPressureSensor(I2C_BUS, I2C_PRE_ADDRESS, I2C_ADDRESS_SIZE, I2C_FREQUENCY);
			boolean status = m_pressureSensor.initDevice();
			if (!status)
				s_logger.error("Unable to initialize pressure sensor.");
			else {
				s_logger.info("Pressure : {}", m_pressureSensor.getPressure());
				s_logger.info("Temperature : {}", m_pressureSensor.getTemperature());
			}

		}
		else {
			LPS25H.closeDevice();
		}

		if (m_humEnable) {

			m_humiditySensor = HTS221.getHumiditySensor(I2C_BUS, I2C_HUM_ADDRESS, I2C_ADDRESS_SIZE, I2C_FREQUENCY);
			boolean status = m_humiditySensor.initDevice();
			if (!status)
				s_logger.error("Unable to initialize humidity sensor.");
			else {
				s_logger.info("Humidity : {}", m_humiditySensor.getHumidity());
				s_logger.info("Temperature : {}", m_humiditySensor.getTemperature());
			}

		}
		else {
			HTS221.closeDevice();
		}

		if (m_lcdEnable) {

			frameBuffer = FrameBuffer.getFrameBuffer(m_ctx);

			FrameBuffer.setRotation(m_screenRotation);
			//			frameBuffer.showLetter(m_screenLetter, Colors.RED, Colors.BLUE);

			//			frameBuffer.flipHorizontal(letter);
			//			frameBuffer.flipVertical(letter);

			frameBuffer.showMessage(m_screenMessage, Colors.ORANGE, Colors.BLACK);

		}
		else {
			if (frameBuffer != null) {
				frameBuffer.clearFrameBuffer();
				frameBuffer.closeFrameBuffer();
				frameBuffer = null;
			}
		}

		if (m_stickEnable) {

			SenseHatJoystick = Joystick.getJoystick();
			runThread = true;

			m_worker = Executors.newSingleThreadScheduledExecutor();
			m_handle = m_worker.submit(new Runnable() {
				@Override
				public void run() {

					while (runThread) {
						je = SenseHatJoystick.read();
						logJoystick (je);
					}

				}
			});

		}
		else {
			runThread = false;
			if (m_handle != null)
				m_handle.cancel(true);
			if (m_worker != null)
				m_worker.shutdownNow();
			if (SenseHatJoystick != null)
				SenseHatJoystick.closeJoystick();
		}
	}

	private void logJoystick (JoystickEvent je) {

		if (je.getCode() == Joystick.KEY_ENTER) {
			if (je.getValue() == Joystick.STATE_PRESS) {
				s_logger.info("Enter key pressed.");
			}
			else if (je.getValue() == Joystick.STATE_RELEASE) {
				s_logger.info("Enter key released.");
			}
			else if (je.getValue() == Joystick.STATE_HOLD) {
				s_logger.info("Enter key held.");
			}
		}
		else if (je.getCode() == Joystick.KEY_LEFT) {
			if (je.getValue() == Joystick.STATE_PRESS) {
				s_logger.info("Lef key pressed.");
			}
			else if (je.getValue() == Joystick.STATE_RELEASE) {
				s_logger.info("Left key released.");
			}
			else if (je.getValue() == Joystick.STATE_HOLD) {
				s_logger.info("Left key held.");
			}
		}
		else if (je.getCode() == Joystick.KEY_RIGHT) {
			if (je.getValue() == Joystick.STATE_PRESS) {
				s_logger.info("Right key pressed.");
			}
			else if (je.getValue() == Joystick.STATE_RELEASE) {
				s_logger.info("Right key released.");
			}
			else if (je.getValue() == Joystick.STATE_HOLD) {
				s_logger.info("Right key held.");
			}
		}
		else if (je.getCode() == Joystick.KEY_UP) {
			if (je.getValue() == Joystick.STATE_PRESS) {
				s_logger.info("Up key pressed.");
			}
			else if (je.getValue() == Joystick.STATE_RELEASE) {
				s_logger.info("Up key released.");
			}
			else if (je.getValue() == Joystick.STATE_HOLD) {
				s_logger.info("Up key held.");
			}
		}
		if (je.getCode() == Joystick.KEY_DOWN) {
			if (je.getValue() == Joystick.STATE_PRESS) {
				s_logger.info("Down key pressed.");
			}
			else if (je.getValue() == Joystick.STATE_RELEASE) {
				s_logger.info("Down key released.");
			}
			else if (je.getValue() == Joystick.STATE_HOLD) {
				s_logger.info("Down key held.");
			}
		}

	}

	// ----------------------------------------------------------------
	//
	//   Cloud Application Callback Methods
	//
	// ----------------------------------------------------------------

	@Override
	public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {

	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {

	}

	@Override
	public void onConnectionLost() {

	}

	@Override
	public void onConnectionEstablished() {

	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {

	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {

	}

	/*
	 * TO DO
	 * 1. Stick
	 * riorganizzare la classe
	 * aggiungere getStickDevice
	 * 2. Sensors
	 * 3. LCD screen
	 * riorganizzare la classe
	 * aggiungere getFramebuffer
	 * aggiungere metodi
	 */

}

