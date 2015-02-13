package org.eclipse.kura.lwm2m.resources;

import org.eclipse.kura.position.PositionService;

import leshan.client.resource.string.StringLwM2mExchange;
import leshan.client.resource.string.StringLwM2mResource;

public class LatitudeStringResource extends StringLwM2mResource {

	private final PositionService ps;
		
	public LatitudeStringResource(PositionService ps) {
		super();
		this.ps = ps;
	}

	@Override
	protected void handleRead(StringLwM2mExchange exchange) {
		exchange.respondContent(String.valueOf(ps.getNmeaPosition().getLatitude()));
	}
	
}
