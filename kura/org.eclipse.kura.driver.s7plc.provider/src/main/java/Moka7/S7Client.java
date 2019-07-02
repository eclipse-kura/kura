/*=============================================================================|
|  PROJECT Moka7                                                         1.0.2 |
|==============================================================================|
|  Copyright (C) 2013, 2016 Davide Nardella                                    |
|  All rights reserved.                                                        |
|==============================================================================|
|  SNAP7 is free software: you can redistribute it and/or modify               |
|  it under the terms of the Lesser GNU General Public License as published by |
|  the Free Software Foundation, either version 3 of the License, or under     |
|  EPL Eclipse Public License 1.0.                                             |
|                                                                              |
|  This means that you have to chose in advance which take before you import   |
|  the library into your project.                                              |
|                                                                              |
|  SNAP7 is distributed in the hope that it will be useful,                    |
|  but WITHOUT ANY WARRANTY; without even the implied warranty of              |
|  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE whatever license you    |
|  decide to adopt.                                                            |
|                                                                              |
|=============================================================================*/
package Moka7;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;

/**
 *
 * @author Dave Nardella
 */
public class S7Client {

    // WordLength
    private static final byte S7WLByte = 0x02;
    private static final byte S7WLCounter = 0x1C;
    private static final byte S7WLTimer = 0x1D;
    // Error Codes
    public static final int errTCPConnectionFailed = 0x0001;
    public static final int errTCPDataSend = 0x0002;
    public static final int errTCPDataRecv = 0x0003;
    public static final int errTCPDataRecvTout = 0x0004;
    public static final int errTCPConnectionReset = 0x0005;
    public static final int errISOInvalidPDU = 0x0006;
    public static final int errISOConnectionFailed = 0x0007;
    public static final int errISONegotiatingPDU = 0x0008;
    public static final int errS7InvalidPDU = 0x0009;
    public static final int errS7DataRead = 0x000A;
    public static final int errS7DataWrite = 0x000B;
    public static final int errS7BufferTooSmall = 0x000C;
    public static final int errS7FunctionError = 0x000D;
    public static final int errS7InvalidParams = 0x000E;

    // Public fields
    public boolean Connected = false;
    public int LastError = 0;
    public int RecvTimeout = 5000;

    // Privates
    private static final int ISOTCP = 102; // ISOTCP Port
    private static final int MinPduSize = 16;
    private static final int DefaultPduSizeRequested = 480;
    private static final int IsoHSize = 7; // TPKT+COTP Header Size
    private static final int MaxPduSize = DefaultPduSizeRequested + IsoHSize;
    private Socket TCPSocket;
    private final byte[] PDU = new byte[2048];

    private DataInputStream InStream = null;
    private DataOutputStream OutStream = null;

    private String IPAddress;
    private int TCPPort;

    private byte LocalTSAP_HI;
    private byte LocalTSAP_LO;
    private byte RemoteTSAP_HI;
    private byte RemoteTSAP_LO;
    private byte LastPDUType;

    private short ConnType = S7.PG;
    private int _PDULength = 0;

    // Telegrams
    // ISO Connection Request telegram (contains also ISO Header and COTP Header)
    private static final byte ISO_CR[] = {
            // TPKT (RFC1006 Header)
            (byte) 0x03, // RFC 1006 ID (3)
            (byte) 0x00, // Reserved, always 0
            (byte) 0x00, // High part of packet lenght (entire frame, payload and TPDU included)
            (byte) 0x16, // Low part of packet lenght (entire frame, payload and TPDU included)
            // COTP (ISO 8073 Header)
            (byte) 0x11, // PDU Size Length
            (byte) 0xE0, // CR - Connection Request ID
            (byte) 0x00, // Dst Reference HI
            (byte) 0x00, // Dst Reference LO
            (byte) 0x00, // Src Reference HI
            (byte) 0x01, // Src Reference LO
            (byte) 0x00, // Class + Options Flags
            (byte) 0xC0, // PDU Max Length ID
            (byte) 0x01, // PDU Max Length HI
            (byte) 0x0A, // PDU Max Length LO
            (byte) 0xC1, // Src TSAP Identifier
            (byte) 0x02, // Src TSAP Length (2 bytes)
            (byte) 0x01, // Src TSAP HI (will be overwritten)
            (byte) 0x00, // Src TSAP LO (will be overwritten)
            (byte) 0xC2, // Dst TSAP Identifier
            (byte) 0x02, // Dst TSAP Length (2 bytes)
            (byte) 0x01, // Dst TSAP HI (will be overwritten)
            (byte) 0x02  // Dst TSAP LO (will be overwritten)
    };

    // S7 PDU Negotiation Telegram (contains also ISO Header and COTP Header)
    private static final byte S7_PN[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x19, (byte) 0x02, (byte) 0xf0,
            (byte) 0x80, // TPKT + COTP (see above for info)
            (byte) 0x32, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x08,
            (byte) 0x00, (byte) 0x00, (byte) 0xf0, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x1e // PDU Length Requested = HI-LO 480 bytes
    };

    // S7 Read/Write Request Header (contains also ISO Header and COTP Header)
    private static final byte S7_RW[] = { // 31-35 bytes
            (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x1f,  // Telegram Length (Data Size + 31 or 35)
            (byte) 0x02, (byte) 0xf0, (byte) 0x80, // COTP (see above for info)
            (byte) 0x32,             // S7 Protocol ID
            (byte) 0x01,             // Job Type
            (byte) 0x00, (byte) 0x00,  // Redundancy identification
            (byte) 0x05, (byte) 0x00,  // PDU Reference
            (byte) 0x00, (byte) 0x0e,  // Parameters Length
            (byte) 0x00, (byte) 0x00,  // Data Length = Size(bytes) + 4
            (byte) 0x04,             // Function 4 Read Var, 5 Write Var
            (byte) 0x01,             // Items count
            (byte) 0x12,             // Var spec.
            (byte) 0x0a,             // Length of remaining bytes
            (byte) 0x10,             // Syntax ID
            S7WLByte,               // Transport Size
            (byte) 0x00, (byte) 0x00,  // Num Elements
            (byte) 0x00, (byte) 0x00,  // DB Number (if any, else 0)
            (byte) 0x84,             // Area Type
            (byte) 0x00, (byte) 0x00, (byte) 0x00, // Area Offset
            // WR area
            (byte) 0x00,             // Reserved
            (byte) 0x04,             // Transport size
            (byte) 0x00, (byte) 0x00,  // Data Length * 8 (if not timer or counter)
    };
    private static final int Size_RD = 31;
    private static final int Size_WR = 35;

    // S7 Get Block Info Request Header (contains also ISO Header and COTP Header)
    private static final byte S7_BI[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x25, (byte) 0x02, (byte) 0xf0,
            (byte) 0x80, (byte) 0x32, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x00,
            (byte) 0x08, (byte) 0x00, (byte) 0x0c, (byte) 0x00, (byte) 0x01, (byte) 0x12, (byte) 0x04, (byte) 0x11,
            (byte) 0x43, (byte) 0x03, (byte) 0x00, (byte) 0xff, (byte) 0x09, (byte) 0x00, (byte) 0x08, (byte) 0x30,
            (byte) 0x41, // Block Type
            (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, // ASCII Block Number
            (byte) 0x41 };

    // SZL First telegram request
    private static final byte S7_SZL_FIRST[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x21, (byte) 0x02,
            (byte) 0xf0, (byte) 0x80, (byte) 0x32, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x00, // Sequence
                                                                                                                    // out
            (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x12, (byte) 0x04,
            (byte) 0x11, (byte) 0x44, (byte) 0x01, (byte) 0x00, (byte) 0xff, (byte) 0x09, (byte) 0x00, (byte) 0x04,
            (byte) 0x00, (byte) 0x00, // ID (29)
            (byte) 0x00, (byte) 0x00  // Index (31)
    };

    // SZL Next telegram request
    private static final byte S7_SZL_NEXT[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x21, (byte) 0x02,
            (byte) 0xf0, (byte) 0x80, (byte) 0x32, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x00,
            (byte) 0x00, (byte) 0x0c, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x12, (byte) 0x08,
            (byte) 0x12, (byte) 0x44, (byte) 0x01, (byte) 0x01, // Sequence
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    // Get Date/Time request
    private static final byte S7_GET_DT[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x1d, (byte) 0x02,
            (byte) 0xf0, (byte) 0x80, (byte) 0x32, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x38, (byte) 0x00,
            (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x12, (byte) 0x04,
            (byte) 0x11, (byte) 0x47, (byte) 0x01, (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    // Set Date/Time command
    private static final byte S7_SET_DT[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x27, (byte) 0x02,
            (byte) 0xf0, (byte) 0x80, (byte) 0x32, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x89, (byte) 0x03,
            (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x0e, (byte) 0x00, (byte) 0x01, (byte) 0x12, (byte) 0x04,
            (byte) 0x11, (byte) 0x47, (byte) 0x02, (byte) 0x00, (byte) 0xff, (byte) 0x09, (byte) 0x00, (byte) 0x0a,
            (byte) 0x00, (byte) 0x19, // Hi part of Year
            (byte) 0x13, // Lo part of Year
            (byte) 0x12, // Month
            (byte) 0x06, // Day
            (byte) 0x17, // Hour
            (byte) 0x37, // Min
            (byte) 0x13, // Sec
            (byte) 0x00, (byte) 0x01 // ms + Day of week
    };

    // S7 STOP request
    private static final byte S7_STOP[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x21, (byte) 0x02,
            (byte) 0xf0, (byte) 0x80, (byte) 0x32, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x00,
            (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x29, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x50, (byte) 0x5f, (byte) 0x50, (byte) 0x52, (byte) 0x4f,
            (byte) 0x47, (byte) 0x52, (byte) 0x41, (byte) 0x4d };

    // S7 HOT Start request
    private static final byte S7_HOT_START[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x25, (byte) 0x02,
            (byte) 0xf0, (byte) 0x80, (byte) 0x32, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x00,
            (byte) 0x00, (byte) 0x14, (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfd, (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x50,
            (byte) 0x5f, (byte) 0x50, (byte) 0x52, (byte) 0x4f, (byte) 0x47, (byte) 0x52, (byte) 0x41, (byte) 0x4d };

    // S7 COLD Start request
    private static final byte S7_COLD_START[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x27, (byte) 0x02,
            (byte) 0xf0, (byte) 0x80, (byte) 0x32, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x0f, (byte) 0x00,
            (byte) 0x00, (byte) 0x16, (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfd, (byte) 0x00, (byte) 0x02, (byte) 0x43, (byte) 0x20,
            (byte) 0x09, (byte) 0x50, (byte) 0x5f, (byte) 0x50, (byte) 0x52, (byte) 0x4f, (byte) 0x47, (byte) 0x52,
            (byte) 0x41, (byte) 0x4d };

    // S7 Get PLC Status
    private static final byte S7_GET_STAT[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x21, (byte) 0x02,
            (byte) 0xf0, (byte) 0x80, (byte) 0x32, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x2c, (byte) 0x00,
            (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x12, (byte) 0x04,
            (byte) 0x11, (byte) 0x44, (byte) 0x01, (byte) 0x00, (byte) 0xff, (byte) 0x09, (byte) 0x00, (byte) 0x04,
            (byte) 0x04, (byte) 0x24, (byte) 0x00, (byte) 0x00 };

    // S7 Set Session Password
    private static final byte S7_SET_PWD[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x25, (byte) 0x02,
            (byte) 0xf0, (byte) 0x80, (byte) 0x32, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x27, (byte) 0x00,
            (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x0c, (byte) 0x00, (byte) 0x01, (byte) 0x12, (byte) 0x04,
            (byte) 0x11, (byte) 0x45, (byte) 0x01, (byte) 0x00, (byte) 0xff, (byte) 0x09, (byte) 0x00, (byte) 0x08,
            // 8 Char Encoded Password
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    // S7 Clear Session Password
    private static final byte S7_CLR_PWD[] = { (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x1d, (byte) 0x02,
            (byte) 0xf0, (byte) 0x80, (byte) 0x32, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x29, (byte) 0x00,
            (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x12, (byte) 0x04,
            (byte) 0x11, (byte) 0x45, (byte) 0x02, (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    public S7Client() {
        // Placeholder for future implementations
    }

    public static String ErrorText(int Error) {
        switch (Error) {
        case errTCPConnectionFailed:
            return "TCP Connection failed.";
        case errTCPDataSend:
            return "TCP Sending error.";
        case errTCPDataRecv:
            return "TCP Receiving error.";
        case errTCPDataRecvTout:
            return "Data Receiving timeout.";
        case errTCPConnectionReset:
            return "Connection reset by the peer.";
        case errISOInvalidPDU:
            return "Invalid ISO PDU received.";
        case errISOConnectionFailed:
            return "ISO connection refused by the CPU.";
        case errISONegotiatingPDU:
            return "ISO error negotiating the PDU length.";
        case errS7InvalidPDU:
            return "Invalid S7 PDU received.";
        case errS7DataRead:
            return "S7 Error reading data from the CPU.";
        case errS7DataWrite:
            return "S7 Error writing data to the CPU.";
        case errS7BufferTooSmall:
            return "The Buffer supplied to the function is too small.";
        case errS7FunctionError:
            return "S7 function refused by the CPU.";
        case errS7InvalidParams:
            return "Invalid parameters supplied to the function.";
        default:
            return "Unknown error : 0x" + Integer.toHexString(Error);
        }
    }

    private int TCPConnect() {
        SocketAddress sockaddr = new InetSocketAddress(this.IPAddress, TCPPort);
        this.LastError = 0;
        try {
            this.TCPSocket = new Socket();
            this.TCPSocket.connect(sockaddr, 5000);
            this.TCPSocket.setTcpNoDelay(true);
            this.InStream = new DataInputStream(this.TCPSocket.getInputStream());
            this.OutStream = new DataOutputStream(this.TCPSocket.getOutputStream());
        } catch (IOException e) {
            this.LastError = errTCPConnectionFailed;
        }
        return this.LastError;
    }

    private int WaitForData(int Size, int Timeout) {
        int cnt = 0;
        this.LastError = 0;
        int SizeAvail;
        boolean Expired = false;
        try {
            SizeAvail = this.InStream.available();
            while (SizeAvail < Size && !Expired && this.LastError == 0) {

                cnt++;
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    this.LastError = errTCPDataRecvTout;
                }
                SizeAvail = this.InStream.available();
                Expired = cnt > Timeout;
                // If timeout we clean the buffer
                if (Expired && SizeAvail > 0 && this.LastError == 0) {
                    this.InStream.read(this.PDU, 0, SizeAvail);
                }
            }
        } catch (IOException ex) {
            this.LastError = errTCPDataRecvTout;
        }
        if (cnt >= Timeout) {
            this.LastError = errTCPDataRecvTout;
        }
        return this.LastError;
    }

    private int RecvPacket(byte[] Buffer, int Start, int Size) {
        int BytesRead = 0;
        this.LastError = WaitForData(Size, this.RecvTimeout);
        if (this.LastError == 0) {
            try {
                BytesRead = this.InStream.read(Buffer, Start, Size);
            } catch (IOException ex) {
                this.LastError = errTCPDataRecv;
            }
            if (BytesRead == 0) {
                this.LastError = errTCPConnectionReset;
            }
        }
        return this.LastError;
    }

    private void SendPacket(byte[] Buffer, int Len) {
        this.LastError = 0;
        try {
            this.OutStream.write(Buffer, 0, Len);
            this.OutStream.flush();
        } catch (IOException ex) {
            this.Connected = false;
            try {
                this.Connect();
                try {
                    this.OutStream.write(Buffer, 0, Len);
                    this.OutStream.flush();
                } catch (IOException ex1) {
                    this.LastError = errTCPDataSend;
                }
            } catch (Exception e) {
                this.LastError = errTCPDataSend;
            }

        }
    }

    private void SendPacket(byte[] Buffer) {
        SendPacket(Buffer, Buffer.length);
    }

    private int RecvIsoPacket() {
        Boolean Done = false;
        int Size = 0;
        while (this.LastError == 0 && !Done) {
            // Get TPKT (4 bytes)
            RecvPacket(this.PDU, 0, 4);
            if (this.LastError == 0) {
                Size = S7.GetWordAt(this.PDU, 2);
                // Check 0 bytes Data Packet (only TPKT+COTP = 7 bytes)
                if (Size == IsoHSize) {
                    RecvPacket(this.PDU, 4, 3); // Skip remaining 3 bytes and Done is still false
                } else {
                    if (Size > MaxPduSize || Size < MinPduSize) {
                        this.LastError = errISOInvalidPDU;
                    } else {
                        Done = true; // a valid Length !=7 && >16 && <247
                    }
                }
            }
        }
        if (this.LastError == 0) {
            RecvPacket(this.PDU, 4, 3); // Skip remaining 3 COTP bytes
            this.LastPDUType = this.PDU[5];   // Stores PDU Type, we need it
            // Receives the S7 Payload
            RecvPacket(this.PDU, 7, Size - IsoHSize);
        }
        if (this.LastError == 0) {
            return Size;
        } else {
            return 0;
        }
    }

    private int ISOConnect() {
        int Size;
        ISO_CR[16] = this.LocalTSAP_HI;
        ISO_CR[17] = this.LocalTSAP_LO;
        ISO_CR[20] = this.RemoteTSAP_HI;
        ISO_CR[21] = this.RemoteTSAP_LO;

        // Sends the connection request telegram
        SendPacket(ISO_CR);
        if (this.LastError == 0) {
            // Gets the reply (if any)
            Size = RecvIsoPacket();
            if (this.LastError == 0) {
                if (Size == 22) {
                    if (this.LastPDUType != (byte) 0xD0) {
                        this.LastError = errISOConnectionFailed;
                    }
                } else {
                    this.LastError = errISOInvalidPDU;
                }
            }
        }
        return this.LastError;
    }

    private int NegotiatePduLength() {
        int Length;
        // Set PDU Size Requested
        S7.SetWordAt(S7_PN, 23, DefaultPduSizeRequested);
        // Sends the connection request telegram
        SendPacket(S7_PN);
        if (this.LastError == 0) {
            Length = RecvIsoPacket();
            if (this.LastError == 0) {
                // check S7 Error
                if (Length == 27 && this.PDU[17] == 0 && this.PDU[18] == 0)  // 20 = size of Negotiate Answer
                {
                    // Get PDU Size Negotiated
                    this._PDULength = S7.GetWordAt(this.PDU, 25);
                    if (this._PDULength > 0) {
                        return 0;
                    } else {
                        this.LastError = errISONegotiatingPDU;
                    }
                } else {
                    this.LastError = errISONegotiatingPDU;
                }
            }
        }
        return this.LastError;
    }

    public void SetConnectionType(short ConnectionType) {
        this.ConnType = ConnectionType;
    }

    public int Connect() {
        this.LastError = 0;
        if (!this.Connected) {
            TCPConnect();
            if (this.LastError == 0) // First stage : TCP Connection
            {
                ISOConnect();
                if (this.LastError == 0) // Second stage : ISOTCP (ISO 8073) Connection
                {
                    this.LastError = NegotiatePduLength(); // Third stage : S7 PDU negotiation
                }
            }
        }
        this.Connected = this.LastError == 0;

        // In case the connection is not completely established (TCP connection + ISO connection + PDU negotiation)
        // we close the socket and its IO streams to revert the object back to pre-Connect() state
        if (!this.Connected) {
            if (this.TCPSocket != null) {
                try {
                    this.TCPSocket.close();
                } catch (IOException ex) {
                }
            }
            if (this.InStream != null) {
                try {
                    this.InStream.close();
                } catch (IOException ex) {
                }
            }
            if (this.OutStream != null) {
                try {
                    this.OutStream.close();
                } catch (IOException ex) {
                }
            }
            this._PDULength = 0;
        }

        return this.LastError;
    }

    public void Disconnect() {
        if (this.Connected) {
            try {
                this.OutStream.close();
                this.InStream.close();
                this.TCPSocket.close();
                this._PDULength = 0;
            } catch (IOException ex) {
            }
            this.Connected = false;
        }
    }

    public int ConnectTo(String Address, int Rack, int Slot) {
        // Use the default port
        return ConnectTo(Address, Rack, Slot, ISOTCP);
    }

    public int ConnectTo(String Address, int Rack, int Slot, int Port) {
        int RemoteTSAP = (this.ConnType << 8) + Rack * 0x20 + Slot;
        SetConnectionParams(Address, Port, 0x0100, RemoteTSAP);
        return Connect();
    }

    public int PDULength() {
        return this._PDULength;
    }

    public void SetConnectionParams(String Address, int LocalTSAP, int RemoteTSAP) {
        SetConnectionParams(Address, ISOTCP, LocalTSAP, RemoteTSAP);
    }

    public void SetConnectionParams(String Address, int Port, int LocalTSAP, int RemoteTSAP) {
        int LocTSAP = LocalTSAP & 0x0000FFFF;
        int RemTSAP = RemoteTSAP & 0x0000FFFF;
        this.TCPPort = Port;
        this.IPAddress = Address;
        this.LocalTSAP_HI = (byte) (LocTSAP >> 8);
        this.LocalTSAP_LO = (byte) (LocTSAP & 0x00FF);
        this.RemoteTSAP_HI = (byte) (RemTSAP >> 8);
        this.RemoteTSAP_LO = (byte) (RemTSAP & 0x00FF);
    }

    public int ReadArea(int Area, int DBNumber, int Start, int Amount, byte[] Data) {
        int Address;
        int NumElements;
        int MaxElements;
        int TotElements;
        int SizeRequested;
        int Length;
        int Offset = 0;
        int WordSize = 1;

        this.LastError = 0;

        // If we are addressing Timers or counters the element size is 2
        if (Area == S7.S7AreaCT || Area == S7.S7AreaTM) {
            WordSize = 2;
        }

        MaxElements = (this._PDULength - 18) / WordSize; // 18 = Reply telegram header
        TotElements = Amount;

        while (TotElements > 0 && this.LastError == 0) {
            NumElements = TotElements;
            if (NumElements > MaxElements) {
                NumElements = MaxElements;
            }

            SizeRequested = NumElements * WordSize;

            // Setup the telegram
            System.arraycopy(S7_RW, 0, this.PDU, 0, Size_RD);
            // Set DB Number
            this.PDU[27] = (byte) Area;
            // Set Area
            if (Area == S7.S7AreaDB) {
                S7.SetWordAt(this.PDU, 25, DBNumber);
            }

            // Adjusts Start and word length
            if (Area == S7.S7AreaCT || Area == S7.S7AreaTM) {
                Address = Start;
                if (Area == S7.S7AreaCT) {
                    this.PDU[22] = S7WLCounter;
                } else {
                    this.PDU[22] = S7WLTimer;
                }
            } else {
                Address = Start << 3;
            }

            // Num elements
            S7.SetWordAt(this.PDU, 23, NumElements);

            // Address into the PLC (only 3 bytes)
            this.PDU[30] = (byte) (Address & 0x0FF);
            Address = Address >> 8;
            this.PDU[29] = (byte) (Address & 0x0FF);
            Address = Address >> 8;
            this.PDU[28] = (byte) (Address & 0x0FF);

            SendPacket(this.PDU, Size_RD);
            if (this.LastError == 0) {
                Length = RecvIsoPacket();
                if (this.LastError == 0) {
                    if (Length >= 25) {
                        if (Length - 25 == SizeRequested && this.PDU[21] == (byte) 0xFF
                                && SizeRequested <= (Data.length - Offset)) {
                            System.arraycopy(this.PDU, 25, Data, Offset, SizeRequested);
                            Offset += SizeRequested;
                        } else {
                            this.LastError = errS7DataRead;
                        }
                    } else {
                        this.LastError = errS7InvalidPDU;
                    }
                }
            }

            TotElements -= NumElements;
            Start += NumElements * WordSize;
        }
        return this.LastError;
    }

    public int WriteArea(int Area, int DBNumber, int Start, int Amount, byte[] Data) {
        int Address;
        int NumElements;
        int MaxElements;
        int TotElements;
        int DataSize;
        int IsoSize;
        int Length;
        int Offset = 0;
        int WordSize = 1;

        this.LastError = 0;

        // If we are addressing Timers or counters the element size is 2
        if (Area == S7.S7AreaCT || Area == S7.S7AreaTM) {
            WordSize = 2;
        }

        MaxElements = (this._PDULength - 35) / WordSize; // 18 = Reply telegram header
        TotElements = Amount;

        while (TotElements > 0 && this.LastError == 0) {
            NumElements = TotElements;
            if (NumElements > MaxElements) {
                NumElements = MaxElements;
            }

            DataSize = NumElements * WordSize;
            IsoSize = Size_WR + DataSize;

            // Setup the telegram
            System.arraycopy(S7_RW, 0, this.PDU, 0, Size_WR);
            // Whole telegram Size
            S7.SetWordAt(this.PDU, 2, IsoSize);
            // Data Length
            Length = DataSize + 4;
            S7.SetWordAt(this.PDU, 15, Length);
            // Function
            this.PDU[17] = (byte) 0x05;
            // Set DB Number
            this.PDU[27] = (byte) Area;
            if (Area == S7.S7AreaDB) {
                S7.SetWordAt(this.PDU, 25, DBNumber);
            }

            // Adjusts Start and word length
            if (Area == S7.S7AreaCT || Area == S7.S7AreaTM) {
                Address = Start;
                Length = DataSize;
                if (Area == S7.S7AreaCT) {
                    this.PDU[22] = S7WLCounter;
                } else {
                    this.PDU[22] = S7WLTimer;
                }
            } else {
                Address = Start << 3;
                Length = DataSize << 3;
            }
            // Num elements
            S7.SetWordAt(this.PDU, 23, NumElements);
            // Address into the PLC
            this.PDU[30] = (byte) (Address & 0x0FF);
            Address = Address >> 8;
            this.PDU[29] = (byte) (Address & 0x0FF);
            Address = Address >> 8;
            this.PDU[28] = (byte) (Address & 0x0FF);
            // Length
            S7.SetWordAt(this.PDU, 33, Length);

            // Copies the Data
            System.arraycopy(Data, Offset, this.PDU, 35, DataSize);

            SendPacket(this.PDU, IsoSize);
            if (this.LastError == 0) {
                Length = RecvIsoPacket();
                if (this.LastError == 0) {
                    if (Length == 22) {
                        if (S7.GetWordAt(this.PDU, 17) != 0 || this.PDU[21] != (byte) 0xFF) {
                            this.LastError = errS7DataWrite;
                        }
                    } else {
                        this.LastError = errS7InvalidPDU;
                    }
                }
            }

            Offset += DataSize;
            TotElements -= NumElements;
            Start += NumElements * WordSize;
        }
        return this.LastError;
    }

    public int GetAgBlockInfo(int BlockType, int BlockNumber, S7BlockInfo Block) {
        int Length;
        this.LastError = 0;
        // Block Type
        S7_BI[30] = (byte) BlockType;
        // Block Number
        S7_BI[31] = (byte) (BlockNumber / 10000 + 0x30);
        BlockNumber = BlockNumber % 10000;
        S7_BI[32] = (byte) (BlockNumber / 1000 + 0x30);
        BlockNumber = BlockNumber % 1000;
        S7_BI[33] = (byte) (BlockNumber / 100 + 0x30);
        BlockNumber = BlockNumber % 100;
        S7_BI[34] = (byte) (BlockNumber / 10 + 0x30);
        BlockNumber = BlockNumber % 10;
        S7_BI[35] = (byte) (BlockNumber / 1 + 0x30);

        SendPacket(S7_BI);
        if (this.LastError == 0) {
            Length = RecvIsoPacket();
            if (Length > 32) // the minimum expected
            {
                if (S7.GetWordAt(this.PDU, 27) == 0 && this.PDU[29] == (byte) 0xFF) {
                    Block.Update(this.PDU, 42);
                } else {
                    this.LastError = errS7FunctionError;
                }
            } else {
                this.LastError = errS7InvalidPDU;
            }
        }

        return this.LastError;
    }

    /**
     *
     * @param DBNumber
     *            DB Number
     * @param Buffer
     *            Destination buffer
     * @param SizeRead
     *            How many bytes were read
     * @return
     */
    public int DBGet(int DBNumber, byte[] Buffer, IntByRef SizeRead) {
        S7BlockInfo Block = new S7BlockInfo();
        // Query the DB Length
        this.LastError = GetAgBlockInfo(S7.Block_DB, DBNumber, Block);
        if (this.LastError == 0) {
            int SizeToRead = Block.MC7Size();
            // Checks the room
            if (SizeToRead <= Buffer.length) {
                this.LastError = ReadArea(S7.S7AreaDB, DBNumber, 0, SizeToRead, Buffer);
                if (this.LastError == 0) {
                    SizeRead.Value = SizeToRead;
                }
            } else {
                this.LastError = errS7BufferTooSmall;
            }
        }
        return this.LastError;
    }

    public int ReadSZL(int ID, int Index, S7Szl SZL) {
        int Length;
        int DataSZL;
        int Offset = 0;
        boolean Done = false;
        boolean First = true;
        byte Seq_in = 0x00;
        int Seq_out = 0x0000;

        this.LastError = 0;
        SZL.DataSize = 0;
        do {
            if (First) {
                S7.SetWordAt(S7_SZL_FIRST, 11, ++Seq_out);
                S7.SetWordAt(S7_SZL_FIRST, 29, ID);
                S7.SetWordAt(S7_SZL_FIRST, 31, Index);
                SendPacket(S7_SZL_FIRST);
            } else {
                S7.SetWordAt(S7_SZL_NEXT, 11, ++Seq_out);
                this.PDU[24] = Seq_in;
                SendPacket(S7_SZL_NEXT);
            }
            if (this.LastError != 0) {
                return this.LastError;
            }

            Length = RecvIsoPacket();
            if (this.LastError == 0) {
                if (First) {
                    if (Length > 32) // the minimum expected
                    {
                        if (S7.GetWordAt(this.PDU, 27) == 0 && this.PDU[29] == (byte) 0xFF) {
                            // Gets Amount of this slice
                            DataSZL = S7.GetWordAt(this.PDU, 31) - 8; // Skips extra params (ID, Index ...)
                            Done = this.PDU[26] == 0x00;
                            Seq_in = this.PDU[24]; // Slice sequence

                            SZL.LENTHDR = S7.GetWordAt(this.PDU, 37);
                            SZL.N_DR = S7.GetWordAt(this.PDU, 39);
                            SZL.Copy(this.PDU, 41, Offset, DataSZL);
                            Offset += DataSZL;
                            SZL.DataSize += DataSZL;
                        } else {
                            this.LastError = errS7FunctionError;
                        }
                    } else {
                        this.LastError = errS7InvalidPDU;
                    }
                } else {
                    if (Length > 32) // the minimum expected
                    {
                        if (S7.GetWordAt(this.PDU, 27) == 0 && this.PDU[29] == (byte) 0xFF) {
                            // Gets Amount of this slice
                            DataSZL = S7.GetWordAt(this.PDU, 31);
                            Done = this.PDU[26] == 0x00;
                            Seq_in = this.PDU[24]; // Slice sequence
                            SZL.Copy(this.PDU, 37, Offset, DataSZL);
                            Offset += DataSZL;
                            SZL.DataSize += DataSZL;
                        } else {
                            this.LastError = errS7FunctionError;
                        }
                    } else {
                        this.LastError = errS7InvalidPDU;
                    }
                }
            }
            First = false;
        } while (!Done && this.LastError == 0);

        return this.LastError;
    }

    public int GetCpuInfo(S7CpuInfo Info) {
        S7Szl SZL = new S7Szl(1024);

        this.LastError = ReadSZL(0x001C, 0x0000, SZL);
        if (this.LastError == 0) {
            Info.Update(SZL.Data, 0);
        }
        return this.LastError;
    }

    public int GetCpInfo(S7CpInfo Info) {
        S7Szl SZL = new S7Szl(1024);

        this.LastError = ReadSZL(0x0131, 0x0001, SZL);
        if (this.LastError == 0) {
            Info.Update(SZL.Data, 0);
        }
        return this.LastError;
    }

    public int GetOrderCode(S7OrderCode Code) {
        S7Szl SZL = new S7Szl(1024);

        this.LastError = ReadSZL(0x0011, 0x0000, SZL);
        if (this.LastError == 0) {
            Code.Update(SZL.Data, 0, SZL.DataSize);
        }
        return this.LastError;
    }

    public int GetPlcDateTime(Date DateTime) {
        int Length;

        this.LastError = 0;
        SendPacket(S7_GET_DT);
        if (this.LastError == 0) {
            Length = RecvIsoPacket();
            if (Length > 30) // the minimum expected
            {
                if (S7.GetWordAt(this.PDU, 27) == 0 && this.PDU[29] == (byte) 0xFF) {
                    DateTime = S7.GetDateAt(this.PDU, 34);
                } else {
                    this.LastError = errS7FunctionError;
                }
            } else {
                this.LastError = errS7InvalidPDU;
            }
        }

        return this.LastError;
    }

    public int SetPlcDateTime(Date DateTime) {
        int Length;

        this.LastError = 0;
        S7.SetDateAt(S7_SET_DT, 31, DateTime);

        SendPacket(S7_SET_DT);
        if (this.LastError == 0) {
            Length = RecvIsoPacket();
            if (Length > 30) // the minimum expected
            {
                if (S7.GetWordAt(this.PDU, 27) != 0) {
                    this.LastError = errS7FunctionError;
                }
            } else {
                this.LastError = errS7InvalidPDU;
            }
        }

        return this.LastError;
    }

    public int SetPlcSystemDateTime() {
        return SetPlcDateTime(new Date());
    }

    public int PlcStop() {
        int Length;

        this.LastError = 0;
        SendPacket(S7_STOP);
        if (this.LastError == 0) {
            Length = RecvIsoPacket();
            if (Length > 18) // 18 is the minimum expected
            {
                if (S7.GetWordAt(this.PDU, 17) != 0) {
                    this.LastError = errS7FunctionError;
                }
            } else {
                this.LastError = errS7InvalidPDU;
            }
        }
        return this.LastError;
    }

    public int PlcHotStart() {
        int Length;

        this.LastError = 0;
        SendPacket(S7_HOT_START);
        if (this.LastError == 0) {
            Length = RecvIsoPacket();
            if (Length > 18) // the minimum expected
            {
                if (S7.GetWordAt(this.PDU, 17) != 0) {
                    this.LastError = errS7FunctionError;
                }
            } else {
                this.LastError = errS7InvalidPDU;
            }
        }
        return this.LastError;
    }

    public int PlcColdStart() {
        int Length;

        this.LastError = 0;
        SendPacket(S7_COLD_START);
        if (this.LastError == 0) {
            Length = RecvIsoPacket();
            if (Length > 18) // the minimum expected
            {
                if (S7.GetWordAt(this.PDU, 17) != 0) {
                    this.LastError = errS7FunctionError;
                }
            } else {
                this.LastError = errS7InvalidPDU;
            }
        }
        return this.LastError;
    }

    public int GetPlcStatus(IntByRef Status) {
        int Length;

        this.LastError = 0;
        SendPacket(S7_GET_STAT);
        if (this.LastError == 0) {
            Length = RecvIsoPacket();
            if (Length > 30) // the minimum expected
            {
                if (S7.GetWordAt(this.PDU, 27) == 0) {
                    switch (this.PDU[44]) {
                    case S7.S7CpuStatusUnknown:
                    case S7.S7CpuStatusRun:
                    case S7.S7CpuStatusStop:
                        Status.Value = this.PDU[44];
                        break;
                    default:
                        // Since RUN status is always 0x08 for all CPUs and CPs, STOP status
                        // sometime can be coded as 0x03 (especially for old cpu...)
                        Status.Value = S7.S7CpuStatusStop;
                    }
                } else {
                    this.LastError = errS7FunctionError;
                }
            } else {
                this.LastError = errS7InvalidPDU;
            }
        }
        return this.LastError;
    }

    public int SetSessionPassword(String Password) {
        byte[] pwd = { 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20 };
        int Length;

        this.LastError = 0;
        // Adjusts the Password length to 8
        if (Password.length() > 8) {
            Password = Password.substring(0, 8);
        } else {
            while (Password.length() < 8) {
                Password = Password + " ";
            }
        }

        try {
            pwd = Password.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            this.LastError = errS7InvalidParams;
        }
        if (this.LastError == 0) {
            // Encodes the password
            pwd[0] = (byte) (pwd[0] ^ 0x55);
            pwd[1] = (byte) (pwd[1] ^ 0x55);
            for (int c = 2; c < 8; c++) {
                pwd[c] = (byte) (pwd[c] ^ 0x55 ^ pwd[c - 2]);
            }
            System.arraycopy(pwd, 0, S7_SET_PWD, 29, 8);
            // Sends the telegrem
            SendPacket(S7_SET_PWD);
            if (this.LastError == 0) {
                Length = RecvIsoPacket();
                if (Length > 32) // the minimum expected
                {
                    if (S7.GetWordAt(this.PDU, 27) != 0) {
                        this.LastError = errS7FunctionError;
                    }
                } else {
                    this.LastError = errS7InvalidPDU;
                }
            }
        }
        return this.LastError;
    }

    public int ClearSessionPassword() {
        int Length;

        this.LastError = 0;
        SendPacket(S7_CLR_PWD);
        if (this.LastError == 0) {
            Length = RecvIsoPacket();
            if (Length > 30) // the minimum expected
            {
                if (S7.GetWordAt(this.PDU, 27) != 0) {
                    this.LastError = errS7FunctionError;
                }
            } else {
                this.LastError = errS7InvalidPDU;
            }
        }
        return this.LastError;
    }

    public int GetProtection(S7Protection Protection) {
        S7Szl SZL = new S7Szl(256);

        this.LastError = ReadSZL(0x0232, 0x0004, SZL);
        if (this.LastError == 0) {
            Protection.Update(SZL.Data);
        }
        return this.LastError;
    }

}
