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
package org.eclipse.kura.comm;

import java.net.URISyntaxException;

import javax.comm.SerialPort;

/**
 * Represents a Uniform Resource Identifier (URI) for a Comm/Serial Port.
 *
 */
public class CommURI {

    public static final int DATABITS_5 = SerialPort.DATABITS_5;
    public static final int DATABITS_6 = SerialPort.DATABITS_6;
    public static final int DATABITS_7 = SerialPort.DATABITS_7;
    public static final int DATABITS_8 = SerialPort.DATABITS_8;

    public static final int PARITY_EVEN = SerialPort.PARITY_EVEN;
    public static final int PARITY_MARK = SerialPort.PARITY_MARK;
    public static final int PARITY_NONE = SerialPort.PARITY_NONE;
    public static final int PARITY_ODD = SerialPort.PARITY_ODD;
    public static final int PARITY_SPACE = SerialPort.PARITY_SPACE;

    public static final int STOPBITS_1 = SerialPort.STOPBITS_1;
    public static final int STOPBITS_1_5 = SerialPort.STOPBITS_1_5;
    public static final int STOPBITS_2 = SerialPort.STOPBITS_2;

    public static final int FLOWCONTROL_NONE = SerialPort.FLOWCONTROL_NONE;
    public static final int FLOWCONTROL_RTSCTS_IN = SerialPort.FLOWCONTROL_RTSCTS_IN;
    public static final int FLOWCONTROL_RTSCTS_OUT = SerialPort.FLOWCONTROL_RTSCTS_OUT;
    public static final int FLOWCONTROL_XONXOFF_IN = SerialPort.FLOWCONTROL_XONXOFF_IN;
    public static final int FLOWCONTROL_XONXOFF_OUT = SerialPort.FLOWCONTROL_XONXOFF_OUT;

    private final String m_port;
    private final int m_baudRate;
    private final int m_dataBits;
    private final int m_stopBits;
    private final int m_parity;
    private final int m_flowControl;
    private final int m_timeout;

    /**
     * Constructor to build the CommURI
     *
     * @param builder
     *            the builder that contains the comm port parameters
     */
    private CommURI(Builder builder) {
        this.m_port = builder.m_port;
        this.m_baudRate = builder.m_baudRate;
        this.m_dataBits = builder.m_dataBits;
        this.m_stopBits = builder.m_stopBits;
        this.m_parity = builder.m_parity;
        this.m_flowControl = builder.m_flowControl;
        this.m_timeout = builder.m_timeout;
    }

    /**
     * The COM port or device node associated with this port
     *
     * @return a {@link String } representing the port
     */
    public String getPort() {
        return this.m_port;
    }

    /**
     * The baud rate associated with the port
     *
     * @return an int representing the baud rate
     */
    public int getBaudRate() {
        return this.m_baudRate;
    }

    /**
     * The number of data bits associated with the port
     *
     * @return an int representing the number of data bits
     */
    public int getDataBits() {
        return this.m_dataBits;
    }

    /**
     * The number of stop bits associated with the port
     *
     * @return an int representing the number of stop bits
     */
    public int getStopBits() {
        return this.m_stopBits;
    }

    /**
     * The parity associated with the port
     *
     * @return an int representing the parity
     */
    public int getParity() {
        return this.m_parity;
    }

    /**
     * The flow control associated with the port
     *
     * @return an int representing the flow control
     */
    public int getFlowControl() {
        return this.m_flowControl;
    }

    /**
     * The timeout associated with the port
     *
     * @return an int representing the timeout in milliseconds
     */
    public int getTimeout() {
        return this.m_timeout;
    }

    /**
     * The {@link String } representing the CommURI
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("comm:").append(this.m_port).append(";baudrate=").append(this.m_baudRate).append(";databits=")
                .append(this.m_dataBits).append(";stopbits=").append(this.m_stopBits).append(";parity=")
                .append(this.m_parity).append(";flowcontrol=").append(this.m_flowControl).append(";timeout=")
                .append(this.m_timeout);
        return sb.toString();
    }

    /**
     * Converts a String of the CommURI form to a CommURI Object
     *
     * @param uri
     *            the {@link String } representing the CommURI
     * @return a CommURI Object based on the uri String
     * @throws URISyntaxException
     */
    public static CommURI parseString(String uri) throws URISyntaxException {
        if (!uri.startsWith("comm:")) {
            throw new URISyntaxException(uri, "Does not start with comm:");
        }

        // get port
        int idx = uri.indexOf(";") == -1 ? uri.length() : uri.indexOf(";");
        String port = uri.substring(5, idx);
        Builder builder = new Builder(port);

        // get params
        if (idx != uri.length()) {

            String[] params = uri.substring(idx).split(";");
            for (String param : params) {

                int i = param.indexOf("=");
                if (i != -1) {
                    String name = param.substring(0, i);
                    String value = param.substring(i + 1);
                    if ("baudrate".equals(name)) {
                        builder = builder.withBaudRate(Integer.parseInt(value));
                    } else if ("databits".equals(name)) {
                        builder = builder.withDataBits(Integer.parseInt(value));
                    } else if ("stopbits".equals(name)) {
                        builder = builder.withStopBits(Integer.parseInt(value));
                    } else if ("parity".equals(name)) {
                        builder = builder.withParity(Integer.parseInt(value));
                    } else if ("flowcontrol".equals(name)) {
                        builder = builder.withFlowControl(Integer.parseInt(value));
                    } else if ("timeout".equals(name)) {
                        builder = builder.withTimeout(Integer.parseInt(value));
                    }
                }
            }
        }
        return builder.build();
    }

    /**
     * Builder class used as a helper in building the components of a CommURI
     *
     * @author eurotech
     *
     */
    public static class Builder {

        private final String m_port;
        private int m_baudRate = 19200;
        private int m_dataBits = 8;
        private int m_stopBits = 1;
        private int m_parity = 0;
        private int m_flowControl = 0;
        private int m_timeout = 2000;

        public Builder(String port) {
            this.m_port = port;
        }

        public Builder withBaudRate(int baudRate) {
            this.m_baudRate = baudRate;
            return this;
        }

        public Builder withDataBits(int dataBits) {
            this.m_dataBits = dataBits;
            return this;
        }

        public Builder withStopBits(int stopBits) {
            this.m_stopBits = stopBits;
            return this;
        }

        public Builder withParity(int parity) {
            this.m_parity = parity;
            return this;
        }

        public Builder withFlowControl(int flowControl) {
            this.m_flowControl = flowControl;
            return this;
        }

        public Builder withTimeout(int timeout) {
            this.m_timeout = timeout;
            return this;
        }

        public CommURI build() {
            return new CommURI(this);
        }
    }
}
