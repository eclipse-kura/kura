package org.eclipse.kura.raspberrypi.sensehat.example;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.raspberrypi.sensehat.SenseHat;
import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.Colors;
import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.FrameBuffer;
import org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1;
import org.eclipse.kura.raspsberrypi.sensehat.joystick.Joystick;
import org.eclipse.kura.raspsberrypi.sensehat.joystick.JoystickEvent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenseHatExample implements ConfigurableComponent {

	private static final Logger s_logger = LoggerFactory.getLogger(SenseHatExample.class);

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
	private static final String IMU_SAMPLES     = "imu.sample.number";
	private static final String PRE_ENABLE = "pressure.enable";
	private static final String HUM_ENABLE = "humidity.enable";
	private static final String LCD_ENABLE = "screen.enable";
	private static final String STICK_ENABLE = "stick.enable";
	private static final String SCREEN_MESSAGE = "screen.message";
	private static final String SCREEN_ROTATION = "screen.rotation";
	private static final String SCREEN_TEXT_COLOR = "screen.text.color";

	private boolean             m_imuAccEnable = false;
	private boolean             m_imuGyroEnable = false;
	private boolean             m_imuCompEnable = false;
	private int                 m_imuSamples = 20;
	private boolean             m_preEnable = false;
	private boolean             m_humEnable = false;
	private boolean             m_lcdEnable = false;
	private boolean             m_stickEnable = false;
	private String              m_screenMessage = "";
	private int                 m_screenRotation = 0;
	private short[]             m_screenTextColor = Colors.ORANGE;

	private Joystick SenseHatJoystick;
	private JoystickEvent je;
	private boolean runThread;

	private FrameBuffer frameBuffer;

	private ScheduledExecutorService m_Joystickworker;
	private Future<?>           m_Joystickhandle;

	private SenseHat            m_senseHat;

	private LSM9DS1             m_imuSensor;          // Inertial Measurement Unit (Accelerometer, Gyroscope, Magnetometer)
	private LPS25H              m_pressureSensor;     // Atmospheric Pressure
	private HTS221              m_humiditySensor;     // Humidity
	private Map<String, Object> m_properties;
	
	private static ScheduledFuture<?> startUpdateThread;
	private ScheduledThreadPoolExecutor	m_executor;

	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public SenseHatExample() 
	{
		super();
	}

	public void setSenseHatService(SenseHat senseHat) {
		m_senseHat = senseHat;
	}

	public void unsetSenseHatService(SenseHat senseHat) {
		m_senseHat = null;
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

		m_executor = new ScheduledThreadPoolExecutor(1);
		m_executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		m_executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		
		m_properties = properties;
		if (m_properties.get(IMU_ACC_ENABLE) != null) 
			m_imuAccEnable = (Boolean) m_properties.get(IMU_ACC_ENABLE);
		if (m_properties.get(IMU_GYRO_ENABLE) != null) 
			m_imuGyroEnable = (Boolean) m_properties.get(IMU_GYRO_ENABLE);
		if (m_properties.get(IMU_COMP_ENABLE) != null) 
			m_imuCompEnable = (Boolean) m_properties.get(IMU_COMP_ENABLE);
		if (m_properties.get(IMU_SAMPLES) != null) 
			m_imuSamples = (Integer) m_properties.get(IMU_SAMPLES);		
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
		if (m_properties.get(SCREEN_TEXT_COLOR) != null) {
			if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("RED")) 
				m_screenTextColor = Colors.RED;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("ORANGE")) 
				m_screenTextColor = Colors.ORANGE;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("YELLOW")) 
				m_screenTextColor = Colors.YELLOW;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("GREEN")) 
				m_screenTextColor = Colors.GREEN;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("BLUE")) 
				m_screenTextColor = Colors.BLUE;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("PURPLE")) 
				m_screenTextColor = Colors.PURPLE;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("VIOLET")) 
				m_screenTextColor = Colors.VIOLET;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("WHITE")) 
				m_screenTextColor = Colors.WHITE;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("BLACK")) 
				m_screenTextColor = Colors.BLACK;
		}

		if (startUpdateThread != null) {
			startUpdateThread.cancel(true);
			startUpdateThread = null;
		}
		startUpdateThread = m_executor.schedule(new Runnable() {
			public void run() {
				update();
			}
		}, 0, TimeUnit.MILLISECONDS);
		
		s_logger.info("Activating Sense Hat Application... Done.");
	}


	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("Deactivating Sense Hat Application...");

		LPS25H.closeDevice();
		HTS221.closeDevice();
		LSM9DS1.closeDevice();

		if (m_Joystickhandle != null)
			m_Joystickhandle.cancel(true);
		if (m_Joystickworker != null)
			m_Joystickworker.shutdown();
		if (SenseHatJoystick != null)
			Joystick.closeJoystick();


		if (frameBuffer != null) {
			frameBuffer.clearFrameBuffer();
			FrameBuffer.closeFrameBuffer();
			frameBuffer = null;
		}

		if (startUpdateThread != null) {
			startUpdateThread.cancel(true);
			startUpdateThread = null;
		}
		m_executor = null;
		
		s_logger.info("Deactivating Sense Hat Application... Done.");
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
		if (m_properties.get(IMU_SAMPLES) != null) 
			m_imuSamples = (Integer) m_properties.get(IMU_SAMPLES);
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
		if (m_properties.get(SCREEN_TEXT_COLOR) != null) {
			if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("RED")) 
				m_screenTextColor = Colors.RED;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("ORANGE")) 
				m_screenTextColor = Colors.ORANGE;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("YELLOW")) 
				m_screenTextColor = Colors.YELLOW;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("GREEN")) 
				m_screenTextColor = Colors.GREEN;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("BLUE")) 
				m_screenTextColor = Colors.BLUE;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("PURPLE")) 
				m_screenTextColor = Colors.PURPLE;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("VIOLET")) 
				m_screenTextColor = Colors.VIOLET;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("WHITE")) 
				m_screenTextColor = Colors.WHITE;
			else if (((String) m_properties.get(SCREEN_TEXT_COLOR)).contains("BLACK")) 
				m_screenTextColor = Colors.BLACK;
		}

		if (startUpdateThread != null) {
			startUpdateThread.cancel(true);
			startUpdateThread = null;
		}
		startUpdateThread = m_executor.schedule(new Runnable() {
			public void run() {
				update();
			}
		}, 0, TimeUnit.MILLISECONDS);
		
		s_logger.info("Updated Sense Hat Application... Done.");
	}

	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------

	private void update() {
		if (m_imuAccEnable || m_imuGyroEnable || m_imuCompEnable) {

			m_imuSensor = m_senseHat.getIMUSensor(I2C_BUS, I2C_ACC_ADDRESS, I2C_MAG_ADDRESS, I2C_ADDRESS_SIZE, I2C_FREQUENCY);
			boolean status = m_imuSensor.initDevice(m_imuAccEnable, m_imuGyroEnable, m_imuCompEnable);
			if (!status)
				s_logger.error("Unable to initialize IMU sensor.");
			else {
				if (m_imuAccEnable) {
					float[] acc = new float[3];
					for (int i = 0; i < m_imuSamples; i++) {
						acc = m_imuSensor.getAccelerometerRaw();
					}
					s_logger.info("Acceleration X : " + acc[0] + " Y : " + acc[1] + " Z : " + acc[2]);
				}
				if (m_imuGyroEnable) {
					float[] gyro = new float[3];
					for (int i = 0; i < m_imuSamples; i++) {
						gyro = m_imuSensor.getGyroscopeRaw();
					}
					s_logger.info("Orientation X : " + gyro[0] + " Y : " + gyro[1] + " Z : " + gyro[2]);
				}
				if (m_imuCompEnable) {
					float[] comp = new float[3];
					for (int i = 0; i < m_imuSamples; i++) {
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

			m_pressureSensor = m_senseHat.getPressureSensor(I2C_BUS, I2C_PRE_ADDRESS, I2C_ADDRESS_SIZE, I2C_FREQUENCY);
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

			m_humiditySensor = m_senseHat.getHumiditySensor(I2C_BUS, I2C_HUM_ADDRESS, I2C_ADDRESS_SIZE, I2C_FREQUENCY);
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

			frameBuffer = m_senseHat.getFrameBuffer(m_ctx);
			FrameBuffer.setRotation(m_screenRotation);
			frameBuffer.showMessage(m_screenMessage, m_screenTextColor, Colors.BLACK);

		}
		else {
			if (frameBuffer != null) {
				frameBuffer.clearFrameBuffer();
				FrameBuffer.closeFrameBuffer();
				frameBuffer = null;
			}
		}

		if (m_stickEnable) {

			SenseHatJoystick = m_senseHat.getJoystick();
			runThread = true;

			m_Joystickworker = Executors.newSingleThreadScheduledExecutor();
			m_Joystickhandle = m_Joystickworker.submit(new Runnable() {
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
			if (m_Joystickhandle != null)
				m_Joystickhandle.cancel(true);
			if (m_Joystickworker != null)
				m_Joystickworker.shutdownNow();
			if (SenseHatJoystick != null)
				Joystick.closeJoystick();
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

}

