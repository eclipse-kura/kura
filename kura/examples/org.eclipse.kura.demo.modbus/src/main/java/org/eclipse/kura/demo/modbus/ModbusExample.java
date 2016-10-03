/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
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

import java.lang.Thread.State;
import java.util.Map;
import java.util.Properties;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.protocol.modbus.ModbusProtocolDeviceService;
import org.eclipse.kura.protocol.modbus.ModbusProtocolException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusExample implements ConfigurableComponent {

    private static final Logger s_logger = LoggerFactory.getLogger(ModbusExample.class);

    static final boolean isTCP = true;
    private ModbusProtocolDeviceService m_protocolDevice;
    private Thread m_thread;
    private Map<String, Object> m_properties;
    private static Properties modbusProperties;
    private boolean configured;
    private boolean m_threadShouldStop;
    private int m_slaveAddr;

    public void setModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
        this.m_protocolDevice = modbusService;
    }

    public void unsetModbusProtocolDeviceService(ModbusProtocolDeviceService modbusService) {
        this.m_protocolDevice = null;
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        this.configured = false;
        this.m_properties = properties;
        modbusProperties = getModbusProperties();
        this.m_slaveAddr = Integer.valueOf(modbusProperties.getProperty("slaveAddr")).intValue();
        this.m_threadShouldStop = false;
        this.m_thread = new Thread(new Runnable() {

            @Override
            public void run() {
                doModbusLoop();
            }
        });
        this.m_thread.start();
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("Modbus deactivate");
        this.m_threadShouldStop = true;
        while (this.m_thread.getState() != State.TERMINATED) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        s_logger.info(this.m_thread.getState().toString());
        s_logger.info("Modbus polling thread killed");

        if (this.m_protocolDevice != null) {
            try {
                this.m_protocolDevice.disconnect();
            } catch (ModbusProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        this.configured = false;
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("updated...");
        this.m_properties = properties;
        modbusProperties = getModbusProperties();
        this.m_slaveAddr = Integer.valueOf(modbusProperties.getProperty("slaveAddr")).intValue();
        this.configured = false;
    }

    private int bcd2Dec(int bcdVal) {
        byte bcd = (byte) bcdVal;
        int decimal = (bcd & 0x000F) + ((bcd & 0x000F0) >> 4) * 10 + ((bcd & 0x00F00) >> 8) * 100
                + ((bcd & 0x0F000) >> 12) * 1000 + ((bcd & 0xF0000) >> 16) * 10000;

        return decimal;
    }

    private void doModbusLoop() {
        while (!this.m_threadShouldStop) {
            if (!this.configured) {
                try {
                    if (modbusProperties != null) {
                        configureDevice();
                        // boolean[] ab=m_protocolDevice.readExceptionStatus();
                        // m_protocolDevice.writeSingleCoil( 2048, false);
                        // int[] regs=m_protocolDevice.readHoldingRegisters(40000, 8);
                        // for(int reg:regs)
                        // s_logger.info(String.valueOf(reg));
                        // boolean[] ab=m_protocolDevice.readCoils(2048, 8);
                        // for(boolean b:ab)
                        // s_logger.info(String.valueOf(b));
                        initializeLeds();
                    }
                } catch (ModbusProtocolException e) {
                    // s_logger.error(e.getMessage());
                    e.printStackTrace();
                }
            } else {
                try {
                    int[] analogInputs = this.m_protocolDevice.readInputRegisters(this.m_slaveAddr, 512, 8);
                    int qc = bcd2Dec(analogInputs[7]);
                    s_logger.info("qc = " + qc);
                } catch (ModbusProtocolException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.m_threadShouldStop = false;
        s_logger.info("Sortie de doModbusLoop");
    }

    private void configureDevice() throws ModbusProtocolException {
        if (this.m_protocolDevice != null) {
            this.m_protocolDevice.disconnect();

            this.m_protocolDevice.configureConnection(modbusProperties);

            this.configured = true;
        }
    }

    private void initializeLeds() throws ModbusProtocolException {
        s_logger.debug("Initializing LEDs");	// once on startup, turn on each light
        for (int led = 1; led <= 6; led++) {
            this.m_protocolDevice.writeSingleCoil(this.m_slaveAddr, 2047 + led, true);
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.m_protocolDevice.writeSingleCoil(this.m_slaveAddr, 2047 + led, false);
        }
    }

    private Properties getModbusProperties() {
        Properties prop = new Properties();

        if (isTCP) {
            prop.setProperty("connectionType", "ETHERTCP");
            prop.setProperty("port", "502");
            prop.setProperty("ipAddress", "192.168.1.3");
        } else {
            if (this.m_properties != null) {
                String portName = null;
                String serialMode = null;
                String baudRate = null;
                String bitsPerWord = null;
                String stopBits = null;
                String parity = null;
                String ctopic = null;
                String Slave = null;
                if (this.m_properties.get("slaveAddr") != null) {
                    Slave = (String) this.m_properties.get("slaveAddr");
                }
                if (this.m_properties.get("port") != null) {
                    portName = (String) this.m_properties.get("port");
                }
                if (this.m_properties.get("serialMode") != null) {
                    serialMode = (String) this.m_properties.get("serialMode");
                }
                if (this.m_properties.get("baudRate") != null) {
                    baudRate = (String) this.m_properties.get("baudRate");
                }
                if (this.m_properties.get("bitsPerWord") != null) {
                    bitsPerWord = (String) this.m_properties.get("bitsPerWord");
                }
                if (this.m_properties.get("stopBits") != null) {
                    stopBits = (String) this.m_properties.get("stopBits");
                }
                if (this.m_properties.get("parity") != null) {
                    parity = (String) this.m_properties.get("parity");
                }
                if (this.m_properties.get("controlTopic") != null) {
                    ctopic = (String) this.m_properties.get("controlTopic");
                }

                prop.setProperty("connectionType", "SERIAL");
                if (portName == null) {
                    return null;
                }
                if (Slave == null) {
                    Slave = "1";
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
                if (ctopic == null) {
                    ctopic = "/modbus/manager";
                }

                prop.setProperty("port", portName);
                if (serialMode != null) {
                    prop.setProperty("serialMode", serialMode);
                }
                prop.setProperty("exclusive", "false");
                prop.setProperty("mode", "0");
                prop.setProperty("slaveAddr", Slave);
                prop.setProperty("baudRate", baudRate);
                prop.setProperty("stopBits", stopBits);
                prop.setProperty("parity", parity);
                prop.setProperty("bitsPerWord", bitsPerWord);
                prop.setProperty("controlTopic", ctopic);

            }
        }
        return prop;
    }

}
