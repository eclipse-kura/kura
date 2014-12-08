package org.eclipse.kura.configuration;

import java.util.Map;

import org.osgi.service.event.Event;

public class KuraNetConfigReadyEvent extends Event {
	
	/** Topic of the KuraConfigurationReadyEvent */
	public static final String KURA_NET_CONFIG_EVENT_READY_TOPIC = "org/eclipse/kura/configuration/NetConfigEvent/READY";

	
	public KuraNetConfigReadyEvent(Map<String, ?> properties) {
		super(KURA_NET_CONFIG_EVENT_READY_TOPIC, properties);
	}
}
