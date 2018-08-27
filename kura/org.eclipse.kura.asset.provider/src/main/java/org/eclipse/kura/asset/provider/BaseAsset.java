/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Amit Kumar Mondal
 *     Red Hat Inc
 *
 *******************************************************************************/
package org.eclipse.kura.asset.provider;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.channel.ChannelFlag.FAILURE;
import static org.eclipse.kura.channel.ChannelType.READ;
import static org.eclipse.kura.channel.ChannelType.READ_WRITE;
import static org.eclipse.kura.channel.ChannelType.WRITE;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.annotation.Extensible;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.internal.asset.provider.BaseAssetConfiguration;
import org.eclipse.kura.internal.asset.provider.DriverTrackerCustomizer;
import org.eclipse.kura.type.DataType;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class BaseAsset is basic implementation of {@code Asset}.
 *
 * BaseAsset persists the AssetConfguration using the {@code ConfigurationService}.
 *
 * The configuration properites must conform to the following
 * specifications.<br>
 * <br>
 *
 * <ul>
 * <li>The value associated with <b><i>driver.pid</i></b> key in the map denotes
 * the driver instance PID (kura.service.pid) to be consumed by this asset</li>
 * <li>A value associated with <b><i>asset.desc</i></b> key denotes the asset
 * description</li>
 * <li>[name#property]</li> where name is a string denoting the channel's unique
 * name and the {@code [property]} denotes the protocol specific properties.
 * The name of a channel must be unique in the channels configurations of an Asset, and is not
 * allowed to contain spaces or any of the following characters: <b>#</b>, <b>_</b>.
 * </ul>
 *
 * The configuration properties of a channel belong to one of this two groups: generic channel properties and
 * driver specific properties.
 * <br>
 * Generic channel properties begin with the '+' character, and are driver independent.
 * The following generic channel properties must always be present in the channel configuration:
 * <ul>
 * <li>{@code +type} identifies the channel type (READ, WRITE or READ_WRITE) as specified by {@code ChannelType}</li>
 * <li>{@code +value.type} identifies the {@link DataType} of the channel.</li>
 * </ul>
 * For example, the property keys above for a channel named channel1 would be encoded as channel1#+type and
 * channel1#+value.type<br>
 *
 * The values of the <b>+value.type</b> and <b>+type</b> properties must me mappable
 * respectively to a {@link DataType} and {@code ChannelType} instance.
 * <br>
 * The value of these property can be either an instance of the corresponding type,
 * or a string representation that equals the value returned by calling the {@code toString()} method
 * on one of the enum variants of that type.
 *
 * <br>
 * Driver specific properties are defined by the driver, their keys cannot begin with a '+' character.
 * For example, valid driver specific properties can be channel1#modbus.register,
 * channel1#modbus.unit.id etc.<br>
 * <br>
 *
 * @see AssetOptions
 * @see AssetConfiguration
 */
@Extensible
public class BaseAsset implements Asset, SelfConfiguringComponent {

    /** Configuration PID Property. */
    protected static final String CONF_PID = "org.eclipse.kura.asset";

    private static final Logger logger = LoggerFactory.getLogger(BaseAsset.class);

    /** Container of channel listeners registered by this Asset. */
    protected final Set<ChannelListenerRegistration> channelListeners = new HashSet<>();

    private BaseAssetConfiguration config;

    private ComponentContext context;

    private ServiceTracker<Driver, Driver> driverServiceTracker;

    private BaseAssetExecutor executor;

    private AtomicReference<DriverState> driverState = new AtomicReference<>();

    /**
     * OSGi service component callback while activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the service properties
     */
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.info("activating...");
        this.context = componentContext;
        this.executor = initBaseAssetExecutor();
        updated(properties);
        logger.info("activating...done");
    }

    /**
     * OSGi service component update callback.
     *
     * @param properties
     *            the service properties
     */
    public void updated(final Map<String, Object> properties) {

        logger.info("loading asset configuration...");
        final long start = System.currentTimeMillis();
        try {
            this.config = new BaseAssetConfiguration(properties);
        } catch (final Exception e) {
            logger.warn("Failed to retrieve properties from config", e);
        }
        logger.info("loading asset configuration...done in {} ms", System.currentTimeMillis() - start);

        reopenDriverTracker(this.config.getAssetConfiguration().getDriverPid());
    }

    /**
     * OSGi service component callback while deactivation.
     *
     * @param context
     *            the component context
     */
    protected void deactivate(final ComponentContext context) {
        logger.debug("deactivating...");

        if (this.driverServiceTracker != null) {
            this.driverServiceTracker.close();
        }

        executor.shutdown();

        logger.debug("deactivating...done");
    }

    /**
     * Tracks the Driver in the OSGi service registry with the specified driver
     * PID.
     *
     * @param driverId
     *            the identifier of the driver
     * @throws NullPointerException
     *             if driver id provided is null
     */
    private void reopenDriverTracker(final String driverId) {
        requireNonNull(driverId, "Driver PID cannot be null");
        logger.debug("Attaching driver instance...");

        if (this.driverServiceTracker != null) {
            this.driverServiceTracker.close();
            this.driverServiceTracker = null;
        }
        final DriverTrackerCustomizer driverTrackerCustomizer = new DriverTrackerCustomizer(context.getBundleContext(),
                this, driverId);
        this.driverServiceTracker = new ServiceTracker<>(this.context.getBundleContext(), Driver.class.getName(),
                driverTrackerCustomizer);
        this.driverServiceTracker.open();

        logger.debug("Attaching driver instance...Done");
    }

    /** {@inheritDoc} */
    @Override
    public AssetConfiguration getAssetConfiguration() {
        return this.config.getAssetConfiguration();
    }

    public void setDriver(final Driver driver) {

        final DriverState newState = new DriverState(driver);
        final DriverState oldState = this.driverState.getAndSet(newState);

        executor.runConfig(() -> {
            if (oldState != null) {
                oldState.shutdown();
            }
            this.config.complete(getOCD(), context, getAssetChannelDescriptor(), newState.getDriver());
            final List<ChannelRecord> readRecords = this.config.getAllReadRecords();
            if (!readRecords.isEmpty()) {
                final PreparedRead preparedRead = newState.tryPrepareRead(readRecords);
                if (preparedRead != null) {
                    onPreparedReadCreated(preparedRead);
                }
            }
            updateChannelListenerRegistrations(this.channelListeners, this.config.getAssetConfiguration());
            newState.syncChannelListeners(this.channelListeners, config.getAssetConfiguration().getAssetChannels());
        });
    }

    public void unsetDriver() {
        final DriverState oldState = this.driverState.getAndSet(null);

        if (oldState != null) {
            executor.runConfig(() -> {
                final PreparedRead preparedRead = oldState.getPreparedRead();
                if (preparedRead != null) {
                    onPreparedReadReleased(preparedRead);
                }
                oldState.shutdown();
            });
        }
    }

    public Driver getDriver() {
        final DriverState state = this.driverState.get();

        if (state == null) {
            return null;
        }

        return state.getDriver();
    }

    /** {@inheritDoc} */
    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {

        final Map<String, Object> properties = this.config.getProperties();

        final String componentName = properties.get(ConfigurationService.KURA_SERVICE_PID).toString();

        Tocd ocd = config.getDefinition();

        if (ocd == null) {
            ocd = getOCD();
        }

        return new ComponentConfigurationImpl(componentName, ocd, new HashMap<>(properties));
    }

    /**
     * Return the Factory PID of the component
     *
     * @return the factory PID
     */
    protected String getFactoryPid() {
        return CONF_PID;
    }

    protected String getKuraServicePid() throws KuraException {
        return config.getKuraServicePid();
    }

    /** {@inheritDoc} */
    @Override
    public List<ChannelRecord> readAllChannels() throws KuraException {
        logger.debug("Reading asset channels...");

        final DriverState state = this.driverState.get();

        if (state == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Driver not attached");
        }

        final BaseAssetConfiguration conf = this.config;

        final List<ChannelRecord> channelRecords = unwrap(executor.runIO(() -> {
            final List<ChannelRecord> records;
            final PreparedRead preparedRead = state.getPreparedRead();
            if (preparedRead != null) {
                records = preparedRead.execute();
            } else {
                records = conf.getAllReadRecords();
                state.getDriver().read(records);
            }
            return records;
        }));

        logger.debug("Reading asset channels...Done");
        return channelRecords;
    }

    private void validateChannel(final Channel channel, final EnumSet<ChannelType> allowedTypes,
            final String typeNotAllowedMessage) {
        if (channel == null) {
            throw new IllegalArgumentException("Channel not available");
        } else if (!allowedTypes.contains(channel.getType())) {
            throw new IllegalArgumentException(typeNotAllowedMessage);
        } else if (!channel.isEnabled()) {
            throw new IllegalArgumentException("Channel is not enabled");
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<ChannelRecord> read(final Set<String> channelNames) throws KuraException {
        logger.debug("Reading asset channels...");

        final DriverState state = this.driverState.get();

        if (state == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Driver not attached");
        }

        final Map<String, Channel> channels = this.config.getAssetConfiguration().getAssetChannels();

        final List<ChannelRecord> channelRecords = new ArrayList<>(channelNames.size());
        final List<ChannelRecord> validRecords = new ArrayList<>(channelNames.size());

        for (final String name : channelNames) {

            final Channel channel = channels.get(name);
            try {
                validateChannel(channel, EnumSet.of(READ, READ_WRITE),
                        "Channel type not within expected types (READ or READ_WRITE)");
            } catch (Exception e) {
                final ChannelRecord record = ChannelRecord.createStatusRecord(name,
                        new ChannelStatus(FAILURE, e.getMessage(), e));
                record.setTimestamp(System.currentTimeMillis());
                channelRecords.add(record);
                continue;
            }

            final ChannelRecord record = channel.createReadRecord();
            validRecords.add(record);
            channelRecords.add(record);
        }

        if (!validRecords.isEmpty()) {
            unwrap(executor.runIO(() -> {
                state.getDriver().read(validRecords);
                return (Void) null;
            }));
        }
        logger.debug("Reading asset channels...Done");
        return channelRecords;
    }

    public boolean hasReadChannels() {
        return this.config.hasReadChannels();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void registerChannelListener(final String channelName, final ChannelListener channelListener)
            throws KuraException {
        requireNonNull(channelName, "Channel name cannot be null");
        requireNonNull(channelListener, "Asset Listener cannot be null");

        logger.debug("Registering Channel Listener for monitoring...");
        final Map<String, Channel> channels = this.config.getAssetConfiguration().getAssetChannels();

        final Channel channel = channels.get(channelName);

        if (channel == null) {
            throw new IllegalArgumentException("Channel not found");
        }

        final ChannelListenerRegistration reg = new ChannelListenerRegistration(channelName, channelListener);

        if (this.channelListeners.contains(reg)) {
            return;
        }

        this.channelListeners.add(reg);

        final DriverState state = this.driverState.get();

        if (state == null) {
            return;
        }

        executor.runConfig(() -> state.syncChannelListeners(this.channelListeners, channels));
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void unregisterChannelListener(final ChannelListener channelListener) throws KuraException {
        requireNonNull(channelListener, "Asset Listener cannot be null");

        final Iterator<ChannelListenerRegistration> i = this.channelListeners.iterator();

        while (i.hasNext()) {
            final ChannelListenerRegistration reg = i.next();
            if (reg.listener == channelListener) {
                i.remove();
            }
        }

        final DriverState state = this.driverState.get();

        if (state == null) {
            return;
        }

        final Map<String, Channel> channels = config.getAssetConfiguration().getAssetChannels();

        executor.runConfig(() -> state.syncChannelListeners(this.channelListeners, channels));
    }

    protected void onPreparedReadCreated(PreparedRead preparedRead) {
        // intended to be overridden by subclasses
    }

    protected void onPreparedReadReleased(PreparedRead preparedRead) {
        // intended to be overridden by subclasses
    }

    public BaseAssetExecutor getBaseAssetExecutor() {
        return executor;
    }

    protected BaseAssetExecutor initBaseAssetExecutor() {

        // TODO make this configurable and maybe allow using shared thread pools

        final ExecutorService ioExecutor = new ThreadPoolExecutor(1, 5, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        final ExecutorService configExecutor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

        return new BaseAssetExecutor(ioExecutor, configExecutor);
    }

    /** {@inheritDoc} */
    @Override
    public void write(final List<ChannelRecord> channelRecords) throws KuraException {
        logger.debug("Writing to channels...");

        final DriverState state = this.driverState.get();

        if (state == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Driver not attached");
        }

        final Map<String, Channel> channels = this.config.getAssetConfiguration().getAssetChannels();

        final List<ChannelRecord> validRecords = new ArrayList<>(channelRecords.size());

        for (final ChannelRecord channelRecord : channelRecords) {
            final String channelName = channelRecord.getChannelName();

            final Channel channel = channels.get(channelName);
            try {
                validateChannel(channel, EnumSet.of(WRITE, READ_WRITE),
                        "Channel type not within expected types (WRITE or READ_WRITE)");
            } catch (Exception e) {
                channelRecord.setChannelStatus(new ChannelStatus(FAILURE, e.getMessage(), e));
                channelRecord.setTimestamp(System.currentTimeMillis());
                continue;
            }

            channelRecord.setChannelConfig(channel.getConfiguration());
            validRecords.add(channelRecord);
        }

        if (!validRecords.isEmpty()) {
            unwrap(executor.runIO(() -> {
                state.getDriver().write(validRecords);
                return (Void) null;
            }));
        }
        logger.debug("Writing to channels...Done");
    }

    private static <T> T unwrap(final CompletableFuture<T> future) throws KuraException {
        try {
            return future.get();
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, cause, cause.getMessage());
        } catch (final Exception e) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e, e.getMessage());
        }
    }

    protected boolean isChannelListenerValid(final ChannelListenerRegistration reg, final Channel channel) {
        return channel != null;
    }

    protected void updateChannelListenerRegistrations(final Set<ChannelListenerRegistration> listeners,
            final AssetConfiguration config) {
        final Map<String, Channel> channels = config.getAssetChannels();

        final Iterator<ChannelListenerRegistration> i = listeners.iterator();

        while (i.hasNext()) {
            final ChannelListenerRegistration reg = i.next();

            if (!isChannelListenerValid(reg, channels.get(reg.getChannelName()))) {
                i.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected List<Tad> getAssetChannelDescriptor() {
        return (List<Tad>) BaseChannelDescriptor.get().getDescriptor();
    }

    protected Tocd getOCD() {
        return new BaseAssetOCD();
    }

    @Override
    public String toString() {
        return "BaseAsset [Asset Configuration=" + this.config + "]";
    }

    protected static class ChannelListenerRegistration {

        private final String channelName;
        private final ChannelListener listener;

        public ChannelListenerRegistration(String channelName, ChannelListener listener) {
            this.channelName = channelName;
            this.listener = listener;
        }

        public String getChannelName() {
            return this.channelName;
        }

        public ChannelListener getChannelListener() {
            return this.listener;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((channelName == null) ? 0 : channelName.hashCode());
            result = prime * result + ((listener == null) ? 0 : listener.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ChannelListenerRegistration other = (ChannelListenerRegistration) obj;
            if (channelName == null) {
                if (other.channelName != null)
                    return false;
            } else if (!channelName.equals(other.channelName))
                return false;
            if (listener == null) {
                if (other.listener != null)
                    return false;
            }
            return listener == other.listener;
        }
    }
}
