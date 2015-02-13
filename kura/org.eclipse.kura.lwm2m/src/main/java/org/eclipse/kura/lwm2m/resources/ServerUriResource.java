package org.eclipse.kura.lwm2m.resources;

import java.net.URI;

import leshan.client.resource.string.StringLwM2mResource;

public class ServerUriResource extends StringLwM2mResource {
	private URI m_URI;		

	public ServerUriResource(final String initialValue) {
		m_URI = URI.create(initialValue);
	}

	public void setValue(final String newValue) {
		m_URI = URI.create(newValue);
		notifyResourceUpdated();
	}

	public String getValue() {
		return m_URI.toString();
	}
}
