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
import static org.eclipse.kura.asset.AssetConstants.ASSET_DESC_PROP;
import static org.eclipse.kura.asset.AssetConstants.ASSET_DRIVER_PROP;
import static org.eclipse.kura.asset.AssetFlag.FAILURE;
import static org.eclipse.kura.asset.AssetFlag.SUCCESS;
import static org.eclipse.kura.asset.ChannelType.READ;
import static org.eclipse.kura.asset.ChannelType.READ_WRITE;
import static org.eclipse.kura.asset.ChannelType.WRITE;
import static org.eclipse.kura.driver.DriverConstants.CHANNEL_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.annotation.Extensible;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetConstants;
import org.eclipse.kura.asset.AssetEvent;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.AssetStatus;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.ChannelType;
import org.eclipse.kura.asset.listener.AssetListener;
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
import org.eclipse.kura.driver.DriverConstants;
import org.eclipse.kura.driver.DriverEvent;
import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.internal.asset.provider.AssetOptions;
import org.eclipse.kura.internal.asset.provider.DriverTrackerCustomizer;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class BaseAsset is basic implementation of {@code Asset}.
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

    /** Container of mapped asset listeners and drivers listener. */
    private final Map<AssetListener, DriverListener> assetListeners;

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
        this.assetListeners = CollectionUtil.newConcurrentHashMap();
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
                this.driver.disconnect();
            }
        } catch (final ConnectionException e) {
            logger.error(message.errorDriverDisconnection(), e);
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
            List<DriverRecord> readRecords = getAllReadRecords();
            hasReadChannels = !readRecords.isEmpty();
            tryPrepareRead(readRecords);
        }
    }

    public Driver getDriver() {
        return this.driver;
    }

    /**
     * Retrieves the specific asset record by driver record from the list of
     * provided asset records
     *
     * @param assetRecords
     *            the provided list of driver records
     * @param driverRecord
     *            the specific driver record
     * @return the found asset record or null
     * @throws NullPointerException
     *             if any of the arguments is null
     * @throws IllegalArgumentException
     *             the provided list is empty
     */
    private AssetRecord getAssetRecordByDriverRecord(final List<AssetRecord> assetRecords,
            final DriverRecord driverRecord) {
        requireNonNull(assetRecords, message.assetRecordsNonNull());
        if (assetRecords.isEmpty()) {
            throw new IllegalArgumentException(message.assetRecordsNonEmpty());
        }
        requireNonNull(driverRecord, message.driverRecordNonNull());

        for (final AssetRecord assetRecord : assetRecords) {
            final Map<String, Object> driverConfig = driverRecord.getChannelConfig();
            if (driverConfig != null) {
                final String channelName = driverConfig.get(CHANNEL_NAME.value()).toString();
                if (channelName.equals(assetRecord.getChannelName())) {
                    return assetRecord;
                }
            }
        }
        return null;
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

    /**
     * Prepares the provided asset record with the relevant values from the
     * provided asset record
     *
     * @param driverRecord
     *            the provided driver record
     * @param assetRecord
     *            the provided asset record
     * @throws KuraException
     *             if any driver flag is error specific
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    private void prepareAssetRecord(final DriverRecord driverRecord, final AssetRecord assetRecord)
            throws KuraException {
        requireNonNull(driverRecord, message.driverRecordNonNull());
        requireNonNull(assetRecord, message.assetRecordNonNull());

        final DriverStatus status = driverRecord.getDriverStatus();
        final DriverFlag driverFlag = status.getDriverFlag();

        AssetStatus assetStatus;
        if (assetRecord.getAssetStatus() == null) {
            switch (driverFlag) {
            case READ_SUCCESSFUL:
            case WRITE_SUCCESSFUL:
                assetStatus = new AssetStatus(SUCCESS);
                assetRecord.setAssetStatus(assetStatus);
                break;
            default:
                assetStatus = new AssetStatus(FAILURE, status.getExceptionMessage(), status.getException());
                assetRecord.setAssetStatus(assetStatus);
                break;
            }
        }
        assetRecord.setTimestamp(driverRecord.getTimestamp());
        final TypedValue<?> recordValue = driverRecord.getValue();
        if (recordValue != null) {
            assetRecord.setValue(recordValue);
        }
    }

    private List<DriverRecord> getAllReadRecords() {
        List<DriverRecord> readRecords = new ArrayList<DriverRecord>();

        if (this.assetConfiguration != null) {
            for (Entry<String, Channel> e : assetConfiguration.getAssetChannels().entrySet()) {
                final Channel channel = e.getValue();
                if (channel.getType() == ChannelType.READ || channel.getType() == ChannelType.READ_WRITE) {
                    readRecords.add(createDriverRecordForChannelRead(channel));
                }
            }
        }

        return readRecords;
    }

    private DriverRecord createDriverRecordForChannelRead(final Channel channel) {
        final DriverRecord driverRecord = new DriverRecord();

        driverRecord.setChannelConfig(channel.getConfiguration());
        return driverRecord;
    }

    private AssetRecord createAssetRecordFromDriverRecord(DriverRecord driverRecord) throws KuraException {
        String channelName = (String) driverRecord.getChannelConfig().get(DriverConstants.CHANNEL_NAME.value());

        AssetRecord assetRecord = new AssetRecord(channelName);
        prepareAssetRecord(driverRecord, assetRecord);
        return assetRecord;
    }

    /** {@inheritDoc} */
    @Override
    public List<AssetRecord> readAllChannels() throws KuraException {
        requireNonNull(this.driver, message.driverNonNull());
        logger.debug(message.readingChannels());

        final List<AssetRecord> assetRecords = CollectionUtil.newArrayList();
        final List<DriverRecord> driverRecords;

        this.monitor.lock();
        try {
            if (preparedRead != null) {
                driverRecords = preparedRead.execute();
            } else {
                driverRecords = getAllReadRecords();
                driver.read(driverRecords);
            }
        } catch (final ConnectionException ce) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
        } finally {
            this.monitor.unlock();
        }

        if (driverRecords != null) {
            for (final DriverRecord driverRecord : driverRecords) {
                assetRecords.add(createAssetRecordFromDriverRecord(driverRecord));
            }
        }
        logger.debug(message.readingChannelsDone());
        return assetRecords;
    }

    /** {@inheritDoc} */
    @Override
    public List<AssetRecord> read(final Set<String> channelNames) throws KuraException {
        requireNonNull(this.driver, message.driverNonNull());
        logger.debug(message.readingChannels());
        final List<AssetRecord> assetRecords = CollectionUtil.newArrayList();
        final List<DriverRecord> driverRecords = CollectionUtil.newArrayList();

        // preparing asset records
        for (final String channelName : channelNames) {
            assetRecords.add(new AssetRecord(channelName));
        }
        final Map<String, Channel> channels = this.assetConfiguration.getAssetChannels();
        for (final AssetRecord assetRecord : assetRecords) {
            final String name = assetRecord.getChannelName();

            final Channel channel = channels.get(name);
            if (channel == null) {
                assetRecord.setAssetStatus(new AssetStatus(FAILURE, message.channelUnavailable(), null));
                continue;
            } else if (!(channel.getType() == READ || channel.getType() == READ_WRITE)) {
                assetRecord.setAssetStatus(new AssetStatus(FAILURE, message.channelTypeNotReadable(), null));
                continue;
            }

            driverRecords.add(createDriverRecordForChannelRead(channel));
        }

        if (!driverRecords.isEmpty()) {
            this.monitor.lock();
            try {
                this.driver.read(driverRecords);
            } catch (final ConnectionException ce) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
            } finally {
                this.monitor.unlock();
            }

            for (final DriverRecord driverRecord : driverRecords) {
                prepareAssetRecord(driverRecord, getAssetRecordByDriverRecord(assetRecords, driverRecord));
            }
        }
        logger.debug(message.readingChannelsDone());
        return assetRecords;
    }

    public boolean hasReadChannels() {
        return hasReadChannels;
    }

    /** {@inheritDoc} */
    @Override
    public void registerAssetListener(final String channelName, final AssetListener assetListener)
            throws KuraException {
        requireNonNull(channelName, message.channelNameNonNull());
        requireNonNull(assetListener, message.listenerNonNull());
        requireNonNull(this.driver, message.driverNonNull());

        logger.debug(message.registeringListener());
        final Map<String, Channel> channels = this.assetConfiguration.getAssetChannels();

        final Channel channel = channels.get(channelName.trim());

        if (channel == null) {
            throw new IllegalArgumentException(message.channelNameNotFound());
        }

        /**
         * This is a basic driver listener used to listen for driver events so
         * that it can be propagated upwards to the respective asset listener
         *
         * @see AssetListener
         * @see DriverListener
         * @see AssetEvent
         * @see DriverEvent
         */
        final class BaseDriverListener implements DriverListener {

            /** The asset listener instance. */
            private final AssetListener assetListener;

            /**
             * Instantiates a new base driver listener.
             *
             * @param assetListener
             *            the asset listener
             * @throws NullPointerException
             *             if the argument is null
             */
            BaseDriverListener(final AssetListener assetListener) {
                requireNonNull(assetListener, message.listenerNonNull());
                this.assetListener = assetListener;
            }

            /** {@inheritDoc} */
            @Override
            public void onDriverEvent(final DriverEvent event) {
                requireNonNull(event, message.driverEventNonNull());
                final DriverRecord driverRecord = event.getDriverRecord();
                final Map<String, Object> driverRecordConf = driverRecord.getChannelConfig();
                String channelName = (String) driverRecordConf.get(CHANNEL_NAME.value());
                if (channelName == null) {
                    return;
                }
                final AssetRecord assetRecord = new AssetRecord(channelName);
                try {
                    prepareAssetRecord(driverRecord, assetRecord);
                } catch (final KuraException e) {
                    logger.error(message.errorPreparingAssetRecord(), e);
                }
                final AssetEvent assetEvent = new AssetEvent(assetRecord);
                this.assetListener.onAssetEvent(assetEvent);
            }
        }

        final DriverListener driverListener = new BaseDriverListener(assetListener);
        this.assetListeners.put(assetListener, driverListener);

        this.monitor.lock();
        try {
            this.driver.registerDriverListener(channel.getConfiguration(), driverListener);
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
    public void unregisterAssetListener(final AssetListener assetListener) throws KuraException {
        requireNonNull(assetListener, message.listenerNonNull());
        requireNonNull(this.driver, message.driverNonNull());

        logger.debug(message.unregisteringListener());
        this.monitor.lock();
        try {
            if (this.assetListeners.containsKey(assetListener)) {
                try {
                    this.driver.unregisterDriverListener(this.assetListeners.get(assetListener));
                } catch (final ConnectionException ce) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
                }
            }
        } finally {
            this.monitor.unlock();
        }
        this.assetListeners.remove(assetListener);
        logger.debug(message.unregisteringListenerDone());
    }

    private synchronized void tryPrepareRead(List<DriverRecord> readRecords) {
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
    public List<AssetRecord> write(final List<AssetRecord> assetRecords) throws KuraException {
        requireNonNull(this.driver, message.driverNonNull());
        logger.debug(message.writing());
        final List<DriverRecord> driverRecords = CollectionUtil.newArrayList();
        final Map<String, Channel> channels = this.assetConfiguration.getAssetChannels();
        for (final AssetRecord assetRecord : assetRecords) {
            final String channelName = assetRecord.getChannelName();

            final Channel channel = channels.get(channelName);
            if (channel == null) {
                assetRecord.setAssetStatus(new AssetStatus(FAILURE, message.channelUnavailable(), null));
                continue;
            } else if (!(channel.getType() == WRITE || channel.getType() == READ_WRITE)) {
                assetRecord.setAssetStatus(new AssetStatus(FAILURE, message.channelTypeNotReadable(), null));
                continue;
            }

            final DriverRecord driverRecord = new DriverRecord();
            driverRecord.setChannelConfig(channel.getConfiguration());

            final TypedValue<?> value = assetRecord.getValue();
            if (value != null) {
                driverRecord.setValue(value);
            }
            driverRecords.add(driverRecord);
        }

        if (!driverRecords.isEmpty()) {
            this.monitor.lock();
            try {
                this.driver.write(driverRecords);
            } catch (final ConnectionException ce) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
            } finally {
                this.monitor.unlock();
            }

            for (final DriverRecord driverRecord : driverRecords) {
                final AssetRecord assetRecord = getAssetRecordByDriverRecord(assetRecords, driverRecord);
                requireNonNull(assetRecord, message.assetRecordNonNull());
                prepareAssetRecord(driverRecord, assetRecord);
            }
        }
        logger.debug(message.writingDone());
        return assetRecords;
    }
}