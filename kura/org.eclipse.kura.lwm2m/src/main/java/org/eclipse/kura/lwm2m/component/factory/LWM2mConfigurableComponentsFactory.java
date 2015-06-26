package org.eclipse.kura.lwm2m.component.factory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.eclipse.leshan.client.resource.ObjectsInitializer;
/*
import leshan.client.coap.californium.CaliforniumBasedObject;
import leshan.client.resource.LwM2mClientObject;
import leshan.client.resource.LwM2mClientObjectDefinition;
import leshan.client.resource.LwM2mClientObjectInstance;
import leshan.client.resource.LwM2mClientResourceDefinition;
import leshan.client.resource.MultipleResourceDefinition;
import leshan.client.resource.SingleResourceDefinition;
import leshan.client.resource.bool.BooleanLwM2mExchange;
import leshan.client.resource.bool.BooleanLwM2mResource;
import leshan.client.resource.integer.IntegerLwM2mExchange;
import leshan.client.resource.integer.IntegerLwM2mResource;
import leshan.client.resource.multiple.MultipleLwM2mExchange;
import leshan.client.resource.multiple.MultipleLwM2mResource;
import leshan.client.resource.opaque.OpaqueLwM2mResource;
import leshan.client.resource.string.StringLwM2mExchange;
import leshan.client.resource.string.StringLwM2mResource;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.lwm2m.resources.ActivateComponentExecutableResource;
import org.eclipse.kura.lwm2m.resources.BooleanValueResource;
import org.eclipse.kura.lwm2m.resources.DeactivateComponentExecutableResource;
import org.eclipse.kura.lwm2m.resources.InstallComponentExecutableResource;
import org.eclipse.kura.lwm2m.resources.IntegerValueResource;
import org.eclipse.kura.lwm2m.resources.StringValueResource;
import org.eclipse.kura.lwm2m.resources.UninstallComponentExecutableResource;
*/
public class LWM2mConfigurableComponentsFactory {

	private static final String SoftwarePackageUri = "https://console-sandbox.everyware-cloud.com/updates/";

	private final static LWM2mConfigurableComponentsFactory _instance = null;
	
	//private static ConfigurationService m_configurationService = null;

	private static final int LWM2M_OBJECT_INDEX_METATYPE_GENERIC = 100;
	private static final int LWM2M_OBJECT_INDEX_COMPONENTS_METATYPES = 91;
	private static final int LWM2M_OBJECT_INDEX_COMPONENTS = 90; // Starting
																	// from this
																	// index,
																	// and going
																	// forth

	private LWM2mConfigurableComponentsFactory() {}
	/*
	public static LWM2mConfigurableComponentsFactory getDefault() {
		if (_instance == null) {
			_instance = new LWM2mConfigurableComponentsFactory();
		}
		return _instance;
	}

	public void setConfigurationService(ConfigurationService cs) {
		m_configurationService = cs;
	}

	public CaliforniumBasedObject[] createComponentObjects() {
		ObjectsInitializer initializer = new ObjectsInitializer();
		initializer.set
		ArrayList<CaliforniumBasedObject> list = new ArrayList<CaliforniumBasedObject>();

		list.add(getSoftwareManagementObjectFromComponent());

		// Set<String> pids =
		// m_configurationService.getConfigurableComponentPids();
		// int index = LWM2M_OBJECT_INDEX_COMPONENTS;
		// for(String s : pids){
		// try {
		// System.out.println("\tComponent: "+s);
		//
		// list.addAll(getCoapObjectFromComponent(m_configurationService.getComponentConfiguration(s),
		// index++));
		// } catch (KuraException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }

		list.addAll(getComponentsObject());

		return list.toArray(new CaliforniumBasedObject[0]);
	}

	private LwM2mClientObjectDefinition getMetatypeLwM2mDef() {
		LwM2mClientResourceDefinition[] resources = new LwM2mClientResourceDefinition[11];

		resources[0] = new SingleResourceDefinition(0, new StringValueResource("id", 0), true);
		resources[1] = new SingleResourceDefinition(1, new StringValueResource("name", 1), true);
		resources[2] = new SingleResourceDefinition(2, new MetatypeComponentResource(Scalar.BOOLEAN), true);
		resources[3] = new SingleResourceDefinition(3, new IntegerValueResource(0, 3), true);
		resources[4] = new SingleResourceDefinition(4, new BooleanValueResource(false, 4), true);
		resources[5] = new SingleResourceDefinition(5, new StringValueResource("default", 5), true);
		resources[6] = new SingleResourceDefinition(6, new StringValueResource("description", 6), true);
		resources[7] = new SingleResourceDefinition(7, new StringValueResource("min", 7), false);
		resources[8] = new SingleResourceDefinition(8, new StringValueResource("max", 8), false);
		resources[9] = new MultipleResourceDefinition(9, new MultipleStringComponentResource(new String[] {}), false);
		resources[10] = new MultipleResourceDefinition(10, new MultipleStringComponentResource(new String[] {}), false);

		return new LwM2mClientObjectDefinition(LWM2M_OBJECT_INDEX_METATYPE_GENERIC, false, false, resources);
	}

	private void addMetatypeResourceToObject(LwM2mClientObjectInstance instance, AD data) {

		instance.addResource(0, new StringValueResource(data.getId(), 0));
		instance.addResource(1, new StringValueResource(data.getName(), 1));
		instance.addResource(2, new MetatypeComponentResource(data.getType()));
		instance.addResource(3, new IntegerValueResource(data.getCardinality(), 3));
		instance.addResource(4, new BooleanValueResource(data.isRequired(), 4));
		instance.addResource(5, new StringValueResource(data.getDefault(), 5));
		instance.addResource(6, new StringValueResource(data.getDescription(), 6));
		instance.addResource(7, new StringValueResource(data.getMin(), 7));
		instance.addResource(8, new StringValueResource(data.getMax(), 8));
		ArrayList<String> labels = new ArrayList<String>();
		ArrayList<String> options = new ArrayList<String>();
		for (Option o : data.getOption()) {
			labels.add(o.getLabel());
			options.add(o.getValue());
		}
		if (labels.size() > 0) {
			instance.addResource(9, new MultipleStringComponentResource(labels.toArray(new String[] {})));
			instance.addResource(10, new MultipleStringComponentResource(options.toArray(new String[] {})));
		}

	}

	private List<CaliforniumBasedObject> getComponentsObject() {
		LwM2mClientResourceDefinition[] resources = new LwM2mClientResourceDefinition[5];

		resources[0] = new SingleResourceDefinition(0, new StringValueResource("ComponentName", 0), true);
		resources[1] = new SingleResourceDefinition(1, new StringValueResource("Description", 1), true);
		resources[2] = new SingleResourceDefinition(2, new StringValueResource("Icon", 2), true);
		resources[3] = new MultipleResourceDefinition(3, new MultiplePropertiesComponentResource(new ArrayList<AD>(), new HashMap()), true);
		resources[4] = new MultipleResourceDefinition(4, new MultipleIntegerResource(new Integer[] {}), true);

		// Component Objects
		final LwM2mClientObjectDefinition objectComponentDef = new LwM2mClientObjectDefinition(LWM2M_OBJECT_INDEX_COMPONENTS, false, false, resources);
		CaliforniumBasedObject calObjectComponents = new CaliforniumBasedObject(objectComponentDef);
		LwM2mClientObject lwm2mObjectBundles = calObjectComponents.getLwM2mClientObject();

		// Metatype definitions
		final LwM2mClientObjectDefinition objectMetatypeDef = getMetatypeLwM2mDef();
		CaliforniumBasedObject calObjectMetatypes = new CaliforniumBasedObject(objectMetatypeDef);
		LwM2mClientObject lwm2mObjectMetatypes = calObjectMetatypes.getLwM2mClientObject();

		int comp_index = 0;
		int metatype_index = 0;
		try {
			for (ComponentConfiguration comp : m_configurationService.getComponentConfigurations()) {
				LwM2mClientObjectInstance instance = new LwM2mClientObjectInstance(comp_index++, lwm2mObjectBundles, objectComponentDef);

				instance.addResource(0, new StringValueResource(comp.getDefinition().getName(), 0));
				instance.addResource(1, new StringValueResource(comp.getDefinition().getDescription(), 1));
				instance.addResource(2, new StringValueResource(comp.getDefinition().getIcon().get(0).getResource(), 2));
				instance.addResource(3, new MultiplePropertiesComponentResource(comp.getDefinition().getAD(), comp.getConfigurationProperties()));
				
				List<AD> ads = comp.getDefinition().getAD();
				ArrayList<Integer> metatypes_list = new ArrayList<Integer>();
				
				for(int i=0; i< ads.size(); i++){
					metatypes_list.add(metatype_index);
					
					LwM2mClientObjectInstance metatype = new LwM2mClientObjectInstance(metatype_index, lwm2mObjectMetatypes, objectMetatypeDef);
					
					addMetatypeResourceToObject(metatype, ads.get(i));
					
					metatype_index++;
					calObjectMetatypes.onSuccessfulCreate(metatype);
				}
				instance.addResource(4,  new MultipleIntegerResource(metatypes_list.toArray(new Integer[0])));

				calObjectComponents.onSuccessfulCreate(instance);


			}
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ArrayList<CaliforniumBasedObject> result = new ArrayList<CaliforniumBasedObject>();
		result.add(calObjectComponents);
		result.add(calObjectMetatypes);

		return result;
	}

	private CaliforniumBasedObject getSoftwareManagementObjectFromComponent() {
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
			for (ComponentConfiguration comp : m_configurationService.getComponentConfigurations()) {
				// create object instance
				LwM2mClientObjectInstance instance = new LwM2mClientObjectInstance(comp_index++, lwm2mObjectBundles, objectBundlesDef);

				// add resources to the instance
				instance.addResource(0, new StringValueResource(comp.getDefinition().getDescription(), 0));
				instance.addResource(1, new StringValueResource(comp.getDefinition().getId(), 1));
				instance.addResource(2, new OpaqueLwM2mResource());
				instance.addResource(3, new StringValueResource(SoftwarePackageUri + comp.getPid() + ".pkg", 3));
				instance.addResource(4, new InstallComponentExecutableResource(SoftwarePackageUri + comp.getPid() + ".pkg"));
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

	private Collection<? extends CaliforniumBasedObject> getCoapObjectFromComponent(ComponentConfiguration conf, int ObjectIndex) {
		Map<String, Object> props = conf.getConfigurationProperties();
		ArrayList<LwM2mClientResourceDefinition> resources = new ArrayList<LwM2mClientResourceDefinition>();
		ArrayList<LwM2mClientResourceDefinition> ads = new ArrayList<LwM2mClientResourceDefinition>();

		ArrayList<CaliforniumBasedObject> result = new ArrayList<CaliforniumBasedObject>();

		int index = 0;
		for (AD ad : conf.getDefinition().getAD()) {

			Object prop = props.get(ad.getId());
			if (prop == null) {
				prop = ad.getDefault();
			}
			switch (ad.getType()) {
			case BOOLEAN:
				resources.add(new SingleResourceDefinition(index, new BooleanComponentResource(conf.getPid(), ad.getId(), (Boolean) prop), true));
				break;
			case DOUBLE:
			case FLOAT:
				break;
			case BYTE:
			case INTEGER:
			case SHORT:
			case LONG:
				break;
			case CHAR:
			case STRING:
			case PASSWORD:
				break;
			}

			ads.add(new SingleResourceDefinition(index, new MetatypeComponentResource(ad.getType()), true));
			index++;
		}

		for (String s : props.keySet()) {
			Object prop = props.get(s);
			if (prop instanceof Boolean) {
				resources.add(new SingleResourceDefinition(index++, new BooleanComponentResource(conf.getPid(), s, (Boolean) prop), true));
			}
			if (prop instanceof Integer) {
				resources.add(new SingleResourceDefinition(index++, new IntegerComponentResource(conf.getPid(), s, (Integer) prop), true));
			}
			if (prop instanceof String) {
				resources.add(new SingleResourceDefinition(index++, new StringComponentResource(conf.getPid(), s, (String) prop), true));
			}
			System.out.println("[" + prop.getClass().getCanonicalName() + "]\t\t" + s + "=" + prop.toString());
		}
		LwM2mClientResourceDefinition[] prova = resources.toArray(new LwM2mClientResourceDefinition[0]);
		LwM2mClientObjectDefinition ob_def = new LwM2mClientObjectDefinition(ObjectIndex, true, true, prova);

		result.add(new CaliforniumBasedObject(ob_def));

		return result;

	}

	private void updateConfiguration(String component_pid, String resource_key, Object value) {
		try {
			Map<String, Object> props = m_configurationService.getComponentConfiguration(component_pid).getConfigurationProperties();
			props.put(resource_key, value);
			m_configurationService.updateConfiguration(component_pid, props);
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class MultipleIntegerResource extends MultipleLwM2mResource {
		private final Map<Integer, byte[]> values;

		public MultipleIntegerResource(Integer[] values) {
			this.values = new HashMap<Integer, byte[]>();
			for (int i = 0; i < values.length; i++) {
				this.values.put(i, ByteBuffer.allocate(4).putInt(values[i].intValue()).array());
			}
		}

		@Override
		protected void handleRead(MultipleLwM2mExchange exchange) {
			exchange.respondContent(values);
		}

	}

	private class MetatypeComponentResource extends IntegerLwM2mResource {
		private final Scalar type;

		public MetatypeComponentResource(Scalar type) {
			super();
			this.type = type;
		}

		public Scalar getValue() {
			return type;
		}

		@Override
		public void handleRead(final IntegerLwM2mExchange exchange) {
			exchange.respondContent(type.ordinal());
		}

	}

	private class IntegerComponentResource extends IntegerLwM2mResource {
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

	private class BooleanComponentResource extends BooleanLwM2mResource {
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

	private class MultiplePropertiesComponentResource extends MultipleLwM2mResource {
		private final Map<Integer, byte[]> values;
		private final List<AD> props;

		public MultiplePropertiesComponentResource(List<AD> props, Map<String, Object> prop_values) {
			this.props = props;
			values = new HashMap<Integer, byte[]>();
			for (int i = 0; i < props.size(); i++) {
				Object val = prop_values.get(props.get(i).getId());
				if (null == val) {
					val = props.get(i).getDefault();
				}
				this.values.put(i, ByteBuffer.allocate(255).put(val.toString().getBytes()).array());
			}
		}

		@Override
		protected void handleRead(MultipleLwM2mExchange exchange) {
			exchange.respondContent(values);
		}

		@Override
		protected void handleWrite(MultipleLwM2mExchange exchange) {
			// TODO Auto-generated method stub
			super.handleWrite(exchange);
		}

	}

	private class MultipleStringComponentResource extends MultipleLwM2mResource {
		private final Map<Integer, byte[]> values;

		public MultipleStringComponentResource(String[] values) {
			this.values = new HashMap();
			for (int i = 0; i < values.length; i++) {
				this.values.put(i, ByteBuffer.allocate(255).put(values[i].getBytes()).array());
			}
		}

		@Override
		protected void handleRead(MultipleLwM2mExchange exchange) {
			exchange.respondContent(values);
		}

	}

	private class StringComponentResource extends StringLwM2mResource {
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

	}*/

}
