/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.OCDService;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.driver.descriptor.DriverDescriptor;
import org.eclipse.kura.driver.descriptor.DriverDescriptorService;
import org.eclipse.kura.internal.wire.asset.WireAssetChannelDescriptor;
import org.eclipse.kura.internal.wire.asset.WireAssetOCD;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.model.GwtWireComponentConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireComponentDescriptor;
import org.eclipse.kura.web.shared.model.GwtWireComposerStaticInfo;
import org.eclipse.kura.web.shared.model.GwtWireConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireGraphConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.wire.graph.MultiportWireConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireComponentDefinition;
import org.eclipse.kura.wire.graph.WireComponentDefinitionService;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class GwtWireServiceImpl implements {@link GwtWireService}
 */
public final class GwtWireServiceImpl extends OsgiRemoteServiceServlet implements GwtWireService {

    private static final GwtConfigComponent WIRE_ASSET_OCD = GwtServerUtil.toGwtConfigComponent(
            new ComponentConfigurationImpl("org.eclipse.kura.wire.WireAsset", new WireAssetOCD(), new HashMap<>()));

    private static final GwtConfigComponent WIRE_ASSET_CHANNEL_DESCRIPTOR = GwtServerUtil.toGwtConfigComponent(null,
            WireAssetChannelDescriptor.get().getDescriptor());

    private static final Logger logger = LoggerFactory.getLogger(GwtWireServiceImpl.class);
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

    private List<GwtConfigComponent> getAdditionalConfigurations(List<ComponentConfiguration> configurations,
            Set<String> wireComponentsInGraph, Set<String> driverPids) {
        final List<GwtConfigComponent> result = new ArrayList<>();

        for (ComponentConfiguration config : configurations) {
            final String pid = config.getPid();
            final Object factoryPid = config.getConfigurationProperties().get(ConfigurationAdmin.SERVICE_FACTORYPID);
            final boolean isDriver = driverPids.contains(pid);
            final boolean isAssetNotInGraph = factoryPid != null && "org.eclipse.kura.wire.WireAsset".equals(factoryPid)
                    && !wireComponentsInGraph.contains(pid);
            if (isDriver || isAssetNotInGraph) {
                final GwtConfigComponent gwtConfig = GwtServerUtil.toGwtConfigComponent(config);
                gwtConfig.setIsDriver(isDriver);
                result.add(gwtConfig);
            }
        }

        return result;
    }

    @Override
    public GwtWireGraphConfiguration getWiresConfiguration(final GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        final GwtWireGraphConfiguration result = new GwtWireGraphConfiguration();

        final WireGraphConfiguration wireGraphConfiguration = ServiceLocator
                .applyToServiceOptionally(WireGraphService.class, WireGraphService::get);

        final Set<String> wireComponentsInGraph = new HashSet<>();

        result.setWireComponentConfigurations(
                wireGraphConfiguration.getWireComponentConfigurations().stream().map(config -> {
                    final GwtWireComponentConfiguration gwtWireComponentConfig = new GwtWireComponentConfiguration();
                    final GwtConfigComponent gwtConfig = GwtServerUtil.toGwtConfigComponent(config.getConfiguration());
                    gwtConfig.setIsWireComponent(true);
                    gwtWireComponentConfig.setConfiguration(gwtConfig);
                    fillGwtRenderingProperties(gwtWireComponentConfig, config.getProperties());
                    wireComponentsInGraph.add(gwtConfig.getComponentId());
                    return gwtWireComponentConfig;
                }).collect(Collectors.toList()));

        result.setWires(wireGraphConfiguration.getWireConfigurations().stream().map(config -> {
            final GwtWireConfiguration gwtConfig = new GwtWireConfiguration();
            gwtConfig.setEmitterPid(config.getEmitterPid());
            gwtConfig.setEmitterPort(config.getEmitterPort());
            gwtConfig.setReceiverPid(config.getReceiverPid());
            gwtConfig.setReceiverPort(config.getReceiverPort());
            return gwtConfig;
        }).collect(Collectors.toList()));

        final List<ComponentConfiguration> componentConfigurations = ServiceLocator
                .applyToServiceOptionally(ConfigurationService.class, ConfigurationService::getComponentConfigurations);

        final Set<String> driverPids = ServiceLocator.applyToServiceOptionally(DriverDescriptorService.class,
                driverDescriptorService -> driverDescriptorService.listDriverDescriptors().stream()
                        .map(DriverDescriptor::getPid).collect(Collectors.toSet()));

        result.setAllActivePids(componentConfigurations.stream().map(ComponentConfiguration::getPid)
                .filter(Objects::nonNull).collect(Collectors.toList()));
        result.setAdditionalConfigurations(
                getAdditionalConfigurations(componentConfigurations, wireComponentsInGraph, driverPids));

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

        final Set<String> receivedConfigurationPids = gwtConfigurations.getWireComponentConfigurations().stream()
                .map(config -> config.getConfiguration().getComponentId()).collect(Collectors.toSet());

        additionalGwtConfigs.stream().map(GwtConfigComponent::getComponentId).forEach(receivedConfigurationPids::add);

        final Map<String, ComponentConfiguration> originalConfigs = new HashMap<>();

        ServiceLocator.applyToServiceOptionally(ConfigurationService.class, configurationService -> {
            configurationService.getComponentConfigurations().stream()
                    .filter(config -> receivedConfigurationPids.contains(config.getPid()))
                    .forEach(config -> originalConfigs.put(config.getPid(), config));
            return (Void) null;
        });

        final List<WireComponentConfiguration> wireComponentConfigurations = gwtConfigurations
                .getWireComponentConfigurations().stream().map(gwtConfig -> {

                    final GwtConfigComponent receivedConfig = gwtConfig.getConfiguration();
                    final ComponentConfiguration config = GwtServerUtil.fromGwtConfigComponent(receivedConfig,
                            originalConfigs.get(receivedConfig.getComponentId()));

                    final Map<String, Object> renderingProperties = getRenderingProperties(gwtConfig);

                    return new WireComponentConfiguration(config, renderingProperties);
                }).collect(Collectors.toList());

        final List<MultiportWireConfiguration> wireConfigurations = gwtConfigurations
                .getWires().stream().map(gwtWire -> new MultiportWireConfiguration(gwtWire.getEmitterPid(),
                        gwtWire.getReceiverPid(), gwtWire.getEmitterPort(), gwtWire.getReceiverPort()))
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
    }

    @Deprecated
    private GwtConfigComponent getWireAssetDefinition() { // TODO provide a metatype for WireAsset

        return WIRE_ASSET_OCD;
    }

    private void fillWireComponentDefinitions(List<GwtWireComponentDescriptor> resultDescriptors,
            List<GwtConfigComponent> resultDefinitions) throws GwtKuraException {

        ServiceLocator.applyToServiceOptionally(WireComponentDefinitionService.class,
                wireComponentDefinitionService -> {
                    for (WireComponentDefinition wireComponentDefinition : wireComponentDefinitionService
                            .getComponentDefinitions()) {
                        final GwtWireComponentDescriptor result = new GwtWireComponentDescriptor(
                                wireComponentDefinition.getFactoryPid(), wireComponentDefinition.getMinInputPorts(),
                                wireComponentDefinition.getMaxInputPorts(),
                                wireComponentDefinition.getDefaultInputPorts(),
                                wireComponentDefinition.getMinOutputPorts(),
                                wireComponentDefinition.getMaxOutputPorts(),
                                wireComponentDefinition.getDefaultOutputPorts(),
                                wireComponentDefinition.getInputPortNames(),
                                wireComponentDefinition.getOutputPortNames());

                        final GwtConfigComponent ocd = GwtServerUtil
                                .toGwtConfigComponent(wireComponentDefinition.getComponentOCD());
                        if (ocd != null) {
                            resultDefinitions.add(ocd);
                        }

                        resultDescriptors.add(result);
                    }
                    resultDefinitions.add(getWireAssetDefinition());
                    return (Void) null;
                });
    }

    private void fillDriverDefinitions(List<GwtConfigComponent> resultDefinitions) throws GwtKuraException {
        ServiceLocator.applyToServiceOptionally(OCDService.class, ocdService -> {

            for (ComponentConfiguration config : ocdService.getServiceProviderOCDs("org.eclipse.kura.driver.Driver")) {
                final GwtConfigComponent descriptor = GwtServerUtil.toGwtConfigComponent(config);
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

            driverDescriptorService.listDriverDescriptors().stream().map(GwtServerUtil::toGwtConfigComponent)
                    .filter(Objects::nonNull).forEach(resultDescriptors::add);
            return (Void) null;
        });
    }

    @Override
    public GwtWireComposerStaticInfo getWireComposerStaticInfo(GwtXSRFToken xsrfToken) throws GwtKuraException {
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
        result.setBaseChannelDescriptor(WIRE_ASSET_CHANNEL_DESCRIPTOR);

        return result;
    }
}