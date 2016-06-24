/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.localization;

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

	@En("Activating DB Wire Record Filter...")
	public String activatingFilter();

	@En("Activating DB Wire Record Filter...Done")
	public String activatingFilterDone();

	@En("Activating DB Wire Record Store...")
	public String activatingStore();

	@En("Activating DB Wire Record Store...Done")
	public String activatingStoreDone();

	@En("Activating Timer...")
	public String activatingTimer();

	@En("Activating Timer...Done")
	public String activatingTimerDone();

	@En("Activating Wire Service...")
	public String activatingWireService();

	@En("Activating Wire Service...Done")
	public String activatingWireServiceDone();

	@En("<br /><br /><b>Active wires:</b><br />")
	public String activeWires();

	@En("Adding Wire Components....")
	public String addingWireComponent();

	@En("Adding Wire Components....Done")
	public String addingWireComponentDone();

	@En("altitude")
	public String altitude();

	@En("Bundle context cannot be null")
	public String bundleContextNonNull();

	@En("Choose a Wire Emitter")
	public String chooseEmitter();

	@En("Choose a Wire Receiver")
	public String chooseReceiver();

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

	@En("Component name cannot be null")
	public String componentNameNonNull();

	@En("Connection instance cannnot be null")
	public String connectionNonNull();

	@En("Create a new Wire")
	public String creatingNewWire();

	@En("Creating table DR_{0}...")
	public String creatingTable(String tableName);

	@En("Creating wire between {0} and {1}....")
	public String creatingWire(String emitterName, String receiverName);

	@En("Creating wire component of {0}....")
	public String creatingWireComponent(String componentName);

	@En("Creating wire between {0} and {1}....Done")
	public String creatingWireDone(String emitterName, String receiverName);

	@En("Creating wires.....")
	public String creatingWires();

	@En("Creating wires.....Done")
	public String creatingWiresDone();

	@En("Data Service cannot be null")
	public String dataServiceNonNull();

	@En("DB Service cannot be null")
	public String dbServiceNonNull();

	@En("Deactivating Cloud Publisher Wire Component...")
	public String deactivatingCloudPublisher();

	@En("Deactivating Cloud Publisher Wire Component...Done")
	public String deactivatingCloudPublisherDone();

	@En("Dectivating DB Wire Record Filter...")
	public String deactivatingFilter();

	@En("Dectivating DB Wire Record Filter...Done")
	public String deactivatingFilterDone();

	@En("Deactivating DB Wire Record Store...")
	public String deactivatingStore();

	@En("Deactivating DB Wire Record Store...Done")
	public String deactivatingStoreDone();

	@En("Dectivating Timer...")
	public String deactivatingTimer();

	@En("Dectivating Timer...Done")
	public String deactivatingTimerDone();

	@En("Deactivating Wire Service Component...")
	public String deactivatingWireService();

	@En("Deactivating Wire Service Component...Done")
	public String deactivatingWireServiceDone();

	@En("Delay cannot be negative")
	public String delayNonNegative();

	@En("Emitter name cannot be null")
	public String emitterNameNonNull();

	@En("Error while creating wire component...")
	public String errorCreatingWireComponent();

	@En("Error while creating wires...")
	public String errorCreatingWires();

	@En("Error while disconnecting cloud publisher...")
	public String errorDisconnectingCloudPublisher();

	@En("Error in emitting wire records...")
	public String errorEmitting();

	@En("Error while filtering wire records...")
	public String errorFiltering();

	@En("Failed to persist wires...")
	public String errorPersistingWires();

	@En("Error in publishing wire records using cloud publisher..")
	public String errorPublishingWireRecords();

	@En("Error in truncating the table ")
	public String errorTruncatingTable();

	@En("Error during Wire Service Component update! Something went wrong...")
	public String errorUpdatingWireService();

	@En("Executing SQL query...")
	public String execSql();

	@En("Executing SQL query...Done")
	public String execSqlDone();

	@En("Factory PID cannot be null")
	public String factoryPidNonNull();

	@En("Wire record filtering started...")
	public String filteringStarted();

	@En("heading")
	public String heading();

	@En("Interface class cannot be null")
	public String interfaceClassNonNull();

	@En("latitude")
	public String latitude();

	@En("longitude")
	public String longitude();

	@En("Minutes cannot be negative")
	public String minutesNonNegative();

	@En("multiton.instance.name for the resulting component. If left null it will be equal to service.pid")
	public String multitonInstanceName();

	@En("Do not delete any instance")
	public String noDeleteInstance();

	@En("Do not delete any wire")
	public String noDeleteWire();

	@En("No new instance")
	public String noNewInstance();

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

	@En("Receiver name cannot be null")
	public String receiverNameNonNull();

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

	@En("Registering Wire Emitter...")
	public String registeringEmitter();

	@En("Registering Wire Receiver...")
	public String registeringReceiver();

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

	@En("Storing data record from emitter {0} into table {1}...")
	public String storingRecord(String emitterName, String tableName);

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

	@En("Truncating table DR_{0}...")
	public String truncatingTable(String tableName);

	@En("Updating Cloud Publisher Wire Component...")
	public String updatingCloudPublisher();

	@En("Updating Cloud Publisher Wire Component...Done")
	public String updatingCloudPublisherDone();

	@En("Updating DB Wire Record Filter...")
	public String updatingFilter();

	@En("Updating DB Wire Record Filter...Done")
	public String updatingFilterDone();

	@En("Updating DB Wire Record Store with...")
	public String updatingStore();

	@En("Updating DB Wire Record Store...Done")
	public String updatingStoreDone();

	@En("Updating Timer...")
	public String updatingTimer();

	@En("Updating Timer...Done")
	public String updatingTimerDone();

	@En("Updating Wire Service Component...: ")
	public String updatingWireService();

	@En("Updating Wire Service Component...Done")
	public String updatingWireServiceDone();

	@En("Value cannot be null")
	public String valueNonNull();

	@En("Wire Component name cannot be null")
	public String wireComponentNameNonNull();

	@En("Wire Envelope cannot be null")
	public String wireEnvelopeNonNull();

	@En("Wire Enveloped received...")
	public String wireEnvelopeReceived();

	@En("Wire Record cannot be null")
	public String wireRecordNonNull();

	@En("Received WireEnvelope from {0}")
	public String wireRecordReceived(String emitterName);

	@En("Wire Records cannot be null")
	public String wireRecordsNonNull();

	@En("Wire Service")
	public String wireService();

	@En("Wires cannot be null")
	public String wiresNonNull();

}
