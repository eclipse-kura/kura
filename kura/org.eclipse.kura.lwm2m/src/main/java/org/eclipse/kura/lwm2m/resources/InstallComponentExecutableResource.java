package org.eclipse.kura.lwm2m.resources;

import leshan.client.exchange.LwM2mExchange;
import leshan.client.resource.string.StringLwM2mResource;
import leshan.client.response.ExecuteResponse;

public class InstallComponentExecutableResource extends StringLwM2mResource {
	
	private final String update_URL;
	
	public InstallComponentExecutableResource(String update_URL) {
		super();
		this.update_URL = update_URL;
	}

	@Override
	public void handleExecute(final LwM2mExchange exchange) {
		System.out.println("Installing / Updating the package from "+update_URL);

		exchange.respond(ExecuteResponse.success());
	}
}
