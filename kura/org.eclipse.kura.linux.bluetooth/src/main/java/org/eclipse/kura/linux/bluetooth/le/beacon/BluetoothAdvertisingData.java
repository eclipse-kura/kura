package org.eclipse.kura.linux.bluetooth.le.beacon;

public class BluetoothAdvertisingData {
	
	private static final String PKT_BYTES_NUMBER     = "1e";
	private static final String AD_BYTES_NUMBER      = "02";
	private static final String AD_FLAG              = "01";
	private static final String PAYLOAD_BYTES_NUMBER = "1a";
	private static final String MANUFACTURER_AD      = "ff";
	private static final String BEACON_ID            = "0215";
	
	public static String getData(String uuid, Integer major, Integer minor, String companyCode, Integer txPower, boolean LELimited, boolean LEGeneral,
			boolean BR_EDRSupported, boolean LE_BRController, boolean LE_BRHost) {
		
		String data = "";

		// Create flags
		String flags = "000";
		flags += Integer.toString((LE_BRHost) ? 1:0);
		flags += Integer.toString((LE_BRController) ? 1:0);
		flags += Integer.toString((BR_EDRSupported) ? 1:0);
		flags += Integer.toString((LEGeneral) ? 1:0);
		flags += Integer.toString((LELimited) ? 1:0);
		String flagsString = Integer.toHexString(Integer.parseInt(flags,2));
		if (flagsString.length() == 1)
			flagsString = "0" + flagsString;
		
		// Convert TxPower
		String txPowerString;
		if (txPower > 0) {
			txPowerString = Integer.toHexString(txPower);
			if (txPowerString.length() == 1)
				txPowerString = "0" + txPowerString;
		}
		else {
			txPowerString = Integer.toHexString(txPower);
			txPowerString = txPowerString.substring(txPowerString.length()-2, txPowerString.length());
		}
		
		// Create Advertising data
		data += PKT_BYTES_NUMBER;
		data += AD_BYTES_NUMBER;
		data += AD_FLAG;
		data += flagsString;
		data += PAYLOAD_BYTES_NUMBER;
		data += MANUFACTURER_AD;
		data += companyCode.subSequence(2, 4);
		data += companyCode.subSequence(0, 2);
		data += BEACON_ID;
		data += uuid.toString();
		data += to2BytesHex(major);
		data += to2BytesHex(minor);
		data += txPowerString;
		
		return data;
	}
	
	public static String to2BytesHex(Integer in) {
		
		String out = Integer.toHexString(in);
		if (out.length() == 1)
			out = "000" + out;
		else if (out.length() == 2)
			out = "00" + out;
		else if (out.length() == 3)
			out = "0" + out;
		
		return out;
		
	}
	
}
