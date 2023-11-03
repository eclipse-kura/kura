/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.cloudconnection.provider;

import static java.lang.String.format;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.factory.CloudServiceFactory;
import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudComponentFactoriesDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudConnectionEntryDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudEntryDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudPubSubEntryDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.ConfigComponentDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.ConfigParameterDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.util.ComponentUtils;
import org.eclipse.kura.internal.rest.cloudconnection.provider.util.PidUtils;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.util.service.ServiceUtil.ServiceConsumer;
import org.eclipse.kura.util.service.ServiceUtil.ServiceReferenceConsumer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class CloudConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(CloudConnectionService.class);

    private static final String CLOUD_CONNECTION_FACTORY_FILTER = "(" + "|"
            + "(objectClass=org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory)"
            + "(objectClass=org.eclipse.kura.cloud.factory.CloudServiceFactory)" + ")";
    private static final String DRIVER_PID = "driver.pid";
    private static final String SERVICE_FACTORY_PID = "service.factoryPid";

    private static final String KURA_UI_CSF_PID_DEFAULT = "kura.ui.csf.pid.default";
    private static final String KURA_UI_CSF_PID_REGEX = "kura.ui.csf.pid.regex";

    private static final String CLOUD_PUBLISHER = CloudPublisher.class.getName();
    private static final String CLOUD_SUBSCRIBER = CloudSubscriber.class.getName();

    private final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private final ConfigurationService configurationService;

    public CloudConnectionService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public List<CloudEntryDTO> findCloudEntries() throws KuraException {

        final List<CloudEntryDTO> result = new ArrayList<>();

        withAllCloudConnectionFactories(service -> {

            final String factoryPid = service.getFactoryPid();
            if (factoryPid == null) {
                return;
            }

            for (final String pid : service.getManagedCloudConnectionPids()) {
                if (pid == null) {
                    continue;
                }

                final CloudConnectionEntryDTO cloudConnectionEntry = new CloudConnectionEntryDTO(pid, factoryPid);

                fillState(cloudConnectionEntry);

                result.add(cloudConnectionEntry);
            }

        });

        result.addAll(getPublisherInstances());
        result.addAll(getSubscriberInstances());

        return result;
    }

    public List<ConfigComponentDTO> getStackConfigurationsByFactory(final String factoryPid,
            final String cloudServicePid) throws KuraException {

        final List<String> result = new ArrayList<>();

        withAllCloudConnectionFactories(factory -> {
            if (factoryPid.equals(factory.getFactoryPid())) {
                result.addAll(factory.getStackComponentsPids(cloudServicePid));
            }
        });

        return findComponentConfigurations(PidUtils.getPidFilter(result.iterator()));
    }

    private List<ConfigComponentDTO> findComponentConfigurations(String osgiFilter) throws KuraException {

        try {
            final Filter filter = FrameworkUtil.createFilter(osgiFilter);
            return this.configurationService.getComponentConfigurations(filter) //
                    .stream() //
                    .map(this::createMetatypeOnlyComponentConfigurationInternal) //
                    .filter(Objects::nonNull) //
                    .collect(Collectors.toList());

        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, osgiFilter, e);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public String findSuggestedCloudServicePid(String factoryPid) throws KuraException {

        final AtomicReference<String> result = new AtomicReference<>();

        withAllCloudConnectionFactoryRefs((ref, ctx) -> {
            final CloudConnectionFactory cloudServiceFactory = wrap(ctx.getService(ref));
            try {
                if (!cloudServiceFactory.getFactoryPid().equals(factoryPid)) {
                    return;
                }
                Object propertyObject = ref.getProperty(KURA_UI_CSF_PID_DEFAULT);
                if (propertyObject != null) {
                    result.set((String) propertyObject);
                }
            } finally {
                ctx.ungetService(ref);
            }
        });

        return result.get();
    }

    public String findCloudServicePidRegex(String factoryPid) throws KuraException {

        final AtomicReference<String> result = new AtomicReference<>();

        withAllCloudConnectionFactoryRefs((ref, ctx) -> {
            final CloudConnectionFactory cloudServiceFactory = wrap(ctx.getService(ref));
            try {
                if (!cloudServiceFactory.getFactoryPid().equals(factoryPid)) {
                    return;
                }
                Object propertyObject = ref.getProperty(KURA_UI_CSF_PID_REGEX);
                ServiceUtil.ungetService(this.bundleContext, ref);
                if (propertyObject != null) {
                    result.set((String) propertyObject);
                }
            } finally {
                ctx.ungetService(ref);
            }
        });

        return result.get();
    }

    public void createCloudServiceFromFactory(String factoryPid, String cloudServicePid) throws KuraException {
        if (factoryPid == null || factoryPid.trim().isEmpty() || cloudServicePid == null
                || cloudServicePid.trim().isEmpty()) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        withAllCloudConnectionFactories(service -> {
            if (service.getFactoryPid().equals(factoryPid)) {
                service.createConfiguration(cloudServicePid);
            }
        });
    }

    public void deleteCloudServiceFromFactory(String factoryPid, String cloudServicePid) throws KuraException {
        if (factoryPid == null || factoryPid.trim().isEmpty() || cloudServicePid == null
                || cloudServicePid.trim().isEmpty()) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        withAllCloudConnectionFactories(service -> {
            if (service.getFactoryPid().equals(factoryPid)) {
                service.deleteConfiguration(cloudServicePid);
            }
        });
    }

    public CloudComponentFactoriesDTO getCloudComponentFactories() throws KuraException {
        final List<String> cloudConnectionFactoryPids = new ArrayList<>();

        withAllCloudConnectionFactories(service -> cloudConnectionFactoryPids.add(service.getFactoryPid()));

        final List<CloudEntryDTO> pubSubFactories = getPubSubFactories();

        return new CloudComponentFactoriesDTO(cloudConnectionFactoryPids, pubSubFactories);
    }

    public void createPubSubInstance(final String pid, final String factoryPid, final String cloudConnectionPid)
            throws KuraException {

        requireIsPubSubFactory(factoryPid);

        ServiceUtil.applyToServiceOptionally(this.bundleContext, ConfigurationService.class, cs -> {
            cs.createFactoryConfiguration(factoryPid, pid, Collections.singletonMap(
                    CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value(), cloudConnectionPid), true);

            return null;
        });
    }

    public void deletePubSubInstance(final String pid) throws KuraException {

        requireIsPubSub(pid);

        ServiceUtil.applyToServiceOptionally(this.bundleContext, ConfigurationService.class, cs -> {
            cs.deleteFactoryConfiguration(pid, true);

            return null;
        });
    }

    public ConfigComponentDTO getPubSubConfiguration(String pid) throws KuraException {

        requireIsPubSub(pid);

        return findFilteredComponentConfiguration(pid).get(0);
    }

    public void updateStackComponentConfiguration(ConfigComponentDTO component) throws KuraException {

        if (!(isPubSub(component.getComponentId()) || isComponentManagedByFactory(component.getComponentId()))) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
        }

        updateComponentConfiguration(component);
    }

    private void updateComponentConfiguration(ConfigComponentDTO componentConfig) throws KuraException {

        ComponentConfiguration componentConfiguration = ComponentUtils
                .getComponentConfiguration(this.configurationService, componentConfig);

        this.configurationService.updateConfiguration(componentConfig.getComponentId(),
                componentConfiguration.getConfigurationProperties());

    }

    private boolean isComponentManagedByFactory(final String pid) {
        final AtomicBoolean result = new AtomicBoolean(false);

        try {
            withAllCloudConnectionFactories(f -> {
                for (final String stackPid : f.getManagedCloudConnectionPids()) {
                    if (f.getStackComponentsPids(stackPid).contains(pid)) {
                        result.set(true);
                        return;
                    }
                }
            });
        } catch (final Exception e) {
            return false;
        }

        return result.get();
    }

    private List<ConfigComponentDTO> findFilteredComponentConfiguration(String componentPid) throws KuraException {
        return findFilteredComponentConfigurationInternal(componentPid);
    }

    private List<ConfigComponentDTO> findFilteredComponentConfigurationInternal(String componentPid)
            throws KuraException {

        List<ConfigComponentDTO> configs = new ArrayList<>();
        try {
            ComponentConfiguration config = this.configurationService.getComponentConfiguration(componentPid);

            if (config != null) {
                ConfigComponentDTO configComponent = createMetatypeOnlyComponentConfigurationInternal(config);
                configs.add(configComponent);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }

        return configs;
    }

    private void requireIsPubSub(final String pid) throws KuraException {
        if (!isPubSub(pid)) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
    }

    private boolean isPubSub(final String pid) {
        return ServiceUtil.providesService(this.bundleContext, pid, CloudPublisher.class)
                || ServiceUtil.providesService(this.bundleContext, pid, CloudSubscriber.class);
    }

    private List<CloudEntryDTO> getPubSubFactories() throws KuraException {

        return ServiceUtil.applyToServiceOptionally(this.bundleContext, ServiceComponentRuntime.class, scr ->

        scr.getComponentDescriptionDTOs().stream().map(this::pubSubToCloudEntry).filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    private void requireIsPubSubFactory(final String factoryPid) throws KuraException {
        final boolean isPubSub = ServiceUtil.applyToServiceOptionally(this.bundleContext, ServiceComponentRuntime.class,
                scr -> scr.getComponentDescriptionDTOs().stream().anyMatch(c -> {
                    final Map<String, Object> properties = c.properties;

                    if (properties == null) {
                        return false;
                    }

                    return Objects.equals(factoryPid, properties.get("service.pid")) && pubSubToCloudEntry(c) != null;
                }));

        if (!isPubSub) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
    }

    private CloudEntryDTO pubSubToCloudEntry(final ComponentDescriptionDTO component) {

        if (Arrays.stream(component.serviceInterfaces)
                .noneMatch(intf -> CLOUD_PUBLISHER.equals(intf) || CLOUD_SUBSCRIBER.equals(intf))) {
            return null;
        }

        final String ccsfFactoryPidPropName = CloudConnectionConstants.CLOUD_CONNECTION_FACTORY_PID_PROP_NAME.value();

        final Object ccsfFactoryPid = component.properties.get(ccsfFactoryPidPropName);
        final Object factoryPid = component.properties.get("service.pid");
        final Object defaultFactoryPid = component.properties.get(KURA_UI_CSF_PID_DEFAULT);
        final Object defaultFactoryPidRegex = component.properties.get(KURA_UI_CSF_PID_REGEX);

        if (!(factoryPid instanceof String)) {
            logger.warn(
                    "component {} defines a CloudPublisher or CloudSubscriber but does not specify the service.pid property, ignoring it",
                    component.name);
            return null;
        }

        if (!(ccsfFactoryPid instanceof String)) {
            logger.warn(
                    "component {} defines a CloudPublisher or CloudSubscriber but does not specify the {} property, ignoring it",
                    component.name, ccsfFactoryPidPropName);
            return null;
        }

        final CloudEntryDTO entry = new CloudEntryDTO();

        entry.setPid((String) factoryPid);
        entry.setFactoryPid((String) ccsfFactoryPid);
        entry.setDefaultFactoryPid((String) defaultFactoryPid);
        entry.setDefaultFactoryPidRegex((String) defaultFactoryPidRegex);

        return entry;
    }

    private ConfigComponentDTO createMetatypeOnlyComponentConfigurationInternal(ComponentConfiguration config) {
        ConfigComponentDTO configComponent = null;

        OCD ocd = config.getDefinition();
        if (ocd != null) {

            configComponent = new ConfigComponentDTO();
            configComponent.setComponentId(config.getPid());

            Map<String, Object> props = config.getConfigurationProperties();
            if (props != null && props.get(DRIVER_PID) != null) {
                configComponent.setDriverPid((String) props.get(DRIVER_PID));
            }

            if (props != null && props.get(SERVICE_FACTORY_PID) != null) {
                String pid = PidUtils.stripPidPrefix(config.getPid());
                configComponent.setComponentName(pid);
                configComponent.setFactoryComponent(true);
                configComponent.setFactoryPid(String.valueOf(props.get(ConfigurationAdmin.SERVICE_FACTORYPID)));
            } else {
                configComponent.setComponentName(ocd.getName());
                configComponent.setFactoryComponent(false);
            }

            configComponent.setComponentDescription(ocd.getDescription());
            if (ocd.getIcon() != null && !ocd.getIcon().isEmpty()) {
                Icon icon = ocd.getIcon().get(0);
                configComponent.setComponentIcon(icon.getResource());
            }

            List<ConfigParameterDTO> params = new ArrayList<>();
            configComponent.setParameters(params);

            if (config.getConfigurationProperties() != null) {
                List<ConfigParameterDTO> metatypeProps = getADProperties(config);
                params.addAll(metatypeProps);
            }
        }
        return configComponent;
    }

    private void fillState(final CloudConnectionEntryDTO cloudConnectionEntry) throws KuraException {

        cloudConnectionEntry.setState(CloudConnectionState.UNREGISTERED);

        final String filter = format("(%s=%s)", KURA_SERVICE_PID, cloudConnectionEntry.getPid());

        ServiceUtil.withAllServices(this.bundleContext, null, filter, service -> {
            if (service instanceof CloudConnectionManager) {
                cloudConnectionEntry
                        .setState(((CloudConnectionManager) service).isConnected() ? CloudConnectionState.CONNECTED
                                : CloudConnectionState.DISCONNECTED);
                cloudConnectionEntry.setConnectionType(CloudConnectionType.CONNECTION);
            } else if (service instanceof CloudEndpoint) {
                cloudConnectionEntry.setConnectionType(CloudConnectionType.ENDPOINT);
            } else if (service instanceof CloudService) {
                cloudConnectionEntry.setState(((CloudService) service).isConnected() ? CloudConnectionState.CONNECTED
                        : CloudConnectionState.DISCONNECTED);
                cloudConnectionEntry.setConnectionType(CloudConnectionType.CONNECTION);
            }
        });
    }

    private void withAllCloudConnectionFactoryRefs(final ServiceReferenceConsumer<Object> consumer)
            throws KuraException {
        ServiceUtil.withAllServiceReferences(this.bundleContext, CLOUD_CONNECTION_FACTORY_FILTER, consumer);
    }

    private void withAllCloudConnectionFactories(final ServiceConsumer<CloudConnectionFactory> consumer)
            throws KuraException {
        ServiceUtil.withAllServices(this.bundleContext, CLOUD_CONNECTION_FACTORY_FILTER,
                o -> consumer.consume(wrap(o)));
    }

    private CloudConnectionFactory wrap(final Object o) {
        if (o instanceof CloudConnectionFactory) {
            return (CloudConnectionFactory) o;
        } else if (o instanceof CloudServiceFactory) {
            final CloudServiceFactory f = (CloudServiceFactory) o;

            return new CloudConnectionFactory() {

                @Override
                public List<String> getStackComponentsPids(String pid) throws KuraException {
                    return f.getStackComponentsPids(pid);
                }

                @Override
                public Set<String> getManagedCloudConnectionPids() throws KuraException {
                    return f.getManagedCloudServicePids();
                }

                @Override
                public String getFactoryPid() {
                    return f.getFactoryPid();
                }

                @Override
                public void deleteConfiguration(String pid) throws KuraException {
                    f.deleteConfiguration(pid);
                }

                @Override
                public void createConfiguration(String pid) throws KuraException {
                    f.createConfiguration(pid);
                }
            };
        }
        return null;
    }

    private Set<CloudPubSubEntryDTO> getPublisherInstances() throws KuraException {

        final Set<CloudPubSubEntryDTO> result = new HashSet<>();

        try {
            this.bundleContext.getServiceReferences(CloudPublisher.class, null).stream()
                    .map(ref -> pubSubRefToDTO(ref, CloudPubSubType.PUBLISHER)).filter(Objects::nonNull)
                    .forEach(result::add);

            return result;
        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Unexpected error");
        }
    }

    private Set<CloudPubSubEntryDTO> getSubscriberInstances() throws KuraException {

        final Set<CloudPubSubEntryDTO> result = new HashSet<>();

        try {
            this.bundleContext.getServiceReferences(CloudSubscriber.class, null).stream()
                    .map(ref -> pubSubRefToDTO(ref, CloudPubSubType.SUBSCRIBER)).filter(Objects::nonNull)
                    .forEach(result::add);

            return result;
        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Unexpected error");
        }
    }

    private static CloudPubSubEntryDTO pubSubRefToDTO(final ServiceReference<?> ref, final CloudPubSubType type) {
        final Object ccsPid = ref.getProperty(CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value());
        final Object factoryPid = ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);

        if (!(ccsPid instanceof String && factoryPid instanceof String)) {
            return null;
        }

        final String kuraServicePid = (String) ref.getProperty(ConfigurationService.KURA_SERVICE_PID);

        return new CloudPubSubEntryDTO(kuraServicePid, (String) factoryPid, (String) ccsPid, type);

    }

    private static List<ConfigParameterDTO> getADProperties(ComponentConfiguration config) {
        List<ConfigParameterDTO> configParams = new ArrayList<>();

        OCD ocd = config.getDefinition();
        for (AD ad : ocd.getAD()) {

            ConfigParameterDTO configParam = new ConfigParameterDTO();
            configParam.setId(ad.getId());
            configParam.setName(ad.getName());
            configParam.setDescription(ad.getDescription());
            configParam.setType(ConfigParameterType.valueOf(ad.getType().name()));
            configParam.setRequired(ad.isRequired());
            configParam.setCardinality(ad.getCardinality());

            if (ad.getOption() != null && !ad.getOption().isEmpty()) {
                Map<String, String> options = new HashMap<>();
                for (Option option : ad.getOption()) {
                    options.put(option.getLabel(), option.getValue());
                }
                configParam.setOptions(options);
            }
            configParam.setMin(ad.getMin());
            configParam.setMax(ad.getMax());

            // handle the value based on the cardinality of the attribute
            int cardinality = ad.getCardinality();
            Object value = config.getConfigurationProperties().get(ad.getId());
            if (value != null) {
                if (cardinality == 0 || cardinality == 1 || cardinality == -1) {
                    if (configParam.getType().equals(ConfigParameterType.PASSWORD)) {
                        configParam.setValue("Placeholder");
                    } else {
                        configParam.setValue(String.valueOf(value));
                    }
                } else {
                    // this could be an array value
                    if (value instanceof Object[]) {
                        Object[] objValues = (Object[]) value;
                        List<String> strValues = new ArrayList<>();
                        for (Object v : objValues) {
                            if (v != null) {
                                if (configParam.getType().equals(ConfigParameterType.PASSWORD)) {
                                    strValues.add("Placeholder");
                                } else {
                                    strValues.add(String.valueOf(v));
                                }
                            }
                        }
                        configParam.setValues(strValues.toArray(new String[] {}));
                    }
                }
            }
            configParams.add(configParam);
        }
        return configParams;
    }

}
