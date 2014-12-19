package org.eclipse.kura.net.modem;

import java.util.Map;

import org.osgi.service.event.Event;

public class ModemGpsDisabledEvent extends Event {
	
	/** Topic of the ModemGpsDisabledEvent */
    public static final String MODEM_EVENT_GPS_DISABLED_TOPIC = "org/eclipse/kura/net/modem/gps/DISABLED";
    
    public ModemGpsDisabledEvent(Map<String, Object> properties) {
        super(MODEM_EVENT_GPS_DISABLED_TOPIC, properties);
    }
}