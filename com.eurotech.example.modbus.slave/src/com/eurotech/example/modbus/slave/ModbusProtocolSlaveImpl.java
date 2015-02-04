package com.eurotech.example.modbus.slave;

import java.util.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ModbusProtocolSlaveImpl implements ModbusProtocol{

	private static final Logger s_logger = LoggerFactory.getLogger(ModbusProtocolSlaveImpl.class);

	@Override
	public boolean[] readCoils(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean[] readDiscreteInputs(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeSingleCoil(int unitAddr, int dataAddress, boolean data)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeMultipleCoils(int unitAddr, int dataAddress, boolean[] data)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] readHoldingRegisters(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		byte[] cmd = new byte[2*count + 3];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_HOLDING_REGS;
		cmd[2] = (byte) (2*count);
		
		cmd[3] = (byte) 0; //not used
		cmd[4] = (byte) 3; //start recharge, recharge is booked, next day solar irradiation
		
		
		cmd[5] = (byte) 25; //booking minutes
		cmd[6] = (byte) 12; //booking hour
		
		
		
		Calendar cal = Calendar.getInstance();
		cmd[7] = (byte) cal.get(Calendar.MONTH); //Booking Date -> month
		cmd[8] = (byte) cal.get(Calendar.DAY_OF_MONTH); //Booking Date -> day
		cmd[9] = (byte) ((cal.get(Calendar.YEAR) >> 8) & 0xFF);
		cmd[10] = (byte) (cal.get(Calendar.YEAR) & 0xFF); //booking Date -> year
		
		cmd[11] = (byte) cal.get(Calendar.MINUTE); //Current date -> minutes
		cmd[12] = (byte) cal.get(Calendar.HOUR_OF_DAY); //Current date -> hours
		
		cmd[13] = (byte) cal.get(Calendar.MONTH); //Current Date -> month
		cmd[14] = (byte) cal.get(Calendar.DAY_OF_MONTH); //Current Date -> day
		cmd[15] = (byte) ((cal.get(Calendar.YEAR) >> 8) & 0xFF);
		cmd[16] = (byte) (cal.get(Calendar.YEAR) & 0xFF); //Current date -> year
		
		return cmd;
	}

	@Override
	public int[] readInputRegisters(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeSingleRegister(int unitAddr, int dataAddress, int data)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean[] readExceptionStatus(int unitAddr)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModbusCommEvent getCommEventCounter(int unitAddr)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModbusCommEvent getCommEventLog(int unitAddr)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] writeMultipleRegister(int unitAddr, int dataAddress, byte[] data, int numRegisters)
			throws ModbusProtocolException {
		// TODO Auto-generated method stub

		int powerOut= buildShort(data[0], data[1]);
		int rechargeTime= buildShort(data[2], data[3]);
		int energyOut= buildShort(data[4], data[5]);
		int powerPV= buildShort(data[6], data[7]);
		int faultFlag= data[9] & 0x01;
		int rechargeAvailable= (data[9]>>1) & 0x01;
		int rechargeInProgress= (data[9]>>2) & 0x01;
		int pVSystemActive= (data[9]>>3) & 0x01;
		int auxChargerActive= (data[9]>>4) & 0x01;
		int storageBatteryContactorStatus= (data[9]>>5) & 0x01;
		int converterContactorStatus= (data[9]>>6) & 0x01;
		int faultString1= buildShort(data[10], data[11]);
		int faultString2= buildShort(data[12], data[13]);
		int iGBTTemp= buildShort(data[14], data[15]);
		int storeBattTemp= buildShort(data[16], data[17]);
		int storBatterySOC= buildShort(data[18], data[19]);
		int vOut= buildShort(data[20], data[21]);
		int storageBatteryV= buildShort(data[22], data[23]);
		int pVSystemV= buildShort(data[24], data[25]);
		int iOut= buildShort(data[26], data[27]);
		int storageBatteryI= buildShort(data[28], data[29]);
		s_logger.info("Slave-> power out: " + powerOut +
				" rechargeTime: " + rechargeTime + 
				" energyOut: " + energyOut +
				" powerPV: " + powerPV +
				" faultFlag: " + faultFlag +
				" rechargeAvailable: " + rechargeAvailable +
				" rechargeInProgress: " + rechargeInProgress +
				" pVSystemActive: " + pVSystemActive +
				" auxChargerActive: " + auxChargerActive +
				" storageBatteryContactorStatus: " + storageBatteryContactorStatus +
				" converterContactorStatus: " + converterContactorStatus +
				" faultString1: " + faultString1 +
				" faultString2: " + faultString2 +
				" iGBTTemp: " + iGBTTemp +
				" storeBattTemp: " + storeBattTemp +
				" storBatterySOC: " + storBatterySOC +
				" vOut: " + vOut +
				" storageBatteryV: " + storageBatteryV +
				" pVSystemV: " + pVSystemV +
				" iOut: " + iOut +
				" storageBatteryI: " + storageBatteryI);
		
		
		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.PRESET_MULTIPLE_REGS;
		cmd[2] = (byte) ((dataAddress >> 8) & 0xFF);
		cmd[3] = (byte) (dataAddress & 0xFF);
		
		cmd[4] = (byte) ((numRegisters >> 8) & 0xFF);
		cmd[5] = (byte) (numRegisters & 0xFF);
		
		return cmd;
	}
	
	public int buildShort(byte high, byte low){
		return ((0xFF & (int) high) << 8) + ((0xFF & (int) low));
	}
}
