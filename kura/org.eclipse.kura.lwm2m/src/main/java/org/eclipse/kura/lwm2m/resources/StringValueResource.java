package org.eclipse.kura.lwm2m.resources;

import leshan.client.resource.string.StringLwM2mExchange;
import leshan.client.resource.string.StringLwM2mResource;

public class StringValueResource extends StringLwM2mResource {
	private String value;
	private final int resourceId;

	public StringValueResource(final String initialValue, final int resourceId) {
		value = initialValue == null ? "null" : initialValue;
		this.resourceId = resourceId;
	}

	public void setValue(final String newValue) {
		value = newValue;
		notifyResourceUpdated();
	}

	public String getValue() {
		return value;
	}

	@Override
	public void handleWrite(final StringLwM2mExchange exchange) {
		System.out.println("\tDevice: Writing on Resource " + resourceId);
		setValue(exchange.getRequestPayload());

		exchange.respondSuccess();
	}

	@Override
	public void handleRead(final StringLwM2mExchange exchange) {
		System.out.println("\tDevice: Reading on Resource " + resourceId);
		exchange.respondContent(value);
	}

}
