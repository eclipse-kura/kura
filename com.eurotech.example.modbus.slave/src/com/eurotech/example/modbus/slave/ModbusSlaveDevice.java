package com.eurotech.example.modbus.slave;

import java.util.Properties;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusSlaveDevice implements ModbusSlaveDeviceService{
	private static final Logger s_logger = LoggerFactory.getLogger(ModbusSlaveDevice.class);
	
	private ModbusProtocolSlaveComm m_communication;

	private ConnectionFactory m_connectionFactory;
	
	private Thread t;

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.m_connectionFactory = connectionFactory;
	}
	
	public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
		this.m_connectionFactory = null;
	}
	
	protected void activate(ComponentContext componentContext) 
	{
		s_logger.info("activate...");
		try {
			m_communication= new ModbusProtocolSlaveComm();
			connect();
		} catch (ModbusProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("deactivate...");
		try {
			disconnect();
		} catch (ModbusProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	@Override
	public String getProtocolName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureConnection(Properties connectionConfig)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getConnectStatus() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void connect() throws ModbusProtocolException {
		m_communication.connect();
		t= new Thread(m_communication);
		t.start();
	}

	@Override
	public void disconnect() throws ModbusProtocolException {
		// TODO Auto-generated method stub
		t.interrupt();
		m_communication.disconnect();
		m_communication= null;
	}

}
