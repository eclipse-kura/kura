/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server.util;

import static java.util.Objects.isNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.service.GwtWireGraphService;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.graph.WireComponentDefinition;
import org.eclipse.kura.wire.graph.WireComponentDefinitionService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GwtComponentServiceInternal {

    private static final String SUCCESSFULLY_LISTED_COMPONENT_CONFIGS_MESSAGE = "UI Component - Success - Successfully listed component configs for user: {}, session {}";

    private static final String SUCCESSFULLY_LISTED_PIDS_FROM_TARGET_MESSAGE = "UI Component - Success - Successfully listed pids from target for user: {}, session: {}, pid: {}";

    private static final String FAILED_TO_CREATE_COMPONENT_CONFIG_MESSAGE = "UI Component - Failure - Failed to create component config for user: {}, session {}";

    private static final String FAILED_TO_LIST_COMPONENT_CONFIGS_MESSAGE = "UI Component - Failure - Failed to list component configs for user: {}, session {}";

    private static final String SUCCESSFULLY_LISTED_COMPONENT_CONFIG_MESSAGE = "UI Component - Success - Successfully listed component config for user: {}, session {}";

    private static final String FAILED_TO_LIST_COMPONENT_CONFIG_MESSAGE = "UI Component - Failure - Failed to list component config for user: {}, session {}";

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private static final String DRIVER_PID = "driver.pid";
    private static final String KURA_SERVICE_PID = ConfigurationService.KURA_SERVICE_PID;
    private static final String SERVICE_FACTORY_PID = "service.factoryPid";
    private static final String KURA_UI_SERVICE_HIDE = "kura.ui.service.hide";
    private static final String PATTERN_SERVICE_PROVIDE_DRIVER = "provide interface=\"org.eclipse.kura.driver.Driver\"";

    private static final int SERVICE_WAIT_TIMEOUT = 60;

    private GwtComponentServiceInternal() {
    }

    public static List<String> findTrackedPids() throws GwtKuraException {
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);

        return new ArrayList<>(cs.getConfigurableComponentPids());
    }

    public static List<GwtConfigComponent> findFilteredComponentConfigurations(HttpSession session)
            throws GwtKuraException {
        return findFilteredComponentConfigurationsInternal(session);
    }

    public static List<GwtConfigComponent> findComponentConfigurations(HttpSession session, String osgiFilter)
            throws GwtKuraException {

        try {
            final Filter filter = FrameworkUtil.createFilter(osgiFilter);
            List<GwtConfigComponent> result = ServiceLocator.applyToServiceOptionally(ConfigurationService.class,
                    configurationService -> configurationService.getComponentConfigurations(filter) //
                            .stream() //
                            .map(GwtComponentServiceInternal::createMetatypeOnlyGwtComponentConfigurationInternal) //
                            .filter(Objects::nonNull) //
                            .collect(Collectors.toList()));

            auditLogger.info(SUCCESSFULLY_LISTED_COMPONENT_CONFIGS_MESSAGE,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            return result;
        } catch (InvalidSyntaxException e) {
            auditLogger.warn(FAILED_TO_LIST_COMPONENT_CONFIGS_MESSAGE,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
        } catch (Exception e) {
            auditLogger.warn(FAILED_TO_LIST_COMPONENT_CONFIGS_MESSAGE,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static List<GwtConfigComponent> findFilteredComponentConfiguration(HttpSession session, String componentPid)
            throws GwtKuraException {
        return findFilteredComponentConfigurationInternal(session, componentPid);
    }

    public static List<GwtConfigComponent> findComponentConfigurations(HttpSession session) throws GwtKuraException {
        return findComponentConfigurationsInternal(session);
    }

    public static List<GwtConfigComponent> findComponentConfiguration(HttpSession session, String componentPid)
            throws GwtKuraException {
        return findComponentConfigurationInternal(session, componentPid);
    }

    public static void updateComponentConfiguration(HttpSession session, GwtConfigComponent gwtCompConfig)
            throws GwtKuraException {

        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        try {
            // Build the new properties
            Map<String, Object> properties = new HashMap<>();
            ComponentConfiguration currentCC = cs.getComponentConfiguration(gwtCompConfig.getComponentId());

            Map<String, Object> currentConfigProp = currentCC.getConfigurationProperties();
            for (GwtConfigParameter gwtConfigParam : gwtCompConfig.getParameters()) {
                Object objValue;
                Object currentValue = currentConfigProp.get(gwtConfigParam.getId());

                boolean isReadOnly = gwtConfigParam.getMin() != null
                        && gwtConfigParam.getMin().equals(gwtConfigParam.getMax());
                if (isReadOnly) {
                    objValue = currentValue;
                } else {
                    objValue = GwtServerUtil.getUserDefinedObject(gwtConfigParam, currentValue);
                }
                properties.put(gwtConfigParam.getId(), objValue);
            }

            // Force kura.service.pid into properties, if originally present
            if (currentConfigProp.get(KURA_SERVICE_PID) != null) {
                properties.put(KURA_SERVICE_PID, currentConfigProp.get(KURA_SERVICE_PID));
            }
            //
            // apply them
            cs.updateConfiguration(gwtCompConfig.getComponentId(), properties);
            auditLogger.info(
                    "UI Component - Success - Successfully updated component config for user: {}, session {}, component ID: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(),
                    gwtCompConfig.getComponentId());
        } catch (KuraException e) {
            auditLogger.warn("UI Component - Failure - Failed to update component config for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            KuraExceptionHandler.handle(e);
        }
    }

    public static void createFactoryComponent(HttpSession session, String factoryPid, String pid)
            throws GwtKuraException {
        internalCreateFactoryComponent(session, factoryPid, pid, null);
    }

    public static void createFactoryComponent(HttpSession session, String factoryPid, String pid,
            GwtConfigComponent properties) throws GwtKuraException {

        Map<String, Object> propertiesMap = GwtServerUtil.fillPropertiesFromConfiguration(properties, null);

        internalCreateFactoryComponent(session, factoryPid, pid, propertiesMap);
    }

    private static void internalCreateFactoryComponent(HttpSession session, String factoryPid, String pid,
            Map<String, Object> properties) throws GwtKuraException {
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        try {
            cs.createFactoryConfiguration(factoryPid, pid, properties, true);

            String filterString = "(" + ConfigurationService.KURA_SERVICE_PID + "=" + pid + ")";

            if (!ServiceUtil.waitForService(filterString, SERVICE_WAIT_TIMEOUT, TimeUnit.SECONDS).isPresent()) {
                throw new GwtKuraException("Created component did not start in " + SERVICE_WAIT_TIMEOUT + " seconds");
            }

            auditLogger.info("UI Component - Success - Successfully created component config for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
        } catch (KuraException e) {
            auditLogger.warn(FAILED_TO_CREATE_COMPONENT_CONFIG_MESSAGE,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException("A component with the same name already exists!");
        } catch (InterruptedException e) {
            auditLogger.warn(FAILED_TO_CREATE_COMPONENT_CONFIG_MESSAGE,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException("Interrupted while waiting for component creation");
        } catch (InvalidSyntaxException e) {
            auditLogger.warn(FAILED_TO_CREATE_COMPONENT_CONFIG_MESSAGE,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException("Invalid value for " + ConfigurationService.KURA_SERVICE_PID + ": " + pid);
        }
    }

    public static void deleteFactoryConfiguration(HttpSession session, String pid, boolean takeSnapshot)
            throws GwtKuraException {

        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);

        try {
            cs.deleteFactoryConfiguration(pid, takeSnapshot);
            auditLogger.info("UI Component - Success - Successfully deleted component config for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
        } catch (KuraException e) {
            auditLogger.warn("UI Component - Failure - Failed to delete component config for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException("Could not delete component configuration!");
        }
    }

    public static List<String> findFactoryComponents(HttpSession session) throws GwtKuraException {

        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<String> result = new ArrayList<>();
        List<String> servicesToBeHidden = new ArrayList<>();

        // finding all wire components to remove from the list as these factory
        // instances
        // are only shown in Kura Wires UI
        List<String> allWireComponents = findWireComponents();

        // find components that declare the
        // kura.ui.factory.hide component property
        List<String> hiddenFactories = findFactoryHideComponents();

        // finding services with kura.service.ui.hide property
        fillServicesToHideList(servicesToBeHidden);

        // get all the factory PIDs tracked by Configuration Service
        result.addAll(cs.getFactoryComponentPids());

        // remove all the wire components and the services to be hidden as these
        // are shown in different UI
        result.removeAll(allWireComponents);
        result.removeAll(servicesToBeHidden);
        result.removeAll(hiddenFactories);

        auditLogger.info(SUCCESSFULLY_LISTED_COMPONENT_CONFIGS_MESSAGE,
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
        return result;
    }

    private static List<String> findWireComponents() throws GwtKuraException {
        return ServiceLocator.applyToServiceOptionally(WireComponentDefinitionService.class,
                wireComponentDefinitionService -> wireComponentDefinitionService.getComponentDefinitions().stream()
                        .map(WireComponentDefinition::getFactoryPid).collect(Collectors.toList()));
    }

    private static List<String> findFactoryHideComponents() throws GwtKuraException {
        return ServiceLocator.applyToServiceOptionally(ServiceComponentRuntime.class,
                scr -> scr.getComponentDescriptionDTOs().stream()
                        .filter(dto -> dto.properties.containsKey("kura.ui.factory.hide")).map(dto -> dto.name)
                        .collect(Collectors.toList()));
    }

    private static List<ComponentConfiguration> sortConfigurationsByName(List<ComponentConfiguration> configs) {
        Collections.sort(configs, (arg0, arg1) -> {
            String name0;
            int start = arg0.getPid().lastIndexOf('.');
            int substringIndex = start + 1;
            if (start != -1 && substringIndex < arg0.getPid().length()) {
                name0 = arg0.getPid().substring(substringIndex);
            } else {
                name0 = arg0.getPid();
            }

            String name1;
            start = arg1.getPid().lastIndexOf('.');
            substringIndex = start + 1;
            if (start != -1 && substringIndex < arg1.getPid().length()) {
                name1 = arg1.getPid().substring(substringIndex);
            } else {
                name1 = arg1.getPid();
            }
            return name0.compareTo(name1);
        });
        return configs;
    }

    private static String stripPidPrefix(String pid) {
        int start = pid.lastIndexOf('.');
        if (start < 0) {
            return pid;
        } else {
            int begin = start + 1;
            if (begin < pid.length()) {
                return pid.substring(begin);
            } else {
                return pid;
            }
        }
    }

    private static void fillServicesToHideList(List<String> hidePidsList) throws GwtKuraException {
        Collection<ServiceReference<ConfigurableComponent>> configurableComponentReferences = ServiceLocator
                .getInstance().getServiceReferences(ConfigurableComponent.class, null);

        Collection<ServiceReference<SelfConfiguringComponent>> selfConfiguringComponentReferences = ServiceLocator
                .getInstance().getServiceReferences(SelfConfiguringComponent.class, null);

        List<ServiceReference<?>> componentReferences = new ArrayList<>();
        componentReferences.addAll(configurableComponentReferences);
        componentReferences.addAll(selfConfiguringComponentReferences);

        for (ServiceReference<?> componentReference : componentReferences) {
            Object propertyObject = componentReference.getProperty(KURA_SERVICE_PID);
            if (componentReference.getProperty(KURA_UI_SERVICE_HIDE) != null && propertyObject != null) {
                String servicePid = (String) propertyObject;
                hidePidsList.add(servicePid);
            }
            ServiceLocator.getInstance().ungetService(componentReference);
        }
    }

    private static List<GwtConfigComponent> findFilteredComponentConfigurationsInternal(HttpSession session)
            throws GwtKuraException {

        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<GwtConfigComponent> gwtConfigs = new ArrayList<>();
        try {

            List<ComponentConfiguration> configs = cs.getComponentConfigurations();
            sortConfigurationsByName(configs);

            for (ComponentConfiguration config : configs) {
                GwtConfigComponent gwtConfigComponent = createMetatypeOnlyGwtComponentConfiguration(config);
                if (gwtConfigComponent != null) {
                    gwtConfigs.add(gwtConfigComponent);
                }
            }
        } catch (Exception e) {
            auditLogger.warn(FAILED_TO_LIST_COMPONENT_CONFIGS_MESSAGE,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            KuraExceptionHandler.handle(e);
        }

        auditLogger.info(SUCCESSFULLY_LISTED_COMPONENT_CONFIGS_MESSAGE,
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
        return gwtConfigs;
    }

    private static List<GwtConfigComponent> findComponentConfigurationInternal(HttpSession session, String componentPid)
            throws GwtKuraException {

        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<GwtConfigComponent> gwtConfigs = new ArrayList<>();
        try {
            ComponentConfiguration config = cs.getComponentConfiguration(componentPid);
            GwtConfigComponent gwtConfigComponent = null;
            if (config != null) {
                gwtConfigComponent = createMetatypeOnlyGwtComponentConfiguration(config);
            }
            GwtConfigComponent fullGwtConfigComponent = null;
            if (gwtConfigComponent != null) {
                fullGwtConfigComponent = addNonMetatypeProperties(gwtConfigComponent, config);
            }
            if (fullGwtConfigComponent != null) {
                gwtConfigs.add(fullGwtConfigComponent);
            }
        } catch (Exception e) {
            auditLogger.warn(FAILED_TO_LIST_COMPONENT_CONFIG_MESSAGE,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            KuraExceptionHandler.handle(e);
        }

        auditLogger.info(SUCCESSFULLY_LISTED_COMPONENT_CONFIG_MESSAGE,
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
        return gwtConfigs;
    }

    private static List<GwtConfigComponent> findFilteredComponentConfigurationInternal(HttpSession session,
            String componentPid) throws GwtKuraException {

        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<GwtConfigComponent> gwtConfigs = new ArrayList<>();
        try {
            ComponentConfiguration config = cs.getComponentConfiguration(componentPid);

            if (config != null) {
                GwtConfigComponent gwtConfigComponent = createMetatypeOnlyGwtComponentConfiguration(config);
                gwtConfigs.add(gwtConfigComponent);
            }
        } catch (Exception e) {
            auditLogger.warn(FAILED_TO_LIST_COMPONENT_CONFIG_MESSAGE,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            KuraExceptionHandler.handle(e);
        }

        auditLogger.info(SUCCESSFULLY_LISTED_COMPONENT_CONFIG_MESSAGE,
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
        return gwtConfigs;
    }

    private static GwtConfigComponent addNonMetatypeProperties(GwtConfigComponent baseGwtConfig,
            ComponentConfiguration config) {
        GwtConfigComponent gwtConfigComponent = null;
        OCD ocd = config.getDefinition();
        if (ocd != null && baseGwtConfig != null) {
            gwtConfigComponent = new GwtConfigComponent();

            gwtConfigComponent.setComponentDescription(baseGwtConfig.getComponentDescription());
            gwtConfigComponent.setComponentId(baseGwtConfig.getComponentId());
            gwtConfigComponent.setComponentIcon(baseGwtConfig.getComponentIcon());
            gwtConfigComponent.setComponentName(baseGwtConfig.getComponentName());
            gwtConfigComponent.setProperties(baseGwtConfig.getProperties());

            List<GwtConfigParameter> gwtParams = new ArrayList<>();
            gwtConfigComponent.setParameters(gwtParams);

            List<GwtConfigParameter> nonMetatypeConfigParameters = new ArrayList<>();

            if (config.getConfigurationProperties() != null) {

                List<GwtConfigParameter> nonMetatypeProps = getNonMetatypeProperties(config);
                nonMetatypeConfigParameters.addAll(nonMetatypeProps);
            }
            gwtConfigComponent.setParameters(nonMetatypeConfigParameters);
        }
        return gwtConfigComponent;
    }

    private static List<GwtConfigComponent> findComponentConfigurationsInternal(HttpSession session)
            throws GwtKuraException {

        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<GwtConfigComponent> gwtConfigs = new ArrayList<>();
        try {

            List<ComponentConfiguration> configs = cs.getComponentConfigurations();
            sortConfigurationsByName(configs);

            for (ComponentConfiguration config : configs) {
                GwtConfigComponent gwtConfigComponent = createMetatypeOnlyGwtComponentConfiguration(config);
                gwtConfigs.add(gwtConfigComponent);
            }
        } catch (Exception e) {
            auditLogger.warn(FAILED_TO_LIST_COMPONENT_CONFIG_MESSAGE,
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            KuraExceptionHandler.handle(e);
        }

        auditLogger.info(SUCCESSFULLY_LISTED_COMPONENT_CONFIG_MESSAGE,
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
        return gwtConfigs;
    }

    private static GwtConfigComponent createMetatypeOnlyGwtComponentConfigurationInternal(
            ComponentConfiguration config) {
        GwtConfigComponent gwtConfig = null;

        OCD ocd = config.getDefinition();
        if (ocd != null) {

            gwtConfig = new GwtConfigComponent();
            gwtConfig.setComponentId(config.getPid());

            Map<String, Object> props = config.getConfigurationProperties();
            if (props != null && props.get(DRIVER_PID) != null) {
                gwtConfig.set(DRIVER_PID, props.get(DRIVER_PID));
            }

            if (props != null && props.get(SERVICE_FACTORY_PID) != null) {
                String pid = stripPidPrefix(config.getPid());
                gwtConfig.setComponentName(pid);
                gwtConfig.setFactoryComponent(true);
                gwtConfig.setFactoryPid(String.valueOf(props.get(ConfigurationAdmin.SERVICE_FACTORYPID)));
            } else {
                gwtConfig.setComponentName(ocd.getName());
                gwtConfig.setFactoryComponent(false);
            }

            gwtConfig.setComponentDescription(ocd.getDescription());
            if (ocd.getIcon() != null && !ocd.getIcon().isEmpty()) {
                Icon icon = ocd.getIcon().get(0);
                gwtConfig.setComponentIcon(icon.getResource());
            }

            List<GwtConfigParameter> gwtParams = new ArrayList<>();
            gwtConfig.setParameters(gwtParams);

            if (config.getConfigurationProperties() != null) {
                List<GwtConfigParameter> metatypeProps = getADProperties(config);
                gwtParams.addAll(metatypeProps);
            }
        }
        return gwtConfig;
    }

    private static GwtConfigComponent createMetatypeOnlyGwtComponentConfiguration(ComponentConfiguration config)
            throws GwtKuraException {
        final GwtConfigComponent gwtConfig = createMetatypeOnlyGwtComponentConfigurationInternal(config);
        if (gwtConfig != null) {
            gwtConfig.setIsWireComponent(ServiceLocator.applyToServiceOptionally(WireHelperService.class,
                    wireHelperService -> wireHelperService.getServicePid(gwtConfig.getComponentName()) != null));
        }
        return gwtConfig;
    }

    private static List<GwtConfigParameter> getNonMetatypeProperties(ComponentConfiguration config) {
        List<GwtConfigParameter> gwtParams = new ArrayList<>();
        for (Map.Entry<String, Object> entry : config.getConfigurationProperties().entrySet()) {
            GwtConfigParameter gwtParam = new GwtConfigParameter();
            gwtParam.setId(entry.getKey());
            Object value = entry.getValue();

            // this could be an array value
            if (value instanceof Object[]) {
                Object[] objValues = (Object[]) value;
                List<String> strValues = new ArrayList<>();
                for (Object v : objValues) {
                    if (v != null) {
                        strValues.add(String.valueOf(v));
                    }
                }
                gwtParam.setValues(strValues.toArray(new String[] {}));
            } else if (value != null) {
                gwtParam.setValue(String.valueOf(value));
            }

            gwtParams.add(gwtParam);
        }
        return gwtParams;
    }

    private static List<GwtConfigParameter> getADProperties(ComponentConfiguration config) {
        List<GwtConfigParameter> gwtParams = new ArrayList<>();
        OCD ocd = config.getDefinition();
        for (AD ad : ocd.getAD()) {
            GwtConfigParameter gwtParam = new GwtConfigParameter();
            gwtParam.setId(ad.getId());
            gwtParam.setName(ad.getName());
            gwtParam.setDescription(ad.getDescription());
            gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
            gwtParam.setRequired(ad.isRequired());
            gwtParam.setCardinality(ad.getCardinality());
            if (ad.getOption() != null && !ad.getOption().isEmpty()) {
                Map<String, String> options = new HashMap<>();
                for (Option option : ad.getOption()) {
                    options.put(option.getLabel(), option.getValue());
                }
                gwtParam.setOptions(options);
            }
            gwtParam.setMin(ad.getMin());
            gwtParam.setMax(ad.getMax());

            // handle the value based on the cardinality of the attribute
            int cardinality = ad.getCardinality();
            Object value = config.getConfigurationProperties().get(ad.getId());
            if (value != null) {
                if (cardinality == 0 || cardinality == 1 || cardinality == -1) {
                    if (gwtParam.getType().equals(GwtConfigParameterType.PASSWORD)) {
                        gwtParam.setValue(GwtServerUtil.PASSWORD_PLACEHOLDER);
                    } else {
                        gwtParam.setValue(String.valueOf(value));
                    }
                } else {
                    // this could be an array value
                    if (value instanceof Object[]) {
                        Object[] objValues = (Object[]) value;
                        List<String> strValues = new ArrayList<>();
                        for (Object v : objValues) {
                            if (v != null) {
                                if (gwtParam.getType().equals(GwtConfigParameterType.PASSWORD)) {
                                    strValues.add(GwtServerUtil.PASSWORD_PLACEHOLDER);
                                } else {
                                    strValues.add(String.valueOf(v));
                                }
                            }
                        }
                        gwtParam.setValues(strValues.toArray(new String[] {}));
                    }
                }
            }
            gwtParams.add(gwtParam);
        }
        return gwtParams;
    }

    public static boolean updateProperties(HttpSession session, String pid, Map<String, Object> properties)
            throws GwtKuraException {

        final ConfigurationAdmin configAdmin = ServiceLocator.getInstance().getService(ConfigurationAdmin.class);
        final WireHelperService wireHelperService = ServiceLocator.getInstance().getService(WireHelperService.class);
        try {
            final String servicePid = wireHelperService.getServicePid(pid);
            Configuration conf = null;
            if (servicePid != null) {
                conf = configAdmin.getConfiguration(servicePid);
            }
            Dictionary<String, Object> props = null;
            if (conf != null) {
                props = conf.getProperties();
            }
            if (props == null) {
                props = new Hashtable<>();
            }
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                props.put(key, value != null ? value : "");
            }
            if (conf != null) {
                conf.update(props);
            }
        } catch (IOException e) {
            auditLogger.info(
                    "UI Component - Failure - Failed to update component config for user: {}, session: {}, pid: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), pid);
            return false;
        }

        auditLogger.info(
                "UI Component - Success - Successfully updated component config for user: {}, session: {}, pid: {}",
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), pid);

        return true;
    }

    public static List<String> getDriverFactoriesList(HttpSession session) throws GwtKuraException {

        List<String> driverFactoriesPids = new ArrayList<>();
        final Bundle[] bundles = FrameworkUtil.getBundle(GwtWireGraphService.class).getBundleContext().getBundles();
        for (final Bundle bundle : bundles) {
            final Enumeration<URL> enumeration = bundle.findEntries("OSGI-INF", "*.xml", false);
            if (Objects.isNull(enumeration)) {
                continue;
            }

            while (enumeration.hasMoreElements()) {
                final URL entry = enumeration.nextElement();
                try (InputStreamReader inputStream = new InputStreamReader(entry.openConnection().getInputStream());
                        BufferedReader reader = new BufferedReader(inputStream);) {
                    final StringBuilder contents = getManifestContent(reader);
                    // Configuration Policy=Require and
                    // SelfConfiguringComponent or ConfigurableComponent
                    if ((contents.toString().contains(GwtServerUtil.PATTERN_SERVICE_PROVIDE_SELF_CONFIGURING_COMP)
                            || contents.toString().contains(GwtServerUtil.PATTERN_SERVICE_PROVIDE_CONFIGURABLE_COMP))
                            && contents.toString().contains(GwtServerUtil.PATTERN_CONFIGURATION_REQUIRE)) {
                        driverFactoriesPids.addAll(manageRequiredConfigurableComponents(entry, contents));
                    }
                } catch (final Exception ex) {
                    auditLogger.info(
                            "UI Component - Failure - Failed to list driver factories for user: {}, session: {}",
                            session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
                    throw new GwtKuraException(GwtKuraErrorCode.RESOURCE_FETCHING_FAILURE);
                }
            }

        }

        auditLogger.info("UI Component - Success - Successfully listed driver factories for user: {}, session: {}",
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());

        return driverFactoriesPids;
    }

    private static StringBuilder getManifestContent(BufferedReader reader) throws IOException {
        final StringBuilder contents = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            contents.append(line);
        }
        return contents;
    }

    private static List<String> manageRequiredConfigurableComponents(final URL entry, final StringBuilder contents)
            throws ParserConfigurationException, SAXException, IOException {
        List<String> driverFactoriesPids = new ArrayList<>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        final Document dom = dbf.newDocumentBuilder().parse(entry.openConnection().getInputStream());
        final NodeList nl = dom.getElementsByTagName("property");
        for (int i = 0; i < nl.getLength(); i++) {
            final Node n = nl.item(i);
            if (n instanceof Element) {
                final String name = ((Element) n).getAttribute("name");
                if ("service.pid".equals(name)) {
                    final String factoryPid = ((Element) n).getAttribute("value");
                    if (contents.toString().contains(PATTERN_SERVICE_PROVIDE_DRIVER)) {
                        driverFactoriesPids.add(factoryPid);
                    }
                }
            }
        }
        return driverFactoriesPids;
    }

    public static List<String> getPidsFromTarget(HttpSession session, String pid, String targetRef) {

        List<String> result = new ArrayList<>();

        final BundleContext context = FrameworkUtil.getBundle(GwtWireGraphService.class).getBundleContext();
        ServiceReference<ServiceComponentRuntime> scrServiceRef = context
                .getServiceReference(ServiceComponentRuntime.class);
        try {
            final ServiceComponentRuntime scrService = context.getService(scrServiceRef);

            final Set<String> referenceInterfaces = scrService.getComponentDescriptionDTOs().stream()
                    .filter(componentDescription -> scrService.getComponentConfigurationDTOs(componentDescription)
                            .stream().anyMatch(componentConfiguration -> {
                                String kuraServicePid = (String) componentConfiguration.properties
                                        .get(ConfigurationService.KURA_SERVICE_PID);
                                return kuraServicePid != null && kuraServicePid.equals(pid);
                            }))
                    .map(componentDescription -> {
                        ReferenceDTO[] references = componentDescription.references;
                        for (ReferenceDTO reference : references) {
                            if (targetRef.equals(reference.name)) {
                                return reference.interfaceName;
                            }
                        }
                        return null;
                    }).filter(Objects::nonNull).collect(Collectors.toSet());

            referenceInterfaces.forEach(reference -> {
                try {
                    ServiceReference<?>[] serviceReferences = context.getServiceReferences(reference, null);

                    if (isNull(serviceReferences)) {
                        return;
                    }

                    Arrays.stream(serviceReferences).forEach(serviceReference -> {
                        result.add((String) serviceReference.getProperty(KURA_SERVICE_PID));
                        ServiceLocator.getInstance().ungetService(serviceReference);
                    });
                } catch (InvalidSyntaxException e) {
                    // Nothing to do
                }
            });

        } finally {
            context.ungetService(scrServiceRef);
        }

        auditLogger.info(SUCCESSFULLY_LISTED_PIDS_FROM_TARGET_MESSAGE,
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), pid);
        return result;
    }

}
