package org.eclipse.kura.lwm2m.component.factory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import leshan.client.coap.californium.CaliforniumBasedObject;
import leshan.client.resource.LwM2mClientObject;
import leshan.client.resource.LwM2mClientObjectDefinition;
import leshan.client.resource.LwM2mClientObjectInstance;
import leshan.client.resource.LwM2mClientResourceDefinition;
import leshan.client.resource.SingleResourceDefinition;
import leshan.client.resource.bool.BooleanLwM2mExchange;
import leshan.client.resource.bool.BooleanLwM2mResource;
import leshan.client.resource.integer.IntegerLwM2mExchange;
import leshan.client.resource.integer.IntegerLwM2mResource;
import leshan.client.resource.opaque.OpaqueLwM2mResource;
import leshan.client.resource.string.StringLwM2mExchange;
import leshan.client.resource.string.StringLwM2mResource;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.lwm2m.resources.ActivateComponentExecutableResource;
import org.eclipse.kura.lwm2m.resources.BooleanValueResource;
import org.eclipse.kura.lwm2m.resources.DeactivateComponentExecutableResource;
import org.eclipse.kura.lwm2m.resources.InstallComponentExecutableResource;
import org.eclipse.kura.lwm2m.resources.IntegerValueResource;
import org.eclipse.kura.lwm2m.resources.StringValueResource;
import org.eclipse.kura.lwm2m.resources.UninstallComponentExecutableResource;

public class LWM2mConfigurableComponentsFactory {
	
	private static final String SoftwarePackageUri="https://console-sandbox.everyware-cloud.com/updates/";
	
	private final static LWM2mConfigurableComponentsFactory _instance = new LWM2mConfigurableComponentsFactory();
	private static ConfigurationService m_configurationService = null;
	
	public static LWM2mConfigurableComponentsFactory getDefault(){
		return _instance;				
	}
	
	public void setConfigurationService(ConfigurationService cs){
		m_configurationService = cs;
	}
	
	public CaliforniumBasedObject[] createComponentObjects(){
		ArrayList<CaliforniumBasedObject> list = new ArrayList<CaliforniumBasedObject>();
		
		list.add(getSoftwareManagementObjectFromComponent());
		
		Set<String> pids = m_configurationService.getConfigurableComponentPids();
		int index = 20;
		for(String s : pids){
			try {
				System.out.println("\tComponent: "+s);
				list.add(getCoapObjectFromComponent(m_configurationService.getComponentConfiguration(s), index++));
			} catch (KuraException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return list.toArray(new CaliforniumBasedObject[0]);
	}
	
	private CaliforniumBasedObject getSoftwareManagementObjectFromComponent(){
		LwM2mClientResourceDefinition[] resources = new LwM2mClientResourceDefinition[13];
				
		resources[0] = new SingleResourceDefinition(0, new StringValueResource("PkgName", 0), true);
		resources[1] = new SingleResourceDefinition(1, new StringValueResource("PkgVersion", 1), true);
		resources[2] = new SingleResourceDefinition(2, new OpaqueLwM2mResource(), true);
		resources[3] = new SingleResourceDefinition(3, new StringValueResource("Package URI", 3), true);
		resources[4] = new SingleResourceDefinition(4, new InstallComponentExecutableResource("Install URL"), true);
		resources[5] = new SingleResourceDefinition(5, new StringValueResource("Install Options", 5), false);
		resources[6] = new SingleResourceDefinition(6, new UninstallComponentExecutableResource(), true);
		resources[7] = new SingleResourceDefinition(7, new IntegerValueResource(1, 7), true);
		resources[8] = new SingleResourceDefinition(8, new BooleanValueResource(false, 8), false);
		resources[9] = new SingleResourceDefinition(9, new IntegerValueResource(0, 9), true);
		resources[10] = new SingleResourceDefinition(10, new ActivateComponentExecutableResource(), true);
		resources[11] = new SingleResourceDefinition(11, new DeactivateComponentExecutableResource(), true);
		resources[12] = new SingleResourceDefinition(12, new BooleanValueResource(false, 12), true);
		
		final LwM2mClientObjectDefinition objectBundlesDef = new LwM2mClientObjectDefinition(9, false, false, resources);
		CaliforniumBasedObject calObjectBundles = new CaliforniumBasedObject(objectBundlesDef);
		LwM2mClientObject lwm2mObjectBundles = calObjectBundles.getLwM2mClientObject();

		int comp_index = 0;
		try {
			for(ComponentConfiguration comp : m_configurationService.getComponentConfigurations()){
				// create object instance
				LwM2mClientObjectInstance instance = new LwM2mClientObjectInstance(comp_index++, lwm2mObjectBundles, objectBundlesDef);
				
				// add resources to the instance
				instance.addResource(0, new StringValueResource(comp.getDefinition().getDescription(), 0));
				instance.addResource(1, new StringValueResource(comp.getDefinition().getId(), 1));
				instance.addResource(2, new OpaqueLwM2mResource());
				instance.addResource(3, new StringValueResource(SoftwarePackageUri+comp.getPid()+".pkg", 3));
				instance.addResource(4, new InstallComponentExecutableResource(SoftwarePackageUri+comp.getPid()+".pkg"));
				instance.addResource(5, new StringValueResource(comp.getDefinition().getName(), 5));
				instance.addResource(6, new UninstallComponentExecutableResource());
				instance.addResource(7, new IntegerValueResource(1, 7));
				instance.addResource(8, new BooleanValueResource(false, 8));
				instance.addResource(9, new IntegerValueResource(0, 9));
				instance.addResource(10, new ActivateComponentExecutableResource());
				instance.addResource(11, new DeactivateComponentExecutableResource());
				instance.addResource(12, new BooleanValueResource(false, 12));
				
				// add the object instance
				calObjectBundles.onSuccessfulCreate(instance);
			}
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return calObjectBundles;		
	}

	private CaliforniumBasedObject getCoapObjectFromComponent(ComponentConfiguration conf, int ObjectIndex){
		Map<String, Object> props = conf.getConfigurationProperties();
		ArrayList<LwM2mClientResourceDefinition> resources = new ArrayList<LwM2mClientResourceDefinition>();
		
		int index = 0;
		for (String s : props.keySet()){
			Object prop = props.get(s);
			if(prop instanceof Boolean){
				resources.add(new SingleResourceDefinition(index++, new BooleanComponentResource(conf.getPid(), s, (Boolean)prop), true));
			}
			if(prop instanceof Integer){
				resources.add(new SingleResourceDefinition(index++, new IntegerComponentResource(conf.getPid(), s, (Integer)prop), true));
			}
			if(prop instanceof String){
				resources.add(new SingleResourceDefinition(index++, new StringComponentResource(conf.getPid(), s, (String)prop), true));
			}
			//System.out.println("["+prop.getClass().getCanonicalName()+"]\t\t"+s+"="+prop.toString());
		}
		LwM2mClientResourceDefinition[] prova = resources.toArray(new LwM2mClientResourceDefinition[0]);
		LwM2mClientObjectDefinition ob_def = new LwM2mClientObjectDefinition(ObjectIndex, true, true, prova);
		return new CaliforniumBasedObject(ob_def);
		
	}

	private void updateConfiguration(String component_pid, String resource_key, Object value){
		try {
			Map<String, Object> props = m_configurationService.getComponentConfiguration(component_pid).getConfigurationProperties();
			props.put(resource_key, value);
			m_configurationService.updateConfiguration(component_pid, props);
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private class IntegerComponentResource extends IntegerLwM2mResource{
		final String component_pid;
		final String resource_key;
		private int value;
		
		public IntegerComponentResource(String component_pid, String resource_key, int value) {
			this.component_pid = component_pid;
			this.resource_key = resource_key;
			this.value = value;
		}
		
		public void setValue(final Integer newValue) {
			value = newValue;
			updateConfiguration(component_pid, resource_key, value);			
			notifyResourceUpdated();
		}

		public Integer getValue() {
			return value;
		}

		@Override
		public void handleWrite(final IntegerLwM2mExchange exchange) {
			setValue(exchange.getRequestPayload());
			
			exchange.respondSuccess();
		}

		@Override
		public void handleRead(final IntegerLwM2mExchange exchange) {
			exchange.respondContent(value);
		}

	}
	
	private class BooleanComponentResource extends BooleanLwM2mResource{
		final String component_pid;
		final String resource_key;
		private Boolean value;
		
		public BooleanComponentResource(String component_pid, String resource_key, Boolean value) {
			this.component_pid = component_pid;
			this.resource_key = resource_key;
			this.value = value;
		}
		
		public void setValue(final Boolean newValue) {
			value = newValue;
			updateConfiguration(component_pid, resource_key, value);
			notifyResourceUpdated();
		}

		public Boolean getValue() {
			return value;
		}

		@Override
		public void handleWrite(final BooleanLwM2mExchange exchange) {
			setValue(exchange.getRequestPayload());
			
			exchange.respondSuccess();
		}

		@Override
		public void handleRead(final BooleanLwM2mExchange exchange) {
			exchange.respondContent(value);
		}

	}

	private class StringComponentResource extends StringLwM2mResource{
		final String component_pid;
		final String resource_key;
		private String value;
		
		public StringComponentResource(String component_pid, String resource_key, String value) {
			this.component_pid = component_pid;
			this.resource_key = resource_key;
			this.value = value;
		}
		
		public void setValue(final String newValue) {
			value = newValue;
			updateConfiguration(component_pid, resource_key, value);			
			notifyResourceUpdated();
		}

		public String getValue() {
			return value;
		}

		@Override
		public void handleWrite(final StringLwM2mExchange exchange) {
			setValue(exchange.getRequestPayload());
			
			exchange.respondSuccess();
		}

		@Override
		public void handleRead(final StringLwM2mExchange exchange) {
			exchange.respondContent(value);
		}

	}

}
