/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.example.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.clock.ClockService;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.position.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleComponent implements CloudClientListener, EventHandler {

    private static final Logger s_logger = LoggerFactory.getLogger(ExampleComponent.class);

    // Cloud Application identifier
    private static final String APP_ID = "EXAMPLE_COMPONENT";
    private static final int POLL_DELAY_SEC = 10;

    private CloudService cloudService;
    private PositionService positionService;
    private ConfigurationService configurationService;
    private ClockService clockService;
    private CloudClient cloudClient;
    private UsbService usbService;

    private ConnectionFactory connectionFactory;
    private ScheduledThreadPoolExecutor worker;
    private ScheduledFuture<?> handle;
    private ScheduledExecutorService gpsWorker;
    private ScheduledFuture<?> gpsHandle;
    private ScheduledThreadPoolExecutor systemPropsWorker;
    private ScheduledFuture<?> systemPropsHandle;
    private Thread serialThread;
    private int counter;
    private StringBuilder serialSb;

    InputStream in;
    OutputStream out;
    CommConnection conn = null;

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.cloudService = null;
    }

    public void setPositionService(PositionService positionService) {
        this.positionService = positionService;
    }

    public void unsetPositionService(PositionService positionService) {
        this.positionService = null;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = null;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void unsetConfigurationService(ConfigurationService configurationService) {
        this.configurationService = null;
    }

    public void setClockService(ClockService clockService) {
        this.clockService = clockService;
    }

    public void unsetClockService(ClockService clockService) {
        this.clockService = null;
    }

    public void setUsbService(UsbService usbService) {
        this.usbService = usbService;
    }

    public void unsetUsbService(UsbService usbService) {
        this.usbService = null;
    }

    private boolean clockIsSynced = false;

    protected void activate(ComponentContext componentContext) {
        s_logger.debug("Activating ExampleComponent");

        List<UsbTtyDevice> ttyDevices = this.usbService.getUsbTtyDevices();
        if (ttyDevices != null && !ttyDevices.isEmpty()) {
            for (UsbTtyDevice device : ttyDevices) {
                System.out.println("Device: " + device.getVendorId() + ":" + device.getProductId());
                System.out.println("\t" + device.getDeviceNode());
                System.out.println("\t" + device.getManufacturerName());
                System.out.println("\t" + device.getProductName());
                System.out.println("\t" + device.getUsbPort());
            }
        }

        /*
         * m_worker = new ScheduledThreadPoolExecutor(1);
         *
         * m_worker.schedule(new Runnable() {
         *
         * @Override
         * public void run() {
         * try {
         * System.out.println("m_networkService.getState(): " + m_networkService.getState());
         *
         * List<String> interfaceNames = m_networkService.getAllNetworkInterfaceNames();
         * if(interfaceNames != null && interfaceNames.size() > 0) {
         * for(String interfaceName : interfaceNames) {
         * System.out.println("Interface Name: " + interfaceName + " with State: " +
         * m_networkService.getState(interfaceName));
         * }
         * }
         *
         * List<NetInterface<? extends NetInterfaceAddress>> activeNetworkInterfaces =
         * m_networkService.getActiveNetworkInterfaces();
         * if(activeNetworkInterfaces != null && activeNetworkInterfaces.size() > 0) {
         * for(NetInterface<? extends NetInterfaceAddress> activeNetworkInterface : activeNetworkInterfaces) {
         * System.out.println("ActiveNetworkInterface: " + activeNetworkInterface);
         * }
         * }
         *
         * List<NetInterface<? extends NetInterfaceAddress>> networkInterfaces =
         * m_networkService.getNetworkInterfaces();
         * if(networkInterfaces != null && networkInterfaces.size() > 0) {
         * for(NetInterface<? extends NetInterfaceAddress> networkInterface : networkInterfaces) {
         * System.out.println("NetworkInterface: " + networkInterface);
         * }
         * }
         *
         * List<WifiAccessPoint> wifiAccessPoints = m_networkService.getAllWifiAccessPoints();
         * if(wifiAccessPoints != null && wifiAccessPoints.size() > 0) {
         * for(WifiAccessPoint wifiAccessPoint : wifiAccessPoints) {
         * System.out.println("WifiAccessPoint: " + wifiAccessPoint);
         * }
         * }
         *
         * List<WifiAccessPoint> wlan0wifiAccessPoints = m_networkService.getAllWifiAccessPoints();
         * if(wlan0wifiAccessPoints != null && wlan0wifiAccessPoints.size() > 0) {
         * for(WifiAccessPoint wifiAccessPoint : wlan0wifiAccessPoints) {
         * System.out.println("wlan0 WifiAccessPoint: " + wifiAccessPoint);
         * }
         * }
         * } catch(Exception e) {
         * e.printStackTrace();
         * }
         * }
         *
         * }, 0, TimeUnit.SECONDS);
         */

        doGpsUpdate();

        /*
         * // install event listener for serial ports and specific topics of interest
         * Dictionary props = new Hashtable<String, String>();
         * props.put(EventConstants.EVENT_TOPIC, "CLOCK_SERVICE_EVENT");
         * BundleContext bc = componentContext.getBundleContext();
         * bc.registerService(EventHandler.class.getName(), this, props);
         *
         * try {
         * if(m_clockService.getLastSync() != null) {
         * clockIsSynced = true;
         * }
         * } catch (KuraException e) {
         * // TODO Auto-generated catch block
         * e.printStackTrace();
         * }
         *
         * try {
         * List<ComponentConfiguration> configs = m_configurationService.getComponentConfigurations();
         * for(ComponentConfiguration config : configs) {
         * System.out.println(config.getPid());
         * }
         * } catch (KuraException e) {
         * e.printStackTrace();
         * }
         */

        // doGpsUpdate();

        /*
         * m_systemPropsWorker = new ScheduledThreadPoolExecutor(1);
         * m_systemPropsHandle = m_systemPropsWorker.scheduleAtFixedRate(new Runnable() {
         *
         * @Override
         * public void run() {
         * try {
         *
         * String[] values = {"Zero", "One", "Two", "Three", "Four"};
         *
         * for(int i=0; i<5; i++) {
         * Map<String, Object> map = new Hashtable<String, Object>();
         * System.out.println("SETTING TO " + values[i]);
         * map.put("0", values[i]);
         * m_systemPropertiesService.setValues(map);
         * if(m_systemPropertiesService.getValue("0").equals(values[i])) {
         * System.out.println("SUCCESS... " + m_systemPropertiesService.getValue("0"));
         * } else {
         * System.out.println("FAILURE!!! " + m_systemPropertiesService.getValue("0"));
         * }
         * }
         * } catch (Exception e) {
         * e.printStackTrace();
         * }
         *
         * }
         * }, 10, (Integer) 10, TimeUnit.SECONDS);
         */

        /*
         * // get the mqtt client for this application
         * try {
         * s_logger.info("Getting CloudApplicationClient for {}...", APP_ID);
         * m_cloudClient = m_cloudService.getCloudApplicationClient(APP_ID);
         * m_cloudClient.addCloudCallbackHandler(this);
         *
         * // initialize a COM port
         * Properties props = new Properties();
         * props.setProperty("port", "/dev/ttyUSB0");
         * props.setProperty("baudRate", "9600");
         * props.setProperty("stopBits", "1");
         * props.setProperty("parity", "0");
         * props.setProperty("bitsPerWord", "8");
         * try {
         * initSerialCom(props);
         * } catch (ProtocolException e) {
         * // TODO Auto-generated catch block
         * e.printStackTrace();
         * }
         * m_serialSb = new StringBuilder();
         * if(conn!=null){
         * m_serialThread = new Thread(new Runnable() {
         *
         * @Override
         * public void run() {
         * while(conn!=null){
         * doSerial();
         * }
         * }
         * });
         * m_serialThread.start();
         * }
         * counter = 0;
         * doUpdate();
         * doGpsUpdate();
         * } catch (KuraException e) {
         * s_logger.error("Cannot activate", e);
         * throw new ComponentException(e);
         * }
         */
    }

    private boolean serialPortExists(String portName) {
        if (portName != null) {
            File f = new File(portName);
            if (f.exists()) {
                return true;
                // List<UsbTtyDevice> utd=m_usbService.getUsbTtyDevices();
                // if(utd!=null){
                // for (UsbTtyDevice u : utd) {
                // if(portName.contains(u.getDeviceNode()))
                // return true;
                // }
                // }
            }
        }
        return false;
    }

    private void initSerialCom(Properties connectionConfig) throws KuraException {
        String sPort;
        String sBaud;
        String sStop;
        String sParity;
        String sBits;

        if ((sPort = connectionConfig.getProperty("port")) == null
                || (sBaud = connectionConfig.getProperty("baudRate")) == null
                || (sStop = connectionConfig.getProperty("stopBits")) == null
                || (sParity = connectionConfig.getProperty("parity")) == null
                || (sBits = connectionConfig.getProperty("bitsPerWord")) == null) {
            throw new KuraException(KuraErrorCode.SERIAL_PORT_INVALID_CONFIGURATION);
        }

        int baud = Integer.valueOf(sBaud).intValue();
        int stop = Integer.valueOf(sStop).intValue();
        int parity = Integer.valueOf(sParity).intValue();
        int bits = Integer.valueOf(sBits).intValue();
        if (!serialPortExists(sPort)) {
            throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING);
        }

        String uri = new CommURI.Builder(sPort).withBaudRate(baud).withDataBits(bits).withStopBits(stop)
                .withParity(parity).withTimeout(2000).build().toString();

        try {
            this.conn = (CommConnection) this.connectionFactory.createConnection(uri, 1, false);
            s_logger.info(sPort + " initialized");
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // get the streams
        try {
            this.in = this.conn.openInputStream();
            this.out = this.conn.openOutputStream();
            byte[] array = "Port opened \r\n".getBytes();
            this.out.write(array);
            this.out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doSerial() {
        synchronized (this.in) {
            try {
                if (this.in.available() == 0) {
                    try {
                        Thread.sleep(10);	// avoid a high cpu load
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return;
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            int c = 0;
            try {
                c = this.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // on reception of CR, publish the received sentence
            if (c == 13) {
                s_logger.debug("Received : " + this.serialSb.toString());
                KuraPayload payload = new KuraPayload();
                payload.addMetric("sentence", this.serialSb.toString());
                try {
                    this.cloudClient.publish("message", payload, 0, false);
                } catch (KuraException e) {
                    e.printStackTrace();
                }
                this.serialSb = new StringBuilder();
            } else if (c != 10) {
                this.serialSb.append((char) c);
            }
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.debug("Deactivating ExampleComponent");
        if (this.conn != null) {
            try {
                this.conn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.conn = null;
        }
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("updated...");
        // m_properties = properties;
        // modbusProperties = getModbusProperties();
        // m_serialPortExist=serialPortExists(); // check if /dev/ttyxxx exists
        // configured=false;
    }

    private void doUpdate() {
        if (this.handle != null) {
            this.handle.cancel(true);
        }

        this.handle = this.worker.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                doPublish();
            }
        }, 0, POLL_DELAY_SEC, TimeUnit.SECONDS);
    }

    private void doGpsUpdate() {
        if (this.gpsHandle != null) {
            this.gpsHandle.cancel(true);
        }

        this.gpsWorker = Executors.newSingleThreadScheduledExecutor();
        this.gpsHandle = this.gpsWorker.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                Position position = ExampleComponent.this.positionService.getPosition();
                s_logger.debug("Latitude: " + position.getLatitude());
                s_logger.debug("Longitude: " + position.getLongitude());
                s_logger.debug("Altitude: " + position.getAltitude());
                s_logger.debug("Speed: " + position.getSpeed());
                s_logger.debug("Track: " + position.getTrack());
                s_logger.debug("Time: " + ExampleComponent.this.positionService.getNmeaTime());
                s_logger.debug("Date: " + ExampleComponent.this.positionService.getNmeaDate());
                s_logger.debug("Last Sentence: " + ExampleComponent.this.positionService.getLastSentence());
            }
        }, 0, POLL_DELAY_SEC, TimeUnit.SECONDS);
    }

    public void doPublish() {

        try {
            if (this.cloudClient != null) {
                KuraPayload payload = new KuraPayload();
                payload.addMetric("counter", this.counter);
                this.cloudClient.publish("sensor", payload, 0, false);
                this.counter++;
                if (this.counter == 4) {
                    if (this.conn != null) {
                        try {
                            this.conn.close();
                            s_logger.info("conn closed");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        this.conn = null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        s_logger.debug("control arrived for " + deviceId + " on topic " + appTopic);
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        s_logger.debug("publish arrived for " + deviceId + " on topic " + appTopic);
    }

    @Override
    public void onConnectionLost() {
        s_logger.debug("connection lost");
    }

    @Override
    public void onConnectionEstablished() {
        s_logger.debug("connection restored");
    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        s_logger.debug("published: " + messageId);
    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        s_logger.debug("published: " + messageId);
    }

    @Override
    public void handleEvent(Event event) {
        System.out.println("Got clock event: " + event);
        this.clockIsSynced = true;
    }
}
