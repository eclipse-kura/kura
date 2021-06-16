/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Scott Ware
 ******************************************************************************/
package org.eclipse.kura.bluetooth.le.beacon;

import org.osgi.annotation.versioning.ProviderType;

/**
 * AdvertisingReportEventType represents the event type of an advertising packet.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.3
 */
@ProviderType
public enum AdvertisingReportEventType {

    ADV_IND((byte) 0x00),
    ADV_DIRECT_IND((byte) 0x01),
    ADV_SCAN_IND((byte) 0x02),
    ADV_NONCONN_IND((byte) 0x03),
    SCAN_RSP((byte) 0x04),

    /**
     * @since 2.2
     */
    ADV_IND_EXT((byte) 0x13),
    /**
     * @since 2.2
     */
    ADV_DIRECT_IND_EXT((byte) 0x15),
    /**
     * @since 2.2
     */
    ADV_SCAN_IND_EXT((byte) 0x12),
    /**
     * @since 2.2
     */
    ADV_NONCONN_IND_EXT((byte) 0x10),
    /**
     * @since 2.2
     */
    SCAN_RSP_ADV_IND_EXT((byte) 0x1B),
    /**
     * @since 2.2
     */
    SCAN_RSP_ADV_SCAN_IND_EXT((byte) 0x1A),
    /**
     * @since 2.2
     */
    DATA_STATUS_COMPLETED((byte) 0x00),
    /**
     * @since 2.2
     */
    DATA_STATUS_INCOMPLETE_ONGOING((byte) 0x20),
    /**
     * @since 2.2
     */
    DATA_STATUS_INCOMPLETE_FINISHED((byte) 0x40);

    private DataStatus dataStatus = DataStatus.UNKNOWN;
    private boolean connectable = false;
    private boolean directed = false;
    private boolean scannable = false;
    private boolean scanResponse = false;
    private final byte eventType;

    private AdvertisingReportEventType(byte eventType) {
        this.eventType = eventType;
    }

    /**
     * @since 2.2
     */
    public DataStatus getDataStatus() {
        return this.dataStatus;
    }

    /**
     * @since 2.2
     */
    public boolean isConnectable() {
        return connectable;
    }

    /**
     * @since 2.2
     */
    public boolean isDirected() {
        return directed;
    }

    /**
     * @since 2.2
     */
    public boolean isScannable() {
        return scannable;
    }

    /**
     * @since 2.2
     */
    public boolean isScanResponse() {
        return scanResponse;
    }

    /**
     * @since 2.2
     */
    public byte getEventTypeCode() {
        return this.eventType;
    }

    /**
     * @since 2.2
     */
    public static AdvertisingReportEventType valueOf(int event, boolean extendedReport) {
        AdvertisingReportEventType type = null;

        // Extended report has scope to use 2 octets but currently only uses 1
        byte eventData = (byte) (event & 0xFF);

        if (extendedReport) {
            type = getLegacyType(event, eventData);
        } else {
            if (event == ADV_IND.eventType) {
                type = ADV_IND;
                type.connectable = true;
            } else if (event == ADV_DIRECT_IND.eventType) {
                type = ADV_DIRECT_IND;
                type.connectable = true;
                type.directed = true;
            } else if (event == ADV_SCAN_IND.eventType) {
                type = ADV_SCAN_IND;
                type.scannable = true;
            } else if (event == SCAN_RSP.eventType) {
                type = SCAN_RSP;
                type.scanResponse = true;
            } else if (event != ADV_NONCONN_IND.eventType) {
                throw new IllegalArgumentException("Report Event type not recognized");
            }
        }

        return type;
    }

    private static AdvertisingReportEventType getLegacyType(int event, byte eventData) {
        AdvertisingReportEventType type = null;
        // Check for legacy advertising PDU
        if ((eventData & 0x10) == 0) {
            // Data Status
            byte status = (byte) (eventData & 0x60);

            if (status == DATA_STATUS_COMPLETED.eventType) {
                type = DATA_STATUS_COMPLETED;
                type.dataStatus = DataStatus.COMPLETED;
            } else if (status == DATA_STATUS_INCOMPLETE_ONGOING.eventType) {
                type = DATA_STATUS_INCOMPLETE_ONGOING;
                type.dataStatus = DataStatus.INCOMPLETE_ONGOING;
            } else if (status == DATA_STATUS_INCOMPLETE_FINISHED.eventType) {
                type = DATA_STATUS_INCOMPLETE_FINISHED;
                type.dataStatus = DataStatus.INCOMPLETE_FINISHED;
            } else {
                throw new IllegalArgumentException("Data status type not recognized");
            }

            // Connectable Flag
            type.connectable = (eventData & 0x01) != 0;

            // Scannable Flag
            type.scannable = (eventData & 0x02) != 0;

            // Directed Flag
            type.directed = (eventData & 0x04) != 0;

            // Scan Response Flag
            type.scanResponse = (eventData & 0x08) != 0;
        } else {
            if (event == ADV_IND_EXT.eventType) {
                type = ADV_IND_EXT;
                type.connectable = true;
            } else if (event == ADV_DIRECT_IND_EXT.eventType) {
                type = ADV_DIRECT_IND_EXT;
                type.connectable = true;
                type.directed = true;
            } else if (event == ADV_SCAN_IND_EXT.eventType) {
                type = ADV_SCAN_IND_EXT;
                type.scannable = true;
            } else if (event == SCAN_RSP_ADV_IND_EXT.eventType) {
                type = SCAN_RSP_ADV_IND_EXT;
                type.connectable = true;
                type.scanResponse = true;
            } else if (event == SCAN_RSP_ADV_SCAN_IND_EXT.eventType) {
                type = SCAN_RSP_ADV_SCAN_IND_EXT;
                type.scannable = true;
                type.scanResponse = true;
            } else if (event != ADV_NONCONN_IND_EXT.eventType) {
                throw new IllegalArgumentException("Report Event type not recognized");
            }
        }

        return type;
    }

    /**
     * 
     * @deprecated since 2.2 use instead {@link valueOf(int event, boolean extendedReport)}
     */
    @Deprecated
    public static AdvertisingReportEventType valueOf(byte event) {
        return valueOf(event, false);
    }

    /**
     * @since 2.2
     */
    public enum DataStatus {
        UNKNOWN,
        COMPLETED,
        INCOMPLETE_ONGOING,
        INCOMPLETE_FINISHED
    }
}
