/*******************************************************************************
 * Copyright (c) 2016, 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *  Red Hat Inc
 *
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.OCDService;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.driver.descriptor.DriverDescriptor;
import org.eclipse.kura.driver.descriptor.DriverDescriptorService;
import org.eclipse.kura.internal.wire.asset.WireAssetChannelDescriptor;
import org.eclipse.kura.internal.wire.asset.WireAssetOCD;
import org.eclipse.kura.locale.LocaleContextHolder;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.KuraExceptionHandler;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.FilterUtil;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.IdHelper;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.model.GwtWireComponentConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireComponentDescriptor;
import org.eclipse.kura.web.shared.model.GwtWireComposerStaticInfo;
import org.eclipse.kura.web.shared.model.GwtWireConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireGraph;
import org.eclipse.kura.web.shared.model.GwtWireGraphConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtWireGraphService;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.graph.MultiportWireConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireComponentDefinition;
import org.eclipse.kura.wire.graph.WireComponentDefinitionService;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class GwtWireGraphServiceImpl implements {@link GwtWireGraphService}
 */
public final class GwtWireGraphServiceImpl extends OsgiRemoteServiceServlet implements GwtWireGraphService {

    private static final String DRIVER_PID = "driver.pid";
    private static final String SERVICE_FACTORY_PID = "service.factoryPid";
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private static final ComponentConfiguration WIRE_ASSET_OCD_CONFIG = new ComponentConfigurationImpl(
            "org.eclipse.kura.wire.WireAsset", new WireAssetOCD(), new HashMap<>());

    // private static final GwtConfigComponent WIRE_ASSET_CHANNEL_DESCRIPTOR = GwtServerUtil.toGwtConfigComponent(null,
    // WireAssetChannelDescriptor.get().getDescriptor(), "");

    private static final Filter DRIVER_FILTER = getFilterUnchecked("(objectClass=org.eclipse.kura.driver.Driver)");
    private static final Filter ADDITIONAL_CONFIGS_FILTER = getFilterUnchecked(
            "(|(objectClass=org.eclipse.kura.driver.Driver)(service.factoryPid=org.eclipse.kura.wire.WireAsset))");

    private static final long serialVersionUID = -6577843865830245755L;

    @Override
    public GwtConfigComponent getGwtChannelDescriptor(final GwtXSRFToken xsrfToken, final String driverPid)
            throws GwtKuraException {
        final DriverDescriptorService driverDescriptorService = ServiceLocator.getInstance()
                .getService(DriverDescriptorService.class);

        Optional<DriverDescriptor> driverDescriptorOptional = driverDescriptorService.getDriverDescriptor(driverPid);

        if (driverDescriptorOptional.isPresent()) {
            DriverDescriptor driverDescriptor = driverDescriptorOptional.get();
            return getGwtConfigComponent(driverDescriptor);
        } else {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }
    }

    private GwtConfigComponent getGwtConfigComponent(DriverDescriptor driverDescriptor) {
        @SuppressWarnings("unchecked")
        final List<AD> params = (List<AD>) driverDescriptor.getChannelDescriptor();
        final GwtConfigComponent gwtConfig = new GwtConfigComponent();
        gwtConfig.setComponentId(driverDescriptor.getPid());

        final List<GwtConfigParameter> gwtParams = new ArrayList<>();
        gwtConfig.setParameters(gwtParams);
        for (final AD ad : params) {
            final GwtConfigParameter gwtParam = new GwtConfigParameter();
            gwtParam.setId(ad.getId());
            gwtParam.setName(ad.getName());
            gwtParam.setDescription(ad.getDescription());
            gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
            gwtParam.setRequired(ad.isRequired());
            gwtParam.setCardinality(ad.getCardinality());
            if (ad.getOption() != null && !ad.getOption().isEmpty()) {
                final Map<String, String> options = new HashMap<>();
                for (final Option option : ad.getOption()) {
                    options.put(option.getLabel(), option.getValue());
                }
                gwtParam.setOptions(options);
            }
            gwtParam.setMin(ad.getMin());
            gwtParam.setMax(ad.getMax());
            gwtParam.setDefault(ad.getDefault());

            gwtParams.add(gwtParam);
        }
        return gwtConfig;
    }

    private void fillGwtRenderingProperties(GwtWireComponentConfiguration component,
            Map<String, Object> renderingProperties) {
        component.setInputPortCount((Integer) renderingProperties.get("inputPortCount"));
        component.setOutputPortCount((Integer) renderingProperties.get("outputPortCount"));
        component.setPositionX((Float) renderingProperties.get("position.x"));
        component.setPositionY((Float) renderingProperties.get("position.y"));
    }

    private List<GwtConfigComponent> getAdditionalConfigurations(Set<String> wireComponentsInGraph,
            Set<String> driverPids) throws GwtKuraException {

        final List<ComponentConfiguration> configurations = ServiceLocator.applyToServiceOptionally(
                ConfigurationService.class, cs -> cs.getComponentConfigurations(ADDITIONAL_CONFIGS_FILTER));

        final List<GwtConfigComponent> result = new ArrayList<>();

        for (ComponentConfiguration config : configurations) {
            final String pid = config.getPid();
            final Object factoryPid = config.getConfigurationProperties().get(ConfigurationAdmin.SERVICE_FACTORYPID);
            final boolean isDriver = driverPids.contains(pid);
            final boolean isAssetNotInGraph = factoryPid != null && "org.eclipse.kura.wire.WireAsset".equals(factoryPid)
                    && !wireComponentsInGraph.contains(pid);
            if (isDriver || isAssetNotInGraph) {
                final GwtConfigComponent gwtConfig = GwtServerUtil.toGwtConfigComponent(config,
                        LocaleContextHolder.getLocale().getLanguage());
                gwtConfig.setIsDriver(isDriver);
                result.add(gwtConfig);
            }
        }

        return result;
    }

    @Override
    public GwtWireGraphConfiguration getWiresConfiguration(final GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);

        return getWiresConfigurationInternal();
    }

    private GwtWireGraphConfiguration getWiresConfigurationInternal() throws GwtKuraException {
        final GwtWireGraphConfiguration result = new GwtWireGraphConfiguration();

        final WireGraphConfiguration wireGraphConfiguration = ServiceLocator
                .applyToServiceOptionally(WireGraphService.class, WireGraphService::get);

        final Set<String> wireComponentsInGraph = new HashSet<>();

        result.setWireComponentConfigurations(
                wireGraphConfiguration.getWireComponentConfigurations().stream().map(wireComponentConfig -> {
                    final ComponentConfiguration config = wireComponentConfig.getConfiguration();
                    if (config == null) {
                        return null;
                    }
                    final String pid = config.getPid();
                    final GwtWireComponentConfiguration gwtWireComponentConfig = new GwtWireComponentConfiguration();
                    GwtConfigComponent gwtConfig = GwtServerUtil.toGwtConfigComponent(config,
                            LocaleContextHolder.getLocale().getLanguage());
                    if (gwtConfig == null) {
                        gwtConfig = new GwtConfigComponent();
                        gwtConfig.setComponentId(pid);
                    }
                    gwtConfig.setIsWireComponent(true);
                    gwtWireComponentConfig.setConfiguration(gwtConfig);
                    fillGwtRenderingProperties(gwtWireComponentConfig, wireComponentConfig.getProperties());
                    wireComponentsInGraph.add(pid);
                    return gwtWireComponentConfig;
                }).filter(Objects::nonNull).collect(Collectors.toList()));

        result.setWires(wireGraphConfiguration.getWireConfigurations().stream().map(config -> {
            final GwtWireConfiguration gwtConfig = new GwtWireConfiguration();
            gwtConfig.setEmitterPid(config.getEmitterPid());
            gwtConfig.setEmitterPort(config.getEmitterPort());
            gwtConfig.setReceiverPid(config.getReceiverPid());
            gwtConfig.setReceiverPort(config.getReceiverPort());
            return gwtConfig;
        }).collect(Collectors.toList()));

        final List<String> allActivePids = new ArrayList<>();
        final Set<String> driverPids = new HashSet<>();

        for (final ServiceReference<?> ref : getAllServiceReferences()) {
            final Object kuraServicePid = ref.getProperty(ConfigurationService.KURA_SERVICE_PID);

            if (!(kuraServicePid instanceof String)) {
                continue;
            }

            allActivePids.add((String) kuraServicePid);

            if (DRIVER_FILTER.match(ref)) {
                driverPids.add((String) kuraServicePid);
            }
        }

        result.setAllActivePids(allActivePids);
        result.setAdditionalConfigurations(getAdditionalConfigurations(wireComponentsInGraph, driverPids));

        return result;
    }

    private Map<String, Object> getRenderingProperties(GwtWireComponentConfiguration component) {

        final Map<String, Object> result = new HashMap<>();

        result.put("inputPortCount", component.getInputPortCount());
        result.put("outputPortCount", component.getOutputPortCount());
        result.put("position.x", (float) component.getPositionX());
        result.put("position.y", (float) component.getPositionY());

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void updateWireConfiguration(final GwtXSRFToken xsrfToken, GwtWireGraphConfiguration gwtConfigurations,
            List<GwtConfigComponent> additionalGwtConfigs) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);

        final List<String> receivedConfigurationPids = Stream.concat(gwtConfigurations.getWireComponentConfigurations() //
                .stream()//
                .map(config -> config.getConfiguration().getComponentId()),
                additionalGwtConfigs //
                        .stream() //
                        .map(GwtConfigComponent::getComponentId))
                .collect(Collectors.toList());

        final Iterator<String> receivedConfigurationPidsIterator = receivedConfigurationPids.iterator();

        final Map<String, ComponentConfiguration> originalConfigs;

        if (receivedConfigurationPidsIterator.hasNext()) {
            final Filter receivedConfigurationPidsFilter = getFilter(
                    FilterUtil.getPidFilter(receivedConfigurationPidsIterator));

            originalConfigs = ServiceLocator.applyToServiceOptionally(ConfigurationService.class, cs -> cs //
                    .getComponentConfigurations(receivedConfigurationPidsFilter) //
                    .stream() //
                    .collect(Collectors.toMap(ComponentConfiguration::getPid, c -> c)));
        } else {
            originalConfigs = Collections.emptyMap();
        }

        final List<WireComponentConfiguration> wireComponentConfigurations = gwtConfigurations
                .getWireComponentConfigurations() //
                .stream() //
                .map(gwtConfig -> {

                    final GwtConfigComponent receivedConfig = gwtConfig.getConfiguration();
                    final ComponentConfiguration config = GwtServerUtil.fromGwtConfigComponent(receivedConfig,
                            originalConfigs.get(receivedConfig.getComponentId()));

                    final Map<String, Object> renderingProperties = getRenderingProperties(gwtConfig);

                    return new WireComponentConfiguration(config, renderingProperties);
                }) //
                .collect(Collectors.toList());

        final List<MultiportWireConfiguration> wireConfigurations = gwtConfigurations //
                .getWires() //
                .stream() //
                .map(gwtWire -> new MultiportWireConfiguration(gwtWire.getEmitterPid(), gwtWire.getReceiverPid(),
                        gwtWire.getEmitterPort(), gwtWire.getReceiverPort()))
                .collect(Collectors.toList());

        final List<ComponentConfiguration> additionalConfigs = additionalGwtConfigs.stream().map(gwtConfig -> {
            final ComponentConfiguration originalConfig = originalConfigs.get(gwtConfig.getComponentId());
            if (originalConfig == null) {
                return null;
            }
            return GwtServerUtil.fromGwtConfigComponent(gwtConfig, originalConfig);
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (!additionalConfigs.isEmpty()) {
            ServiceLocator.applyToServiceOptionally(ConfigurationService.class, configurationService -> {
                configurationService.updateConfigurations(additionalConfigs);
                return (Void) null;
            });
        }

        ServiceLocator.applyToServiceOptionally(WireGraphService.class, wireGraphService -> {
            wireGraphService.update(new WireGraphConfiguration(wireComponentConfigurations, wireConfigurations));
            return (Void) null;
        });

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);
        String updatedPids = receivedConfigurationPids.stream().collect(Collectors.joining(","));
        auditLogger.info(
                "UI Wires - Success - Successfully updated wires configuration for user: {}, session: {}, received configuration pids: {}",
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), updatedPids);
    }

    @Deprecated
    private GwtConfigComponent getWireAssetDefinition() { // TODO provide a metatype for WireAsset

        return GwtServerUtil.toGwtConfigComponent(WIRE_ASSET_OCD_CONFIG, LocaleContextHolder.getLocale().getLanguage());
    }

    private void fillWireComponentDefinitions(List<GwtWireComponentDescriptor> resultDescriptors,
            List<GwtConfigComponent> resultDefinitions) throws GwtKuraException {

        ServiceLocator.applyToServiceOptionally(WireComponentDefinitionService.class,
                wireComponentDefinitionService -> {
                    for (WireComponentDefinition wireComponentDefinition : wireComponentDefinitionService
                            .getComponentDefinitions()) {
                        ComponentConfigurationImpl impl = (ComponentConfigurationImpl) wireComponentDefinition
                                .getComponentOCD();
                        if (impl.getPid().equals(WIRE_ASSET_OCD_CONFIG.getPid())) {
                            impl.setDefinition((Tocd) WIRE_ASSET_OCD_CONFIG
                                    .getLocalizedDefinition(LocaleContextHolder.getLocale().getLanguage()));
                        } else {
                            Tocd ocd0 = (Tocd) impl
                                    .getLocalizedDefinition(LocaleContextHolder.getLocale().getLanguage());
                            impl.setDefinition(ocd0);
                        }

                        final GwtWireComponentDescriptor result = new GwtWireComponentDescriptor(
                                toComponentName(wireComponentDefinition), wireComponentDefinition.getFactoryPid(),
                                wireComponentDefinition.getMinInputPorts(), wireComponentDefinition.getMaxInputPorts(),
                                wireComponentDefinition.getDefaultInputPorts(),
                                wireComponentDefinition.getMinOutputPorts(),
                                wireComponentDefinition.getMaxOutputPorts(),
                                wireComponentDefinition.getDefaultOutputPorts(),
                                wireComponentDefinition.getInputPortNames(),
                                wireComponentDefinition.getOutputPortNames());

                        final GwtConfigComponent ocd = GwtServerUtil
                                .toGwtConfigComponent(wireComponentDefinition.getComponentOCD(), null);
                        if (ocd != null) {
                            resultDefinitions.add(ocd);
                        }
                        result.setToolsSorted(wireComponentDefinition.getToolsSorted());
                        resultDescriptors.add(result);
                    }
                    resultDefinitions.add(getWireAssetDefinition());
                    return (Void) null;
                });
    }

    private String toComponentName(WireComponentDefinition wireComponentDefinition) {
        if (wireComponentDefinition.getComponentOCD() == null) {
            return IdHelper.getLastIdComponent(wireComponentDefinition.getFactoryPid());
        }

        if (wireComponentDefinition.getComponentOCD().getDefinition() == null) {
            return IdHelper.getLastIdComponent(wireComponentDefinition.getFactoryPid());
        }

        if (wireComponentDefinition.getComponentOCD().getDefinition().getName() == null) {
            return IdHelper.getLastIdComponent(wireComponentDefinition.getFactoryPid());
        }

        return wireComponentDefinition.getComponentOCD().getDefinition().getName();
    }

    private void fillDriverDefinitions(List<GwtConfigComponent> resultDefinitions) throws GwtKuraException {
        ServiceLocator.applyToServiceOptionally(OCDService.class, ocdService -> {

            for (ComponentConfiguration config : ocdService.getServiceProviderOCDs("org.eclipse.kura.driver.Driver")) {
                final GwtConfigComponent descriptor = GwtServerUtil.toGwtConfigComponent(config,
                        LocaleContextHolder.getLocale().getLanguage());
                if (descriptor != null) {
                    descriptor.setIsDriver(true);
                    resultDefinitions.add(descriptor);
                }
            }
            return (Void) null;
        });
    }

    private void fillDriverDescriptors(List<GwtConfigComponent> resultDescriptors) throws GwtKuraException {

        ServiceLocator.applyToServiceOptionally(DriverDescriptorService.class, driverDescriptorService -> {

            driverDescriptorService.listDriverDescriptors().stream()
                    .map(descriptor -> GwtServerUtil.toGwtConfigComponent(descriptor,
                            LocaleContextHolder.getLocale().getLanguage()))
                    .filter(Objects::nonNull).forEach(resultDescriptors::add);
            return (Void) null;
        });
    }

    @Override
    public GwtWireComposerStaticInfo getWireComposerStaticInfo(GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);

        return getWireComposerStaticInfoInternal();
    }

    private GwtWireComposerStaticInfo getWireComposerStaticInfoInternal() throws GwtKuraException {
        final GwtWireComposerStaticInfo result = new GwtWireComposerStaticInfo();

        final List<GwtWireComponentDescriptor> componentDescriptors = new ArrayList<>();
        final List<GwtConfigComponent> componentDefinitions = new ArrayList<>();
        final List<GwtConfigComponent> driverDescriptors = new ArrayList<>();

        fillWireComponentDefinitions(componentDescriptors, componentDefinitions);
        fillDriverDefinitions(componentDefinitions);
        fillDriverDescriptors(driverDescriptors);

        result.setComponentDefinitions(componentDefinitions);
        result.setWireComponentDescriptors(componentDescriptors);
        result.setDriverDescriptors(driverDescriptors);
        GwtConfigComponent wireAssetChannelDescriptor = GwtServerUtil.toGwtConfigComponent(
                toComponentConfiguration("", WireAssetChannelDescriptor.get().getDescriptor()),
                LocaleContextHolder.getLocale().getLanguage());
        result.setBaseChannelDescriptor(wireAssetChannelDescriptor);

        return result;
    }

    private ComponentConfiguration toComponentConfiguration(String pid, Object descriptor) {
        if (!(descriptor instanceof List<?>)) {
            return null;
        }

        final List<?> ads = (List<?>) descriptor;

        final Tocd ocd = new Tocd();
        ocd.setId(pid);
        for (final Object ad : ads) {
            if (!(ad instanceof Tad)) {
                return null;
            }
            ocd.addAD((Tad) ad);
        }
        Tocd tocd = (Tocd) WIRE_ASSET_OCD_CONFIG.getDefinition();
        ocd.setLocalization(tocd.getLocalization());
        ocd.setLocaleUrls(tocd.getLocaleUrls());
        return new ComponentConfigurationImpl(pid, ocd, null);
    }

    private static Filter getFilterUnchecked(final String filter) {
        try {
            return FrameworkUtil.createFilter(filter);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Filter getFilter(final String filter) throws GwtKuraException {
        try {
            return FrameworkUtil.createFilter(filter);
        } catch (final Exception e) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }
    }

    private static ServiceReference<?>[] getAllServiceReferences() {
        final BundleContext context = FrameworkUtil.getBundle(GwtAssetServiceImpl.class).getBundleContext();
        try {
            return context.getAllServiceReferences(null, null);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public GwtWireGraph getWireGraph(final GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final GwtWireComposerStaticInfo staticInfo = getWireComposerStaticInfoInternal();
        final GwtWireGraphConfiguration wireGraphConfiguration = getWiresConfigurationInternal();

        List<GwtConfigComponent> gwtConfigs = new ArrayList<>();
        try {

            List<ComponentConfiguration> configs = ServiceLocator.applyToServiceOptionally(ConfigurationService.class,
                    ConfigurationService::getComponentConfigurations);
            for (ComponentConfiguration config : configs) {
                if (wireGraphConfiguration.getAllActivePids().stream().anyMatch(a -> a.equals(config.getPid())))
                    continue;
                if (config.getConfigurationProperties().get(ConfigurationService.KURA_SERVICE_PID) == null)
                    continue;
                final Object factoryPid = config.getConfigurationProperties()
                        .get(ConfigurationAdmin.SERVICE_FACTORYPID);
                ComponentConfiguration facroryconfig = null;
                if (factoryPid == null)
                    continue;

                facroryconfig = ServiceLocator.applyToServiceOptionally(ConfigurationService.class,
                        cs -> cs.getDefaultComponentConfiguration((String) factoryPid));
                if (facroryconfig == null || facroryconfig.getDefinition() == null)
                    auditLogger.info("configs:{}", config);
                else if (config.getDefinition() == null)
                    ((ComponentConfigurationImpl) config).setDefinition((Tocd) facroryconfig.getDefinition());
                if (config.getDefinition() != null) {
                    GwtConfigComponent gwtConfigComponent = createMetatypeOnlyGwtComponentConfiguration(config);

                    if (gwtConfigComponent != null) {
                        if (staticInfo.getComponentDefinitions().stream()
                                .anyMatch(def -> def.getComponentId().equals(factoryPid) && def.isDriver()))
                            gwtConfigComponent.setIsDriver(true);
                        gwtConfigs.add(gwtConfigComponent);
                    } else {
                        ServiceLocator.withAllServices(ConfigurationService.class, null,
                                cs -> cs.deleteFactoryConfiguration(config.getPid(), true));
                        auditLogger.warn("delete unsue conponent:{}", config);
                    }
                } else {
                    ServiceLocator.withAllServices(ConfigurationService.class, null,
                            cs -> cs.deleteFactoryConfiguration(config.getPid(), true));
                    auditLogger.warn("delete unsue conponent:{}", config);
                }
            }
            if (!gwtConfigs.isEmpty())
                wireGraphConfiguration.getAdditionalConfigurations().addAll(gwtConfigs);
        } catch (Exception e) {

            KuraExceptionHandler.handle(e);
        }

        return new GwtWireGraph(staticInfo, wireGraphConfiguration);
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

    private GwtConfigComponent createMetatypeOnlyGwtComponentConfigurationInternal(ComponentConfiguration config) {
        GwtConfigComponent gwtConfig = null;

        OCD ocd = config.getLocalizedDefinition(LocaleContextHolder.getLocale().getLanguage());
        if (ocd != null) {

            gwtConfig = new GwtConfigComponent();
            gwtConfig.setComponentId(config.getPid());

            Map<String, Object> props = config.getConfigurationProperties();
            if (props != null && props.get(DRIVER_PID) != null) {
                gwtConfig.set(DRIVER_PID, props.get(DRIVER_PID));
            }

            if (props != null && props.get(SERVICE_FACTORY_PID) != null) {
                String name = ocd.getName();
                if (props.containsKey("name"))
                    name = (String) props.get("name");
                if (name == null || name.equals(""))
                    name = stripPidPrefix(config.getPid());
                gwtConfig.setComponentName(name);
                gwtConfig.setFactoryComponent(true);
                gwtConfig.setFactoryPid(String.valueOf(props.get(ConfigurationAdmin.SERVICE_FACTORYPID)));
            } else {
                gwtConfig.setComponentName(ocd.getName());
                gwtConfig.setFactoryComponent(false);
            }
            String descCription = "";
            if (props.containsKey("componentDescription"))
                descCription = (String) props.get("componentDescription");
            if (descCription == null || descCription.equals(""))
                gwtConfig.setComponentDescription(ocd.getDescription());
            else
                gwtConfig.setComponentDescription(descCription);
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

    private List<GwtConfigParameter> getADProperties(ComponentConfiguration config) {
        List<GwtConfigParameter> gwtParams = new ArrayList<>();
        OCD ocd = config.getLocalizedDefinition(LocaleContextHolder.getLocale().getLanguage());
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

}
