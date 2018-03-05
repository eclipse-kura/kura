/**
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */

package org.eclipse.kura.internal.driver.s7plc;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.block.BlockFactory;
import org.eclipse.kura.driver.block.task.AbstractBlockDriver;
import org.eclipse.kura.driver.block.task.BlockTask;
import org.eclipse.kura.driver.block.task.Mode;
import org.eclipse.kura.driver.block.task.ToplevelBlockTask;
import org.eclipse.kura.internal.driver.s7plc.task.S7PlcTaskBuilder;
import org.eclipse.kura.internal.driver.s7plc.task.S7PlcToplevelBlockTask;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Moka7.S7;
import Moka7.S7Client;

/**
 * The Kura S7PlcDriver is a S7 PLC Driver implementation for Kura Asset-Driver
 * Topology.<br/>
 * <br/>
 *
 * The Kura S7 PLC Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.<br/>
 * <br/>
 *
 * The required properties are enlisted in {@link S7PlcChannelDescriptor} and
 * the driver connection specific properties are enlisted in
 * {@link S7PlcOptions}
 *
 * @see S7PlcChannelDescriptor
 * @see S7PlcOptions
 */
public final class S7PlcDriver extends AbstractBlockDriver<S7PlcDomain> implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(S7PlcDriver.class);

    private S7Client client = new S7Client();

    private S7PlcOptions options;

    private CryptoService cryptoService;

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService() {
        this.cryptoService = null;
    }

    protected synchronized void activate(final ComponentContext componentContext,
            final Map<String, Object> properties) {
        logger.debug("Activating S7 PLC Driver.....");
        requireNonNull(properties, "Properties cannot be null");
        updated(properties);
        logger.debug("Activating S7 PLC Driver.....Done");
    }

    protected synchronized void deactivate(final ComponentContext componentContext) {
        logger.debug("Deactivating S7 PLC Driver.....");
        try {
            this.disconnect();
        } catch (final ConnectionException e) {
            logger.error("Error while disconnecting....", e);
        }
        logger.debug("Deactivating S7 PLC Driver.....Done");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug("Updating S7 PLC Driver.....");
        requireNonNull(properties, "Properties cannot be null");
        this.options = new S7PlcOptions(properties);
        if (client.Connected) {
            try {
                logger.info("Reconnecting after configuration update...");
                disconnect();
                connect();
            } catch (ConnectionException e) {
                logger.warn("Failed to reset connection after update", e);
            }
        }
        logger.debug("Updating S7 PLC Driver.....Done");
    }

    private String decryptPassword(char[] encryptedPassword) throws KuraException {
        final char[] decodedPasswordChars = cryptoService.decryptAes(encryptedPassword);
        return new String(decodedPasswordChars);
    }

    private void authenticate() throws ConnectionException {
        logger.debug("Authenticating");
        int code;
        try {
            code = this.client.SetSessionPassword(decryptPassword(this.options.getPassword().toCharArray()));
        } catch (Exception e) {
            throw new ConnectionException(e);
        }
        if (code != 0) {
            throw new ConnectionException("Authentication failed, SetSessionPassword() failed with code: " + code);
        }
    }

    @Override
    public synchronized void connect() throws ConnectionException {
        try {
            if (!this.client.Connected) {
                logger.debug("Connecting to S7 PLC...");
                client.SetConnectionType(S7.OP);
                int code = this.client.ConnectTo(this.options.getIp(), this.options.getRack(), this.options.getSlot());
                if (code != 0) {
                    throw new ConnectionException("Failed to connect to PLC, ConnectTo() failed with code: " + code);
                }
                if (this.options.shouldAuthenticate()) {
                    authenticate();
                }
                logger.debug("Connecting to S7 PLC...Done");
            }
        } catch (Exception e) {
            throw new ConnectionException("Connection failed, unexpected exception", e);
        }
    }

    @Override
    public synchronized void disconnect() throws ConnectionException {
        if (this.client.Connected) {
            logger.debug("Disconnecting from S7 PLC...");
            this.client.Disconnect();
            logger.debug("Disconnecting from S7 PLC...Done");
        }
    }

    @Override
    protected int getReadMinimumGapSizeForDomain(S7PlcDomain domain) {
        return this.options.getMinimumGapSize();
    }

    @Override
    protected BlockFactory<ToplevelBlockTask> getTaskFactoryForDomain(final S7PlcDomain domain, final Mode mode) {
        return (start, end) -> new S7PlcToplevelBlockTask(S7PlcDriver.this, mode, domain.getDB(), start, end);
    }

    @Override
    protected Stream<Pair<S7PlcDomain, BlockTask>> toTasks(List<ChannelRecord> records, Mode mode) {
        return S7PlcTaskBuilder.build(records, mode);
    }

    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new S7PlcChannelDescriptor();
    }

    @Override
    protected void runTask(BlockTask task) {
        try {
            task.run();
        } catch (Moka7Exception e) {
            handleMoka7IOException(e);
        } catch (Exception e) {
            logger.warn("Unexpected exception", e);
        }
    }

    private void handleMoka7IOException(Moka7Exception e) {
        logger.warn("Operation failed due to IO error", e);
        if (e.getStatusCode() <= S7Client.errTCPConnectionReset) {
            logger.warn("Connection problems detected, disconnecting, will attempt to reconnect at next read/write");
            try {
                disconnect();
            } catch (ConnectionException e1) {
                logger.warn("Unable to Disconnect...", e1);
            }
        }
    }

    public synchronized void write(int db, int offset, byte[] data) throws IOException {
        int result = this.client.WriteArea(S7.S7AreaDB, db, offset, data.length, data);
        if (result != 0) {
            throw new Moka7Exception("DB: " + db + " off: " + offset + " len: " + data.length + " status: " + result,
                    result);
        }
    }

    public synchronized void read(int db, int offset, byte[] data) throws IOException {
        int result = this.client.ReadArea(S7.S7AreaDB, db, offset, data.length, data);
        if (result != 0) {
            throw new Moka7Exception("DB: " + db + " off: " + offset + " len: " + data.length + " status: " + result,
                    result);
        }
    }

    @SuppressWarnings("serial")
    private class Moka7Exception extends IOException {

        private int statusCode;

        public Moka7Exception(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
