/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.position;

import java.io.File;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.net.modem.ModemGpsDisabledEvent;
import org.eclipse.kura.net.modem.ModemGpsEnabledEvent;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionListener;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionLostEvent;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.usb.UsbDeviceAddedEvent;
import org.eclipse.kura.usb.UsbDeviceRemovedEvent;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PositionServiceImpl implements PositionService, ConfigurableComponent, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(PositionServiceImpl.class);

    private final static long THREAD_TERMINATION_TOUT = 1; // in seconds

    private static Future<?> monitorTask;
    private static boolean stopThread;

    private Map<String, Object> properties;
    private Map<String, Object> positionServiceProperties;
    private ConnectionFactory connectionFactory;
    private Map<String, PositionListener> positionListeners;
    private GpsDevice gpsDevice;
    private ExecutorService executor;
    private EventAdmin eventAdmin;
    private UsbService usbService;

    private final int pollInterval = 500;	// milliseconds
    private boolean configured;
    private boolean useGpsd = false;
    private boolean configEnabled;
    private boolean isRunning;
    private boolean hasLock;

    // to avoid NPE don't return a null pointer
    private Position defaultPosition = null;
    private NmeaPosition defaultNmeaPosition = null;

    // add gpsd variables
    private Position gpsdPosition = null;
    private NmeaPosition gpsdNmeaPosition = null;
    private final String gpsdTimeNmea = "";
    private final String gpsdDateNmea = "";
    private final String gpsdLastSentence = "";
    private final boolean gpsdIsValidPosition = false;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = null;
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    public void setUsbService(UsbService usbService) {
        this.usbService = usbService;
    }

    public void unsetUsbService(UsbService usbService) {
        this.usbService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("Activating...");

        this.configured = false;
        this.configEnabled = false;
        this.isRunning = false;
        this.hasLock = false;
        this.useGpsd = false;
        initializeDefaultPosition(0, 0, 0);

        this.executor = Executors.newSingleThreadExecutor();
        this.properties = new HashMap<String, Object>();
        this.positionServiceProperties = new HashMap<String, Object>();

        // install event listener for serial ports
        Dictionary<String, String[]> props = new Hashtable<String, String[]>();
        String[] topic = { UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC,
                UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC,
                ModemGpsEnabledEvent.MODEM_EVENT_GPS_ENABLED_TOPIC,
                ModemGpsDisabledEvent.MODEM_EVENT_GPS_DISABLED_TOPIC };

        props.put(EventConstants.EVENT_TOPIC, topic);
        componentContext.getBundleContext().registerService(EventHandler.class.getName(), this, props);

        updated(properties);
        logger.info("Activating... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        stop();
        if (this.executor != null) {
            logger.debug("Terminating PositionServiceImpl Thread ...");
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted", e);
            }
            logger.info("PositionServiceImpl Thread terminated? - {}", this.executor.isTerminated());
            this.executor = null;
        }

        this.properties = null;
        this.positionServiceProperties = null;
        logger.info("Deactivating... Done.");
    }

    public void updated(Map<String, Object> properties) {

        logger.debug("Updating...");
        if (this.gpsDevice != null) {
            Properties currentConfigProps = this.gpsDevice.getConnectConfig();
            Properties serialProperties = getSerialConnectionProperties(properties);
            if (currentConfigProps != null && serialProperties != null) {
                if (currentConfigProps.getProperty("port").equals(serialProperties.getProperty("port"))
                        && currentConfigProps.getProperty("baudRate").equals(serialProperties.getProperty("baudRate"))
                        && currentConfigProps.getProperty("stopBits").equals(serialProperties.getProperty("stopBits"))
                        && currentConfigProps.getProperty("bitsPerWord")
                                .equals(serialProperties.getProperty("bitsPerWord"))
                        && currentConfigProps.getProperty("parity").equals(serialProperties.getProperty("parity"))) {

                    logger.debug("configureGpsDevice() :: same configuration, no need ot reconfigure GPS device");
                    return;
                }
            }
        }

        if (this.isRunning) {
            stop();
        }

        if (!properties.containsKey("modem")) {
            this.positionServiceProperties.putAll(properties);
        }
        this.properties.putAll(properties);

        this.configured = false;
        this.configEnabled = false;
        this.isRunning = false;
        this.hasLock = false;
        // m_useGpsd = (Boolean)m_properties.get("useGpsd");

        try {
            if ((Boolean) this.properties.get("enabled") && (Boolean) this.properties.get("static")) {
                if (gpsDevice != null) {
                    gpsDevice = null;
                }
                initializeDefaultPosition((Double) this.properties.get("latitude"),
                        (Double) this.properties.get("longitude"), (Double) this.properties.get("altitude"));
                this.eventAdmin.postEvent(new PositionLockedEvent(new HashMap<String, Object>()));
            } else {
                configureGpsDevice();
                start();
            }
        } catch (Exception e) {
            logger.error("Error starting PositionService background operations.", e);
        }
        logger.info("Updating... Done.");
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public Position getPosition() {
        if (this.useGpsd) {
            return this.gpsdPosition;
        } else if (this.gpsDevice != null) {
            return this.gpsDevice.getPosition();
        } else {
            return this.defaultPosition;
        }
    }

    @Override
    public NmeaPosition getNmeaPosition() {
        if (this.useGpsd) {
            return this.gpsdNmeaPosition;
        } else if (this.gpsDevice != null) {
            return this.gpsDevice.getNmeaPosition();
        } else {
            return this.defaultNmeaPosition;
        }
    }

    @Override
    public boolean isLocked() {
        return this.hasLock;
    }

    @Override
    public String getNmeaTime() {
        if (this.useGpsd) {
            return this.gpsdTimeNmea;
        } else if (this.gpsDevice != null) {
            return this.gpsDevice.getTimeNmea();
        } else {
            return null;
        }
    }

    @Override
    public String getNmeaDate() {
        if (this.useGpsd) {
            return this.gpsdDateNmea;
        } else if (this.gpsDevice != null) {
            return this.gpsDevice.getDateNmea();
        } else {
            return null;
        }
    }

    @Override
    public void registerListener(String listenerId, PositionListener positionListener) {
        if (this.positionListeners == null) {
            this.positionListeners = new HashMap<String, PositionListener>();
        }
        this.positionListeners.put(listenerId, positionListener);
        if (this.gpsDevice != null) {
            this.gpsDevice.setListeners(this.positionListeners.values());
        }
    }

    @Override
    public void unregisterListener(String listenerId) {
        if (this.positionListeners != null && this.positionListeners.containsKey(listenerId)) {
            this.positionListeners.remove(listenerId);
            if (this.gpsDevice != null) {
                this.gpsDevice.setListeners(this.positionListeners.values());
            }
        }
    }

    @Override
    public String getLastSentence() {
        if (this.useGpsd) {
            return this.gpsdLastSentence;
        } else if (this.gpsDevice != null) {
            return this.gpsDevice.getLastSentence();
        } else {
            return null;
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (!this.useGpsd) {
            if (UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC.contains(event.getTopic())) {
                if (serialPortExists()) {
                    logger.debug("GPS connected");

                    // we already have properties - just do it
                    try {
                        if (!this.isRunning) {
                            configureGpsDevice();
                            start();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC.contains(event.getTopic())) {
                if (!serialPortExists()) {
                    logger.debug("GPS disconnected");
                    stop();
                }
            } else if (ModemGpsEnabledEvent.MODEM_EVENT_GPS_ENABLED_TOPIC.contains(event.getTopic())) {

                logger.debug("ModemGpsEnabledEvent");

                this.properties.put("port", event.getProperty(ModemGpsEnabledEvent.Port));
                this.properties.put("baudRate", event.getProperty(ModemGpsEnabledEvent.BaudRate));
                this.properties.put("bitsPerWord", event.getProperty(ModemGpsEnabledEvent.DataBits));
                this.properties.put("stopBits", event.getProperty(ModemGpsEnabledEvent.StopBits));
                this.properties.put("parity", event.getProperty(ModemGpsEnabledEvent.Parity));
                this.properties.put("modem", "true");
                updated(this.properties);
            } else if (ModemGpsDisabledEvent.MODEM_EVENT_GPS_DISABLED_TOPIC.contains(event.getTopic())) {
                logger.debug("ModemGpsDisabledEvent");
                updated(this.positionServiceProperties);
            }
        }
    }

    private void start() {
        logger.debug("PositionService configured and starting");
        stopThread = false;
        if (monitorTask == null) {
            monitorTask = this.executor.submit(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("PositionServiceImpl");
                    while (!stopThread) {
                        performPoll();
                        try {
                            Thread.sleep(PositionServiceImpl.this.pollInterval);
                        } catch (InterruptedException e) {
                            // e.printStackTrace();
                            // exit silently ...
                        }
                        ;
                    }
                }
            });
        }

        this.isRunning = true;
    }

    private void stop() {
        logger.debug("PositionService stopping");
        if (monitorTask != null && !monitorTask.isDone()) {
            stopThread = true;
            monitorTask.cancel(true);
            monitorTask = null;
        }
        if (this.gpsDevice != null) {
            this.gpsDevice.disconnect();
        }

        this.configured = false;
        this.configEnabled = false;
        this.isRunning = false;
        this.hasLock = false;
    }

    private void initializeDefaultPosition(double lat, double lon, double alt) {
        Measurement l_latitude = new Measurement(java.lang.Math.toRadians(lat), Unit.rad);
        Measurement l_longitude = new Measurement(java.lang.Math.toRadians(lon), Unit.rad);
        Measurement l_altitude = new Measurement(alt, Unit.m);
        Measurement l_speed = new Measurement(0, Unit.m_s); // conversion speed in knots to m/s : 1 m/s = 1.94384449
        // knots
        Measurement l_track = new Measurement(java.lang.Math.toRadians(0), Unit.rad);
        double l_latitudeNmea = lat;
        double l_longitudeNmea = lon;
        double l_altitudeNmea = alt;
        double l_speedNmea = 0;
        double l_trackNmea = 0;
        int l_fixQuality = 0;
        int l_nrSatellites = 0;
        double l_DOP = 0;
        double l_PDOP = 0;
        double l_HDOP = 0;
        double l_VDOP = 0;
        int l_3Dfix = 0;

        this.defaultPosition = new Position(l_latitude, l_longitude, l_altitude, l_speed, l_track);
        this.defaultNmeaPosition = new NmeaPosition(l_latitudeNmea, l_longitudeNmea, l_altitudeNmea, l_speedNmea,
                l_trackNmea, l_fixQuality, l_nrSatellites, l_DOP, l_PDOP, l_HDOP, l_VDOP, l_3Dfix);

        this.gpsdPosition = new Position(l_latitude, l_longitude, l_altitude, l_speed, l_track);
        this.gpsdNmeaPosition = new NmeaPosition(l_latitudeNmea, l_longitudeNmea, l_altitudeNmea, l_speedNmea,
                l_trackNmea, l_fixQuality, l_nrSatellites, l_DOP, l_PDOP, l_HDOP, l_VDOP, l_3Dfix);
    }

    private void performPoll() {
        if (this.configEnabled && this.configured) {
            boolean isValidPosition;
            if (this.useGpsd) {
                isValidPosition = this.gpsdIsValidPosition;
            } else {
                isValidPosition = this.gpsDevice.isValidPosition();
            }
            if (isValidPosition) {
                if (!this.hasLock) {
                    this.hasLock = true;
                    this.eventAdmin.postEvent(new PositionLockedEvent(new HashMap<String, Object>()));
                    logger.info("The Position is valid");
                    if (!this.useGpsd) {
                        logger.info(this.gpsDevice.getLastSentence());
                        logger.info(this.gpsDevice.toString());
                    }
                }
            } else {
                if (this.hasLock) {
                    this.hasLock = false;
                    this.eventAdmin.postEvent(new PositionLostEvent(new HashMap<String, Object>()));
                    logger.info("The Position is not valid");
                    if (!this.useGpsd) {
                        logger.info(this.gpsDevice.getLastSentence());
                    }
                }
            }
        }
    }

    private void configureGpsDevice() throws Exception {

        Properties serialProperties = getSerialConnectionProperties(this.properties);
        if (serialProperties == null) {
            return;
        }

        if (this.gpsDevice != null) {
            logger.info("configureGpsDevice() :: disconnecting GPS device ...");
            this.gpsDevice.disconnect();
            this.gpsDevice = null;
        }

        if (!serialPortExists()) {
            logger.warn("GPS device is not present - waiting for it to be ready");
            return;
        }

        try {
            if (serialProperties != null) {
                logger.debug("Connecting to serial port: {}", serialProperties.getProperty("port"));

                // configure connection & protocol
                GpsDevice gpsDevice = new GpsDevice();
                gpsDevice.configureConnection(this.connectionFactory, serialProperties);
                gpsDevice.configureProtocol(getProtocolProperties());
                this.gpsDevice = gpsDevice;
                this.configured = true;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private boolean serialPortExists() {
        String portName;
        if (this.properties != null) {
            if (this.properties.get("port") != null) {
                portName = (String) this.properties.get("port");

                if (portName != null) {
                    if (portName.contains("/dev/") || portName.contains("COM")) {
                        File f = new File(portName);
                        if (f.exists()) {
                            return true;
                        }
                    } else {
                        List<UsbTtyDevice> utd = this.usbService.getUsbTtyDevices();
                        if (utd != null) {
                            for (UsbTtyDevice u : utd) {
                                if (portName.equals(u.getUsbPort())) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private Properties getSerialConnectionProperties(Map<String, Object> props) {
        Properties prop = new Properties();

        if (props != null) {
            String portName = null;
            int baudRate = -1;
            int bitsPerWord = -1;
            int stopBits = -1;
            int parity = -1;

            if (props.get("enabled") != null) {
                this.configEnabled = (Boolean) props.get("enabled");
                if (!this.configEnabled) {
                    return null;
                }
            } else {
                this.configEnabled = false;
                return null;
            }

            portName = (String) props.get("port");
            if (portName != null && !portName.contains("/dev/") && !portName.contains("COM")) {
                List<UsbTtyDevice> utds = this.usbService.getUsbTtyDevices();
                for (UsbTtyDevice utd : utds) {
                    if (utd.getUsbPort().equals(portName)) {
                        portName = utd.getDeviceNode();
                        break;
                    }
                }
            }
            if (props.get("baudRate") != null) {
                baudRate = (Integer) props.get("baudRate");
            }
            if (props.get("bitsPerWord") != null) {
                bitsPerWord = (Integer) props.get("bitsPerWord");
            }
            if (props.get("stopBits") != null) {
                stopBits = (Integer) props.get("stopBits");
            }
            if (props.get("parity") != null) {
                parity = (Integer) props.get("parity");
            }

            if (portName == null) {
                return null;
            }
            prop.setProperty("port", portName);
            prop.setProperty("baudRate", Integer.toString(baudRate));
            prop.setProperty("stopBits", Integer.toString(stopBits));
            prop.setProperty("parity", Integer.toString(parity));
            prop.setProperty("bitsPerWord", Integer.toString(bitsPerWord));

            logger.debug("port name: {}", portName);
            logger.debug("baud rate {}", baudRate);
            logger.debug("stop bits {}", stopBits);
            logger.debug("parity {}", parity);
            logger.debug("bits per word {}", bitsPerWord);
            return prop;
        } else {
            return null;
        }
    }

    private Properties getProtocolProperties() {
        Properties prop = new Properties();

        prop.setProperty("unitName", "Gps");

        return prop;
    }
}