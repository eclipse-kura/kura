/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
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

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.asset.provider.AssetConstants.ASSET_DESC_PROP;
import static org.eclipse.kura.asset.provider.AssetConstants.ASSET_DRIVER_PROP;
import static org.eclipse.kura.channel.ChannelFlag.FAILURE;
import static org.eclipse.kura.channel.ChannelType.READ;
import static org.eclipse.kura.channel.ChannelType.READ_WRITE;
import static org.eclipse.kura.channel.ChannelType.WRITE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.internal.asset.provider.AssetOptions;
import org.eclipse.kura.internal.asset.provider.DriverTrackerCustomizer;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.framework.InvalidSyntaxException;
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
    private static final String CONF_PID = "org.eclipse.kura.asset";

    private static final Logger logger = LoggerFactory.getLogger(BaseAsset.class);

    private static final AssetMessages message = LocalizationAdapter.adapt(AssetMessages.class);

    /** The provided asset configuration wrapper instance. */
    private AssetConfiguration assetConfiguration;

    /** Container of channel listeners registered by this Asset. */
    private final Set<ChannelListener> channelListeners;

    private AssetOptions assetOptions;

    private ComponentContext context;

    private volatile Driver driver;

    /** Synchronization Monitor for driver specific operations. */
    private final Lock monitor;

    /** The configurable properties of this service. */
    private Map<String, Object> properties;

    private ServiceTracker<Driver, Driver> driverServiceTracker;

    private PreparedRead preparedRead;

    private boolean hasReadChannels;

    private String kuraServicePid;

    /**
     * Instantiates a new asset instance.
     */
    public BaseAsset() {
        this.channelListeners = new CopyOnWriteArraySet<>();
        this.monitor = new ReentrantLock();
    }

    /**
     * OSGi service component callback while activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the service properties
     */
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug(message.activating());
        this.context = componentContext;
        updated(properties);
        logger.debug(message.activatingDone());
    }

    /**
     * OSGi service component update callback.
     *
     * @param properties
     *            the service properties
     */
    public void updated(final Map<String, Object> properties) {
        logger.debug(message.updating());
        this.properties = properties;
        this.kuraServicePid = (String) this.properties.get(ConfigurationService.KURA_SERVICE_PID);
        retrieveConfigurationsFromProperties(properties);
        attachDriver(this.assetConfiguration.getDriverPid());
        logger.debug(message.updatingDone());
    }

    /**
     * OSGi service component callback while deactivation.
     *
     * @param context
     *            the component context
     */
    protected void deactivate(final ComponentContext context) {
        logger.debug(message.deactivating());
        this.monitor.lock();
        try {
            if (this.driver != null) {
                try {
                    for (final ChannelListener listener : channelListeners) {
                        this.driver.unregisterChannelListener(listener);
                    }
                } catch (final ConnectionException ce) {
                    logger.warn(message.errorDriverDisconnection(), ce);
                }
                try {
                    this.driver.disconnect();
                } catch (final ConnectionException e) {
                    logger.error(message.errorDriverDisconnection(), e);
                }
            }
        } finally {
            this.monitor.unlock();
        }
        this.driver = null;
        if (this.driverServiceTracker != null) {
            this.driverServiceTracker.close();
        }
        logger.debug(message.deactivatingDone());
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
    private synchronized void attachDriver(final String driverId) {
        requireNonNull(driverId, message.driverPidNonNull());
        logger.debug(message.driverAttach());
        try {
            if (this.driverServiceTracker != null) {
                this.driverServiceTracker.close();
                this.driverServiceTracker = null;
            }
            final DriverTrackerCustomizer driverTrackerCustomizer = new DriverTrackerCustomizer(
                    this.context.getBundleContext(), this, driverId);
            this.driverServiceTracker = new ServiceTracker<>(this.context.getBundleContext(), Driver.class.getName(),
                    driverTrackerCustomizer);
            this.driverServiceTracker.open();
        } catch (final InvalidSyntaxException e) {
            logger.error(message.errorDriverTracking(), e);
        }
        logger.debug(message.driverAttachDone());
    }

    /**
     * Clones provided Attribute Definition by prepending the provided channel name and
     * the property separator prefix.
     *
     * @param oldAd
     *            the old Attribute Definition
     * @param channelName
     *            the name of the channel
     * @return the new attribute definition
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private Tad cloneAd(final Tad oldAd, final String channelName) {
        requireNonNull(oldAd, message.oldAdNonNull());
        requireNonNull(channelName, message.channelNameNonNull());

        final String oldAdId = oldAd.getId();
        String prefix = channelName + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value();

        final Tad result = new Tad();
        result.setId(prefix + oldAdId);
        result.setName(prefix + oldAd.getName());
        result.setCardinality(oldAd.getCardinality());
        result.setType(Tscalar.fromValue(oldAd.getType().value()));
        result.setDescription(oldAd.getDescription());
        result.setDefault(oldAd.getDefault());
        result.setMax(oldAd.getMax());
        result.setMin(oldAd.getMin());
        result.setRequired(oldAd.isRequired());
        for (final Option option : oldAd.getOption()) {
            final Toption newOption = new Toption();
            newOption.setLabel(option.getLabel());
            newOption.setValue(option.getValue());
            result.getOption().add(newOption);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public AssetConfiguration getAssetConfiguration() {
        return this.assetConfiguration;
    }

    public synchronized void setDriver(Driver driver) {
        this.driver = driver;
        if (driver != null) {
            List<ChannelRecord> readRecords = getAllReadRecords();
            hasReadChannels = !readRecords.isEmpty();
            tryPrepareRead(readRecords);
        }
    }

    public Driver getDriver() {
        return this.driver;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
        requireNonNull(this.properties, message.propertiesNonNull());
        final String componentName = this.properties.get(ConfigurationService.KURA_SERVICE_PID).toString();

        final Tocd mainOcd = new Tocd();
        mainOcd.setId(getFactoryPid());
        mainOcd.setName(message.ocdName());
        mainOcd.setDescription(message.ocdDescription());

        final Tad assetDescriptionAd = new Tad();
        assetDescriptionAd.setId(ASSET_DESC_PROP.value());
        assetDescriptionAd.setName(ASSET_DESC_PROP.value());
        assetDescriptionAd.setCardinality(0);
        assetDescriptionAd.setType(Tscalar.STRING);
        assetDescriptionAd.setDescription(message.description());
        assetDescriptionAd.setRequired(false);

        final Tad driverNameAd = new Tad();
        driverNameAd.setId(ASSET_DRIVER_PROP.value());
        driverNameAd.setName(ASSET_DRIVER_PROP.value());
        driverNameAd.setCardinality(0);
        driverNameAd.setType(Tscalar.STRING);
        driverNameAd.setDescription(message.driverName());
        driverNameAd.setRequired(true);

        mainOcd.addAD(assetDescriptionAd);
        mainOcd.addAD(driverNameAd);

        final Map<String, Object> props = CollectionUtil.newHashMap();
        for (final Map.Entry<String, Object> entry : this.properties.entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }
        ChannelDescriptor channelDescriptor = null;
        if (this.driver != null) {
            channelDescriptor = this.driver.getChannelDescriptor();
        }
        if (channelDescriptor != null) {
            List<Tad> driverSpecificChannelConfiguration = null;
            final Object descriptor = channelDescriptor.getDescriptor();
            if (descriptor instanceof List<?>) {
                driverSpecificChannelConfiguration = (List<Tad>) descriptor;
            }

            fillDriverSpecificChannelConfiguration(mainOcd, driverSpecificChannelConfiguration);
        }
        return new ComponentConfigurationImpl(componentName, mainOcd, props);
    }

    /**
     * Fills the {@code mainOcd} with the driver specific configuration provided by
     * {@code driverSpecificChannelConfiguration}
     * 
     * @param mainOcd
     *            a {@link Tocd} object.
     * @param driverSpecificChannelConfiguration
     *            the driver specific configuration.
     */
    @SuppressWarnings("unchecked")
    private void fillDriverSpecificChannelConfiguration(final Tocd mainOcd,
            final List<Tad> driverSpecificChannelConfiguration) {
        if (mainOcd == null || driverSpecificChannelConfiguration == null) {
            return;
        }

        final ChannelDescriptor basicChanneldescriptor = new BaseChannelDescriptor();
        final Object baseChannelDescriptor = basicChanneldescriptor.getDescriptor();
        if (nonNull(baseChannelDescriptor) && baseChannelDescriptor instanceof List<?>) {
            List<Tad> channelConfiguration = (List<Tad>) baseChannelDescriptor;
            channelConfiguration.addAll(driverSpecificChannelConfiguration);
            for (final Tad attribute : channelConfiguration) {
                for (final Entry<String, Channel> entry : this.assetConfiguration.getAssetChannels().entrySet()) {
                    final String channelName = entry.getKey();
                    final Tad newAttribute = cloneAd(attribute, channelName);
                    mainOcd.addAD(newAttribute);
                }
            }
        }
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
        if (kuraServicePid == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING);
        }
        return kuraServicePid;
    }

    private List<ChannelRecord> getAllReadRecords() {
        List<ChannelRecord> readRecords = new ArrayList<>();

        if (this.assetConfiguration != null) {
            for (Entry<String, Channel> e : assetConfiguration.getAssetChannels().entrySet()) {
                final Channel channel = e.getValue();
                if (channel.getType() == ChannelType.READ || channel.getType() == ChannelType.READ_WRITE) {
                    readRecords.add(channel.createReadRecord());
                }
            }
        }

        return readRecords;
    }

    /** {@inheritDoc} */
    @Override
    public List<ChannelRecord> readAllChannels() throws KuraException {
        requireNonNull(this.driver, message.driverNonNull());
        logger.debug(message.readingChannels());

        final List<ChannelRecord> channelRecords;

        this.monitor.lock();
        try {
            if (preparedRead != null) {
                channelRecords = preparedRead.execute();
            } else {
                channelRecords = getAllReadRecords();
                driver.read(channelRecords);
            }
        } catch (final ConnectionException ce) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
        } finally {
            this.monitor.unlock();
        }

        logger.debug(message.readingChannelsDone());
        return channelRecords;
    }

    /** {@inheritDoc} */
    @Override
    public List<ChannelRecord> read(final Set<String> channelNames) throws KuraException {
        requireNonNull(this.driver, message.driverNonNull());
        logger.debug(message.readingChannels());

        final List<ChannelRecord> channelRecords = new ArrayList<>(channelNames.size());
        final List<ChannelRecord> validRecords = new ArrayList<>(channelNames.size());

        final Map<String, Channel> channels = this.assetConfiguration.getAssetChannels();

        for (final String name : channelNames) {

            final Channel channel = channels.get(name);
            if (channel == null) {
                channelRecords.add(ChannelRecord.createStatusRecord(name,
                        new ChannelStatus(FAILURE, message.channelUnavailable(), null)));
                continue;
            } else if (!(channel.getType() == READ || channel.getType() == READ_WRITE)) {
                channelRecords.add(ChannelRecord.createStatusRecord(name,
                        new ChannelStatus(FAILURE, message.channelTypeNotReadable(), null)));
                continue;
            }

            final ChannelRecord record = channel.createReadRecord();
            validRecords.add(record);
            channelRecords.add(record);
        }

        if (!validRecords.isEmpty()) {
            this.monitor.lock();
            try {
                this.driver.read(validRecords);
            } catch (final ConnectionException ce) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
            } finally {
                this.monitor.unlock();
            }
        }
        logger.debug(message.readingChannelsDone());
        return channelRecords;
    }

    public boolean hasReadChannels() {
        return hasReadChannels;
    }

    /** {@inheritDoc} */
    @Override
    public void registerChannelListener(final String channelName, final ChannelListener channelListener)
            throws KuraException {
        requireNonNull(channelName, message.channelNameNonNull());
        requireNonNull(channelListener, message.listenerNonNull());
        requireNonNull(this.driver, message.driverNonNull());

        if (channelListeners.contains(channelListener)) {
            return;
        }

        logger.debug(message.registeringListener());
        final Map<String, Channel> channels = this.assetConfiguration.getAssetChannels();

        final Channel channel = channels.get(channelName.trim());

        if (channel == null) {
            throw new IllegalArgumentException(message.channelNameNotFound());
        }

        this.monitor.lock();
        try {
            this.driver.registerChannelListener(channel.getConfiguration(), channelListener);
            this.channelListeners.add(channelListener);
        } catch (final ConnectionException ce) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
        } finally {
            this.monitor.unlock();
        }
        logger.debug(message.registeringListenerDone());
    }

    /**
     * Retrieve channels from the provided properties.
     *
     * @param properties
     *            the properties containing the asset specific configuration
     */
    private void retrieveConfigurationsFromProperties(final Map<String, Object> properties) {
        logger.debug(message.retrievingConf());
        if (this.assetOptions == null) {
            this.assetOptions = new AssetOptions(properties);
        } else {
            this.assetOptions.update(properties);
        }
        if (this.assetOptions != null) {
            this.assetConfiguration = this.assetOptions.getAssetConfiguration();
        }
        logger.debug(message.retrievingConfDone());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "BaseAsset [Asset Configuration=" + this.assetConfiguration + "]";
    }

    /** {@inheritDoc} */
    @Override
    public void unregisterChannelListener(final ChannelListener channelListener) throws KuraException {
        requireNonNull(channelListener, message.listenerNonNull());
        requireNonNull(this.driver, message.driverNonNull());

        logger.debug(message.unregisteringListener());
        this.monitor.lock();
        try {
            if (this.channelListeners.contains(channelListener)) {
                try {
                    this.driver.unregisterChannelListener(channelListener);
                } catch (final ConnectionException ce) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
                }
            }
        } finally {
            this.monitor.unlock();
        }
        this.channelListeners.remove(channelListener);
        logger.debug(message.unregisteringListenerDone());
    }

    private synchronized void tryPrepareRead(List<ChannelRecord> readRecords) {
        if (this.preparedRead != null) {
            try {
                this.preparedRead.close();
            } catch (Exception e) {
                logger.warn(message.errorClosingPreparingRead(), e);
            }
            this.preparedRead = null;
        }

        if (!readRecords.isEmpty() && driver != null) {
            this.preparedRead = driver.prepareRead(readRecords);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(final List<ChannelRecord> channelRecords) throws KuraException {
        requireNonNull(this.driver, message.driverNonNull());
        logger.debug(message.writing());

        final List<ChannelRecord> validRecords = new ArrayList<>(channelRecords.size());

        final Map<String, Channel> channels = this.assetConfiguration.getAssetChannels();

        for (final ChannelRecord channelRecord : channelRecords) {
            final String channelName = channelRecord.getChannelName();

            final Channel channel = channels.get(channelName);
            if (channel == null) {
                channelRecord.setChannelStatus(new ChannelStatus(FAILURE, message.channelUnavailable(), null));
                continue;
            } else if (!(channel.getType() == WRITE || channel.getType() == READ_WRITE)) {
                channelRecord.setChannelStatus(new ChannelStatus(FAILURE, message.channelTypeNotReadable(), null));
                continue;
            }

            channelRecord.setChannelConfig(channel.getConfiguration());
            validRecords.add(channelRecord);
        }

        if (!validRecords.isEmpty()) {
            this.monitor.lock();
            try {
                this.driver.write(validRecords);
            } catch (final ConnectionException ce) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
            } finally {
                this.monitor.unlock();
            }
        }
        logger.debug(message.writingDone());
    }
}