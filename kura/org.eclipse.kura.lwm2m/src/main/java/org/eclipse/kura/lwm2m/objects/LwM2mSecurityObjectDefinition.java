package org.eclipse.kura.lwm2m.objects;

import java.util.ArrayList;

import leshan.client.resource.LwM2mClientObjectDefinition;
import leshan.client.resource.LwM2mClientResourceDefinition;
import leshan.client.resource.SingleResourceDefinition;
import leshan.client.resource.bool.BooleanLwM2mResource;
import leshan.client.resource.integer.IntegerLwM2mResource;
import leshan.client.resource.opaque.OpaqueLwM2mResource;

import org.eclipse.kura.lwm2m.resources.ServerUriResource;

public class LwM2mSecurityObjectDefinition extends LwM2mClientObjectDefinition {

	public LwM2mSecurityObjectDefinition() {
		super(0, true, false, generateResources());
	}
		
	private static LwM2mClientResourceDefinition[] generateResources(){
		ArrayList<LwM2mClientResourceDefinition> defs_list = new ArrayList<LwM2mClientResourceDefinition>();
		
		defs_list.add(new SingleResourceDefinition(0, new ServerUriResource(""), true));
		defs_list.add(new SingleResourceDefinition(1, new BooleanLwM2mResource(), true));
		defs_list.add(new SingleResourceDefinition(2, new IntegerLwM2mResource(), true));
		defs_list.add(new SingleResourceDefinition(3, new OpaqueLwM2mResource(), true));
		defs_list.add(new SingleResourceDefinition(4, new OpaqueLwM2mResource(), true));
		defs_list.add(new SingleResourceDefinition(5, new OpaqueLwM2mResource(), true));
		defs_list.add(new SingleResourceDefinition(6, new IntegerLwM2mResource(), true));
		defs_list.add(new SingleResourceDefinition(7, new OpaqueLwM2mResource(), true));
		defs_list.add(new SingleResourceDefinition(8, new OpaqueLwM2mResource(), true));
		defs_list.add(new SingleResourceDefinition(9, new IntegerLwM2mResource(), true));
		defs_list.add(new SingleResourceDefinition(10, new IntegerLwM2mResource(), false));
		defs_list.add(new SingleResourceDefinition(11, new IntegerLwM2mResource(), true));
		
		return defs_list.toArray(new LwM2mClientResourceDefinition[0]);
	}
	
}
