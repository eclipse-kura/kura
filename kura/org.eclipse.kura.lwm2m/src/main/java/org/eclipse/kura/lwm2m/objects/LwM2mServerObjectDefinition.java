package org.eclipse.kura.lwm2m.objects;

import java.util.ArrayList;

import leshan.client.resource.LwM2mClientObjectDefinition;
import leshan.client.resource.LwM2mClientResourceDefinition;
import leshan.client.resource.SingleResourceDefinition;
import leshan.client.resource.bool.BooleanLwM2mResource;
import leshan.client.resource.integer.IntegerLwM2mResource;
import leshan.client.resource.opaque.OpaqueLwM2mResource;
import leshan.client.resource.string.StringLwM2mResource;

import org.eclipse.kura.lwm2m.resources.BooleanValueResource;
import org.eclipse.kura.lwm2m.resources.DisableServerExecutableResource;
import org.eclipse.kura.lwm2m.resources.IntegerValueResource;
import org.eclipse.kura.lwm2m.resources.RegistrationUpdateTriggerExecutableResource;
import org.eclipse.kura.lwm2m.resources.ServerUriResource;
import org.eclipse.kura.lwm2m.resources.StringValueResource;

public class LwM2mServerObjectDefinition extends LwM2mClientObjectDefinition {

	public LwM2mServerObjectDefinition() {
		super(1, true, false, generateResources());
	}
		
	private static LwM2mClientResourceDefinition[] generateResources(){
		ArrayList<LwM2mClientResourceDefinition> defs_list = new ArrayList<LwM2mClientResourceDefinition>();
		
		defs_list.add(new SingleResourceDefinition(0, new IntegerValueResource(121, 0), true));
		defs_list.add(new SingleResourceDefinition(1, new IntegerValueResource(121, 1), true));
		defs_list.add(new SingleResourceDefinition(2, new IntegerValueResource(121, 2), false));
		defs_list.add(new SingleResourceDefinition(3, new IntegerValueResource(121, 3), false));
		defs_list.add(new SingleResourceDefinition(4, new DisableServerExecutableResource(), false));
		defs_list.add(new SingleResourceDefinition(5, new IntegerValueResource(121, 5), false));
		defs_list.add(new SingleResourceDefinition(6, new BooleanValueResource(false, 6), true));
		defs_list.add(new SingleResourceDefinition(7, new StringValueResource("default", 7), true));
		defs_list.add(new SingleResourceDefinition(8, new RegistrationUpdateTriggerExecutableResource(), true));
		
		return defs_list.toArray(new LwM2mClientResourceDefinition[0]);
	}
	
}
