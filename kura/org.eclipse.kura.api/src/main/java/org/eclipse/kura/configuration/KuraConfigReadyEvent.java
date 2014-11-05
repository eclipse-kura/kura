package org.eclipse.kura.configuration;

import java.util.Map;

import org.osgi.service.event.Event;

public class KuraConfigReadyEvent extends Event {

	/** Topic of the KuraConfigurationReadyEvent */
	public static final String KURA_CONFIG_EVENT_READY_TOPIC = "org/eclipse/kura/configuration/ConfigEvent/READY";

	public KuraConfigReadyEvent(Map<String, ?> properties) {
		super(KURA_CONFIG_EVENT_READY_TOPIC, properties);
	}
}
