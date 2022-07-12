/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.internal.wire.db.store;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.BaseDbService;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.internal.wire.db.common.BaseDbServiceProviderImpl;
import org.eclipse.kura.internal.wire.db.common.DbServiceProvider;
import org.eclipse.kura.internal.wire.db.common.H2DbServiceProviderImpl;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class DbWireRecordStore is a wire component which is responsible to store
 * the received {@link WireRecord}.
 */
public class DbWireRecordStore implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LogManager.getLogger(DbWireRecordStore.class);
    private static final String NULL_TABLE_NAME_ERROR_MSG = "Table name cannot be null";
    private static final String NULL_WIRE_RECORD_ERROR_MSG = "WireRecord cannot be null";

    private DbServiceProvider dbServiceProvider;
    private BaseDbService dbService;
    private DbWireRecordStoreOptions wireRecordStoreOptions;
    private WireHelperService wireHelperService;
    private WireSupport wireSupport;

    public synchronized void bindDbService(BaseDbService dbService) {
        this.dbService = dbService;
        if (this.dbService instanceof H2DbService) {
            this.dbServiceProvider = new H2DbServiceProviderImpl((H2DbService) this.dbService);
        } else {
            this.dbServiceProvider = new BaseDbServiceProviderImpl(this.dbService);
        }
        if (nonNull(this.dbService) && nonNull(this.wireRecordStoreOptions)) {
            reconcileDB(this.wireRecordStoreOptions.getTableName());
        }
    }

    public synchronized void unbindDbService(BaseDbService dbService) {
        if (this.dbService == dbService) {
            this.dbServiceProvider = null;
            this.dbService = null;
            this.wireRecordStoreOptions = null;
        }
    }

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * OSGi Service Component callback for activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the properties
     */
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug("Activating DB Wire Record Store...");
        this.wireRecordStoreOptions = new DbWireRecordStoreOptions(properties);

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        if (nonNull(this.dbService)) {
            reconcileDB(this.wireRecordStoreOptions.getTableName());
        }

        logger.debug("Activating DB Wire Record Store... Done");
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated service component properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug("Updating DB Wire Record Store...");

        this.wireRecordStoreOptions = new DbWireRecordStoreOptions(properties);

        reconcileDB(this.wireRecordStoreOptions.getTableName());

        logger.debug("Updating DB Wire Record Store... Done");
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug("Deactivating DB Wire Record Store...");
        this.dbServiceProvider = null;
        this.dbService = null;
        this.wireRecordStoreOptions = null;
        logger.debug("Deactivating DB Wire Record Store... Done");
    }

    /**
     * Truncates tables containing {@link WireRecord}s
     */
    private void truncate() {
        truncate(this.wireRecordStoreOptions.getNoOfRecordsToKeep());
    }

    /**
     * Truncates the records in the table
     *
     * @param noOfRecordsToKeep
     *            the no of records to keep in the table
     */
    private void truncate(final int noOfRecordsToKeep) {
        final String tableName = this.wireRecordStoreOptions.getTableName();
        final int maxTableSize = this.wireRecordStoreOptions.getMaximumTableSize();

        try {
            this.dbServiceProvider.truncate(noOfRecordsToKeep, tableName, maxTableSize);
        } catch (final SQLException sqlException) {
            logger.error("Error in truncating the table {}...", tableName, sqlException);
        }
    }

    private int getTableSize() throws SQLException {
        final String tableName = this.wireRecordStoreOptions.getTableName();
        return this.dbServiceProvider.getTableSize(tableName);
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEvelope) {
        requireNonNull(wireEvelope, "Wire Envelope cannot be null");
        final List<WireRecord> records = wireEvelope.getRecords();

        if (this.dbServiceProvider == null) {
            logger.warn("DbService instance not attached");
            return;
        }

        try {
            if (getTableSize() >= this.wireRecordStoreOptions.getMaximumTableSize()) {
                truncate();
            }
        } catch (SQLException e) {
            logger.warn("Exception while trying to clean db");
        }

        for (WireRecord wireRecord : records) {
            store(wireRecord);
        }

        // emit the list of Wire Records to the downstream components
        this.wireSupport.emit(records);
    }

    /**
     * Stores the provided {@link WireRecord} in the database
     *
     * @param wireRecord
     *            the {@link WireRecord} to be stored
     * @throws NullPointerException
     *             if the provided argument is null
     */
    private void store(final WireRecord wireRecord) {
        requireNonNull(wireRecord, NULL_WIRE_RECORD_ERROR_MSG);
        int retryCount = 0;
        final String tableName = this.wireRecordStoreOptions.getTableName();
        do {
            try {
                insertDataRecord(tableName, wireRecord);
                break;
            } catch (final SQLException e) {
                logger.error("Insertion failed. Reconciling Table and Columns...", e);
                reconcileDB(wireRecord, tableName);
                retryCount++;
            }
        } while (retryCount < 2);
    }

    /**
     * Tries to reconcile the database.
     *
     * @param wireRecord
     *            against which the database columns have to be reconciled.
     * @param tableName
     *            the table name in the database that needs to be reconciled.
     */
    private void reconcileDB(final WireRecord wireRecord, final String tableName) {
        try {
            if (nonNull(tableName) && !tableName.isEmpty()) {
                reconcileTable(tableName);
                reconcileColumns(tableName, wireRecord);
            }
        } catch (final SQLException ee) {
            logger.error("Error while storing Wire Records...", ee);
        }
    }

    /**
     * Tries to reconcile the database.
     *
     * @param tableName
     *            the table name in the database that needs to be reconciled.
     */
    private synchronized void reconcileDB(final String tableName) {
        try {
            if (nonNull(this.dbServiceProvider) && nonNull(tableName) && !tableName.isEmpty()) {
                reconcileTable(tableName);
            }
        } catch (final SQLException ee) {
            logger.error("Error while storing Wire Records...", ee);
        }
    }

    /**
     * Reconcile table.
     *
     * @param tableName
     *            the table name
     * @throws SQLException
     *             the SQL exception
     * @throws NullPointerException
     *             if the provided argument is null
     */
    private void reconcileTable(final String tableName) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        this.dbServiceProvider.reconcileTable(tableName);
    }

    /**
     * Reconcile columns.
     *
     * @param tableName
     *            the table name
     * @param wireRecord
     *            the data record
     * @throws SQLException
     *             the SQL exception
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private void reconcileColumns(final String tableName, final WireRecord wireRecord) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        requireNonNull(wireRecord, NULL_WIRE_RECORD_ERROR_MSG);
        this.dbServiceProvider.reconcileColumns(tableName, wireRecord);
    }

    /**
     * Insert the provided {@link WireRecord} to the specified table
     *
     * @param tableName
     *            the table name
     * @param wireRecord
     *            the {@link WireRecord}
     * @throws SQLException
     *             the SQL exception
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private void insertDataRecord(final String tableName, final WireRecord wireRecord) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        requireNonNull(wireRecord, NULL_WIRE_RECORD_ERROR_MSG);
        this.dbServiceProvider.insertDataRecord(tableName, wireRecord);
    }

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }
}
