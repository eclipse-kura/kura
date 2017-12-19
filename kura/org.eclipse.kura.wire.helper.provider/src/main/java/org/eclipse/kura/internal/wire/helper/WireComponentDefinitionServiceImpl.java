package org.eclipse.kura.internal.wire.helper;

import static java.util.function.Function.identity;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.metatype.OCDService;
import org.eclipse.kura.wire.graph.WireComponentDefinition;
import org.eclipse.kura.wire.graph.WireComponentDefinitionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WireComponentDefinitionServiceImpl implements WireComponentDefinitionService {

    private static final Logger logger = LoggerFactory.getLogger(WireComponentDefinitionServiceImpl.class);

    private static final Pattern DEFAULT_PORT_NAME_PATTERN = Pattern.compile("(in|out)(\\d+)");
    private static final String WIRE_COMPONENT = "org.eclipse.kura.wire.WireComponent";
    private static final String WIRE_EMITTER = "org.eclipse.kura.wire.WireEmitter";
    private static final String WIRE_RECEIVER = "org.eclipse.kura.wire.WireReceiver";

    private static boolean implementsAnyService(Component component, String[] classes) {
        final String[] services = component.getServices();
        if (services == null) {
            return false;
        }
        for (String className : classes) {
            for (String s : services) {
                if (s.equals(className)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<Integer, String> parsePortNames(Object rawEncoded, int maxPortCount) {
        try {
            if (rawEncoded == null || maxPortCount <= 0) {
                return null;
            }

            String encoded;
            if (rawEncoded instanceof String[]) {
                encoded = String.join("\n", (String[]) rawEncoded);
            } else {
                encoded = rawEncoded.toString();
            }

            final Properties properties = new Properties();
            properties.load(new StringReader(encoded));
            final HashMap<Integer, String> result = new HashMap<>();
            for (Entry<Object, Object> e : properties.entrySet()) {
                final int index = Integer.parseInt(e.getKey().toString());
                final String name = e.getValue().toString();
                if (index < 0 || index >= maxPortCount) {
                    logger.warn("Port index out of range: {}", index);
                    continue;
                }
                if (name == null || name.isEmpty() || DEFAULT_PORT_NAME_PATTERN.matcher(name).matches()) {
                    logger.warn("Invalid port name: {}", name);
                    continue;
                }
                result.put(index, name);
            }
            return result;
        } catch (Exception e) {
            logger.warn("failed to parse port names from: {}", rawEncoded, e);
            return null;
        }
    }

    private WireComponentDefinition getDefinition(Component component, ComponentConfiguration ocd) {
        if (component.getProperties().get("input.cardinality.minimum") != null) {
            return getDefinitionFromComponentProperties(component, ocd);
        } else {
            return getDefinitionLegacy(component, ocd);
        }
    }

    private WireComponentDefinition getDefinitionLegacy(Component component, ComponentConfiguration ocd) {
        WireComponentDefinition wireComponentDefinition = new WireComponentDefinition();
        wireComponentDefinition.setFactoryPid(component.getName());
        for (String service : component.getServices()) {
            if (WIRE_EMITTER.equals(service)) {
                wireComponentDefinition.setMinOutputPorts(1);
                wireComponentDefinition.setMaxOutputPorts(1);
                wireComponentDefinition.setDefaultOutputPorts(1);
            } else if (WIRE_RECEIVER.equals(service)) {
                wireComponentDefinition.setMinInputPorts(1);
                wireComponentDefinition.setMaxInputPorts(1);
                wireComponentDefinition.setDefaultInputPorts(1);
            }
        }
        wireComponentDefinition.setComponentOCD(ocd);

        return wireComponentDefinition;
    }

    private WireComponentDefinition getDefinitionFromComponentProperties(Component component,
            ComponentConfiguration ocd) {
        final Dictionary<?, ?> componentProperties = component.getProperties();
        final WireComponentDefinition wireComponentDefinition = new WireComponentDefinition();

        wireComponentDefinition.setFactoryPid(component.getName());
        wireComponentDefinition.setMinInputPorts((int) componentProperties.get("input.cardinality.minimum"));
        wireComponentDefinition.setMaxInputPorts((int) componentProperties.get("input.cardinality.maximum"));
        wireComponentDefinition.setDefaultInputPorts((int) componentProperties.get("input.cardinality.default"));
        wireComponentDefinition.setMinOutputPorts((int) componentProperties.get("output.cardinality.minimum"));
        wireComponentDefinition.setMaxOutputPorts((int) componentProperties.get("output.cardinality.maximum"));
        wireComponentDefinition.setDefaultOutputPorts((int) componentProperties.get("output.cardinality.default"));
        wireComponentDefinition.setInputPortNames(parsePortNames(componentProperties.get("input.port.names"),
                wireComponentDefinition.getMaxInputPorts()));
        wireComponentDefinition.setOutputPortNames(parsePortNames(componentProperties.get("output.port.names"),
                wireComponentDefinition.getMaxOutputPorts()));
        wireComponentDefinition.setComponentOCD(ocd);

        return wireComponentDefinition;
    }

    @Override
    public List<WireComponentDefinition> getComponentDefinitions() throws KuraException {

        final BundleContext context = FrameworkUtil.getBundle(WireHelperServiceImpl.class).getBundleContext();
        ServiceReference<ScrService> scrServiceRef = context.getServiceReference(ScrService.class);
        ServiceReference<OCDService> ocdServiceRef = context.getServiceReference(OCDService.class);

        try {
            final OCDService ocdService = context.getService(ocdServiceRef);
            final ScrService scrService = context.getService(scrServiceRef);

            final String[] services = new String[] { WIRE_COMPONENT, WIRE_RECEIVER, WIRE_EMITTER };

            final Map<String, Component> componentDefinitions = Arrays.stream(scrService.getComponents())
                    .filter(component -> implementsAnyService(component, services))
                    .collect(Collectors.toMap(Component::getName, identity(), (first, second) -> second));

            final Map<String, ComponentConfiguration> ocds = ocdService.getServiceProviderOCDs(services).stream()
                    .collect(Collectors.toMap(ComponentConfiguration::getPid, identity(), (first, second) -> second));

            return componentDefinitions.entrySet().stream()
                    .map(entry -> getDefinition(entry.getValue(), ocds.get(entry.getKey())))
                    .collect(Collectors.toList());

        } finally {
            context.ungetService(scrServiceRef);
            context.ungetService(ocdServiceRef);
        }

    }
}
