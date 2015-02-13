package org.eclipse.kura.lwm2m.resources;

import java.text.SimpleDateFormat;
import java.util.Date;

import leshan.client.resource.time.TimeLwM2mExchange;
import leshan.client.resource.time.TimeLwM2mResource;

import org.eclipse.kura.position.PositionService;

public class GPSTimestampResource extends TimeLwM2mResource {

	private final PositionService ps;
	private final SimpleDateFormat NMEADateFormatter = new SimpleDateFormat("hhmmss.SS,dd,MM,yyyy");
		
	public GPSTimestampResource(PositionService ps) {
		super();
		this.ps = ps;
	}

	@Override
	protected void handleRead(TimeLwM2mExchange exchange) {
		try {
			String NMEADate = ps.getNmeaDate().substring(0, 19);
			exchange.respondContent(NMEADateFormatter.parse(NMEADate));
		} catch (Exception e) {
			exchange.respondContent(new Date());;
		}
	}
	
}
