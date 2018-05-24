/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *     Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.web.server;

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

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.kura.KuraErrorCode;
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
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.KuraExceptionHandler;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.graph.WireComponentDefinition;
import org.eclipse.kura.wire.graph.WireComponentDefinitionService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GwtComponentServiceImpl extends OsgiRemoteServiceServlet implements GwtComponentService {

    private static final String DRIVER_PID = "driver.pid";
    private static final String KURA_SERVICE_PID = ConfigurationService.KURA_SERVICE_PID;
    private static final String SERVICE_FACTORY_PID = "service.factoryPid";
    private static final String KURA_UI_SERVICE_HIDE = "kura.ui.service.hide";
    private static final String PATTERN_SERVICE_PROVIDE_DRIVER = "provide interface=\"org.eclipse.kura.driver.Driver\"";

    private static final int SERVICE_WAIT_TIMEOUT = 60;

    private static final long serialVersionUID = -4176701819112753800L;

    @Override
    public List<String> findTrackedPids(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);

        return new ArrayList<>(cs.getConfigurableComponentPids());
    }

    @Override
    public List<GwtConfigComponent> findFilteredComponentConfigurations(GwtXSRFToken xsrfToken)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        return findFilteredComponentConfigurationsInternal();
    }

    @Override
    public List<GwtConfigComponent> findComponentConfigurations(GwtXSRFToken xsrfToken, String osgiFilter)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        try {
            final BundleContext context = FrameworkUtil.getBundle(GwtComponentServiceImpl.class).getBundleContext();
            final Set<String> matchingPids = Arrays.stream(context.getServiceReferences((String) null, osgiFilter))
                    .map(reference -> (String) reference.getProperty(KURA_SERVICE_PID)).collect(Collectors.toSet());
            return ServiceLocator
                    .applyToServiceOptionally(ConfigurationService.class,
                            configurationService -> configurationService.getComponentConfigurations().stream()
                                    .filter(config -> matchingPids.contains(config.getPid())))
                    .map(this::createMetatypeOnlyGwtComponentConfigurationInternal).filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (InvalidSyntaxException e) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
        } catch (Exception e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public List<GwtConfigComponent> findFilteredComponentConfiguration(GwtXSRFToken xsrfToken, String componentPid)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        return findFilteredComponentConfigurationInternal(componentPid);
    }

    @Override
    public List<GwtConfigComponent> findComponentConfigurations(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        return findComponentConfigurationsInternal();
    }

    @Override
    public List<GwtConfigComponent> findComponentConfiguration(GwtXSRFToken xsrfToken, String componentPid)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        return findComponentConfigurationInternal(componentPid);
    }

    @Override
    public void updateComponentConfiguration(GwtXSRFToken xsrfToken, GwtConfigComponent gwtCompConfig)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
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
        } catch (KuraException e) {
            KuraExceptionHandler.handle(e);
        }
    }

    @Override
    public void createFactoryComponent(GwtXSRFToken xsrfToken, String factoryPid, String pid) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        internalCreateFactoryComponent(factoryPid, pid, null);
    }

    @Override
    public void createFactoryComponent(GwtXSRFToken xsrfToken, String factoryPid, String pid,
            GwtConfigComponent properties) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);

        Map<String, Object> propertiesMap = GwtServerUtil.fillPropertiesFromConfiguration(properties, null);

        internalCreateFactoryComponent(factoryPid, pid, propertiesMap);
    }

    private void internalCreateFactoryComponent(String factoryPid, String pid, Map<String, Object> properties)
            throws GwtKuraException {
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        try {
            cs.createFactoryConfiguration(factoryPid, pid, properties, true);

            String filterString = "(" + ConfigurationService.KURA_SERVICE_PID + "=" + pid + ")";

            if (!ServiceUtil.waitForService(filterString, SERVICE_WAIT_TIMEOUT, TimeUnit.SECONDS).isPresent()) {
                throw new GwtKuraException("Created component did not start in " + SERVICE_WAIT_TIMEOUT + " seconds");
            }
        } catch (KuraException e) {
            throw new GwtKuraException("A component with the same name already exists!");
        } catch (InterruptedException e) {
            throw new GwtKuraException("Interrupted while waiting for component creation");
        } catch (InvalidSyntaxException e) {
            throw new GwtKuraException("Invalid value for " + ConfigurationService.KURA_SERVICE_PID + ": " + pid);
        }
    }

    @Override
    public void deleteFactoryConfiguration(GwtXSRFToken xsrfToken, String pid, boolean takeSnapshot)
            throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);

        try {
            cs.deleteFactoryConfiguration(pid, takeSnapshot);
        } catch (KuraException e) {
            throw new GwtKuraException("Could not delete component configuration!");
        }
    }

    @Override
    public List<String> findFactoryComponents(GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
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
        return result;
    }

    // TODO this is a workaround that gives some time to a BaseAsset to track its driver so that it is
    // able to return its OCD
    private ComponentConfiguration waitForComponentConfiguration(ConfigurationService cs, String pid)
            throws InterruptedException, KuraException {
        final long DELAY_MS = 1000;
        long waitTime = 0;
        while (waitTime < SERVICE_WAIT_TIMEOUT * 1000) {
            final ComponentConfiguration config = cs.getComponentConfiguration(pid);
            if (config != null && config.getDefinition() != null) {
                return config;
            }
            Thread.sleep(DELAY_MS);
            waitTime += DELAY_MS;
        }
        throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR);
    }

    @Override
    public GwtConfigComponent findWireComponentConfigurationFromPid(GwtXSRFToken xsrfToken, String pid,
            String factoryPid, Map<String, Object> extraProps) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        GwtConfigComponent comp = null;
        try {
            ComponentConfiguration conf = cs.getComponentConfiguration(pid);
            if (conf == null) {
                conf = cs.getDefaultComponentConfiguration(factoryPid);
                if (conf != null) {
                    conf.getConfigurationProperties().put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
                }
                if (conf != null && conf.getDefinition() == null) {
                    String temporaryName = String.valueOf(System.nanoTime());
                    cs.createFactoryConfiguration(factoryPid, temporaryName, extraProps, false);
                    try {

                        // track and wait for the wire Component
                        String filterString = "(" + ConfigurationService.KURA_SERVICE_PID + "=" + temporaryName + ")";
                        ServiceUtil.waitForService(filterString, SERVICE_WAIT_TIMEOUT, TimeUnit.SECONDS);

                        return createMetatypeOnlyGwtComponentConfiguration(
                                waitForComponentConfiguration(cs, temporaryName));
                    } catch (Exception ex) {
                        throw new GwtKuraException(ex.getMessage());
                    } finally {
                        cs.deleteFactoryConfiguration(temporaryName, false);
                    }
                }
            }
            comp = createMetatypeOnlyGwtComponentConfiguration(conf);
        } catch (KuraException e) {
            throw new GwtKuraException("Could not retrieve component configuration!");
        }
        return comp;
    }

    private List<String> findWireComponents() throws GwtKuraException {
        return ServiceLocator.applyToServiceOptionally(WireComponentDefinitionService.class,
                wireComponentDefinitionService -> wireComponentDefinitionService.getComponentDefinitions().stream()
                        .map(WireComponentDefinition::getFactoryPid).collect(Collectors.toList()));
    }

    private List<String> findFactoryHideComponents() throws GwtKuraException {
        return ServiceLocator.applyToServiceOptionally(ServiceComponentRuntime.class,
                scr -> scr.getComponentDescriptionDTOs().stream()
                        .filter(dto -> dto.properties.containsKey("kura.ui.factory.hide"))
                        .map(dto -> (String) dto.name).collect(Collectors.toList()));
    }

    private List<ComponentConfiguration> sortConfigurationsByName(List<ComponentConfiguration> configs) {
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

    private String stripPidPrefix(String pid) {
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

    private void fillServicesToHideList(List<String> hidePidsList) throws GwtKuraException {
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

    private List<GwtConfigComponent> findFilteredComponentConfigurationsInternal() throws GwtKuraException {
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
            KuraExceptionHandler.handle(e);
        }
        return gwtConfigs;
    }

    private List<GwtConfigComponent> findComponentConfigurationInternal(String componentPid) throws GwtKuraException {
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
            KuraExceptionHandler.handle(e);
        }
        return gwtConfigs;
    }

    private List<GwtConfigComponent> findFilteredComponentConfigurationInternal(String componentPid)
            throws GwtKuraException {
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<GwtConfigComponent> gwtConfigs = new ArrayList<>();
        try {
            ComponentConfiguration config = cs.getComponentConfiguration(componentPid);

            if (config != null) {
                GwtConfigComponent gwtConfigComponent = createMetatypeOnlyGwtComponentConfiguration(config);
                gwtConfigs.add(gwtConfigComponent);
            }
        } catch (Exception e) {
            KuraExceptionHandler.handle(e);
        }
        return gwtConfigs;
    }

    private GwtConfigComponent addNonMetatypeProperties(GwtConfigComponent baseGwtConfig,
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

    private List<GwtConfigComponent> findComponentConfigurationsInternal() throws GwtKuraException {
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
            KuraExceptionHandler.handle(e);
        }
        return gwtConfigs;
    }

    private GwtConfigComponent createMetatypeOnlyGwtComponentConfigurationInternal(ComponentConfiguration config) {
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

    private GwtConfigComponent createMetatypeOnlyGwtComponentConfiguration(ComponentConfiguration config)
            throws GwtKuraException {
        final GwtConfigComponent gwtConfig = createMetatypeOnlyGwtComponentConfigurationInternal(config);
        if (gwtConfig != null) {
            gwtConfig.setIsWireComponent(ServiceLocator.applyToServiceOptionally(WireHelperService.class,
                    wireHelperService -> wireHelperService.getServicePid(gwtConfig.getComponentName()) != null));
        }
        return gwtConfig;
    }

    private List<GwtConfigParameter> getNonMetatypeProperties(ComponentConfiguration config) {
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

    private List<GwtConfigParameter> getADProperties(ComponentConfiguration config) {
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

    @Override
    public boolean updateProperties(GwtXSRFToken xsrfToken, String pid, Map<String, Object> properties)
            throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
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
            return false;
        }

        return true;
    }

    @Override
    public List<String> getDriverFactoriesList(GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);

        List<String> driverFactoriesPids = new ArrayList<>();
        final Bundle[] bundles = FrameworkUtil.getBundle(GwtWireService.class).getBundleContext().getBundles();
        for (final Bundle bundle : bundles) {
            final Enumeration<URL> enumeration = bundle.findEntries("OSGI-INF", "*.xml", false);
            if (enumeration != null) {
                while (enumeration.hasMoreElements()) {
                    final URL entry = enumeration.nextElement();
                    try (InputStreamReader inputStream = new InputStreamReader(entry.openConnection().getInputStream());
                            BufferedReader reader= new BufferedReader(inputStream);){
                        final StringBuilder contents = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            contents.append(line);
                        }
                        // Configruation Policy=Require and
                        // SelfConfiguringComponent or ConfigurableComponent
                        if ((contents.toString().contains(GwtServerUtil.PATTERN_SERVICE_PROVIDE_SELF_CONFIGURING_COMP)
                                || contents.toString()
                                        .contains(GwtServerUtil.PATTERN_SERVICE_PROVIDE_CONFIGURABLE_COMP))
                                && contents.toString().contains(GwtServerUtil.PATTERN_CONFIGURATION_REQUIRE)) {
                            final Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                                    .parse(entry.openConnection().getInputStream());
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
                        }
                    } catch (final Exception ex) {
                        throw new GwtKuraException(GwtKuraErrorCode.RESOURCE_FETCHING_FAILURE);
                    }
                }
            }
        }
        return driverFactoriesPids;
    }

    @Override
    public List<String> getPidsFromTarget(GwtXSRFToken xsrfToken, String pid, String targetRef)
            throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);

        List<String> result = new ArrayList<>();

        final BundleContext context = FrameworkUtil.getBundle(GwtWireService.class).getBundleContext();
        ServiceReference<ServiceComponentRuntime> scrServiceRef = context
                .getServiceReference(ServiceComponentRuntime.class);
        try {
            final ServiceComponentRuntime scrService = context.getService(scrServiceRef);

            final Set<String> referenceInterfaces = scrService.getComponentDescriptionDTOs().stream()
                    .map(component -> {
                        ReferenceDTO[] references = component.references;
                        for (ReferenceDTO reference : references) {
                            if (targetRef.equals(reference.name)) {
                                return reference.interfaceName;
                            }
                        }
                        return null;
                    }).filter(Objects::nonNull).collect(Collectors.toSet());

            referenceInterfaces.forEach(reference -> {
                try {
                    Class<?> t = Class.forName(reference);
                    Collection<?> cloudServiceReferences = ServiceLocator.getInstance().getServiceReferences(t, null);

                    for (Object cloudServiceReferenceObject : cloudServiceReferences) {
                        if (cloudServiceReferenceObject instanceof ServiceReference) {
                            ServiceReference<?> cloudServiceReference = (ServiceReference<?>) cloudServiceReferenceObject;
                            String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
                            result.add(cloudServicePid);
                            ServiceLocator.getInstance().ungetService(cloudServiceReference);
                        }
                    }
                } catch (ClassNotFoundException | GwtKuraException e) {

                }
            });

        } finally {
            context.ungetService(scrServiceRef);
        }
        return result;
    }

}