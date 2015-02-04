package com.eurotech.example.modbus.slave;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ModbusProtocolSlaveComm implements Communicate, Runnable{
	private static final Logger s_logger = LoggerFactory.getLogger(ModbusProtocolSlaveComm.class);
	private static final int DEVICE_INDEX= 0x01;
	private static final int DEVICE_TCP_PORT= 5502;
	private ServerSocket sSocket;

	private InputStream inStream;
	private OutputStream outStream;

	private ModbusProtocol mpsl;

	@Override
	public void connect() {
		// TODO Auto-generated method stub
		try {
			sSocket = new ServerSocket(DEVICE_TCP_PORT);
			mpsl = new ModbusProtocolSlaveImpl();
		} catch (IOException e) {
			s_logger.error("Failed to connect to remote: " + e);
		}
	}

	@Override
	public void disconnect() throws ModbusProtocolException {
		// TODO Auto-generated method stub

		try {
			if(inStream != null){
				inStream.close();
				inStream= null;
			}

			if(outStream != null){
				outStream.close();
				outStream= null;
			}

			if(sSocket != null){
				sSocket.close();
				sSocket = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public int getConnectStatus() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void msgTransaction() throws ModbusProtocolException {
		// TODO Auto-generated method stub
		byte[] request = new byte[262]; // response buffer
		int respIndex = 0;
		int minimumLength = 8; // default minimum message length
		byte[] response= null;

		while (true) {
			while (respIndex < minimumLength) {
				try {
					int resp = inStream.read(request, respIndex, minimumLength - respIndex);
					if (resp > 0) {
						respIndex += resp;
					} else {
						s_logger.error("Socket disconnect in recv");
						//disconnect();
						throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,"Recv failure");
					}
				} catch (SocketTimeoutException e) {
					String failMsg = "Recv timeout";
					s_logger.warn(failMsg);
					throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,failMsg);
				} catch (IOException e) {
					s_logger.error("Socket disconnect in recv: " + e);
					//disconnect();
					throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
							"Recv failure");
				}
			}

			// check for a valid request

			switch (request[1]) {
			case ModbusFunctionCodes.FORCE_SINGLE_COIL:
			case ModbusFunctionCodes.PRESET_SINGLE_REG:
			case ModbusFunctionCodes.FORCE_MULTIPLE_COILS:
			case ModbusFunctionCodes.READ_COIL_STATUS:
			case ModbusFunctionCodes.READ_INPUT_STATUS:
			case ModbusFunctionCodes.READ_INPUT_REGS:
				response= createErrorMessage(request[0], request[1], 0x01);
				break;

			case ModbusFunctionCodes.PRESET_MULTIPLE_REGS:
				int byteCnt = (request[6] & 0xff) + 9;
				if (respIndex < byteCnt)
					// wait for more data
					minimumLength = byteCnt;
				else if(request[0] == DEVICE_INDEX && Crc16.getCrc16(request, byteCnt, 0xffff) == 0){
					int address = buildShort(request[2], request[3]);
					int numRegisters = buildShort(request[4], request[5]);
					int byteCount= request[6];
					if (numRegisters > 1 && numRegisters < 125 && byteCount == (numRegisters * 2)){
						if(address > 0 && address < 65536 && (address + numRegisters) < 65536){
							int requestDataSize = respIndex-2;
							byte[] requestNoCRC=new byte[requestDataSize];

							for(int i=0; i< requestDataSize; i++){
								requestNoCRC[i] = request[i];
							}

							try{
								byte[] data= new byte[request[6]];
								int requestDataIndex= 7;
								for(int i= 0; i<data.length; i++){
									data[i]= request[requestDataIndex];
									requestDataIndex++;
								}
								response= mpsl.writeMultipleRegister(requestNoCRC[0], address, data, numRegisters);
							}catch(ModbusProtocolException e){
								response= createErrorMessage(request[0], request[1], 0x04);
							}
						}else {
							response= createErrorMessage(request[0], request[1], 0x02); 
						}
					}else{
						response= createErrorMessage(request[0], request[1], 0x03); 
					}
				}
				break;

			case ModbusFunctionCodes.READ_HOLDING_REGS:

				int address = buildShort(request[2], request[3]);
				int numRegisters = buildShort(request[4], request[5]);
				if(request[0] == DEVICE_INDEX && Crc16.getCrc16(request, respIndex, 0xffff) == 0){
					if (numRegisters > 1 && numRegisters < 125){
						if(address > 0 && address < 65536 && (address + numRegisters) < 65536){
							int requestDataSize = respIndex-2;
							byte[] requestNoCRC=new byte[requestDataSize];

							for(int i=0; i< requestDataSize; i++){
								requestNoCRC[i] = request[i];
							}
							s_logger.info("Request received from master. The request is correct, needs processing.");
							response= mpsl.readHoldingRegisters(requestNoCRC[0], address, numRegisters);
						}else {
							response= createErrorMessage(request[0], request[1], 0x02); 
						}
					}else{
						response= createErrorMessage(request[0], request[1], 0x03); 
					}
				}
			}
			if(response != null){
				writeResponseMessage(response);
				return;
			}
		}
	}

	private void writeResponseMessage(byte[] msg) throws ModbusProtocolException {
		// TODO Auto-generated method stub
		byte[] cmd = null;

		if(true){ //if(m_txMode == ModbusTransmissionMode.RTU_MODE){
			cmd = new byte[msg.length+2];
			for(int i=0; i<msg.length; i++)
				cmd[i]=msg[i];
			// Add crc calculation to end of message
			int crc = Crc16.getCrc16(msg, msg.length, 0x0ffff);
			cmd[msg.length] = (byte) crc;
			cmd[msg.length + 1] = (byte) (crc >> 8);
		}
		else 				
			throw new ModbusProtocolException(ModbusProtocolErrorCode.METHOD_NOT_SUPPORTED,"Only RTU over TCP/IP supported");


		// Send the message
		try {
			// flush input
			while (inStream.available() > 0)
				inStream.read();
			// send all data
			outStream.write(cmd, 0, cmd.length);
			outStream.flush();
		} catch (IOException e) {
			// Assume this means the socket is closed...make sure it is
			s_logger.error("Socket disconnect in send: " + e);
			disconnect();
			throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,"Send failure: "
					+ e.getMessage());
		}
	}

	private byte[] createErrorMessage(byte id, byte funCode, int exceptionCode) {
		// TODO Auto-generated method stub
		byte[] cmd = new byte[3];
		cmd[0] = id;
		cmd[1] = ((byte) ModbusFunctionCodes.READ_HOLDING_REGS) & 0x80;
		cmd[2] = (byte) exceptionCode;
		return cmd;
	}

	public int buildShort(byte high, byte low){
		return ((0xFF & (int) high) << 8) + ((0xFF & (int) low));
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			Socket connectionSocket = sSocket.accept();
			inStream= connectionSocket.getInputStream();
			outStream= connectionSocket.getOutputStream();
			while(true){
				msgTransaction();
			}
		} catch (IOException | ModbusProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			run();
		}

	}

}
