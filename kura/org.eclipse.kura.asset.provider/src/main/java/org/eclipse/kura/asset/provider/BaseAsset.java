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
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.asset.provider;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.asset.AssetConstants.ASSET_DESC_PROP;
import static org.eclipse.kura.asset.AssetConstants.ASSET_DRIVER_PROP;
import static org.eclipse.kura.asset.AssetConstants.CHANNEL_PROPERTY_POSTFIX;
import static org.eclipse.kura.asset.AssetConstants.CHANNEL_PROPERTY_PREFIX;
import static org.eclipse.kura.asset.AssetConstants.DRIVER_PROPERTY_POSTFIX;
import static org.eclipse.kura.asset.AssetConstants.NAME;
import static org.eclipse.kura.asset.AssetConstants.SEVERITY_LEVEL;
import static org.eclipse.kura.asset.AssetConstants.TYPE;
import static org.eclipse.kura.asset.AssetConstants.VALUE_TYPE;
import static org.eclipse.kura.asset.AssetFlag.FAILURE;
import static org.eclipse.kura.asset.AssetFlag.SUCCESS;
import static org.eclipse.kura.asset.ChannelType.READ;
import static org.eclipse.kura.asset.ChannelType.READ_WRITE;
import static org.eclipse.kura.asset.ChannelType.WRITE;
import static org.eclipse.kura.driver.DriverConstants.CHANNEL_ID;
import static org.eclipse.kura.driver.DriverConstants.CHANNEL_VALUE_TYPE;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.annotation.Extensible;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetEvent;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.AssetStatus;
import org.eclipse.kura.asset.Channel;
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
import org.eclipse.kura.driver.DriverEvent;
import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.internal.asset.provider.AssetOptions;
import org.eclipse.kura.internal.asset.provider.DriverTrackerCustomizer;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.util.base.ThrowableUtil;
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

    /** The Logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(BaseAsset.class);

    /** Localization Resource. */
    private static final AssetMessages message = LocalizationAdapter.adapt(AssetMessages.class);

    /** The provided asset configuration wrapper instance. */
    protected AssetConfiguration assetConfiguration;

    /** Container of mapped asset listeners and drivers listener. */
    private final Map<AssetListener, DriverListener> assetListeners;

    /** The provided asset options instance. */
    private AssetOptions assetOptions;

    /** The service component context. */
    private ComponentContext context;

    /** The Driver instance. */
    public volatile Driver driver;

    /** Synchronization Monitor for driver specific operations. */
    private final Lock monitor;

    /** The configurable properties of this asset. */
    private Map<String, Object> properties;

    /** Asset Driver Tracker. */
    private ServiceTracker<Driver, Driver> serviceTracker;

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
    protected synchronized void activate(final ComponentContext componentContext,
            final Map<String, Object> properties) {
        logger.debug(message.activating());
        this.context = componentContext;
        this.properties = properties;
        this.retrieveConfigurationsFromProperties(properties);
        this.attachDriver(this.assetConfiguration.getDriverPid());
        logger.debug(message.activatingDone());
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
            final DriverTrackerCustomizer driverTrackerCustomizer = new DriverTrackerCustomizer(
                    this.context.getBundleContext(), this, driverId);
            this.serviceTracker = new ServiceTracker<Driver, Driver>(this.context.getBundleContext(),
                    Driver.class.getName(), driverTrackerCustomizer);
            this.serviceTracker.open();
        } catch (final InvalidSyntaxException e) {
            logger.error(ThrowableUtil.stackTraceAsString(e));
        }
        logger.debug(message.driverAttachDone());
    }

    /**
     * Clones provided Attribute Definition by prepending the provided prefix.
     *
     * @param oldAd
     *            the old Attribute Definition
     * @param prefix
     *            the prefix to be prepended (this will be in the format of
     *            {@code x.CH.} or {@code x.CH.DRIVER.} where {@code x} is
     *            channel identifier number. {@code x.CH.} will be used for the
     *            channel specific properties except the driver specific
     *            properties. The driver specific properties in the channel will
     *            use the {@code x.CH.DRIVER.} prefix)
     * @return the new attribute definition
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private Tad cloneAd(final Tad oldAd, final String prefix) {
        requireNonNull(oldAd, message.oldAdNonNull());
        requireNonNull(prefix, message.adPrefixNonNull());

        String pref = prefix;
        final String oldAdId = oldAd.getId();
        if (this.isDriverAttrbuteDefinition(oldAdId)) {
            pref = prefix + DRIVER_PROPERTY_POSTFIX.value() + CHANNEL_PROPERTY_POSTFIX.value();
        }
        final Tad result = new Tad();
        result.setId(pref + oldAdId);
        result.setName(pref + oldAd.getName());
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

    /**
     * OSGi service component callback while deactivation.
     *
     * @param context
     *            the component context
     */
    protected synchronized void deactivate(final ComponentContext context) {
        logger.debug(message.deactivating());
        this.monitor.lock();
        try {
            if (this.driver != null) {
                this.driver.disconnect();
            }
        } catch (final ConnectionException e) {
            logger.error(message.errorDriverDisconnection() + ThrowableUtil.stackTraceAsString(e));
        } finally {
            this.monitor.unlock();
        }
        this.driver = null;
        if (this.serviceTracker != null) {
            this.serviceTracker.close();
        }
        logger.debug(message.deactivatingDone());
    }

    /** {@inheritDoc} */
    @Override
    public AssetConfiguration getAssetConfiguration() {
        return this.assetConfiguration;
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
                final String chId = driverConfig.get(CHANNEL_ID.value()).toString();
                final long channelId = Long.parseLong(chId);
                if (channelId == assetRecord.getChannelId()) {
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
        final String componentName = this.context.getProperties().get(ConfigurationService.KURA_SERVICE_PID).toString();

        final Tocd mainOcd = new Tocd();
        mainOcd.setId(this.getFactoryPid());
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

        final Tad severityLevelAd = new Tad();
        severityLevelAd.setId(SEVERITY_LEVEL.value());
        severityLevelAd.setName(SEVERITY_LEVEL.value());
        // default severity level is SEVERE
        severityLevelAd.setDefault(message.severe());
        severityLevelAd.setCardinality(0);
        severityLevelAd.setType(Tscalar.STRING);
        severityLevelAd.setDescription(message.severityLevelDesc());
        severityLevelAd.setRequired(true);

        final Toption severeLevel = new Toption();
        severeLevel.setValue(message.severe());
        severeLevel.setLabel(message.severe());
        severityLevelAd.getOption().add(severeLevel);

        final Toption configLevel = new Toption();
        configLevel.setValue(message.error());
        configLevel.setLabel(message.error());
        severityLevelAd.getOption().add(configLevel);

        final Toption infoLevel = new Toption();
        infoLevel.setValue(message.info());
        infoLevel.setLabel(message.info());
        severityLevelAd.getOption().add(infoLevel);

        final Toption errorLevel = new Toption();
        errorLevel.setValue(message.config());
        errorLevel.setLabel(message.config());
        severityLevelAd.getOption().add(errorLevel);

        mainOcd.addAD(assetDescriptionAd);
        mainOcd.addAD(driverNameAd);
        mainOcd.addAD(severityLevelAd);

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
            if (driverSpecificChannelConfiguration != null) {
                final ChannelDescriptor basicChanneldescriptor = new BaseChannelDescriptor();
                List<Tad> channelConfiguration = null;
                final Object baseChannelDescriptor = basicChanneldescriptor.getDescriptor();
                if (baseChannelDescriptor instanceof List<?>) {
                    channelConfiguration = (List<Tad>) baseChannelDescriptor;
                }
                if (channelConfiguration != null) {
                    channelConfiguration.addAll(driverSpecificChannelConfiguration);
                }
                for (final Tad attribute : channelConfiguration) {
                    final Set<String> channelPrefixes = this
                            .retrieveChannelPrefixes(this.assetConfiguration.getAssetChannels());
                    for (final String prefix : channelPrefixes) {
                        final Tad newAttribute = this.cloneAd(attribute, prefix);
                        mainOcd.addAD(newAttribute);
                    }
                }
            }
        }
        return new ComponentConfigurationImpl(componentName, mainOcd, props);
    }

    /**
     * Return the Factory PID of the component
     *
     * @return the factory PID
     */
    protected String getFactoryPid() {
        return CONF_PID;
    }

    /**
     * Checks if the provided attribute definition belongs to a driver attribute
     * definition
     *
     * @param oldAdId
     *            the attribute ID to check
     * @return true if the attribute definition belongs to a driver, otherwise
     *         false
     * @throws NullPointerException
     *             if the argument is null
     */
    private boolean isDriverAttrbuteDefinition(final String oldAdId) {
        requireNonNull(oldAdId, message.oldAdNonNull());
        return (oldAdId != ASSET_DESC_PROP.value()) && (oldAdId != ASSET_DRIVER_PROP.value())
                && (oldAdId != NAME.value()) && (oldAdId != TYPE.value()) && (oldAdId != VALUE_TYPE.value())
                && (oldAdId != SEVERITY_LEVEL.value());
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

    /** {@inheritDoc} */
    @Override
    public List<AssetRecord> read(final List<Long> channelIds) throws KuraException {
        requireNonNull(this.driver, message.driverNonNull());
        logger.debug(message.readingChannels());
        final List<AssetRecord> assetRecords = CollectionUtil.newArrayList();
        final List<DriverRecord> driverRecords = CollectionUtil.newArrayList();

        // preparing asset records
        for (final long channelId : channelIds) {
            assetRecords.add(new AssetRecord(channelId));
        }
        final Map<Long, Channel> channels = this.assetConfiguration.getAssetChannels();
        for (final AssetRecord assetRecord : assetRecords) {
            final long id = assetRecord.getChannelId();
            final Map<String, Object> channelConfiguration = CollectionUtil.newHashMap();

            final Channel channel = channels.get(id);
            if (channel == null) {
                assetRecord.setAssetStatus(new AssetStatus(FAILURE, message.channelUnavailable(), null));
            } else if (!((channel.getType() == READ) || (channel.getType() == READ_WRITE))) {
                assetRecord.setAssetStatus(new AssetStatus(FAILURE, message.channelTypeNotReadable(), null));
            }

            final DriverRecord driverRecord = new DriverRecord();
            if (channel != null) {
                channelConfiguration.putAll(channel.getConfiguration());
                channelConfiguration.put(CHANNEL_VALUE_TYPE.value(), channel.getValueType());
            }
            channelConfiguration.put(CHANNEL_ID.value(), assetRecord.getChannelId());
            driverRecord.setChannelConfig(channelConfiguration);
            driverRecords.add(driverRecord);
        }

        this.monitor.lock();
        try {
            this.driver.read(driverRecords);
        } catch (final ConnectionException ce) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
        } finally {
            this.monitor.unlock();
        }

        for (final DriverRecord driverRecord : driverRecords) {
            this.prepareAssetRecord(driverRecord, this.getAssetRecordByDriverRecord(assetRecords, driverRecord));
        }
        logger.debug(message.readingChannelsDone());
        return assetRecords;
    }

    /** {@inheritDoc} */
    @Override
    public void registerAssetListener(final long channelId, final AssetListener assetListener) throws KuraException {
        if (channelId <= 0) {
            throw new IllegalArgumentException(message.channelIdNotLessThanZero());
        }
        requireNonNull(assetListener, message.listenerNonNull());
        requireNonNull(this.driver, message.driverNonNull());

        logger.debug(message.registeringListener());
        final Map<Long, Channel> channels = this.assetConfiguration.getAssetChannels();

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
                long channelId = 0;
                if (driverRecordConf.containsKey(CHANNEL_ID.value())) {
                    channelId = Long.parseLong(driverRecordConf.get(CHANNEL_ID.value()).toString());
                }
                final AssetRecord assetRecord = new AssetRecord(channelId);
                try {
                    BaseAsset.this.prepareAssetRecord(driverRecord, assetRecord);
                } catch (final KuraException e) {
                    logger.error(message.errorPreparingAssetRecord() + ThrowableUtil.stackTraceAsString(e));
                }
                final AssetEvent assetEvent = new AssetEvent(assetRecord);
                this.assetListener.onAssetEvent(assetEvent);
            }
        }
        final Channel channel = channels.get(channelId);
        // Copy the configuration of the channel and put the channel ID and
        // channel value type
        final Map<String, Object> channelConf = CollectionUtil.newHashMap(channel.getConfiguration());
        channelConf.put(CHANNEL_ID.value(), channel.getId());
        channelConf.put(CHANNEL_VALUE_TYPE.value(), channel.getValueType());

        final DriverListener driverListener = new BaseDriverListener(assetListener);
        this.assetListeners.put(assetListener, driverListener);

        this.monitor.lock();
        try {
            this.driver.registerDriverListener(channelConf, driverListener);
        } catch (final ConnectionException ce) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
        } finally {
            this.monitor.unlock();
        }
        logger.debug(message.registeringListenerDone());
    }

    /**
     * Retrieves the set of prefixes of the channels from the map of channels.
     *
     * @param channels
     *            the properties to parse
     * @return the list of channel IDs
     * @throws NullPointerException
     *             if the argument is null
     */
    private Set<String> retrieveChannelPrefixes(final Map<Long, Channel> channels) {
        requireNonNull(channels, message.propertiesNonNull());
        final Set<String> channelPrefixes = CollectionUtil.newHashSet();
        for (final Map.Entry<Long, Channel> entry : channels.entrySet()) {
            final Long key = entry.getKey();
            final String prefix = key + CHANNEL_PROPERTY_POSTFIX.value() + CHANNEL_PROPERTY_PREFIX.value()
                    + CHANNEL_PROPERTY_POSTFIX.value();
            channelPrefixes.add(prefix);
        }
        return channelPrefixes;
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
        if ((this.assetConfiguration == null) && (this.assetOptions != null)) {
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

    /**
     * OSGi service component callback while updation.
     *
     * @param properties
     *            the service properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug(message.updating());
        this.retrieveConfigurationsFromProperties(properties);
        this.attachDriver(this.assetConfiguration.getDriverPid());
        logger.debug(message.updatingDone());

    }

    /** {@inheritDoc} */
    @Override
    public List<AssetRecord> write(final List<AssetRecord> assetRecords) throws KuraException {
        requireNonNull(this.driver, message.driverNonNull());
        logger.debug(message.writing());
        final List<DriverRecord> driverRecords = CollectionUtil.newArrayList();
        final Map<Long, Channel> channels = this.assetConfiguration.getAssetChannels();
        for (final AssetRecord assetRecord : assetRecords) {
            final long id = assetRecord.getChannelId();
            final Map<String, Object> channelConfiguration = CollectionUtil.newHashMap();

            final Channel channel = channels.get(id);
            if (channel == null) {
                assetRecord.setAssetStatus(new AssetStatus(FAILURE, message.channelUnavailable(), null));
            } else if (!((channel.getType() == WRITE) || (channel.getType() == READ_WRITE))) {
                assetRecord.setAssetStatus(new AssetStatus(FAILURE, message.channelTypeNotReadable(), null));
            }

            final DriverRecord driverRecord = new DriverRecord();
            if (channel != null) {
                channelConfiguration.putAll(channel.getConfiguration());
                channelConfiguration.put(CHANNEL_VALUE_TYPE.value(), channel.getValueType());
            }
            channelConfiguration.put(CHANNEL_ID.value(), assetRecord.getChannelId());
            driverRecord.setChannelConfig(channelConfiguration);
            driverRecords.add(driverRecord);

            final TypedValue<?> value = assetRecord.getValue();
            if (value != null) {
                driverRecord.setValue(value);
            }
            driverRecords.add(driverRecord);
        }

        this.monitor.lock();
        try {
            this.driver.write(driverRecords);
        } catch (final ConnectionException ce) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, ce);
        } finally {
            this.monitor.unlock();
        }

        for (final DriverRecord driverRecord : driverRecords) {
            final AssetRecord assetRecord = this.getAssetRecordByDriverRecord(assetRecords, driverRecord);
            requireNonNull(assetRecord, message.assetRecordNonNull());
            this.prepareAssetRecord(driverRecord, assetRecord);
        }
        logger.debug(message.writingDone());
        return assetRecords;
    }

}