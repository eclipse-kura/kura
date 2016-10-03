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
package org.eclipse.kura.example.serial.publisher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleSerialPublisher implements ConfigurableComponent, CloudClientListener {

    private static final Logger s_logger = LoggerFactory.getLogger(ExampleSerialPublisher.class);

    // Cloud Application identifier
    private static final String APP_ID = "EXAMPLE_SERIAL_PUBLISHER";

    // Publishing Property Names
    private static final String PUBLISH_TOPIC_PROP_NAME = "publish.semanticTopic";
    private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
    private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";

    private static final String SERIAL_DEVICE_PROP_NAME = "serial.device";
    private static final String SERIAL_BAUDRATE_PROP_NAME = "serial.baudrate";
    private static final String SERIAL_DATA_BITS_PROP_NAME = "serial.data-bits";
    private static final String SERIAL_PARITY_PROP_NAME = "serial.parity";
    private static final String SERIAL_STOP_BITS_PROP_NAME = "serial.stop-bits";

    private static final String SERIAL_ECHO_PROP_NAME = "serial.echo";
    private static final String SERIAL_CLOUD_ECHO_PROP_NAME = "serial.cloud-echo";

    private CloudService m_cloudService;
    private CloudClient m_cloudClient;

    private ConnectionFactory m_connectionFactory;

    private CommConnection m_commConnection;
    private InputStream m_commIs;
    private OutputStream m_commOs;
    // private BufferedReader m_commBr;
    // private BufferedWriter m_commBw;

    private final ScheduledExecutorService m_worker;
    private Future<?> m_handle;

    private Map<String, Object> m_properties;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public ExampleSerialPublisher() {
        super();
        this.m_worker = Executors.newSingleThreadScheduledExecutor();
    }

    public void setCloudService(CloudService cloudService) {
        this.m_cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.m_cloudService = null;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.m_connectionFactory = connectionFactory;
    }

    public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
        this.m_connectionFactory = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        s_logger.info("Activating ExampleSerialPublisher...");

        this.m_properties = new HashMap<String, Object>();

        // get the mqtt client for this application
        try {

            // Acquire a Cloud Application Client for this Application
            s_logger.info("Getting CloudApplicationClient for {}...", APP_ID);
            this.m_cloudClient = this.m_cloudService.newCloudClient(APP_ID);
            this.m_cloudClient.addCloudClientListener(this);

            // Don't subscribe because these are handled by the default
            // subscriptions and we don't want to get messages twice
            doUpdate(properties);
        } catch (Exception e) {
            s_logger.error("Error during component activation", e);
            throw new ComponentException(e);
        }
        s_logger.info("Activating ExampleSerialPublisher... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("Deactivating ExampleSerialPublisher...");

        this.m_handle.cancel(true);

        // shutting down the worker and cleaning up the properties
        this.m_worker.shutdownNow();

        // Releasing the CloudApplicationClient
        s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
        this.m_cloudClient.release();

        closePort();

        s_logger.info("Deactivating ExampleSerialPublisher... Done.");
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updated ExampleSerialPublisher...");

        // try to kick off a new job
        doUpdate(properties);
        s_logger.info("Updated ExampleSerialPublisher... Done.");
    }

    // ----------------------------------------------------------------
    //
    // Cloud Application Callback Methods
    //
    // ----------------------------------------------------------------

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionLost() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionEstablished() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        // TODO Auto-generated method stub

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

            String topic = (String) this.m_properties.get(PUBLISH_TOPIC_PROP_NAME);
            if (topic != null) {
                try {
                    this.m_cloudClient.unsubscribe(topic);
                } catch (KuraException e) {
                    s_logger.error("Unsubscribe failed", e);
                }
            }

            closePort();

            this.m_properties.clear();
            this.m_properties.putAll(properties);

            openPort();

            Boolean cloudEcho = (Boolean) this.m_properties.get(SERIAL_CLOUD_ECHO_PROP_NAME);
            if (cloudEcho) {
                try {
                    this.m_cloudClient.subscribe(topic, 0);
                } catch (KuraException e) {
                    s_logger.error("Subscribe failed", e);
                }
            }

            this.m_handle = this.m_worker.submit(new Runnable() {

                @Override
                public void run() {
                    doSerial();
                }
            });
        } catch (Throwable t) {
            s_logger.error("Unexpected Throwable", t);
        }
    }

    private void openPort() {
        String port = (String) this.m_properties.get(SERIAL_DEVICE_PROP_NAME);

        if (port == null) {
            s_logger.info("Port name not configured");
            return;
        }

        int baudRate = Integer.valueOf((String) this.m_properties.get(SERIAL_BAUDRATE_PROP_NAME));
        int dataBits = Integer.valueOf((String) this.m_properties.get(SERIAL_DATA_BITS_PROP_NAME));
        int stopBits = Integer.valueOf((String) this.m_properties.get(SERIAL_STOP_BITS_PROP_NAME));

        String sParity = (String) this.m_properties.get(SERIAL_PARITY_PROP_NAME);

        int parity = CommURI.PARITY_NONE;
        if (sParity.equals("none")) {
            parity = CommURI.PARITY_NONE;
        } else if (sParity.equals("odd")) {
            parity = CommURI.PARITY_ODD;
        } else if (sParity.equals("even")) {
            parity = CommURI.PARITY_EVEN;
        }

        String uri = new CommURI.Builder(port).withBaudRate(baudRate).withDataBits(dataBits).withStopBits(stopBits)
                .withParity(parity).withTimeout(1000).build().toString();

        try {
            this.m_commConnection = (CommConnection) this.m_connectionFactory.createConnection(uri, 1, false);
            this.m_commIs = this.m_commConnection.openInputStream();
            this.m_commOs = this.m_commConnection.openOutputStream();

            // m_commBr = new BufferedReader(new InputStreamReader(m_commIs));
            // m_commBw = new BufferedWriter(new OutputStreamWriter(m_commOs));

            s_logger.info(port + " open");
        } catch (IOException e) {
            s_logger.error("Failed to open port", e);
            cleanupPort();
        }
    }

    private void cleanupPort() {
        // if (m_commBr != null) {
        // try {
        // m_commBr.close();
        // } catch (IOException e) {
        // s_logger.error("Cannot close port buffered reader", e);
        // }
        // m_commBr = null;
        // }
        // if (m_commBw != null) {
        // try {
        // m_commBw.close();
        // } catch (IOException e) {
        // s_logger.error("Cannot close port buffered writer", e);
        // }
        // m_commBw = null;
        // }
        if (this.m_commIs != null) {
            try {
                s_logger.info("Closing port input stream...");
                this.m_commIs.close();
                s_logger.info("Closed port input stream");
            } catch (IOException e) {
                s_logger.error("Cannot close port input stream", e);
            }
            this.m_commIs = null;
        }
        if (this.m_commOs != null) {
            try {
                s_logger.info("Closing port output stream...");
                this.m_commOs.close();
                s_logger.info("Closed port output stream");
            } catch (IOException e) {
                s_logger.error("Cannot close port output stream", e);
            }
            this.m_commOs = null;
        }
        if (this.m_commConnection != null) {
            try {
                s_logger.info("Closing port...");
                this.m_commConnection.close();
                s_logger.info("Closed port");
            } catch (IOException e) {
                s_logger.error("Cannot close port", e);
            }
            this.m_commConnection = null;
        }
    }

    private void closePort() {
        cleanupPort();
    }

    private void doSerial() {
        // fetch the publishing configuration from the publishing properties
        String topic = (String) this.m_properties.get(PUBLISH_TOPIC_PROP_NAME);
        Integer qos = (Integer) this.m_properties.get(PUBLISH_QOS_PROP_NAME);
        Boolean retain = (Boolean) this.m_properties.get(PUBLISH_RETAIN_PROP_NAME);

        Boolean echo = (Boolean) this.m_properties.get(SERIAL_ECHO_PROP_NAME);

        if (this.m_commIs != null) {

            try {
                int c = -1;
                StringBuilder sb = new StringBuilder();

                while (this.m_commIs != null) {

                    if (this.m_commIs.available() != 0) {
                        c = this.m_commIs.read();
                    } else {
                        try {
                            Thread.sleep(100);
                            continue;
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    if (echo && this.m_commOs != null) {
                        this.m_commOs.write((char) c);
                    }

                    // on reception of CR, publish the received sentence
                    if (c == 13) {

                        // Allocate a new payload
                        KuraPayload payload = new KuraPayload();

                        // Timestamp the message
                        payload.setTimestamp(new Date());

                        payload.addMetric("line", sb.toString());

                        // Publish the message
                        try {
                            this.m_cloudClient.publish(topic, payload, qos, retain);
                            s_logger.info("Published to {} message: {}", topic, payload);
                        } catch (Exception e) {
                            s_logger.error("Cannot publish topic: " + topic, e);
                        }

                        sb = new StringBuilder();

                    } else if (c != 10) {
                        sb.append((char) c);
                    }
                }
            } catch (IOException e) {
                s_logger.error("Cannot read port", e);
            } finally {
                try {
                    this.m_commIs.close();
                } catch (IOException e) {
                    s_logger.error("Cannot close buffered reader", e);
                }
            }
        }
    }
}
