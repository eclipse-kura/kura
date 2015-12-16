package org.eclipse.kura.net.modem;

import java.util.Collection;

import org.eclipse.kura.KuraException;

public interface ModemManagerService {

	public CellularModem getModemService (String ifaceName);
	public Collection<CellularModem> getAllModemServices();
	public void enableConnection(String ifaceName, CellularModem modem, boolean enable) throws KuraException;
	public boolean disableModemGps(CellularModem modem) throws KuraException;
}
