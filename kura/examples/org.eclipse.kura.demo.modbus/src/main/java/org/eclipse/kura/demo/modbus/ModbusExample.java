/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix build warnigns
 *******************************************************************************/
package org.eclipse.kura.demo.modbus;

import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.protocol.modbus.ModbusProtocolDeviceService;
import org.eclipse.kura.protocol.modbus.ModbusProtocolException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusExample implements ConfigurableComponent, CloudClientListener {

    private static final Logger s_logger = LoggerFactory.getLogger(ModbusExample.class);

    // Cloud Application identifier
    private static final String APP_ID = "MODBUS_EXAMPLE";

    // Publishing Property Names
    private static final String MODBUS_PROTOCOL = "protocol";
    private static final String MODBUS_SLAVE_ADDRESS = "slaveAddr";

    private static final String SERIAL_DEVICE_PROP_NAME = "serial.port";
    private static final String SERIAL_BAUDRATE_PROP_NAME = "serial.baudrate";
    private static final String SERIAL_DATA_BITS_PROP_NAME = "serial.data-bits";
    private static final String SERIAL_PARITY_PROP_NAME = "serial.parity";
    private static final String SERIAL_STOP_BITS_PROP_NAME = "serial.stop-bits";

    private static final String ETHERNET_IP_ADDRESS = "ipAddress";
    private static final String ETHERNET_TCP_PORT = "tcp.port";

    private static final String PUBLISH_TOPIC_PROP_NAME = "publishTopic";
    private static final String POLL_INTERVAL = "pollInterval";
    private static final String PUBLISH_INTERVAL = "publishInterval";

    private static final String INPUT_ADDRESS = "inputAddress";
    private static final String REGISTER_ADDRESS = "registerAddress";

    private CloudService m_cloudService;
    private CloudClient m_cloudClient;
    private static boolean doConnection = true;

    private ScheduledExecutorService m_worker;
    private Future<?> m_handle;

    private ModbusProtocolDeviceService m_protocolDevice;
    private Map<String, Object> m_properties;
    private static Properties modbusProperties;
    private boolean configured;
    private int m_slaveAddr;
    private int m_publish_interval = 0;
    private String m_topic = "";
    private long startPublish = 0;

    private String inputAddress = "";
    private String registerAddress = "";
    private int inputaddr = 0;
    private int registeraddr = 0;

    public void setModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
        this.m_protocolDevice = modbusService;
    }

    public void unsetModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
        this.m_protocolDevice = null;
    }

    public void setCloudService(CloudService cloudService) {
        this.m_cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.m_cloudService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        s_logger.info("Activating ModbusExample...");

        this.m_worker = Executors.newSingleThreadScheduledExecutor();

        this.configured = false;
        doUpdate(properties);
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("ModbusExample deactivate...");
        if (this.m_handle != null) {
            this.m_handle.cancel(true);
        }
        s_logger.info("Modbus polling thread killed");

        if (this.m_protocolDevice != null) {
            try {
                this.m_protocolDevice.disconnect();
            } catch (ModbusProtocolException e) {
                s_logger.error("Failed to disconnect : {}", e.getMessage());
            }
        }
        this.configured = false;

        // Releasing the CloudApplicationClient
        s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
        this.m_cloudClient.release();

    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("updated...");
        this.configured = false;
        doUpdate(properties);
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    /**
     * Called after a new set of properties has been configured on the service
     */
    private void doUpdate(Map<String, Object> properties) {
        try {

            for (String s : properties.keySet()) {
                s_logger.info("Update - " + s + ": " + properties.get(s));
            }

            // cancel a current worker handle if one if active
            if (this.m_handle != null) {
                this.m_handle.cancel(true);
            }

            if (this.m_protocolDevice != null) {
                try {
                    this.m_protocolDevice.disconnect();
                } catch (ModbusProtocolException e) {
                    s_logger.error("Failed to disconnect : {}", e.getMessage());
                }
            }
            this.configured = false;

            this.m_properties = properties;
            modbusProperties = getModbusProperties();
            if (this.m_properties.get(PUBLISH_TOPIC_PROP_NAME) != null) {
                this.m_topic = (String) this.m_properties.get(PUBLISH_TOPIC_PROP_NAME);
            }
            if (this.m_properties.get(PUBLISH_INTERVAL) != null) {
                this.m_publish_interval = Integer.valueOf((String) this.m_properties.get(PUBLISH_INTERVAL));
            }

            if (this.m_properties.get(INPUT_ADDRESS) != null) {
                inputAddress = (String) this.m_properties.get(INPUT_ADDRESS);
                inputaddr = Integer.valueOf(inputAddress);
            }
            else inputAddress="";
            if (this.m_properties.get(REGISTER_ADDRESS) != null) {
                registerAddress = (String) this.m_properties.get(REGISTER_ADDRESS);
                registeraddr = Integer.valueOf(registerAddress);
            }
            else registerAddress="";

            if (!this.configured) {
                try {
                    if (modbusProperties != null) {
                        configureDevice();
                    }
                } catch (ModbusProtocolException e) {
                    s_logger.error("ModbusProtocolException :  {}", e.getMessage());
                }
            }

            // schedule a new worker based on the properties of the service
            int pubrate = 1000;
            if (this.m_properties.get(POLL_INTERVAL) != null) {
                pubrate = Integer.valueOf((String) this.m_properties.get(POLL_INTERVAL));
            }
            s_logger.info("scheduleAtFixedRate {}", pubrate);
            this.m_handle = this.m_worker.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    doModbusLoop();
                }
            }, 0, pubrate, TimeUnit.MILLISECONDS);

        } catch (Throwable t) {
            s_logger.error("Unexpected Throwable", t);
        }
    }

    private boolean doConnectionWork() {
        try {
            if (this.m_cloudClient == null) {
                // Attempt to get Master Client reference
                s_logger.debug("Getting Cloud Client");
                try {
                    // Acquire a Cloud Application Client for this Application
                    s_logger.info("Getting CloudClient for {}...", APP_ID);
                    this.m_cloudClient = this.m_cloudService.newCloudClient(APP_ID);
                    this.m_cloudClient.addCloudClientListener(this);
                } catch (KuraException e) {
                    s_logger.debug("Cannot get a Cloud Client");
                    e.printStackTrace();
                    return true;
                }

            }

            if (!this.m_cloudClient.isConnected()) {
                s_logger.debug("Waiting for Cloud Client to connect");
                return true;
            }

        } catch (Exception e) {
            s_logger.debug("Cloud client is not yet available..");
            return true;
        }

        // Successfully connected - kill the thread
        s_logger.info("Successfully connected the Cloud Client");
        return false;
    }

    private void doModbusLoop() {

        // Connect the Cloud Client
        if (doConnection) {
            doConnection = doConnectionWork();
        }

        try {
            // Allocate a new payload
            KuraPayload payload = new KuraPayload();
            
            if(!inputAddress.isEmpty()){
                boolean[] dicreteInputs = this.m_protocolDevice.readDiscreteInputs(this.m_slaveAddr, inputaddr, 1);
                StringBuilder sb = new StringBuilder().append("Input ").append(inputaddr).append(" = ");                    
                sb.append(dicreteInputs[0]);
                s_logger.info(sb.toString());
                payload.addMetric("input" , Boolean.valueOf(dicreteInputs[0]));
            }
            if(!registerAddress.isEmpty()){                
                int[] analogInputs = this.m_protocolDevice.readInputRegisters(this.m_slaveAddr, registeraddr, 1);
                StringBuilder sb = new StringBuilder().append("Register ").append(registerAddress).append(" = ");                    
                sb.append(analogInputs[0]);
                s_logger.info(sb.toString());
                payload.addMetric("register" , Integer.valueOf(analogInputs[0]));
            }

            // time to publish ?
            long elapsed = System.currentTimeMillis() - this.startPublish;
            if (elapsed > this.m_publish_interval * 1000) {
                this.startPublish = System.currentTimeMillis();
                if (!payload.metrics().isEmpty() && !this.m_topic.isEmpty() && this.m_cloudClient.isConnected()) {

                    // Timestamp the message
                    payload.setTimestamp(new Date());

                    // Publish the message
                    try {
                        int messageId = this.m_cloudClient.publish(this.m_topic, payload, 0, false);
                        s_logger.info("Published to {} message: {} with ID: {}", new Object[] { this.m_topic, payload, messageId });
                    } catch (Exception e) {
                        s_logger.error("Cannot publish topic: " + this.m_topic, e);
                    }
                } else {
                    if (this.m_topic.isEmpty()) {
                        s_logger.info("Topic empty -> no publish");
                    } else if (!this.m_cloudClient.isConnected()) {
                        s_logger.info("Cloud client not connected -> no publish");
                    }
                }
            }
        } catch (ModbusProtocolException e) {
            s_logger.error("ModbusProtocolException :  {}", e.getMessage());
        }
    }

    private void configureDevice() throws ModbusProtocolException {
        if (this.m_protocolDevice != null) {
            this.m_protocolDevice.disconnect();

            this.m_protocolDevice.configureConnection(modbusProperties);

            this.configured = true;
        }
    }

    private Properties getModbusProperties() {

        if (this.m_properties == null) {
            return null;
        }

        Properties prop = new Properties();

        String modbusProtocol = (String) this.m_properties.get(MODBUS_PROTOCOL);
        if (modbusProtocol == null) {
            return null;
        }
        prop.setProperty("connectionType", modbusProtocol);

        String Slave = "1";
        if (this.m_properties.get(MODBUS_SLAVE_ADDRESS) != null) {
            Slave = (String) this.m_properties.get(MODBUS_SLAVE_ADDRESS);
        }
        prop.setProperty("slaveAddr", Slave);

        boolean isTCP = "TCP-RTU".equals(modbusProtocol) || "TCP/IP".equals(modbusProtocol);
        if (isTCP) {
            prop.setProperty("ethport", (String) this.m_properties.get(ETHERNET_TCP_PORT));
            prop.setProperty("ipAddress", (String) this.m_properties.get(ETHERNET_IP_ADDRESS));
        } else {
            if (this.m_properties != null) {
                String portName = null;
                String baudRate = null;
                String bitsPerWord = null;
                String stopBits = null;
                String parity = null;
                if (this.m_properties.get(SERIAL_DEVICE_PROP_NAME) != null) {
                    portName = (String) this.m_properties.get(SERIAL_DEVICE_PROP_NAME);
                }
                if (this.m_properties.get(SERIAL_BAUDRATE_PROP_NAME) != null) {
                    baudRate = (String) this.m_properties.get(SERIAL_BAUDRATE_PROP_NAME);
                }
                if (this.m_properties.get(SERIAL_DATA_BITS_PROP_NAME) != null) {
                    bitsPerWord = (String) this.m_properties.get(SERIAL_DATA_BITS_PROP_NAME);
                }
                if (this.m_properties.get(SERIAL_STOP_BITS_PROP_NAME) != null) {
                    stopBits = (String) this.m_properties.get(SERIAL_STOP_BITS_PROP_NAME);
                }
                if (this.m_properties.get(SERIAL_PARITY_PROP_NAME) != null) {
                    parity = (String) this.m_properties.get(SERIAL_PARITY_PROP_NAME);
                }

                if (portName == null) {
                    return null;
                }
                if (baudRate == null) {
                    baudRate = "9600";
                }
                if (stopBits == null) {
                    stopBits = "1";
                }
                if (parity == null) {
                    parity = "0";
                }
                if (bitsPerWord == null) {
                    bitsPerWord = "8";
                }

                prop.setProperty("port", portName);
                prop.setProperty("exclusive", "false");
                prop.setProperty("baudRate", baudRate);
                prop.setProperty("stopBits", stopBits);
                prop.setProperty("parity", parity);
                prop.setProperty("bitsPerWord", bitsPerWord);
            }
        }
        prop.setProperty("mode", "0");
        prop.setProperty("transmissionMode", "RTU");
        prop.setProperty("respTimeout", "1000");
        this.m_slaveAddr = Integer.valueOf(Slave);
        
        return prop;
    }

    // ----------------------------------------------------------------
    //
    // Cloud Application Callback Methods
    //
    // ----------------------------------------------------------------
    @Override
    public void onConnectionEstablished() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionLost() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onControlMessageArrived(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageArrived(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageConfirmed(int arg0, String arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessagePublished(int arg0, String arg1) {
        // TODO Auto-generated method stub

    }

}
