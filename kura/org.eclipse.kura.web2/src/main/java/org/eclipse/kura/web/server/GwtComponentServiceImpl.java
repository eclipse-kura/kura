/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.KuraExceptionHandler;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.wire.WireHelperService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

public class GwtComponentServiceImpl extends OsgiRemoteServiceServlet implements GwtComponentService {

    private static final String KURA_SERVICE_PID = ConfigurationService.KURA_SERVICE_PID;
    private static final String KURA_UI_SERVICE_HIDE = "kura.ui.service.hide";
    private static final long serialVersionUID = -4176701819112753800L;

    private static final String SERVICE_FACTORY_PID = "service.factoryPid";

    private GwtConfigComponent convertComponentConfigurationByOcd(ComponentConfiguration config)
            throws GwtKuraException {
        WireHelperService wireHelperService = ServiceLocator.getInstance().getService(WireHelperService.class);
        OCD ocd = config.getDefinition();
        GwtConfigComponent gwtConfig = null;
        if (ocd != null) {
            gwtConfig = new GwtConfigComponent();
            gwtConfig.setComponentId(config.getPid());

            Map<String, Object> props = config.getConfigurationProperties();
            if (props != null && props.get("driver.pid") != null) {
                gwtConfig.set("driver.pid", props.get("driver.pid"));
            }

            if ((props != null) && (props.get(SERVICE_FACTORY_PID) != null)) {
                String pid = this.stripPidPrefix(config.getPid());
                gwtConfig.setComponentName(pid);
                gwtConfig.setFactoryComponent(true);
                gwtConfig.setFactoryPid(String.valueOf(props.get(ConfigurationAdmin.SERVICE_FACTORYPID)));
                // check if the PID is assigned to a Wire Component
                gwtConfig.setWireComponent(wireHelperService.getServicePid(pid) != null);
            } else {
                gwtConfig.setComponentName(ocd.getName());
                gwtConfig.setFactoryComponent(false);
                gwtConfig.setWireComponent(false);
            }

            gwtConfig.setComponentDescription(ocd.getDescription());
            if ((ocd.getIcon() != null) && !ocd.getIcon().isEmpty()) {
                Icon icon = ocd.getIcon().get(0);
                gwtConfig.setComponentIcon(icon.getResource());
            }

            List<GwtConfigParameter> gwtParams = new ArrayList<GwtConfigParameter>();
            gwtConfig.setParameters(gwtParams);
            for (AD ad : ocd.getAD()) {
                GwtConfigParameter gwtParam = new GwtConfigParameter();
                gwtParam.setId(ad.getId());
                gwtParam.setName(ad.getName());
                gwtParam.setDescription(ad.getDescription());
                gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
                gwtParam.setRequired(ad.isRequired());
                gwtParam.setCardinality(ad.getCardinality());
                if ((ad.getOption() != null) && !ad.getOption().isEmpty()) {
                    Map<String, String> options = new HashMap<String, String>();
                    for (Option option : ad.getOption()) {
                        options.put(option.getLabel(), option.getValue());
                    }
                    gwtParam.setOptions(options);
                }
                gwtParam.setMin(ad.getMin());
                gwtParam.setMax(ad.getMax());
                if (config.getConfigurationProperties() != null) {

                    // handle the value based on the cardinality of the
                    // attribute
                    int cardinality = ad.getCardinality();
                    Object value = config.getConfigurationProperties().get(ad.getId());
                    if (value != null) {
                        if ((cardinality == 0) || (cardinality == 1) || (cardinality == -1)) {
                            if (gwtParam.getType().equals(GwtConfigParameterType.PASSWORD)) {
                                gwtParam.setValue(PLACEHOLDER);
                            } else {
                                gwtParam.setValue(String.valueOf(value));
                            }
                        } else {
                            // this could be an array value
                            if (value instanceof Object[]) {
                                Object[] objValues = (Object[]) value;
                                List<String> strValues = new ArrayList<String>();
                                for (Object v : objValues) {
                                    if (v != null) {
                                        if (gwtParam.getType().equals(GwtConfigParameterType.PASSWORD)) {
                                            strValues.add(PLACEHOLDER);
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
            }
        }
        return gwtConfig;
    }

    @Override
    public void createFactoryComponent(GwtXSRFToken xsrfToken, String factoryPid, String pid) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        try {
            cs.createFactoryConfiguration(factoryPid, pid, null, true);
        } catch (KuraException e) {
            throw new GwtKuraException("A component with the same name already exists!");
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

    private void fillServicesToHideList(List<String> hidePidsList) throws GwtKuraException {
        Collection<ServiceReference<ConfigurableComponent>> configurableComponentReferences = ServiceLocator
                .getInstance().getServiceReferences(ConfigurableComponent.class, null);

        for (ServiceReference<ConfigurableComponent> configurableComponentReference : configurableComponentReferences) {
            Object propertyObject = configurableComponentReference.getProperty(KURA_SERVICE_PID);
            if ((configurableComponentReference.getProperty(KURA_UI_SERVICE_HIDE) != null)
                    && (propertyObject != null)) {
                String servicePid = (String) propertyObject;
                hidePidsList.add(servicePid);
            }
            ServiceLocator.getInstance().ungetService(configurableComponentReference);
        }

        Collection<ServiceReference<SelfConfiguringComponent>> selfConfiguringComponentReferences = ServiceLocator
                .getInstance().getServiceReferences(SelfConfiguringComponent.class, null);

        for (ServiceReference<SelfConfiguringComponent> selfConfiguringComponentReference : selfConfiguringComponentReferences) {
            Object propertyObject = selfConfiguringComponentReference.getProperty(KURA_SERVICE_PID);
            if ((selfConfiguringComponentReference.getProperty(KURA_UI_SERVICE_HIDE) != null)
                    && (propertyObject != null)) {
                String servicePid = (String) propertyObject;
                hidePidsList.add(servicePid);
            }
            ServiceLocator.getInstance().ungetService(selfConfiguringComponentReference);
        }
    }

    @Override
    public List<GwtConfigComponent> findComponentConfiguration(GwtXSRFToken xsrfToken, String componentPid)
            throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<GwtConfigComponent> gwtConfigs = new ArrayList<GwtConfigComponent>();
        try {
            ComponentConfiguration config = cs.getComponentConfiguration(componentPid);

            OCD ocd = config.getDefinition();
            if (ocd != null) {

                GwtConfigComponent gwtConfig = new GwtConfigComponent();
                gwtConfig.setComponentId(config.getPid());

                Map<String, Object> props = config.getConfigurationProperties();
                if ((props != null) && (props.get(SERVICE_FACTORY_PID) != null)) {
                    String pid = this.stripPidPrefix(config.getPid());
                    gwtConfig.setComponentName(pid);
                } else {
                    gwtConfig.setComponentName(ocd.getName());
                }

                gwtConfig.setComponentDescription(ocd.getDescription());
                if ((ocd.getIcon() != null) && !ocd.getIcon().isEmpty()) {
                    Icon icon = ocd.getIcon().get(0);
                    gwtConfig.setComponentIcon(icon.getResource());
                }

                List<GwtConfigParameter> gwtParams = new ArrayList<GwtConfigParameter>();
                gwtConfig.setParameters(gwtParams);

                if (config.getConfigurationProperties() != null) {

                    for (Map.Entry<String, Object> entry : config.getConfigurationProperties().entrySet()) {
                        GwtConfigParameter gwtParam = new GwtConfigParameter();
                        gwtParam.setId(entry.getKey());
                        Object value = entry.getValue();

                        // this could be an array value
                        if ((value != null) && (value instanceof Object[])) {
                            Object[] objValues = (Object[]) value;
                            List<String> strValues = new ArrayList<String>();
                            for (Object v : objValues) {
                                if (v != null) {
                                    if (gwtParam.getType().equals(GwtConfigParameterType.PASSWORD)) {
                                        strValues.add(PLACEHOLDER);
                                    } else {
                                        strValues.add(String.valueOf(v));
                                    }
                                }
                            }
                            gwtParam.setValues(strValues.toArray(new String[] {}));
                        } else if (value != null) {
                            gwtParam.setValue(String.valueOf(value));
                        }

                        gwtParams.add(gwtParam);
                    }
                }

                gwtConfigs.add(gwtConfig);
            }
        } catch (Exception t) {
            KuraExceptionHandler.handle(t);
        }
        return gwtConfigs;

    }

    @Override
    public List<GwtConfigComponent> findComponentConfigurations(GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<GwtConfigComponent> gwtConfigs = new ArrayList<GwtConfigComponent>();
        try {

            List<ComponentConfiguration> configs = cs.getComponentConfigurations();
            // sort the list alphabetically by service name
            this.sortConfigurations(configs);

            for (ComponentConfiguration config : configs) {

                OCD ocd = config.getDefinition();
                if (ocd != null) {

                    GwtConfigComponent gwtConfig = new GwtConfigComponent();
                    gwtConfig.setComponentId(config.getPid());

                    Map<String, Object> props = config.getConfigurationProperties();
                    if ((props != null) && (props.get(SERVICE_FACTORY_PID) != null)) {
                        String pid = this.stripPidPrefix(config.getPid());
                        gwtConfig.setComponentName(pid);
                    } else {
                        gwtConfig.setComponentName(ocd.getName());
                    }

                    gwtConfig.setComponentDescription(ocd.getDescription());
                    if ((ocd.getIcon() != null) && !ocd.getIcon().isEmpty()) {
                        Icon icon = ocd.getIcon().get(0);
                        gwtConfig.setComponentIcon(icon.getResource());
                    }

                    List<GwtConfigParameter> gwtParams = new ArrayList<GwtConfigParameter>();
                    gwtConfig.setParameters(gwtParams);

                    if (config.getConfigurationProperties() != null) {

                        for (Map.Entry<String, Object> entry : config.getConfigurationProperties().entrySet()) {
                            GwtConfigParameter gwtParam = new GwtConfigParameter();
                            gwtParam.setId(entry.getKey());
                            Object value = entry.getValue();

                            // this could be an array value
                            if ((value != null) && (value instanceof Object[])) {
                                Object[] objValues = (Object[]) value;
                                List<String> strValues = new ArrayList<String>();
                                for (Object v : objValues) {
                                    if (v != null) {
                                        if (gwtParam.getType().equals(GwtConfigParameterType.PASSWORD)) {
                                            strValues.add(PLACEHOLDER);
                                        } else {
                                            strValues.add(String.valueOf(v));
                                        }
                                    }
                                }
                                gwtParam.setValues(strValues.toArray(new String[] {}));
                            } else if (value != null) {
                                gwtParam.setValue(String.valueOf(value));
                            }

                            gwtParams.add(gwtParam);
                        }

                    }
                    gwtConfigs.add(gwtConfig);
                }
            }
        } catch (Exception t) {
            KuraExceptionHandler.handle(t);
        }
        return gwtConfigs;
    }

    @Override
    public List<String> findFactoryComponents(GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<String> result = new ArrayList<>();
        List<String> servicesToBeHidden = new ArrayList<>();
        // finding all wire components to remove from the list as these factory instances
        // are only shown in Kura Wires UI
        List<String> allWireComponents = findWireComponents();
        // finding services with kura.service.ui.hide property
        fillServicesToHideList(servicesToBeHidden);
        // get all the factory PIDs tracked by Configuration Service
        result.addAll(cs.getFactoryComponentPids());
        // remove all the wire components and the services to be hidden as these are shown in different UI
        result.removeAll(allWireComponents);
        result.removeAll(servicesToBeHidden);
        return result;
    }

    private List<String> findWireComponents() throws GwtKuraException {
        List<String> wireEmitterFpids = new ArrayList<>();
        List<String> wireReceiverFpids = new ArrayList<>();
        GwtServerUtil.fillFactoriesLists(wireEmitterFpids, wireReceiverFpids);
        final List<String> onlyProducers = new ArrayList<>(wireEmitterFpids);
        final List<String> onlyConsumers = new ArrayList<>(wireReceiverFpids);
        final List<String> both = new LinkedList<>();
        for (final String dto : wireEmitterFpids) {
            if (wireReceiverFpids.contains(dto)) {
                both.add(dto);
            }
        }
        onlyProducers.removeAll(both);
        onlyConsumers.removeAll(both);
        List<String> allWireComponents = new ArrayList<>(onlyProducers);
        allWireComponents.addAll(onlyConsumers);
        allWireComponents.addAll(both);
        return allWireComponents;
    }

    @Override
    public List<GwtConfigComponent> findFilteredComponentConfiguration(GwtXSRFToken xsrfToken, String componentPid)
            throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        try {
            ComponentConfiguration config = cs.getComponentConfiguration(componentPid);
            GwtConfigComponent component = this.convertComponentConfigurationByOcd(config);
            return Arrays.asList(component);
        } catch (Exception t) {
            KuraExceptionHandler.handle(t);
        }
        return null;
    }

    @Override
    public List<GwtConfigComponent> findFilteredComponentConfigurations(GwtXSRFToken xsrfToken)
            throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<GwtConfigComponent> gwtConfigs = new ArrayList<GwtConfigComponent>();
        try {

            List<ComponentConfiguration> configs = cs.getComponentConfigurations();
            // sort the list alphabetically by service name
            this.sortConfigurations(configs);

            for (ComponentConfiguration config : configs) {
                gwtConfigs.add(this.convertComponentConfigurationByOcd(config));
            }
        } catch (Exception t) {
            KuraExceptionHandler.handle(t);
        }
        return gwtConfigs;
    }

    @Override
    public List<GwtConfigComponent> findServicesConfigurations(GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        List<String> hidePidsList = new ArrayList<String>();

        // identify the services to hide by component configuration property
        this.fillServicesToHideList(hidePidsList);

        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<GwtConfigComponent> gwtConfigs = new ArrayList<GwtConfigComponent>();
        try {

            List<ComponentConfiguration> configs = cs.getComponentConfigurations();
            // sort the list alphabetically by service name
            this.sortConfigurations(configs);

            final Collection<String> servicesToHide = CollectionUtil.newHashSet(
                    Arrays.asList("SystemPropertiesService", "NetworkAdminService", "NetworkConfigurationService",
                            "SslManagerService", "FirewallConfigurationService", "WireService"));

            for (ComponentConfiguration config : configs) {

                final String pid = config.getPid();
                final boolean serviceExists = servicesToHide.stream().anyMatch(pid::endsWith);
                // ignore items we want to hide
                if (hidePidsList.contains(pid) || serviceExists) {
                    continue;
                }

                OCD ocd = config.getDefinition();
                if (ocd != null) {
                    GwtConfigComponent gwtConfig = this.convertComponentConfigurationByOcd(config);
                    gwtConfigs.add(gwtConfig);
                }
            }
        } catch (Exception t) {
            KuraExceptionHandler.handle(t);
        }
        return gwtConfigs;
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
                if ((conf != null) && (conf.getDefinition() == null)) {
                    String temporaryName = String.valueOf(System.nanoTime());
                    cs.createFactoryConfiguration(factoryPid, temporaryName, extraProps, false);
                    try {
                        // wait for the services to be up
                        TimeUnit.MILLISECONDS.sleep(500);
                        conf = cs.getComponentConfiguration(temporaryName);
                        comp = this.convertComponentConfigurationByOcd(conf);
                        return comp;
                    } catch (Exception ex) {
                        throw new GwtKuraException(ex.getMessage());
                    } finally {
                        cs.deleteFactoryConfiguration(temporaryName, false);
                    }
                }
            }
            comp = this.convertComponentConfigurationByOcd(conf);
        } catch (KuraException e) {
            throw new GwtKuraException("Could not retrieve component configuration!");
        }
        return comp;
    }

    private Object getObjectValue(GwtConfigParameter gwtConfigParam, String strValue) {
        Object objValue = null;
        GwtConfigParameterType gwtType = gwtConfigParam.getType();
        if (gwtType == GwtConfigParameterType.STRING) {
            objValue = strValue;
        } else if ((strValue != null) && !strValue.trim().isEmpty()) {
            switch (gwtType) {
            case LONG:
                objValue = Long.parseLong(strValue);
                break;
            case DOUBLE:
                objValue = Double.parseDouble(strValue);
                break;
            case FLOAT:
                objValue = Float.parseFloat(strValue);
                break;
            case INTEGER:
                objValue = Integer.parseInt(strValue);
                break;
            case SHORT:
                objValue = Short.parseShort(strValue);
                break;
            case BYTE:
                objValue = Byte.parseByte(strValue);
                break;
            case BOOLEAN:
                objValue = Boolean.parseBoolean(strValue);
                break;
            case PASSWORD:
                objValue = new Password(strValue);
                break;
            case CHAR:
                objValue = Character.valueOf(strValue.charAt(0));
                break;
            default:
                break;
            }
        }
        return objValue;
    }

    private Object[] getObjectValue(GwtConfigParameter gwtConfigParam, String[] defaultValues) {
        List<Object> values = new ArrayList<Object>();
        GwtConfigParameterType type = gwtConfigParam.getType();
        switch (type) {
        case BOOLEAN:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Boolean.valueOf(value));
                }
            }
            return values.toArray(new Boolean[] {});

        case BYTE:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Byte.valueOf(value));
                }
            }
            return values.toArray(new Byte[] {});

        case CHAR:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(new Character(value.charAt(0)));
                }
            }
            return values.toArray(new Character[] {});

        case DOUBLE:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Double.valueOf(value));
                }
            }
            return values.toArray(new Double[] {});

        case FLOAT:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Float.valueOf(value));
                }
            }
            return values.toArray(new Float[] {});

        case INTEGER:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Integer.valueOf(value));
                }
            }
            return values.toArray(new Integer[] {});

        case LONG:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Long.valueOf(value));
                }
            }
            return values.toArray(new Long[] {});

        case SHORT:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(Short.valueOf(value));
                }
            }
            return values.toArray(new Short[] {});

        case PASSWORD:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(new Password(value));
                }
            }
            return values.toArray(new Password[] {});

        case STRING:
            for (String value : defaultValues) {
                if (!value.trim().isEmpty()) {
                    values.add(value);
                }
            }
            return values.toArray(new String[] {});
        default:
            return null;
        }
    }

    private void sortConfigurations(List<ComponentConfiguration> configs) {
        Collections.sort(configs, new Comparator<ComponentConfiguration>() {

            @Override
            public int compare(ComponentConfiguration arg0, ComponentConfiguration arg1) {
                String name0;
                int start = arg0.getPid().lastIndexOf('.');
                int substringIndex = start + 1;
                if ((start != -1) && (substringIndex < arg0.getPid().length())) {
                    name0 = arg0.getPid().substring(substringIndex);
                } else {
                    name0 = arg0.getPid();
                }

                String name1;
                start = arg1.getPid().lastIndexOf('.');
                substringIndex = start + 1;
                if ((start != -1) && (substringIndex < arg1.getPid().length())) {
                    name1 = arg1.getPid().substring(substringIndex);
                } else {
                    name1 = arg1.getPid();
                }
                return name0.compareTo(name1);
            }
        });
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

    @Override
    public void updateComponentConfiguration(GwtXSRFToken xsrfToken, GwtConfigComponent gwtCompConfig)
            throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        try {

            // Build the new properties
            Map<String, Object> properties = new HashMap<String, Object>();
            ComponentConfiguration backupCC = cs.getComponentConfiguration(gwtCompConfig.getComponentId());
            Map<String, Object> backupConfigProp = backupCC.getConfigurationProperties();
            for (GwtConfigParameter gwtConfigParam : gwtCompConfig.getParameters()) {

                Object objValue = null;

                ComponentConfiguration currentCC = cs.getComponentConfiguration(gwtCompConfig.getComponentId());
                Map<String, Object> currentConfigProp = currentCC.getConfigurationProperties();
                Object currentObjValue = currentConfigProp.get(gwtConfigParam.getId());

                int cardinality = gwtConfigParam.getCardinality();
                if ((cardinality == 0) || (cardinality == 1) || (cardinality == -1)) {

                    String strValue = gwtConfigParam.getValue();

                    if ((currentObjValue instanceof Password) && PLACEHOLDER.equals(strValue)) {
                        objValue = currentConfigProp.get(gwtConfigParam.getId());
                    } else {
                        objValue = this.getObjectValue(gwtConfigParam, strValue);
                    }
                } else {

                    String[] strValues = gwtConfigParam.getValues();

                    if (currentObjValue instanceof Password[]) {
                        Password[] currentPasswordValue = (Password[]) currentObjValue;
                        for (int i = 0; i < strValues.length; i++) {
                            if (PLACEHOLDER.equals(strValues[i])) {
                                strValues[i] = new String(currentPasswordValue[i].getPassword());
                            }
                        }
                    }

                    objValue = this.getObjectValue(gwtConfigParam, strValues);
                }
                properties.put(gwtConfigParam.getId(), objValue);
            }

            // Force kura.service.pid into properties, if originally present
            if (backupConfigProp.get(KURA_SERVICE_PID) != null) {
                properties.put(KURA_SERVICE_PID, backupConfigProp.get(KURA_SERVICE_PID));
            }
            //
            // apply them
            cs.updateConfiguration(gwtCompConfig.getComponentId(), properties);
        } catch (Exception t) {
            KuraExceptionHandler.handle(t);
        }
    }
}