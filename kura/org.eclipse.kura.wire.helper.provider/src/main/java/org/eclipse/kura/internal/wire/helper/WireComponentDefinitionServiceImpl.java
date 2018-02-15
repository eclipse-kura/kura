/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.helper;

import static java.util.function.Function.identity;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.metatype.OCDService;
import org.eclipse.kura.wire.graph.WireComponentDefinition;
import org.eclipse.kura.wire.graph.WireComponentDefinitionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WireComponentDefinitionServiceImpl implements WireComponentDefinitionService {

    private static final Logger logger = LoggerFactory.getLogger(WireComponentDefinitionServiceImpl.class);

    private static final Pattern DEFAULT_PORT_NAME_PATTERN = Pattern.compile("(in|out)(\\d+)");
    private static final String WIRE_COMPONENT = "org.eclipse.kura.wire.WireComponent";
    private static final String WIRE_EMITTER = "org.eclipse.kura.wire.WireEmitter";
    private static final String WIRE_RECEIVER = "org.eclipse.kura.wire.WireReceiver";

    private static boolean implementsAnyService(ComponentDescriptionDTO component, String[] classes) {
        final String[] services = component.serviceInterfaces;
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

    private WireComponentDefinition getDefinition(ComponentDescriptionDTO componentDescriptionDTO,
            ComponentConfiguration ocd) {
        if (componentDescriptionDTO.properties.get("input.cardinality.minimum") != null) {
            return getDefinitionFromComponentProperties(componentDescriptionDTO, ocd);
        } else {
            return getDefinitionLegacy(componentDescriptionDTO, ocd);
        }
    }

    private WireComponentDefinition getDefinitionLegacy(ComponentDescriptionDTO componentDescriptionDTO,
            ComponentConfiguration ocd) {
        WireComponentDefinition wireComponentDefinition = new WireComponentDefinition();
        wireComponentDefinition.setFactoryPid(componentDescriptionDTO.name);
        for (String service : componentDescriptionDTO.serviceInterfaces) {
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

    private WireComponentDefinition getDefinitionFromComponentProperties(
            ComponentDescriptionDTO componentDescriptionDTO, ComponentConfiguration ocd) {
        final Map<?, ?> componentProperties = componentDescriptionDTO.properties;
        final WireComponentDefinition wireComponentDefinition = new WireComponentDefinition();

        wireComponentDefinition.setFactoryPid(componentDescriptionDTO.name);
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
        ServiceReference<ServiceComponentRuntime> scrServiceRef = context
                .getServiceReference(ServiceComponentRuntime.class);
        ServiceReference<OCDService> ocdServiceRef = context.getServiceReference(OCDService.class);

        try {
            final OCDService ocdService = context.getService(ocdServiceRef);
            final ServiceComponentRuntime scrService = context.getService(scrServiceRef);

            final String[] services = new String[] { WIRE_COMPONENT, WIRE_RECEIVER, WIRE_EMITTER };

            final Map<Object, ComponentDescriptionDTO> componentDefinitions = scrService.getComponentDescriptionDTOs()
                    .stream().filter(component -> implementsAnyService(component, services))
                    .collect(Collectors.toMap(c -> c.name, identity(), (first, second) -> second));

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
