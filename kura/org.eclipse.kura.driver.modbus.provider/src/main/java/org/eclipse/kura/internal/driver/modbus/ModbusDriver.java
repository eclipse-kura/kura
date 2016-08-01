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
package org.eclipse.kura.internal.driver.modbus;

import static com.ghgande.j2mod.modbus.Modbus.READ_COILS;
import static com.ghgande.j2mod.modbus.Modbus.READ_HOLDING_REGISTERS;
import static com.ghgande.j2mod.modbus.Modbus.READ_INPUT_DISCRETES;
import static com.ghgande.j2mod.modbus.Modbus.READ_INPUT_REGISTERS;
import static com.ghgande.j2mod.modbus.Modbus.WRITE_COIL;
import static com.ghgande.j2mod.modbus.Modbus.WRITE_MULTIPLE_COILS;
import static com.ghgande.j2mod.modbus.Modbus.WRITE_MULTIPLE_REGISTERS;
import static com.ghgande.j2mod.modbus.Modbus.WRITE_SINGLE_REGISTER;
import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.driver.DriverFlag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
import static org.eclipse.kura.driver.DriverFlag.READ_FAILURE;
import static org.eclipse.kura.driver.DriverFlag.READ_SUCCESSFUL;
import static org.eclipse.kura.driver.DriverFlag.WRITE_FAILURE;
import static org.eclipse.kura.driver.DriverFlag.WRITE_SUCCESSFUL;
import static org.eclipse.kura.internal.driver.modbus.FunctionCode.FC_01_READ_COILS;
import static org.eclipse.kura.internal.driver.modbus.FunctionCode.FC_02_READ_DISCRETE_INPUTS;
import static org.eclipse.kura.internal.driver.modbus.FunctionCode.FC_03_READ_HOLDING_REGISTERS;
import static org.eclipse.kura.internal.driver.modbus.FunctionCode.FC_04_READ_INPUT_REGISTERS;
import static org.eclipse.kura.internal.driver.modbus.FunctionCode.FC_05_WRITE_SINGLE_COIL;
import static org.eclipse.kura.internal.driver.modbus.FunctionCode.FC_06_WRITE_SINGLE_REGISTER;
import static org.eclipse.kura.internal.driver.modbus.FunctionCode.FC_15_WRITE_MULITPLE_COILS;
import static org.eclipse.kura.internal.driver.modbus.FunctionCode.FC_16_WRITE_MULTIPLE_REGISTERS;
import static org.eclipse.kura.internal.driver.modbus.ModbusType.RTU;
import static org.eclipse.kura.internal.driver.modbus.ModbusType.TCP;
import static org.eclipse.kura.internal.driver.modbus.ModbusType.UDP;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.ModbusDriverMessages;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.facade.ModbusUDPMaster;
import com.ghgande.j2mod.modbus.io.AbstractModbusTransport;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleCoilsRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.BitVector;
import com.ghgande.j2mod.modbus.util.SerialParameters;

/**
 * The Class ModbusDriver is a Modbus Driver implementation for Kura
 * Asset-Driver Topology.
 *
 * @see ModbusChannelDescriptor
 */
public final class ModbusDriver implements Driver {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(ModbusDriver.class);

	/** Localization Resource. */
	private static final ModbusDriverMessages s_message = LocalizationAdapter.adapt(ModbusDriverMessages.class);

	/** The Driver Service instance. */
	private volatile DriverService m_driverService;

	/** flag to check if the device is connected. */
	private boolean m_isConnected = false;

	/** Modbus Configuration Parser Options. */
	private ModbusOptions m_options;

	/** Modbus RTU Connection. */
	private ModbusSerialMaster m_rtuMaster;

	/** Modbus TCP Connection. */
	private ModbusTCPMaster m_tcpMaster;

	/** Modbus UDP Connection. */
	private ModbusUDPMaster m_udpMaster;

	/**
	 * OSGi service component callback while activation.
	 *
	 * @param componentContext
	 *            the component context
	 * @param properties
	 *            the service properties
	 */
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.debug(s_message.activating());
		this.extractProperties(properties);
		s_logger.debug(s_message.activatingDone());
	}

	/**
	 * Binds the Driver Service.
	 *
	 * @param driverService
	 *            the Driver Service instance
	 */
	public synchronized void bindDriverService(final DriverService driverService) {
		if (this.m_driverService == null) {
			this.m_driverService = driverService;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void connect() throws ConnectionException {
		if ((this.m_options.getType() == TCP) && (this.m_tcpMaster != null)) {
			try {
				this.m_tcpMaster.connect();
			} catch (final Exception e) {
				throw new ConnectionException(s_message.connectionProblem() + ThrowableUtil.stackTraceAsString(e));
			}
		}
		if ((this.m_options.getType() == UDP) && (this.m_udpMaster != null)) {
			try {
				this.m_udpMaster.connect();
			} catch (final Exception e) {
				throw new ConnectionException(s_message.connectionProblem() + ThrowableUtil.stackTraceAsString(e));
			}
		}
		if ((this.m_options.getType() == RTU) && (this.m_rtuMaster != null)) {
			try {
				this.m_rtuMaster.connect();
			} catch (final Exception e) {
				throw new ConnectionException(s_message.connectionProblem() + ThrowableUtil.stackTraceAsString(e));
			}
		}
		this.m_isConnected = true;
	}

	/**
	 * OSGi service component callback while deactivation.
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.debug(s_message.deactivating());
		try {
			this.disconnect();
		} catch (final ConnectionException e) {
			s_logger.error(s_message.errorDisconnecting() + ThrowableUtil.stackTraceAsString(e));
		}
		this.m_tcpMaster = null;
		this.m_udpMaster = null;
		this.m_rtuMaster = null;
		s_logger.debug(s_message.deactivatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect() throws ConnectionException {
		if ((this.m_options.getType() == TCP) && (this.m_tcpMaster != null)) {
			try {
				this.m_tcpMaster.disconnect();
			} catch (final Exception e) {
				throw new ConnectionException(s_message.disconnectionProblem() + ThrowableUtil.stackTraceAsString(e));
			}
		}
		if ((this.m_options.getType() == UDP) && (this.m_udpMaster != null)) {
			try {
				this.m_udpMaster.disconnect();
			} catch (final Exception e) {
				throw new ConnectionException(s_message.disconnectionProblem() + ThrowableUtil.stackTraceAsString(e));
			}
		}
		if ((this.m_options.getType() == RTU) && (this.m_rtuMaster != null)) {
			try {
				this.m_rtuMaster.disconnect();
			} catch (final Exception e) {
				throw new ConnectionException(s_message.disconnectionProblem() + ThrowableUtil.stackTraceAsString(e));
			}
		}
		this.m_isConnected = false;
	}

	/**
	 * Extract the Modbus specific configurations from the provided properties.
	 *
	 * @param properties
	 *            the provided properties to parse
	 */
	private void extractProperties(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		this.m_options = new ModbusOptions(properties);
		switch (this.m_options.getType()) {
		case TCP:
			this.m_tcpMaster = new ModbusTCPMaster(this.m_options.getIp(), this.m_options.getPort());
			break;
		case UDP:
			this.m_udpMaster = new ModbusUDPMaster(this.m_options.getIp(), this.m_options.getPort());
			break;
		case RTU:
			final SerialParameters parameters = new SerialParameters("", this.m_options.getBaudrate(),
					this.m_options.getFlowControlIn(), this.m_options.getFlowControlOut(), this.m_options.getDatabits(),
					this.m_options.getStopbits(), this.m_options.getParity(), false);
			this.m_rtuMaster = new ModbusSerialMaster(parameters);
			break;
		default:
			break;
		}
	}

	/** {@inheritDoc} */
	@Override
	public ChannelDescriptor getChannelDescriptor() {
		return new ModbusChannelDescriptor();
	}

	/**
	 * Get the integer value of the provided function code.
	 *
	 * @param primaryTable
	 *            the string representation of the address space
	 * @return the function code
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private int getFunctionCode(final String primaryTable) {
		checkNull(primaryTable, s_message.primaryTableNonNull());
		if ("COILS".equals(primaryTable)) {
			return READ_COILS;
		}
		if ("DISCRETE_INPUTS".equals(primaryTable)) {
			return READ_INPUT_DISCRETES;
		}
		if ("INPUT_REGISTERS".equals(primaryTable)) {
			return READ_INPUT_REGISTERS;
		}
		if ("HOLDING_REGISTERS".equals(primaryTable)) {
			return READ_HOLDING_REGISTERS;
		}
		return 0;
	}

	/**
	 * Gets the Typed value as found in the provided response
	 *
	 * @param response
	 *            the provided Modbus response
	 * @return the value
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private TypedValue<?> getValue(final ModbusResponse response) {
		checkNull(response, s_message.responseNonNull());
		if (response instanceof ReadInputRegistersResponse) {
			return TypedValues.newIntegerValue(((ReadInputRegistersResponse) response).getRegisterValue(0));
		}
		if (response instanceof ReadMultipleRegistersResponse) {
			return TypedValues.newIntegerValue(((ReadMultipleRegistersResponse) response).getRegisterValue(0));
		}
		if (response instanceof ReadInputDiscretesResponse) {
			return TypedValues.newBooleanValue(((ReadInputDiscretesResponse) response).getDiscreteStatus(0));
		}
		if (response instanceof ReadCoilsResponse) {
			return TypedValues.newBooleanValue(((ReadCoilsResponse) response).getCoilStatus(0));
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> read(final List<DriverRecord> records) throws ConnectionException {
		if (!this.m_isConnected) {
			this.connect();
		}
		for (final DriverRecord record : records) {
			final int unitId = Integer.valueOf(record.getChannelConfig().get("unit.id").toString());
			final String primaryTable = record.getChannelConfig().get("primary.table").toString();
			final int functionCode = this.getFunctionCode(primaryTable);
			final int memoryAddr = Integer.valueOf(record.getChannelConfig().get("memory.address").toString());
			final int regCount = Integer.valueOf(record.getChannelConfig().get("register.count").toString());
			AbstractModbusTransport transport = null;
			if ((this.m_options.getType() == TCP) && (this.m_tcpMaster != null)) {
				transport = this.m_tcpMaster.getTransport();
			}
			if ((this.m_options.getType() == RTU) && (this.m_rtuMaster != null)) {
				transport = this.m_rtuMaster.getTransport();
			}
			if ((this.m_options.getType() == UDP) && (this.m_udpMaster != null)) {
				transport = this.m_udpMaster.getTransport();
			}
			try {
				final ModbusResponse response = this.readRequest(unitId, transport, functionCode, memoryAddr, regCount);
				final TypedValue<?> value = this.getValue(response);
				record.setValue(value);
				record.setDriverStatus(this.m_driverService.newDriverStatus(READ_SUCCESSFUL, null, null));
			} catch (final ModbusException e) {
				record.setDriverStatus(this.m_driverService.newDriverStatus(READ_FAILURE, null, e));
			} catch (final KuraRuntimeException e) {
				record.setDriverStatus(this.m_driverService.newDriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.transportNonNull() + " OR " + s_message.wrongUnitId(), e));
			}
		}
		return records;
	}

	/**
	 * Executes a read transaction using the function code, register and count.
	 *
	 * @param unitId
	 *            the Unit ID to connect
	 * @param modbusTransport
	 *            the Modbus transport instance
	 * @param functionCode
	 *            Function code to use
	 * @param register
	 *            Register number
	 * @param count
	 *            Number of registers
	 * @return Response object
	 * @throws ModbusException
	 *             the modbus exception
	 * @throws KuraRuntimeException
	 *             if the transport is null or the function code is wrongly set
	 *             or the unit ID is wrongly set
	 */
	private ModbusResponse readRequest(final int unitId, final AbstractModbusTransport modbusTransport,
			final int functionCode, final int register, final int count) throws ModbusException {
		checkNull(modbusTransport, s_message.transportNonNull());
		checkCondition(
				(functionCode != FC_01_READ_COILS.code()) || (functionCode != FC_02_READ_DISCRETE_INPUTS.code())
						|| (functionCode != FC_03_READ_HOLDING_REGISTERS.code())
						|| (functionCode != FC_04_READ_INPUT_REGISTERS.code())
						|| (functionCode != FC_05_WRITE_SINGLE_COIL.code())
						|| (functionCode != FC_06_WRITE_SINGLE_REGISTER.code())
						|| (functionCode != FC_15_WRITE_MULITPLE_COILS.code())
						|| (functionCode != FC_16_WRITE_MULTIPLE_REGISTERS.code()),
				s_message.functionCodesNotInRange());
		checkCondition((unitId > 0) && (unitId < 247), s_message.wrongUnitId());

		ModbusTransaction trans;
		try {
			ModbusRequest req = null;
			// Prepare the request
			switch (functionCode) {
			case Modbus.READ_COILS:
				req = new ReadCoilsRequest(register, count);
				break;
			case Modbus.READ_INPUT_DISCRETES:
				req = new ReadInputDiscretesRequest(register, count);
				break;
			case Modbus.READ_INPUT_REGISTERS:
				req = new ReadInputRegistersRequest(register, count);
				break;
			case Modbus.READ_HOLDING_REGISTERS:
				req = new ReadMultipleRegistersRequest(register, count);
				break;
			default:
				throw new ModbusException(s_message.requestTypeNotSupported(functionCode));
			}
			req.setUnitID(unitId);

			// Prepare the transaction
			trans = modbusTransport.createTransaction();
			trans.setRequest(req);
			if (trans instanceof ModbusTCPTransaction) {
				((ModbusTCPTransaction) trans).setReconnecting(true);
			}

			// Execute the transaction
			trans.execute();
			return trans.getResponse();
		} finally {
			try {
				modbusTransport.close();
			} catch (final IOException e) {
				s_logger.error(ThrowableUtil.stackTraceAsString(e));
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void registerDriverListener(final Map<String, Object> channelConfig, final DriverListener listener)
			throws ConnectionException {
		throw new KuraRuntimeException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
	}

	/**
	 * Unbinds the Driver Service.
	 *
	 * @param driverService
	 *            the Driver Service instance
	 */
	public synchronized void unbindDriverService(final DriverService driverService) {
		if (this.m_driverService == driverService) {
			this.m_driverService = null;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void unregisterDriverListener(final DriverListener listener) throws ConnectionException {
		throw new KuraRuntimeException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
	}

	/**
	 * OSGi service component callback while updating.
	 *
	 * @param properties
	 *            the properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updating());
		this.extractProperties(properties);
		s_logger.debug(s_message.updatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> write(final List<DriverRecord> records) throws ConnectionException {
		if (!this.m_isConnected) {
			this.connect();
		}
		for (final DriverRecord record : records) {
			final int unitId = Integer.valueOf(record.getChannelConfig().get("unit.id").toString());
			final String primaryTable = record.getChannelConfig().get("primary.table").toString();
			final int functionCode = this.getFunctionCode(primaryTable);
			final int memoryAddr = Integer.valueOf(record.getChannelConfig().get("memory.address").toString());
			AbstractModbusTransport transport = null;
			if ((this.m_options.getType() == TCP) && (this.m_tcpMaster != null)) {
				transport = this.m_tcpMaster.getTransport();
			}
			if ((this.m_options.getType() == RTU) && (this.m_rtuMaster != null)) {
				transport = this.m_rtuMaster.getTransport();
			}
			if ((this.m_options.getType() == UDP) && (this.m_udpMaster != null)) {
				transport = this.m_udpMaster.getTransport();
			}
			try {
				this.writeRequest(unitId, transport, functionCode, memoryAddr,
						Integer.valueOf(record.getValue().getValue().toString()));
				record.setDriverStatus(this.m_driverService.newDriverStatus(WRITE_SUCCESSFUL, null, null));
			} catch (final ModbusException e) {
				record.setDriverStatus(this.m_driverService.newDriverStatus(WRITE_FAILURE, null, e));
			} catch (final KuraRuntimeException e) {
				record.setDriverStatus(this.m_driverService.newDriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.transportNonNull() + " OR " + s_message.wrongUnitId(), e));
			}
		}
		return records;
	}

	/**
	 * Executes a write transaction using the function code, register and value
	 *
	 * @param unitId
	 *            the Unit ID to connect
	 * @param modbusTransport
	 *            the Modbus transport instance
	 * @param functionCode
	 *            Function code to use
	 * @param register
	 *            Register number
	 * @param values
	 *            Values to apply
	 * @return Response object
	 * @throws KuraRuntimeException
	 *             if the transport is null or the function code is wrongly set
	 *             or the unit ID is wrongly set
	 */
	private ModbusResponse writeRequest(final int unitId, final AbstractModbusTransport modbusTransport,
			final int functionCode, final int register, final int... values) throws ModbusException {
		checkNull(modbusTransport, s_message.transportNonNull());
		checkCondition(
				(functionCode != FC_01_READ_COILS.code()) || (functionCode != FC_02_READ_DISCRETE_INPUTS.code())
						|| (functionCode != FC_03_READ_HOLDING_REGISTERS.code())
						|| (functionCode != FC_04_READ_INPUT_REGISTERS.code())
						|| (functionCode != FC_05_WRITE_SINGLE_COIL.code())
						|| (functionCode != FC_06_WRITE_SINGLE_REGISTER.code())
						|| (functionCode != FC_15_WRITE_MULITPLE_COILS.code())
						|| (functionCode != FC_16_WRITE_MULTIPLE_REGISTERS.code()),
				s_message.functionCodesNotInRange());
		checkCondition((unitId > 0) && (unitId < 247), s_message.wrongUnitId());

		ModbusTransaction trans;
		try {
			ModbusRequest req = null;
			// Prepare the request
			switch (functionCode) {
			case WRITE_COIL:
				req = new WriteCoilRequest(register, values[0] != 0);
				break;
			case WRITE_SINGLE_REGISTER:
				req = new WriteSingleRegisterRequest(register, new SimpleRegister(values[0]));
				break;
			case WRITE_MULTIPLE_REGISTERS:
				final Register[] regs = new Register[values.length];
				for (int i = 0; i < values.length; i++) {
					regs[i] = new SimpleRegister(values[i]);
				}
				req = new WriteMultipleRegistersRequest(register, regs);
				break;
			case WRITE_MULTIPLE_COILS:
				final BitVector bitVector = new BitVector(values.length);
				for (int i = 0; i < values.length; i++) {
					bitVector.setBit(i, values[i] != 0);
				}
				req = new WriteMultipleCoilsRequest(register, bitVector);
				break;
			default:
				throw new ModbusException(s_message.requestTypeNotSupported(functionCode));
			}
			req.setUnitID(unitId);

			// Prepare the transaction
			trans = modbusTransport.createTransaction();
			trans.setRequest(req);
			if (trans instanceof ModbusTCPTransaction) {
				((ModbusTCPTransaction) trans).setReconnecting(true);
			}

			// Execute the transaction
			trans.execute();
			return trans.getResponse();
		} finally {
			try {
				modbusTransport.close();
			} catch (final IOException e) {
				s_logger.error(ThrowableUtil.stackTraceAsString(e));
			}
		}
	}

}
