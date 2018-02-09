/*******************************************************************************
f * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.localization.resources;

import com.github.rodionmoiseev.c10n.annotations.En;

/**
 * WireMessages is considered to be a localization resource for
 * {@code Wire Internal} bundle. It contains all the necessary translation for
 * every string literals mentioned in {@code Wire Internal} bundle.
 */
public interface WireMessages {

    @En("Activating Cloud Publisher Wire Component...")
    public String activatingCloudPublisher();

    @En("Activating Cloud Publisher Wire Component...Done")
    public String activatingCloudPublisherDone();

    @En("Activating Cloud Subscriber Wire Component...")
    public String activatingCloudSubscriber();

    @En("Activating Cloud Subscriber Wire Component...Done")
    public String activatingCloudSubscriberDone();

    @En("Activating DB Wire Record Filter...")
    public String activatingFilter();

    @En("Activating DB Wire Record Filter...Done")
    public String activatingFilterDone();

    @En("Activating Logger Wire Component...")
    public String activatingLogger();

    @En("Activating Logger Wire Component...Done")
    public String activatingLoggerDone();

    @En("Activating Regex Filter...")
    public String activatingRegexFilter();

    @En("Activating Regex Filter...Done")
    public String activatingRegexFilterDone();

    @En("Activating DB Wire Record Store...")
    public String activatingStore();

    @En("Activating DB Wire Record Store...Done")
    public String activatingStoreDone();

    @En("Activating Timer...")
    public String activatingTimer();

    @En("Activating Timer...Done")
    public String activatingTimerDone();

    @En("Activating Fifo...")
    public String activatingFifo();

    @En("Activating Fifo...Done")
    public String activatingFifoDone();

    @En("Activating Wire Asset...")
    public String activatingWireAsset();

    @En("Activating Wire Asset...Done")
    public String activatingWireAssetDone();

    @En("Activating Wire Service...")
    public String activatingWireService();

    @En("Activating Wire Service...Done")
    public String activatingWireServiceDone();

    @En("Adding Wire Components....")
    public String addingWireComponent();

    @En("Adding Wire Components....Done")
    public String addingWireComponentDone();

    @En("altitude")
    public String altitude();

    @En("asset_flag")
    public String assetFlag();

    @En("asset_name")
    public String assetName();

    @En("Channel Records cannot be empty")
    public String channelRecordsNonEmpty();

    @En("List of Channel Records cannot be null")
    public String channelRecordsNonNull();

    @En("Bundle context cannot be null")
    public String bundleContextNonNull();

    @En("channel_id")
    public String channelId();

    @En("List of Channel IDs cannot be null")
    public String channelIdsNonNull();

    @En("channel_name")
    public String channelName();

    @En("Channel cannot be null")
    public String channelNonNull();

    @En("Choose a Wire Emitter")
    public String chooseEmitter();

    @En("Choose a Wire Receiver")
    public String chooseReceiver();

    @En("Class intance name cannot be null")
    public String clazzNonNull();

    @En("Closing connection instance...")
    public String closingConnection();

    @En("Closing connection instance...Done")
    public String closingConnectionDone();

    @En("Closing all result sets...")
    public String closingResultSet();

    @En("Closing all result sets...Done")
    public String closingResultSetDone();

    @En("Closing all statements...")
    public String closingStatement();

    @En("Closing all statements...Done")
    public String closingStatementDone();

    @En("Cloud Client cannot be null")
    public String cloudClientNonNull();

    @En("Cannot setup CloudClient...")
    public String cloudClientSetupProblem();

    @En("Component context cannot be null")
    public String componentContextNonNull();

    @En("Unable to retrieve Factory PIDs of one of the provided Wire Components because of null")
    public String componentPidsNull();

    @En("The provided Wire Components do not belong to its specified type")
    public String componentsNotApplicable();

    @En("Configurations cannot be null")
    public String configurationNonNull();

    @En("Connection instance cannnot be null")
    public String connectionNonNull();

    @En("Creating table {0}...")
    public String creatingTable(String tableName);

    @En("Creating wire between {0} and {1}....")
    public String creatingWire(String emitterName, String receiverName);

    @En("Creating wire component of {0}....")
    public String creatingWireComponent(String componentName);

    @En("Creating wire between {0} and {1}....Done")
    public String creatingWireDone(String emitterName, String receiverName);

    @En("Creating wires.....")
    public String creatingWires();

    @En("Creating wire.....Done")
    public String creatingWiresDone();

    @En("Cron Expression cannot be null")
    public String cronExpressionNonNull();

    @En("Data Service cannot be null")
    public String dataServiceNonNull();

    @En("DB Filter cannot be null")
    public String dbFilterNonNull();

    @En("DB Service cannot be null")
    public String dbServiceNonNull();

    @En("Deactivating Cloud Publisher Wire Component...")
    public String deactivatingCloudPublisher();

    @En("Deactivating Cloud Publisher Wire Component...Done")
    public String deactivatingCloudPublisherDone();

    @En("Deactivating Cloud Subscriber Wire Component...")
    public String deactivatingCloudSubscriber();

    @En("Deactivating Cloud Subscriber Wire Component...Done")
    public String deactivatingCloudSubscriberDone();

    @En("Dectivating DB Wire Record Filter...")
    public String deactivatingFilter();

    @En("Dectivating DB Wire Record Filter...Done")
    public String deactivatingFilterDone();

    @En("Deactivating Logger Wire Component...")
    public String deactivatingLogger();

    @En("Deactivating Logger Wire Component...Done")
    public String deactivatingLoggerDone();

    @En("Deactivating DB Wire Record Store...")
    public String deactivatingStore();

    @En("Deactivating DB Wire Record Store...Done")
    public String deactivatingStoreDone();

    @En("Dectivating Fifo...")
    public String deactivatingFifo();

    @En("Dectivating Fifo...Done")
    public String deactivatingFifoDone();

    @En("Dectivating Timer...")
    public String deactivatingTimer();

    @En("Dectivating Timer...Done")
    public String deactivatingTimerDone();

    @En("Deactivating Wire Asset...")
    public String deactivatingWireAsset();

    @En("Deactivating Wire Asset...Done")
    public String deactivatingWireAssetDone();

    @En("Deactivating Wire Service Component...")
    public String deactivatingWireService();

    @En("Deactivating Wire Service Component...Done")
    public String deactivatingWireServiceDone();

    @En("Delay cannot be negative")
    public String delayNonNegative();

    @En("Deregistering Wire Emitter {0}...")
    public String deregisteringEmitter(String emitterName);

    @En("Deregistering Wire Receiver {0}...")
    public String deregisteringReceiver(String receiverName);

    @En("WireService leverages Kura Wiring communication functionalities between Wire Components")
    public String description();

    @En("Driver Name")
    public String driverName();

    @En("emitter")
    public String emitter();

    @En("Emitter PID cannot be null")
    public String emitterPidNonNull();

    @En("Emitter Service PID cannot be null")
    public String emitterServicePidNonNull();

    @En("ERROR")
    public String error();

    @En("Error while building a Bundle Context filter.")
    public String errorBuildingBundleContextFilter();

    @En("Error while building Wire Records.")
    public String errorBuildingWireRecords();

    @En("Error in creating cloud client")
    public String errorCreatingCloudClinet();

    @En("Error while creating wire component...")
    public String errorCreatingWireComponent();

    @En("Error while creating wires...")
    public String errorCreatingWires();

    @En("Error while disconnecting cloud publisher...")
    public String errorDisconnectingCloudPublisher();

    @En("Error in emitting Wire Records...")
    public String errorEmitting();

    @En("Error while filtering Wire Records...")
    public String errorFiltering();

    @En("Error while filtering using provided Regular Expression...")
    public String errorFilteringRegex();

    @En("Error while performing read from the Wire Asset...")
    public String errorPerformingRead();

    @En("Error while performing write from the Wire Asset...")
    public String errorPerformingWrite();

    @En("Failed to persist wires...")
    public String errorPersistingWires();

    @En("Error in publishing wire records using cloud publisher..")
    public String errorPublishingWireRecords();

    @En("Error while storing Wire Records...")
    public String errorStoring();

    @En("Error subscribing...")
    public String errorSubscribing();

    @En("Error in truncating the table {0}....")
    public String errorTruncatingTable(String tableName);

    @En("Error unsubscribing...")
    public String errorUnsubscribing();

    @En("Error during Wire Service Component update! Something went wrong...")
    public String errorUpdatingWireService();

    @En("Event Admin cannot be null")
    public String eventAdminNonNull();

    @En("exception")
    public String exceptionWireField();

    @En("Executing SQL query...")
    public String execSql();

    @En("Executing SQL query...Done")
    public String execSqlDone();

    @En("Extracting Propertiess...")
    public String exectractingProp();

    @En("Extracting Propertiess...Done")
    public String exectractingPropDone();

    @En("Factory PID cannot be null")
    public String factoryPidNonNull();

    @En("filter")
    public String filter();

    @En("Wire record filtering started...")
    public String filteringStarted();

    @En("Filter cannot be null")
    public String filterNonNull();

    @En("heading")
    public String heading();

    @En("incoming_wires")
    public String incomingWires();

    @En("Insertion failed. Reconciling Table and Columns...")
    public String insertionFailed();

    @En("Interface class cannot be null")
    public String interfaceClassNonNull();

    @En("Interval cannot be less than or equal to zero")
    public String intervalNonLessThanEqualToZero();

    @En("Invalid time unit")
    public String invalidTimeUnit();

    @En("latitude")
    public String latitude();

    @En("Filtered Wire Envelope ==> {0}")
    public String loggerReceive(String envelope);

    @En("longitude")
    public String longitude();

    @En("Minutes cannot be negative")
    public String minutesNonNegative();

    @En("multiton.instance.name for the resulting component. If left null it will be equal to service.pid")
    public String multitonInstanceName();

    @En("WireService")
    public String name();

    @En("Do not delete any instance")
    public String noDeleteInstance();

    @En("Do not delete any wire")
    public String noDeleteWire();

    @En("No new instance")
    public String noNewInstance();

    @En("outgoing_wires")
    public String outgoingWires();

    @En("Partially emptying table {0}")
    public String partiallyEmptyingTable(String sqlTableName);

    @En("Payload cannot be null")
    public String payloadNonNull();

    @En("Persisting Wires...")
    public String persistingWires();

    @En("Persisting Wires...Done")
    public String persistingWiresDone();

    @En("Wire Component PID cannot be null")
    public String pidNonNull();

    @En("position")
    public String position();

    @En("Position cannot be null")
    public String positionNonNull();

    @En("Properties cannot be null")
    public String propertiesNonNull();

    @En("receiver")
    public String receiver();

    @En("Receiver PID cannot be null")
    public String receiverPidNonNull();

    @En("Receiver Service PID cannot be null")
    public String receiverServicePidNonNull();

    @En("Refreshing boolean value {0}")
    public String refreshBoolean(boolean value);

    @En("Refreshing byte value {0}")
    public String refreshByte(byte value);

    @En("Refreshing byte array value {0}")
    public String refreshByteArray(String value);

    @En("Refreshing double value {0}")
    public String refreshDouble(double value);

    @En("Refreshed typed values")
    public String refreshed();

    @En("Refreshing integer value {0}")
    public String refreshInteger(int value);

    @En("Refreshing long value {0}")
    public String refreshLong(long value);

    @En("Refreshing short value {0}")
    public String refreshShort(short value);

    @En("Refreshing string value {0}")
    public String refreshString(String value);

    @En("{0} - Regular Expression Filtering started..")
    public String regexFilteringStarted(String emitterName);

    @En("{0} - Regular Expression Filtering finished..")
    public String regexFilteringDone(String emitterName);

    @En("Registering Wire Emitter {0}...")
    public String registeringEmitter(String emitterName);

    @En("Registering Wire Receiver {0}...")
    public String registeringReceiver(String receiverName);

    @En("Removing Wire Component...")
    public String removingWireComponent();

    @En("Removing Wire Component...Done")
    public String removingWireComponentDone();

    @En("Removing Wires...")
    public String removingWires();

    @En("Removing Wires...Done")
    public String removingWiresDone();

    @En("Rolling back the connection instance...")
    public String rollback();

    @En("Rolling back the connection instance...Done")
    public String rollbackDone();

    @En("Sanitizing the provided string...")
    public String sanitize();

    @En("Scheduler exception.")
    public String schedulerException();

    @En("Scheduler stopping in Cloud Publisher Disconnect Manager...Done")
    public String schedulerStopped();

    @En("Scheduler stopping in Cloud Publisher Disconnect Manager...")
    public String schedulerStopping();

    @En("Select an Instance from the list. The instance and all connected Wires will be deleted when submitting the changes.")
    public String selectInstance();

    @En("Select a Wire from the list. It will be deleted when submitting the changes.")
    public String selectWire();

    @En("speed")
    public String speed();

    @En("SQL query cannot be null")
    public String sqlQueryNonNull();

    @En("Storing boolean of value {0}")
    public String storeBoolean(boolean value);

    @En("Storing byte of value {0}")
    public String storeByte(byte value);

    @En("Storing byte array of value {0}")
    public String storeByteArray(String value);

    @En("Stored typed value")
    public String stored();

    @En("Storing double of value {0}")
    public String storeDouble(double value);

    @En("Storing integer of value {0}")
    public String storeInteger(int value);

    @En("Storing long of value {0}")
    public String storelong(long value);

    @En("Storing short of value {0}")
    public String storeShort(short value);

    @En("Storing string of value {0}")
    public String storeString(String value);

    @En("Storing data into table {0}...")
    public String storingRecord(String emitterName);

    @En("Provided string cannot be null")
    public String stringNonNull();

    @En("Table name cannot be null")
    public String tableNameNonNull();

    @En("timestamp")
    public String timestamp();

    @En("Starting to track Wire Components....")
    public String trackWireComponents();

    @En("Starting to track Wire Components....Done")
    public String trackWireComponentsDone();

    @En("Truncating table {0}...")
    public String truncatingTable(String tableName);

    @En("typed_value")
    public String typedValue();

    @En("Unknown metric type.")
    public String unknownMetricType();

    @En("Updating Cloud Publisher Wire Component...")
    public String updatingCloudPublisher();

    @En("Updating Cloud Publisher Wire Component...Done")
    public String updatingCloudPublisherDone();

    @En("Updating Cloud Subscriber Wire Component...")
    public String updatingCloudSubscriber();

    @En("Updating Cloud Subscriber Wire Component...Done")
    public String updatingCloudSubscriberDone();

    @En("Updating DB Wire Record Filter...")
    public String updatingFilter();

    @En("Updating DB Wire Record Filter...Done")
    public String updatingFilterDone();

    @En("Updating Logger Wire Component...")
    public String updatingLogger();

    @En("Updating Logger Wire Component...Done")
    public String updatingLoggerDone();

    @En("Updating Regex Filter...")
    public String updatingRegexFilter();

    @En("Updating Regex Filter...Done")
    public String updatingRegexFilterDone();

    @En("Updating DB Wire Record Store...")
    public String updatingStore();

    @En("Updating DB Wire Record Store...Done")
    public String updatingStoreDone();

    @En("Updating Fifo...")
    public String updatingFifo();

    @En("Updating Fifo...Done")
    public String updatingFifoDone();

    @En("Updating Timer...")
    public String updatingTimer();

    @En("Updating Timer...Done")
    public String updatingTimerDone();

    @En("Updating Wire Asset...")
    public String updatingWireAsset();

    @En("Updating Wire Asset...Done")
    public String updatingWireAssetDone();

    @En("Updating Wire Service Component...: ")
    public String updatingWireService();

    @En("Updating Wire Service Component...Done")
    public String updatingWireServiceDone();

    @En("Value cannot be null")
    public String valueNonNull();

    @En("wire_component")
    public String wireComponent();

    @En("Wire Component cannot be null")
    public String wireComponentNonNull();

    @En("Wire Component PID cannot be null")
    public String wireComponentPidNonNull();

    @En("wire_configurations")
    public String wireConf();

    @En("Wire Configuration cannot be null")
    public String wireConfigurationNonNull();

    @En("Wire Envelope cannot be null")
    public String wireEnvelopeNonNull();

    @En("Wire Enveloped received...")
    public String wireEnvelopeReceived();

    @En("Received WireEnvelope from {0}")
    public String wireEnvelopeReceived(String emitterName);

    @En("Wire Helper Service cannot be null")
    public String wireHelperServiceNonNull();

    @En("Wire cannot be null")
    public String wireNonNull();

    @En("Wire Record cannot be null")
    public String wireRecordNonNull();

    @En("Wire records cannot be empty")
    public String wireRecordsNonEmpty();

    @En("Wire Records cannot be null")
    public String wireRecordsNonNull();

    @En("Wire Service cannot be null")
    public String wireServiceNonNull();

    @En("Configured Wire Service properties cannot be null")
    public String wireServicePropNonNull();

    @En("Wires cannot be null")
    public String wiresNonNull();

    @En("Wire supported component cannot be null")
    public String wireSupportedComponentNonNull();

    @En("Unexpected exception while adding new envelope to queue")
    public String fifoUnexpectedExceptionWhileSubmitting();

    @En("Interrupted while adding new envelope to queue")
    public String fifoInterruptedWhileSubmitting();

    @En("Unexpected exception while dispatching envelope")
    public String fifoUnexpectedExceptionWhileDispatching();

    @En("Specifies wheter the values of all READ or READ_WRITE channels should be emitted in case of a channel event. "
            + "If set to true, the values for all channels will be read and emitted, if set to false, only the value for the channel related to the event will be emitted.")
    public String emitAllChannelsDescription();

    @En("If set to PER_CHANNEL, the component will emit a driver-generated timestamp per channel property."
            + " If set to SINGLE_ASSET_GENERATED, the component will emit a single timestamp per request, generated by the Asset itself before emitting the envelope."
            + " If set to SINGLE_DRIVER_GENERATED_MAX or SINGLE_DRIVER_GENERATED_MIN, the component will emit a single driver generated timestamp being respectively the max (most recent) or min (oldest) among the timestamps of the channels.")
    public String timestampModeDescription();

    @En("Specifies wheter errors should be included or not in the emitted envelope")
    public String emitErrorsDescription();

    @En("Failed to retreive \"listen\" property from channel configuration")
    public String errorRetrievingListenable();

    @En("Specifies if WireAsset should emit envelopes on Channel change")
    public String listen();
}
