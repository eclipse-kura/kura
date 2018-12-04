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
 * Describes HIP (Host Interface Protocol) object.
 * <p>
 * The Host Interface Protocol is a multiplexing layer designed to allow
 * several types of message streams to share the same physical link layer. CnS is
 * one of the supported streams.
 * HIP is used to carry control and status data between the modem and the host to
 * provide management of the modem device. This management consists of:
 * <p>
 * � Non-volatile configuration of the device
 * <p>
 * � Run-time configuration of the device
 * <p>
 * � Status reporting and monitoring of the device
 * <p>
 * This protocol does not include checksums. It relies on a high-reliability physical
 * interface between the host and the modem to reduce the processing burden on the
 * microprocessor.
 * <p>
 * HIP format:
 * <table border="1">
 * <th>Byte offset</th>
 * <th>Content</th>
 * <tr>
 * <td>0</td>
 * <td>Framing character (0x7E)</td>
 * </tr>
 * <tr>
 * <td>1-2</td>
 * <td>Length of ooptional payload</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Message ID</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>Parameter</td>
 * </tr>
 * <tr>
 * <td>5-n</td>
 * <td>Optional payload</td>
 * </tr>
 * <tr>
 * <td>n+1</td>
 * <td>Framing character (0x7E)</td>
 * </tr>
 * </table>
 * <p>
 * The minimum length of a HIP packet is six bytes, for a packet without the
 * optional payload. Note that such a packet would have the first field set
 * to 0x0000�no payload.
 * <p>
 *
 */
public class Hip {

    /**
     * Indicates host to modem message direction.
     */
    public static final byte MSGID_CNS_HOST2MODEM = (byte) 0x2B;

    /**
     * Indicates modem to host message direction.
     */
    public static final byte MSGID_CNS_MODEM2HOST = (byte) 0x6B;

    /**
     * Minimum message size.
     */
    public static final int MIN_MSG_SIZE = 6;

    /**
     * Frame byte delimiter.
     */
    public static final byte FRAME_BYTE = (byte) 0x7e;

    private static final byte ESCAPE_BYTE = (byte) 0x7d;
    private static final byte MASK_BYTE = (byte) 0x20;

    private static final int MESSAGE_ID_OFFSET = 2;
    private static final int PARAMETER_OFFSET = 3;
    private static final int PAYLOAD_OFFSET = 4;

    private int payloadlength = 0;
    private byte messageId = 0;
    private byte parameter = 0;

    private byte[] hipmsg = null;
    private byte[] payload = null;

    private boolean is_error = false;

    /**
     * HIP request constructor
     *
     * @param payload
     *            - HIP payload
     */
    public Hip(byte[] payload) {

        this.messageId = MSGID_CNS_HOST2MODEM;
        this.payloadlength = payload.length;

        List<Byte> alMsg = new ArrayList<>();

        // form HIP message
        alMsg.add(Byte.valueOf((byte) (this.payloadlength >> 8 & 0xff)));
        alMsg.add(Byte.valueOf((byte) (this.payloadlength & 0xff)));
        alMsg.add(Byte.valueOf(this.messageId));
        alMsg.add(Byte.valueOf(this.parameter));
        for (int i = 0; i < this.payloadlength; i++) {
            alMsg.add(Byte.valueOf(payload[i]));
        }

        // perform escape character processing
        for (int i = 0; i < alMsg.size(); i++) {
            byte b = alMsg.get(i).byteValue();
            if (b == FRAME_BYTE || b == ESCAPE_BYTE) {
                alMsg.remove(i);
                alMsg.add(i++, Byte.valueOf(ESCAPE_BYTE));
                alMsg.add(i, Byte.valueOf((byte) (b ^ MASK_BYTE)));
            }
        }

        // add start and end framing bytes
        alMsg.add(0, Byte.valueOf(FRAME_BYTE));
        alMsg.add(Byte.valueOf(FRAME_BYTE));

        // convert message to array of bytes
        this.hipmsg = new byte[alMsg.size()];
        for (int i = 0; i < this.hipmsg.length; i++) {
            this.hipmsg[i] = alMsg.get(i).byteValue();
        }
    }

    /**
     * HIP reply constructor
     *
     * @param alMsg
     *            - HIP reply
     */
    public Hip(List<Byte> alMsg) {

        // convert supplied message to array of bytes
        this.hipmsg = new byte[alMsg.size()];
        for (int i = 0; i < this.hipmsg.length; i++) {
            this.hipmsg[i] = alMsg.get(i).byteValue();
        }

        // remove starting and ending flags
        try {
            if (alMsg.get(alMsg.size() - 1).byteValue() == FRAME_BYTE) {
                alMsg.remove(alMsg.size() - 1);
            }
            if (alMsg.get(0).byteValue() == FRAME_BYTE) {
                alMsg.remove(0);
            }

            // perform escape character processing
            for (int i = 0; i < alMsg.size(); i++) {
                byte b = alMsg.get(i).byteValue();
                if (b == ESCAPE_BYTE) {
                    alMsg.remove(i);
                    b = alMsg.get(i).byteValue();
                    alMsg.remove(i);
                    alMsg.add(i, Byte.valueOf((byte) (b ^ MASK_BYTE)));
                }
            }

            // get HIP header fields

            this.payloadlength = alMsg.get(0).byteValue() << 8 & 0x0ffff | alMsg.get(1).byteValue() & 0x0ff;

            this.messageId = alMsg.get(MESSAGE_ID_OFFSET).byteValue();
            this.parameter = alMsg.get(PARAMETER_OFFSET).byteValue();

            // get HIP payload

            this.payload = new byte[this.payloadlength];
            for (int i = 0; i < this.payloadlength; i++) {
                this.payload[i] = alMsg.get(PAYLOAD_OFFSET + i).byteValue();
            }
        } catch (Exception e) {
            this.is_error = true;
        }
    }

    /**
     * Reports HIP message
     *
     * @return byte [] - message
     */
    public byte[] getHIPmessage() {
        return this.hipmsg;
    }

    /**
     * Reports HIP payload
     *
     * @return HIP payload
     */
    public byte[] getPayload() {
        return this.payload;
    }

    /**
     * Reports message ID
     *
     * @return Message ID
     */
    public byte getMessageID() {
        return this.messageId;
    }

    /**
     * Reports if there is an error in processing HIP reply
     *
     * @return boolean <br>
     *         true - error <br>
     *         false - no error
     */
    public boolean isError() {
        return this.is_error;
    }
}
