package com.eurotech.example.modbus.slave;


public interface Communicate {
	public void connect();

	public void disconnect() throws ModbusProtocolException;

	public int getConnectStatus();

	public void msgTransaction() throws ModbusProtocolException;

}
