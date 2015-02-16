package org.eclipse.kura.lwm2m.resources;

import leshan.client.exchange.LwM2mExchange;
import leshan.client.resource.string.StringLwM2mResource;
import leshan.client.response.ExecuteResponse;

public class DeactivateComponentExecutableResource extends StringLwM2mResource {

	@Override
	public void handleExecute(final LwM2mExchange exchange) {
		System.out.println("Deactivating the package");

		exchange.respond(ExecuteResponse.success());
	}
}
