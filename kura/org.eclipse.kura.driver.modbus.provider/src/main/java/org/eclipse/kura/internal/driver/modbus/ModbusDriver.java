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
import static org.eclipse.kura.driver.DriverConstants.CHANNEL_VALUE_TYPE;
import static org.eclipse.kura.driver.DriverFlag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
import static org.eclipse.kura.driver.DriverFlag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION;
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
import static org.eclipse.kura.type.DataType.BOOLEAN;
import static org.eclipse.kura.type.DataType.INTEGER;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.ModbusDriverMessages;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.base.TypeUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Asset-Driver Topology. This Modbus Driver needs specific properties to be
 * provided externally. <br/>
 * <br/>
 *
 * This Modbus Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.<br/>
 * <br/>
 *
 * The required channel specific properties are enlisted in
 * {@link ModbusChannelDescriptor} and the driver connection specific properties
 * are enlisted in {@link ModbusOptions}
 *
 * @see ModbusChannelDescriptor
 * @see ModbusOptions
 */
public final class ModbusDriver implements Driver {

	/** Modbus Memory Address Property */
	private static final String MEMORY_ADDRESS = "memory.address";

	/** Modbus Memory Address Space Property */
	private static final String PRIMARY_TABLE = "primary.table";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(ModbusDriver.class);

	/** Localization Resource. */
	private static final ModbusDriverMessages s_message = LocalizationAdapter.adapt(ModbusDriverMessages.class);

	/** Modbus Unit Identifier Property */
	private static final String UNIT_ID = "unit.id";

	/** flag to check if the driver is connected. */
	private boolean m_isConnected;

	/** Modbus Transport */
	private AbstractModbusTransport m_modbusTransport;

	/** Modbus Configuration Options. */
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

	/** {@inheritDoc} */
	@Override
	public void connect() throws ConnectionException {
		final ModbusType type = this.m_options.getType();
		if ((type == TCP) && (this.m_tcpMaster != null)) {
			try {
				this.m_tcpMaster.connect();
			} catch (final Exception e) {
				throw new ConnectionException(s_message.connectionProblem() + ThrowableUtil.stackTraceAsString(e));
			}
		}
		if ((type == UDP) && (this.m_udpMaster != null)) {
			try {
				this.m_udpMaster.connect();
			} catch (final Exception e) {
				throw new ConnectionException(s_message.connectionProblem() + ThrowableUtil.stackTraceAsString(e));
			}
		}
		if ((type == RTU) && (this.m_rtuMaster != null)) {
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
		final ModbusType type = this.m_options.getType();
		if (this.m_isConnected) {
			if ((type == TCP) && (this.m_tcpMaster != null)) {
				try {
					this.m_tcpMaster.disconnect();
				} catch (final Exception e) {
					throw new ConnectionException(
							s_message.disconnectionProblem() + ThrowableUtil.stackTraceAsString(e));
				}
			}
			if ((type == UDP) && (this.m_udpMaster != null)) {
				try {
					this.m_udpMaster.disconnect();
				} catch (final Exception e) {
					throw new ConnectionException(
							s_message.disconnectionProblem() + ThrowableUtil.stackTraceAsString(e));
				}
			}
			if ((type == RTU) && (this.m_rtuMaster != null)) {
				try {
					this.m_rtuMaster.disconnect();
				} catch (final Exception e) {
					throw new ConnectionException(
							s_message.disconnectionProblem() + ThrowableUtil.stackTraceAsString(e));
				}
			}
			this.m_isConnected = false;
		}
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
			final SerialParameters parameters = new SerialParameters(this.m_options.getRtuPortName(),
					this.m_options.getBaudrate(), this.m_options.getFlowControlIn(), this.m_options.getFlowControlOut(),
					this.m_options.getDatabits(), this.m_options.getStopbits(), this.m_options.getParity(), false);
			parameters.setEncoding(this.m_options.getEncoding());
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
		if ("COILS".equalsIgnoreCase(primaryTable)) {
			return READ_COILS;
		}
		if ("DISCRETE_INPUTS".equalsIgnoreCase(primaryTable)) {
			return READ_INPUT_DISCRETES;
		}
		if ("INPUT_REGISTERS".equalsIgnoreCase(primaryTable)) {
			return READ_INPUT_REGISTERS;
		}
		if ("HOLDING_REGISTERS".equalsIgnoreCase(primaryTable)) {
			return READ_HOLDING_REGISTERS;
		}
		return 0;
	}

	/**
	 * Gets the Typed value as found in the provided response
	 *
	 * @param response
	 *            the provided Modbus response
	 * @param record
	 *            the driver record to check the expected value type
	 * @return the value
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	private TypedValue<?> getValue(final ModbusResponse response, final DriverRecord record) {
		checkNull(response, s_message.responseNonNull());
		checkNull(record, s_message.recordNonNull());

		final Map<String, Object> channelConfig = record.getChannelConfig();
		final DataType expectedValueType = (DataType) channelConfig.get(CHANNEL_VALUE_TYPE.value());
		if (response instanceof ReadInputRegistersResponse) {
			final int registerValue = ((ReadInputRegistersResponse) response).getRegisterValue(0);
			switch (expectedValueType) {
			case LONG:
				return TypedValues.newLongValue(registerValue);
			case DOUBLE:
				return TypedValues.newDoubleValue(registerValue);
			case INTEGER:
				return TypedValues.newIntegerValue(registerValue);
			case STRING:
				return TypedValues.newStringValue(Integer.toString(registerValue));
			case BYTE_ARRAY:
				return TypedValues.newByteArrayValue(TypeUtil.intToBytes(registerValue));
			default:
				return null;
			}
		}
		if (response instanceof ReadMultipleRegistersResponse) {
			final int registerValue = ((ReadMultipleRegistersResponse) response).getRegisterValue(0);
			switch (expectedValueType) {
			case LONG:
				return TypedValues.newLongValue(registerValue);
			case DOUBLE:
				return TypedValues.newDoubleValue(registerValue);
			case INTEGER:
				return TypedValues.newIntegerValue(registerValue);
			case STRING:
				return TypedValues.newStringValue(Integer.toString(registerValue));
			case BYTE_ARRAY:
				return TypedValues.newByteArrayValue(TypeUtil.intToBytes(registerValue));
			default:
				return null;
			}
		}
		if ((response instanceof ReadInputDiscretesResponse) && (expectedValueType == BOOLEAN)) {
			return TypedValues.newBooleanValue(((ReadInputDiscretesResponse) response).getDiscreteStatus(0));
		}
		if ((response instanceof ReadCoilsResponse) && (expectedValueType == BOOLEAN)) {
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
			// check if the channel type configuration is provided
			final Map<String, Object> channelConfig = record.getChannelConfig();
			if (!channelConfig.containsKey(CHANNEL_VALUE_TYPE.value())) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
						s_message.errorRetrievingValueType(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the unit ID configuration is provided
			if (!channelConfig.containsKey(UNIT_ID)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.errorRetrievingUnitId(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the primary table configuration is provided
			if (!channelConfig.containsKey(PRIMARY_TABLE)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingPrimaryTable(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the unit ID configuration is provided
			if (!channelConfig.containsKey(UNIT_ID)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.errorRetrievingUnitId(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the memory address configuration is provided
			if (!channelConfig.containsKey(MEMORY_ADDRESS)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingMemAddr(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			final int unitId = Integer.parseInt(channelConfig.get(UNIT_ID).toString());
			final String primaryTable = channelConfig.get(PRIMARY_TABLE).toString();
			final int functionCode = this.getFunctionCode(primaryTable);
			final int memoryAddr = Integer.parseInt(channelConfig.get(MEMORY_ADDRESS).toString()) - 1;

			final ModbusType type = this.m_options.getType();
			if ((type == TCP) && (this.m_tcpMaster != null)) {
				this.m_modbusTransport = this.m_tcpMaster.getTransport();
			}
			if ((type == RTU) && (this.m_rtuMaster != null)) {
				this.m_modbusTransport = this.m_rtuMaster.getTransport();
			}
			if ((type == UDP) && (this.m_udpMaster != null)) {
				this.m_modbusTransport = this.m_udpMaster.getTransport();
			}
			try {
				// always read single register
				final ModbusResponse response = this.readRequest(unitId, this.m_modbusTransport, functionCode,
						memoryAddr, 1);
				final TypedValue<?> value = this.getValue(response, record);
				if (value == null) {
					record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
							s_message.errorRetrievingValueType(), null));
					record.setTimestamp(System.currentTimeMillis());
					continue;
				}
				record.setValue(value);
				record.setDriverStatus(new DriverStatus(READ_SUCCESSFUL));
			} catch (final ModbusException e) {
				record.setDriverStatus(new DriverStatus(READ_FAILURE, null, e));
			} catch (final KuraRuntimeException e) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.transportNonNull() + " OR " + s_message.wrongUnitId(), e));
			}
			record.setTimestamp(System.currentTimeMillis());
		}
		try {
			this.m_modbusTransport.close();
		} catch (final IOException e) {
			s_logger.error(ThrowableUtil.stackTraceAsString(e));
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
	 *             the Modbus exception
	 * @throws KuraRuntimeException
	 *             if the transport is null or the function code, unit ID or
	 *             register address are wrongly set
	 */
	private synchronized ModbusResponse readRequest(final int unitId, final AbstractModbusTransport modbusTransport,
			final int functionCode, final int register, final int count) throws ModbusException {
		checkNull(modbusTransport, s_message.transportNonNull());
		checkCondition(
				(functionCode != FC_01_READ_COILS.code()) && (functionCode != FC_02_READ_DISCRETE_INPUTS.code())
						&& (functionCode != FC_03_READ_HOLDING_REGISTERS.code())
						&& (functionCode != FC_04_READ_INPUT_REGISTERS.code())
						&& (functionCode != FC_05_WRITE_SINGLE_COIL.code())
						&& (functionCode != FC_06_WRITE_SINGLE_REGISTER.code())
						&& (functionCode != FC_15_WRITE_MULITPLE_COILS.code())
						&& (functionCode != FC_16_WRITE_MULTIPLE_REGISTERS.code()),
				s_message.functionCodesNotInRange());
		checkCondition((unitId < 1) || (unitId > 247), s_message.wrongUnitId());
		checkCondition((register < 0) || (register > 65535), s_message.wrongRegister());

		ModbusTransaction trans;
		ModbusRequest req;
		// Prepare the request
		switch (functionCode) {
		case READ_COILS:
			req = new ReadCoilsRequest(register, count);
			break;
		case READ_INPUT_DISCRETES:
			req = new ReadInputDiscretesRequest(register, count);
			break;
		case READ_INPUT_REGISTERS:
			req = new ReadInputRegistersRequest(register, count);
			break;
		case READ_HOLDING_REGISTERS:
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
	}

	/** {@inheritDoc} */
	@Override
	public void registerDriverListener(final Map<String, Object> channelConfig, final DriverListener listener)
			throws ConnectionException {
		throw new KuraRuntimeException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
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
			// check if the channel type configuration is provided
			final Map<String, Object> channelConfig = record.getChannelConfig();
			if (!channelConfig.containsKey(CHANNEL_VALUE_TYPE.value())) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
						s_message.errorRetrievingValueType(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the unit ID configuration is provided
			if (!channelConfig.containsKey(UNIT_ID)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.errorRetrievingUnitId(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the primary table configuration is provided
			if (!channelConfig.containsKey(PRIMARY_TABLE)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingPrimaryTable(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the unit ID configuration is provided
			if (!channelConfig.containsKey(UNIT_ID)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.errorRetrievingUnitId(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the memory address configuration is provided
			if (!channelConfig.containsKey(MEMORY_ADDRESS)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingMemAddr(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			final int unitId = Integer.parseInt(channelConfig.get(UNIT_ID).toString());
			final String primaryTable = channelConfig.get(PRIMARY_TABLE).toString();
			final int functionCode = this.getFunctionCode(primaryTable);
			final int memoryAddr = Integer.parseInt(channelConfig.get(MEMORY_ADDRESS).toString()) - 1;

			final ModbusType type = this.m_options.getType();
			if ((type == TCP) && (this.m_tcpMaster != null)) {
				this.m_modbusTransport = this.m_tcpMaster.getTransport();
			}
			if ((type == RTU) && (this.m_rtuMaster != null)) {
				this.m_modbusTransport = this.m_rtuMaster.getTransport();
			}
			if ((type == UDP) && (this.m_udpMaster != null)) {
				this.m_modbusTransport = this.m_udpMaster.getTransport();
			}
			try {
				final DataType expectedValueType = (DataType) channelConfig.get(CHANNEL_VALUE_TYPE.value());
				int valueToWrite;
				if (expectedValueType != INTEGER) {
					record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
							s_message.errorRetrievingValueType(), null));
					record.setTimestamp(System.currentTimeMillis());
					continue;
				}
				valueToWrite = Integer.valueOf(record.getValue().getValue().toString());
				if (valueToWrite != 0) {
					this.writeRequest(unitId, this.m_modbusTransport, functionCode, memoryAddr, valueToWrite);
					record.setDriverStatus(new DriverStatus(WRITE_SUCCESSFUL));
				}
			} catch (final ModbusException e) {
				record.setDriverStatus(new DriverStatus(WRITE_FAILURE, null, e));
			} catch (final KuraRuntimeException e) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.transportNonNull() + " OR " + s_message.wrongUnitId(), e));
			} catch (final NumberFormatException nfe) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, null, nfe));
			}
		}
		try {
			this.m_modbusTransport.close();
		} catch (final IOException e) {
			s_logger.error(ThrowableUtil.stackTraceAsString(e));
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
	 *             if the transport is null or the function code, unit ID or
	 *             register address are wrongly set
	 */
	private synchronized ModbusResponse writeRequest(final int unitId, final AbstractModbusTransport modbusTransport,
			final int functionCode, final int register, final int... values) throws ModbusException {
		checkNull(modbusTransport, s_message.transportNonNull());
		checkCondition(
				(functionCode != FC_01_READ_COILS.code()) && (functionCode != FC_02_READ_DISCRETE_INPUTS.code())
						&& (functionCode != FC_03_READ_HOLDING_REGISTERS.code())
						&& (functionCode != FC_04_READ_INPUT_REGISTERS.code())
						&& (functionCode != FC_05_WRITE_SINGLE_COIL.code())
						&& (functionCode != FC_06_WRITE_SINGLE_REGISTER.code())
						&& (functionCode != FC_15_WRITE_MULITPLE_COILS.code())
						&& (functionCode != FC_16_WRITE_MULTIPLE_REGISTERS.code()),
				s_message.functionCodesNotInRange());
		checkCondition((unitId < 1) && (unitId > 247), s_message.wrongUnitId());
		checkCondition((register < 0) || (register > 65535), s_message.wrongRegister());

		ModbusTransaction trans;
		ModbusRequest req;
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
	}

}
