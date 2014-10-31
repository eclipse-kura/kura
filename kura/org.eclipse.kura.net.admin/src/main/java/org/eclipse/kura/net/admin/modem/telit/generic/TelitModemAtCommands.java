package org.eclipse.kura.net.admin.modem.telit.generic;

public enum TelitModemAtCommands {

	at("at\r\n"),
	getModelNumber("at+gmm\r\n"),
    getManufacturer("at+gmi\r\n"),
    getSerialNumber("at#cgsn\r\n"),
    getIMSI("at#cimi\r\n"),
    getICCID("at#ccid\r\n"),
    getRevision("at+gmr\r\n"),
    getSignalStrength("at+csq\r\n"),
    isGpsPowered("at$GPSP?\r\n"),
	gpsPowerUp("at$GPSP=1\r\n"),
	gpsPowerDown("at$GPSP=0\r\n"),
	gpsEnableNMEA("AT$GPSNMUN=3,0,0,1,1,1,1\r\n"),
	gpsDisableNMEA("+++");
	
	private String m_command;
	
	private TelitModemAtCommands(String atCommand) {
		m_command = atCommand;
	}
	
	public String getCommand () {
		return m_command;
	}
}
