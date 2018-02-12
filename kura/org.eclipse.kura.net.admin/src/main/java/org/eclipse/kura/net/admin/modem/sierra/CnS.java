/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.sierra;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes CnS object.
 * <p>
 * CnS format:
 * <table border="1">
 * <th>Byte offset</th>
 * <th>Content</th>
 * <tr>
 * <td>0-1</td>
 * <td>Object ID</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Operation Type</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Reserved</td>
 * </tr>
 * <tr>
 * <td>4-7</td>
 * <td>Application ID</td>
 * </tr>
 * <tr>
 * <td>8-9</td>
 * <td>Length of payload (0�246)</td>
 * </tr>
 * <tr>
 * <td>[10-255]</td>
 * <td>Parameter (if needed for the object)</td>
 * </tr>
 * </table>
 * <p>
 * The maximum length of a CnS message is 255 bytes. The minimum length is 10.
 * <p>
 *
 */
public class CnS {

    // CnS offset number of bytes into the payload
    public static final int PAYLOAD_OFFSET = 10;

    private int objectId = 0;
    private int operationType = 0;
    private int reserved = 0;
    private long applicationId = 0L;
    private int payloadLength = 0;

    private byte[] request = null; // CnS request array
    private byte[] payload = null; // CnS payload array

    /**
     * CnS message constructor.
     *
     * <p>
     * CnS request constructor
     * <p>
     *
     * @param objectID
     *            - object ID
     * @param opType
     *            - operation type
     * @param appID
     *            - application ID
     */
    public CnS(int objectID, byte opType, int appID) {

        this.objectId = objectID;
        this.operationType = opType;

        List<Byte> alMsg = new ArrayList<>();

        // form "Object ID" field
        alMsg.add(Byte.valueOf((byte) (objectID >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (objectID >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (objectID & 0xff)));

        // form "Operation Type" field
        alMsg.add(Byte.valueOf(opType));
        alMsg.add(Byte.valueOf((byte) 0x00)); // reserved

        // form "Application ID" field
        alMsg.add(Byte.valueOf((byte) (appID >> 24 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID >> 16 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID & 0xff)));

        // form "Payload Length" field
        alMsg.add(Byte.valueOf((byte) 0x00));
        alMsg.add(Byte.valueOf((byte) 0x00));

        // form CnS request array
        this.request = new byte[alMsg.size()];
        for (int i = 0; i < alMsg.size(); i++) {
            this.request[i] = alMsg.get(i).byteValue();
        }
    }

    /**
     * CnS message constructor
     *
     * @param objectID
     *            - object ID
     * @param opType
     *            - operation type
     * @param appID
     *            - application ID
     * @param pload
     *            CnS payload
     */
    public CnS(int objectID, byte opType, int appID, byte[] pload) {

        this.objectId = objectID;
        this.operationType = opType;

        List<Byte> alMsg = new ArrayList<>();

        // form "Object ID" field
        alMsg.add(Byte.valueOf((byte) (objectID >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (objectID & 0xff)));

        // form "Operation Type" field
        alMsg.add(Byte.valueOf(opType));
        alMsg.add(Byte.valueOf((byte) 0x00)); // reserved

        // form "Application ID" field
        alMsg.add(Byte.valueOf((byte) (appID >> 24 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID >> 16 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID & 0xff)));

        // form "Payload Length" field
        int ploadlen = pload.length;
        alMsg.add(Byte.valueOf((byte) (ploadlen >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (ploadlen & 0xff)));

        for (int i = 0; i < ploadlen; i++) {
            alMsg.add(Byte.valueOf(pload[i]));
        }

        // form CnS request array
        this.request = new byte[alMsg.size()];
        for (int i = 0; i < alMsg.size(); i++) {
            this.request[i] = (byte) (alMsg.get(i).byteValue() & 0x0ff);
        }
    }

    /**
     * CnS message constructor
     *
     * @param objectID
     *            - object ID
     * @param opType
     *            - operation type
     * @param appID
     *            - application ID
     * @param param
     *            - payload
     */
    public CnS(int objectID, byte opType, int appID, int param) {

        this.objectId = objectID;
        this.operationType = opType;

        List<Byte> alMsg = new ArrayList<>();

        // form "Object ID" field
        alMsg.add(Byte.valueOf((byte) (objectID >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (objectID & 0xff)));

        // form "Operation Type" field
        alMsg.add(Byte.valueOf(opType));
        alMsg.add(Byte.valueOf((byte) 0x00)); // reserved

        // form "Application ID" field
        alMsg.add(Byte.valueOf((byte) (appID >> 24 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID >> 16 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID & 0xff)));

        // form "Payload Length" field
        alMsg.add(Byte.valueOf((byte) 0x00));
        alMsg.add(Byte.valueOf((byte) 0x02));

        alMsg.add(Byte.valueOf((byte) (param >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (param & 0xff)));

        // form CnS request array
        this.request = new byte[alMsg.size()];
        for (int i = 0; i < alMsg.size(); i++) {
            this.request[i] = alMsg.get(i).byteValue();
        }
    }

    /**
     * CnS message constructor
     *
     * @param objectID
     *            - object ID
     * @param opType
     *            - operation type
     * @param appID
     *            - application ID
     * @param param
     *            - payload
     */
    public CnS(int objectID, byte opType, int appID, long param) {

        this.objectId = objectID;
        this.operationType = opType;

        List<Byte> alMsg = new ArrayList<>();

        // form "Object ID" field
        alMsg.add(Byte.valueOf((byte) (objectID >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (objectID & 0xff)));

        // form "Operation Type" field
        alMsg.add(Byte.valueOf(opType));
        alMsg.add(Byte.valueOf((byte) 0x00)); // reserved

        // form "Application ID" field
        alMsg.add(Byte.valueOf((byte) (appID >> 24 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID >> 16 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (appID & 0xff)));

        // form "Payload Length" field
        alMsg.add(Byte.valueOf((byte) 0x00));
        alMsg.add(Byte.valueOf((byte) 0x04));

        alMsg.add(Byte.valueOf((byte) (param >> 24 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (param >> 16 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (param >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (param & 0x0ff)));

        // form CnS request array
        this.request = new byte[alMsg.size()];
        for (int i = 0; i < alMsg.size(); i++) {
            this.request[i] = alMsg.get(i).byteValue();
        }
    }

    /**
     * CnS reply constructor.
     *
     * @param msg
     *            byte array to hold CnS reply from modem
     */
    public CnS(byte[] msg) {

        this.objectId = msg[0] << 8 & 0x0ffff | msg[1] & 0x0ff;

        this.operationType = msg[2] & 0x0ff;
        this.reserved = msg[3] & 0x0ff;

        this.applicationId = (msg[4] << 24 & 0x0ffffffff | msg[5] << 16 & 0x0ffffff | msg[6] << 8 & 0x0ffff
                | msg[7] & 0x0ff) & 0x0ffffffffL;

        this.payloadLength = msg[8] << 8 & 0x0ffff | msg[9] & 0x0ff;

        this.payload = new byte[this.payloadLength];
        for (int i = 0; i < this.payloadLength; i++) {
            this.payload[i] = msg[PAYLOAD_OFFSET + i];
        }
    }

    /**
     * Reports CnS request
     *
     * @return CnS request as byte array
     */
    public byte[] getRequest() {
        return this.request;
    }

    /**
     * Reports payload of CnS message
     *
     * @return payload as byte array
     */
    public byte[] getPayload() {
        return this.payload;
    }

    /**
     * Reports CnS Application ID
     *
     * @return application ID
     */
    public long getApplicationId() {
        return this.applicationId;
    }

    /**
     * Reports CnS object ID
     *
     * @return object ID
     */
    public int getObjectId() {
        return this.objectId;
    }

    /**
     * Reports CnS operation type
     *
     * @return operation type
     */
    public int getOperationType() {
        return this.operationType;
    }

    /**
     * Reports the length of Cns payload
     *
     * @return payload length
     */
    public int getPayloadLength() {
        return this.payloadLength;
    }

    /**
     * Reports reserved word
     *
     * @return - reserved word
     */
    public int getReserved() {
        return this.reserved;
    }
}
