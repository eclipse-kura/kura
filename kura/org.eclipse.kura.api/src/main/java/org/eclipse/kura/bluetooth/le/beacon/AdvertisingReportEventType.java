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
public class AdvertisingReportEventType {

    private static final byte ADV_IND = 0x00;
    private static final byte ADV_DIRECT_IND = 0x01;
    private static final byte ADV_SCAN_IND = 0x02;
    private static final byte ADV_NONCONN_IND = 0x03;
    private static final byte SCAN_RSP = 0x04;

    private static final byte ADV_IND_EXT = 0x13;
    private static final byte ADV_DIRECT_IND_EXT = 0x15;
    private static final byte ADV_SCAN_IND_EXT = 0x12;
    private static final byte ADV_NONCONN_IND_EXT = 0x10;
    private static final byte SCAN_RSP_ADV_IND_EXT = 0x1B;
    private static final byte SCAN_RSP_ADV_SCAN_IND_EXT = 0x1A;

    private static final byte DATA_STATUS_COMPLETED = 0x00;
    private static final byte DATA_STATUS_INCOMPLETE_ONGOING = 0x20;
    private static final byte DATA_STATUS_INCOMPLETE_FINISHED = 0x40;

    private DataStatus dataStatus = DataStatus.UNKNOWN;
    private boolean connectable = false;
    private boolean directed = false;
    private boolean scannable = false;
    private boolean scanResponse = false;

    public DataStatus getDataStatus() {
        return this.dataStatus;
    }

    public void setDataStatus(DataStatus status) {
        this.dataStatus = status;
    }

    public boolean isConnectable() {
        return connectable;
    }

    public void setConnectable(boolean connectable) {
        this.connectable = connectable;
    }

    public boolean isDirected() {
        return directed;
    }

    public void setDirected(boolean directed) {
        this.directed = directed;
    }

    public boolean isScannable() {
        return scannable;
    }

    public void setScannable(boolean scannable) {
        this.scannable = scannable;
    }

    public boolean isScanResponse() {
        return scanResponse;
    }

    public void setScanResponse(boolean scanResponse) {
        this.scanResponse = scanResponse;
    }

    public static AdvertisingReportEventType valueOf(int event, boolean extendedReport) {
        AdvertisingReportEventType type = new AdvertisingReportEventType();

        // Extended report has scope to use 2 octets but currently only uses 1
        byte eventData = (byte) (event & 0xFF);

        if (extendedReport) {
            // Check for legacy advertising PDU
            if ((eventData & 0x10) == 0) {
                // Connectable Flag
                type.setConnectable((eventData & 0x01) != 0);

                // Scannable Flag
                type.setScannable((eventData & 0x02) != 0);

                // Directed Flag
                type.setDirected((eventData & 0x04) != 0);

                // Scan Response Flag
                type.setScanResponse((eventData & 0x08) != 0);

                // Data Status
                byte status = (byte) (eventData & 0x60);

                if (status == DATA_STATUS_COMPLETED) {
                    type.setDataStatus(DataStatus.COMPLETED);
                } else if (status == DATA_STATUS_INCOMPLETE_ONGOING) {
                    type.setDataStatus(DataStatus.INCOMPLETE_ONGOING);
                } else if (status == DATA_STATUS_INCOMPLETE_FINISHED) {
                    type.setDataStatus(DataStatus.INCOMPLETE_FINISHED);
                } else {
                    type.setDataStatus(DataStatus.UNKNOWN);
                }
            } else {
                if (event == ADV_IND_EXT) {
                    type.setConnectable(true);
                } else if (event == ADV_DIRECT_IND_EXT) {
                    type.setConnectable(true);
                    type.setDirected(true);
                } else if (event == ADV_SCAN_IND_EXT) {
                    type.setScannable(true);
                } else if (event == SCAN_RSP_ADV_IND_EXT) {
                    type.setConnectable(true);
                    type.setScanResponse(true);
                } else if (event == SCAN_RSP_ADV_SCAN_IND_EXT) {
                    type.setScannable(true);
                    type.setScanResponse(true);
                } else if (event != ADV_NONCONN_IND_EXT) {
                    throw new IllegalArgumentException("Report Event type not recognized");
                }
            }
        } else {
            if (event == ADV_IND) {
                type.setConnectable(true);
            } else if (event == ADV_DIRECT_IND) {
                type.setConnectable(true);
                type.setDirected(true);
            } else if (event == ADV_SCAN_IND) {
                type.setScannable(true);
            } else if (event == SCAN_RSP) {
                type.setScanResponse(true);
            } else if (event != ADV_NONCONN_IND) {
                throw new IllegalArgumentException("Report Event type not recognized");
            }
        }

        return type;
    }

    public enum DataStatus {
        UNKNOWN,
        COMPLETED,
        INCOMPLETE_ONGOING,
        INCOMPLETE_FINISHED
    }
}
