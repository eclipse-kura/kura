package org.eclipse.kura.lwm2m.resources;

import leshan.client.resource.bool.BooleanLwM2mExchange;
import leshan.client.resource.bool.BooleanLwM2mResource;

public class BooleanValueResource extends BooleanLwM2mResource {
	private Boolean value;
	private final int resourceId;
	
	public BooleanValueResource(Boolean value, int resourceId) {
		this.value = value;
		this.resourceId = resourceId;
	}

	public Boolean getValue() {
		return value;
	}

	public void setValue(final Boolean value) {
		this.value = value;
		notifyResourceUpdated();
	}

	@Override
	protected void handleRead(BooleanLwM2mExchange exchange) {
		System.out.println("\tDevice: Reading on BooleanResource " + resourceId);
		exchange.respondContent(value);
	}

	@Override
	protected void handleWrite(BooleanLwM2mExchange exchange) {
		System.out.println("\tDevice: Writing on Boolean Resource " + resourceId);
		value = exchange.getRequestPayload();
		exchange.respondSuccess();
	}	
	
}
