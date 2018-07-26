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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

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
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.configuration.util.ComponentUtil;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.internal.asset.provider.AssetOptions;
import org.eclipse.kura.internal.asset.provider.DriverTrackerCustomizer;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.util.collection.CollectionUtil;
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

    private static final String DRIVER_NOT_NULL_MSG = "Driver cannot be null";

    /** The provided asset configuration wrapper instance. */
    private AssetConfiguration assetConfiguration;

    /** Container of channel listeners registered by this Asset. */
    protected final Set<ChannelListenerRegistration> channelListeners = new HashSet<>();

    private AssetOptions assetOptions;

    private ComponentContext context;

    protected volatile Driver driver;

    /** The configurable properties of this service. */
    private Map<String, Object> properties;
    private Tocd ocd;

    private ServiceTracker<Driver, Driver> driverServiceTracker;

    protected PreparedRead preparedRead;

    private boolean hasReadChannels;

    private String kuraServicePid;

    /**
     * OSGi service component callback while activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the service properties
     */
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug("Activating Asset...");
        this.context = componentContext;
        updated(properties);
        logger.debug("Activating Asset...Done");
    }

    /**
     * OSGi service component update callback.
     *
     * @param properties
     *            the service properties
     */
    public void updated(final Map<String, Object> properties) {
        logger.debug("Initializing Asset Configurations...");
        invalidateDefinition();
        this.properties = properties;
        this.kuraServicePid = (String) this.properties.get(ConfigurationService.KURA_SERVICE_PID);
        retrieveConfigurationsFromProperties(properties);
        attachDriver(this.assetConfiguration.getDriverPid());
        logger.debug("Initializing Asset Configurations...Done");
    }

    /**
     * OSGi service component callback while deactivation.
     *
     * @param context
     *            the component context
     */
    protected void deactivate(final ComponentContext context) {
        logger.debug("Release Asset Resources...");

        if (this.driverServiceTracker != null) {
            this.driverServiceTracker.close();
        }
        logger.debug("Release Asset Resources...Done");
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
        requireNonNull(driverId, "Driver PID cannot be null");
        logger.debug("Attaching driver instance...");

        if (this.driverServiceTracker != null) {
            this.driverServiceTracker.close();
            this.driverServiceTracker = null;
        }
        final DriverTrackerCustomizer driverTrackerCustomizer = new DriverTrackerCustomizer(
                this.context.getBundleContext(), this, driverId);
        this.driverServiceTracker = new ServiceTracker<>(this.context.getBundleContext(), Driver.class.getName(),
                driverTrackerCustomizer);
        this.driverServiceTracker.open();

        logger.debug("Attaching driver instance...Done");
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
        requireNonNull(oldAd, "Old Attribute Definition cannot be null");
        requireNonNull(channelName, "Channel name cannot be null");

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
        tryClosePreparedRead();
        invalidateDefinition();
        if (driver != null) {
            try {
                updateExistingProperties(driver);
            } catch (KuraException e) {
                logger.warn("Failed to update current configuration from Driver Descriptor", e);
            }
            List<ChannelRecord> readRecords = getAllReadRecords();
            this.hasReadChannels = !readRecords.isEmpty();
            tryPrepareRead(readRecords);
            tryAttachChannelListeners();
        }
    }

    public synchronized void unsetDriver() {
        tryClosePreparedRead();
        detachAllListeners();
        invalidateDefinition();
        this.driver = null;
    }

    public synchronized Driver getDriver() {
        return this.driver;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized ComponentConfiguration getConfiguration() throws KuraException {
        requireNonNull(this.properties, "Properties cannot be null");
        final String componentName = this.properties.get(ConfigurationService.KURA_SERVICE_PID).toString();

        return new ComponentConfigurationImpl(componentName, getDefinition(), new HashMap<>(this.properties));
    }

    private List<?> getDriverDescriptor() {
        return (List<?>) this.driver.getChannelDescriptor().getDescriptor();
    }

    private synchronized void invalidateDefinition() {
        this.ocd = null;
    }

    private Tocd getDefinition() {
        if (this.ocd != null) {
            return this.ocd;
        }

        final List<?> driverDescriptor;

        final Tocd newOcd = getOCD();

        try {
            driverDescriptor = getDriverDescriptor();
        } catch (Exception e) {
            logger.warn("Failed to get Driver descriptor", e);
            return newOcd;
        }

        Stream.concat(getAssetChannelDescriptor().stream(), driverDescriptor.stream()).forEach(attribute -> {
            for (final Entry<String, Channel> entry : this.assetConfiguration.getAssetChannels().entrySet()) {
                final String channelName = entry.getKey();
                if (!(attribute instanceof Tad)) {
                    return;
                }
                final Tad newAttribute = cloneAd((Tad) attribute, channelName);
                newOcd.addAD(newAttribute);
            }
        });

        this.ocd = newOcd;
        return newOcd;
    }

    @SuppressWarnings("unchecked")
    private void updateExistingProperties(final Driver driver) throws KuraException {
        if (driver == null || this.properties == null || this.assetConfiguration == null) {
            return;
        }
        Object opaqueDriverDescriptor = null;
        try {
            final ChannelDescriptor channelDescriptor = driver.getChannelDescriptor();
            if (channelDescriptor == null) {
                return;
            }
            opaqueDriverDescriptor = channelDescriptor.getDescriptor();
            if (!(opaqueDriverDescriptor instanceof List<?>)) {
                return;
            }
        } catch (Exception e) {
            logger.warn("Failed to get channel descriptor", e);
            return;
        }
        Map<String, Object> newConfiguration = null;
        final List<Tad> driverDescriptor = (List<Tad>) opaqueDriverDescriptor;
        final Tocd tempOcd = new Tocd();
        getAssetChannelDescriptor().forEach(tempOcd::addAD);
        driverDescriptor.forEach(tempOcd::addAD);
        final Map<String, Object> defaultValues = ComponentUtil.getDefaultProperties(tempOcd, this.context);
        final Map<String, Channel> channels = getAssetConfiguration().getAssetChannels();
        for (AD tad : tempOcd.getAD()) {
            if (!tad.isRequired()) {
                continue;
            }
            final String id = tad.getId();
            for (final Channel channel : channels.values()) {
                final Map<String, Object> config = channel.getConfiguration();
                if (config.get(id) == null) {
                    if (newConfiguration == null) {
                        newConfiguration = CollectionUtil.newHashMap();
                        newConfiguration.putAll(this.properties);
                    }
                    final Object defaultValue = defaultValues.get(id);
                    newConfiguration.put(channel.getName() + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value() + id,
                            defaultValue);
                }
            }
        }
        if (newConfiguration != null) {
            this.properties = newConfiguration;
            retrieveConfigurationsFromProperties(this.properties);
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
        if (this.kuraServicePid == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING);
        }
        return this.kuraServicePid;
    }

    private List<ChannelRecord> getAllReadRecords() {
        List<ChannelRecord> readRecords = new ArrayList<>();

        if (this.assetConfiguration != null) {
            for (Entry<String, Channel> e : this.assetConfiguration.getAssetChannels().entrySet()) {
                final Channel channel = e.getValue();
                if (channel.isEnabled()
                        && (channel.getType() == ChannelType.READ || channel.getType() == ChannelType.READ_WRITE)) {
                    readRecords.add(channel.createReadRecord());
                }
            }
        }

        return Collections.unmodifiableList(readRecords);
    }

    /** {@inheritDoc} */
    @Override
    public List<ChannelRecord> readAllChannels() throws KuraException {
        requireNonNull(this.driver, DRIVER_NOT_NULL_MSG);
        logger.debug("Reading asset channels...");

        final List<ChannelRecord> channelRecords;

        synchronized (this) {
            try {
                if (this.preparedRead != null) {
                    channelRecords = this.preparedRead.execute();
                } else {
                    channelRecords = getAllReadRecords();
                    this.driver.read(channelRecords);
                }
            } catch (final ConnectionException ce) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce, ce.getMessage());
            }
        }

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
        requireNonNull(this.driver, DRIVER_NOT_NULL_MSG);
        logger.debug("Reading asset channels...");

        final List<ChannelRecord> channelRecords = new ArrayList<>(channelNames.size());
        final List<ChannelRecord> validRecords = new ArrayList<>(channelNames.size());

        final Map<String, Channel> channels = this.assetConfiguration.getAssetChannels();

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
            synchronized (this) {
                try {
                    this.driver.read(validRecords);
                } catch (final ConnectionException ce) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce, ce.getMessage());
                }
            }
        }
        logger.debug("Reading asset channels...Done");
        return channelRecords;
    }

    public boolean hasReadChannels() {
        return this.hasReadChannels;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void registerChannelListener(final String channelName, final ChannelListener channelListener)
            throws KuraException {
        requireNonNull(channelName, "Channel name cannot be null");
        requireNonNull(channelListener, "Asset Listener cannot be null");

        logger.debug("Registering Channel Listener for monitoring...");
        final Map<String, Channel> channels = this.assetConfiguration.getAssetChannels();

        final Channel channel = channels.get(channelName);

        if (channel == null) {
            throw new IllegalArgumentException("Channel not found");
        }

        final ChannelListenerRegistration reg = new ChannelListenerRegistration(channel.getName(), channelListener);

        if (this.channelListeners.contains(reg)) {
            return;
        }

        this.channelListeners.add(reg);

        if (this.driver != null && channel.isEnabled()) {
            tryAttachListener(channel, reg);
        }
    }

    /**
     * Retrieve channels from the provided properties.
     *
     * @param properties
     *            the properties containing the asset specific configuration
     */
    private void retrieveConfigurationsFromProperties(final Map<String, Object> properties) {
        logger.debug("Retrieving configurations from the properties...");
        if (this.assetOptions == null) {
            this.assetOptions = new AssetOptions(properties);
        } else {
            this.assetOptions.update(properties);
        }
        this.assetConfiguration = this.assetOptions.getAssetConfiguration();
        logger.debug("Retrieving configurations from the properties...Done");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "BaseAsset [Asset Configuration=" + this.assetConfiguration + "]";
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void unregisterChannelListener(final ChannelListener channelListener) throws KuraException {
        requireNonNull(channelListener, "Asset Listener cannot be null");

        final Iterator<ChannelListenerRegistration> i = this.channelListeners.iterator();

        while (i.hasNext()) {
            final ChannelListenerRegistration reg = i.next();
            if (reg.listener != channelListener) {
                continue;
            }
            if (this.driver != null) {
                tryDetachListener(reg);
                i.remove();
            } else {
                reg.isValid = false;
            }
        }
    }

    private synchronized void tryPrepareRead(List<ChannelRecord> readRecords) {
        if (this.preparedRead != null) {
            try {
                this.preparedRead.close();
            } catch (Exception e) {
                logger.warn("Failed to close prepared read", e);
            }
            onPreparedReadReleased(preparedRead);
            this.preparedRead = null;
        }

        if (!readRecords.isEmpty() && this.driver != null) {
            try {
                this.preparedRead = this.driver.prepareRead(readRecords);
                onPreparedReadCreated(preparedRead);
            } catch (Exception e) {
                logger.warn("Failed to get prepared read", e);
            }
        }
    }

    protected void onPreparedReadCreated(PreparedRead preparedRead) {
        // intended to be overridden by sublcasses
    }

    protected void onPreparedReadReleased(PreparedRead preparedRead) {
        // intended to be overridden by sublcasses
    }

    /** {@inheritDoc} */
    @Override
    public void write(final List<ChannelRecord> channelRecords) throws KuraException {
        requireNonNull(this.driver, DRIVER_NOT_NULL_MSG);
        logger.debug("Writing to channels...");

        final List<ChannelRecord> validRecords = new ArrayList<>(channelRecords.size());

        final Map<String, Channel> channels = this.assetConfiguration.getAssetChannels();

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
            synchronized (this) {
                try {
                    this.driver.write(validRecords);
                } catch (final ConnectionException ce) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce, ce.getMessage());
                }
            }
        }
        logger.debug("Writing to channels...Done");
    }

    private void tryClosePreparedRead() {
        if (this.preparedRead != null) {
            try {
                this.preparedRead.close();
            } catch (Exception e) {
                logger.warn("Failed to close prepared read", e);
            }
            this.preparedRead = null;
        }
    }

    protected void tryAttachListener(Channel channel, ChannelListenerRegistration registration) {
        try {
            logger.debug("Registering Channel Listener for monitoring...");
            this.driver.registerChannelListener(channel.getConfiguration(), registration.listener);
            logger.debug("Registering Channel Listener for monitoring...Done");
        } catch (Exception e) {
            logger.warn("Failed to register channel listener", e);
        }
    }

    protected void tryDetachListener(ChannelListenerRegistration registration) {
        try {
            logger.debug("Unregistering Asset Listener...");
            this.driver.unregisterChannelListener(registration.listener);
            logger.debug("Unregistering Asset Listener...Done");
        } catch (Exception e) {
            logger.warn("Failed to unregister channel listener", e);
        }
    }

    protected void detachAllListeners() {
        final Iterator<ChannelListenerRegistration> i = this.channelListeners.iterator();
        while (i.hasNext()) {
            tryDetachListener(i.next());
        }
    }

    protected boolean isChannelListenerValid(final Channel channel, final ChannelListenerRegistration reg) {
        return channel != null && reg.isValid;
    }

    protected void tryAttachChannelListeners() {
        final Map<String, Channel> channels = this.assetConfiguration.getAssetChannels();
        final Iterator<ChannelListenerRegistration> i = this.channelListeners.iterator();
        while (i.hasNext()) {
            final ChannelListenerRegistration reg = i.next();
            final Channel channel = channels.get(reg.channelName);
            if (!isChannelListenerValid(channel, reg)) {
                i.remove();
            } else if (channel.isEnabled()) {
                tryAttachListener(channel, reg);
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

    protected static class ChannelListenerRegistration {

        private final String channelName;
        private final ChannelListener listener;
        private boolean isValid;

        public ChannelListenerRegistration(String channelName, ChannelListener listener) {
            this.channelName = channelName;
            this.listener = listener;
            this.isValid = true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (this.channelName == null ? 0 : this.channelName.hashCode());
            result = prime * result + (this.listener == null ? 0 : this.listener.hashCode());
            return result;
        }

        public String getChannelName() {
            return this.channelName;
        }

        public ChannelListener getChannelListener() {
            return this.listener;
        }

        public boolean isValid() {
            return this.isValid;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ChannelListenerRegistration other = (ChannelListenerRegistration) obj;
            if (this.channelName == null) {
                if (other.channelName != null) {
                    return false;
                }
            } else if (!this.channelName.equals(other.channelName)) {
                return false;
            }
            return other.listener == this.listener;
        }
    }
}
