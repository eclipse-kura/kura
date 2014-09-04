package org.eclipse.kura.net.modem;

import java.util.Collection;

public interface ModemManagerService {

	public CellularModem getModemService (String ifaceName);
	public Collection<CellularModem> getAllModemServices();	
}
