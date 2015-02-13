package org.eclipse.kura.lwm2m.resources;

import leshan.client.resource.integer.IntegerLwM2mExchange;
import leshan.client.resource.integer.IntegerLwM2mResource;

public class IntegerValueResource extends IntegerLwM2mResource {
	private Integer value;
	private final int resourceId;

	public IntegerValueResource(final int initialValue, final int resourceId) {
		value = initialValue;
		this.resourceId = resourceId;
	}

	public void setValue(final Integer newValue) {
		value = newValue;
		notifyResourceUpdated();
	}

	public Integer getValue() {
		return value;
	}

	@Override
	public void handleWrite(final IntegerLwM2mExchange exchange) {
		System.out.println("\tDevice: Writing on Integer Resource " + resourceId);
		setValue(exchange.getRequestPayload());

		exchange.respondSuccess();
	}

	@Override
	public void handleRead(final IntegerLwM2mExchange exchange) {
		System.out.println("\tDevice: Reading on IntegerResource " + resourceId);
		exchange.respondContent(value);
	}
		
}
