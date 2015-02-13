package org.eclipse.kura.lwm2m.objects;

import java.util.ArrayList;

import leshan.client.resource.LwM2mClientObjectDefinition;
import leshan.client.resource.LwM2mClientResourceDefinition;
import leshan.client.resource.SingleResourceDefinition;

import org.eclipse.kura.lwm2m.resources.AltitudeStringResource;
import org.eclipse.kura.lwm2m.resources.BooleanValueResource;
import org.eclipse.kura.lwm2m.resources.DisableServerExecutableResource;
import org.eclipse.kura.lwm2m.resources.GPSTimestampResource;
import org.eclipse.kura.lwm2m.resources.IntegerValueResource;
import org.eclipse.kura.lwm2m.resources.LatitudeStringResource;
import org.eclipse.kura.lwm2m.resources.LongitudeStringResource;
import org.eclipse.kura.lwm2m.resources.RegistrationUpdateTriggerExecutableResource;
import org.eclipse.kura.lwm2m.resources.StringValueResource;
import org.eclipse.kura.position.PositionService;

public class LwM2mPositionObjectDefinition extends LwM2mClientObjectDefinition {

	private static PositionService ps;
	
	public LwM2mPositionObjectDefinition(PositionService ps) {
		super(6, true, false, generateResources());
		LwM2mPositionObjectDefinition.ps = ps;
	}
		
	private static LwM2mClientResourceDefinition[] generateResources(){
		ArrayList<LwM2mClientResourceDefinition> defs_list = new ArrayList<LwM2mClientResourceDefinition>();
		
		defs_list.add(new SingleResourceDefinition(0, new LatitudeStringResource(ps), true));
		defs_list.add(new SingleResourceDefinition(1, new LongitudeStringResource(ps), true));
		defs_list.add(new SingleResourceDefinition(2, new AltitudeStringResource(ps), false));
		//Uncertainity not implemented
		//3GPP 23.032 GAD Velocity not implemented
		defs_list.add(new SingleResourceDefinition(5, new GPSTimestampResource(ps), true));
		
		return defs_list.toArray(new LwM2mClientResourceDefinition[0]);
	}
	
}
