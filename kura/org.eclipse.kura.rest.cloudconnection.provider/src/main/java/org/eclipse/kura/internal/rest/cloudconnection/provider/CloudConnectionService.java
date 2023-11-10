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
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudComponentFactories;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudConnectionFactoryInfo;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudConnectionState;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudEndpointInstance;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudEndpointType;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudPubSubType;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.PubSubFactoryInfo;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.PubSubInstance;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationDTO;
import org.eclipse.kura.rest.configuration.api.DTOUtil;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.util.service.ServiceUtil.ServiceConsumer;
import org.eclipse.kura.util.service.ServiceUtil.ServiceReferenceConsumer;
import org.osgi.framework.BundleContext;
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

    private static final String KURA_UI_CSF_PID_DEFAULT = "kura.ui.csf.pid.default";
    private static final String KURA_UI_CSF_PID_REGEX = "kura.ui.csf.pid.regex";

    private static final String CLOUD_PUBLISHER = CloudPublisher.class.getName();
    private static final String CLOUD_SUBSCRIBER = CloudSubscriber.class.getName();

    private final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private final ConfigurationService configurationService;

    public CloudConnectionService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public List<CloudEndpointInstance> findCloudEndpointInstances() throws KuraException {

        final List<CloudEndpointInstance> result = new ArrayList<>();

        withAllCloudConnectionFactories(service -> {

            final String factoryPid = service.getFactoryPid();
            if (factoryPid == null) {
                return;
            }

            for (final String pid : service.getManagedCloudConnectionPids()) {
                if (pid == null) {
                    continue;
                }

                final CloudEndpointInstance cloudConnectionEntry = new CloudEndpointInstance(pid, factoryPid);

                fillState(cloudConnectionEntry);

                result.add(cloudConnectionEntry);
            }

        });

        return result;
    }

    public List<PubSubInstance> findPubsubInstances() throws KuraException {
        final List<PubSubInstance> result = new ArrayList<>();

        result.addAll(getPubSubInstances(CloudPubSubType.PUBLISHER));
        result.addAll(getPubSubInstances(CloudPubSubType.SUBSCRIBER));

        return result;
    }

    public Set<String> getStackComponentsPids(final String factoryPid, final String cloudEndpointPid)
            throws KuraException {

        final Set<String> result = new HashSet<>();

        withAllCloudConnectionFactories(factory -> {
            if (factoryPid.equals(factory.getFactoryPid())) {
                result.addAll(getStackComponentPids(factory, cloudEndpointPid));
            }
        });

        return result;
    }

    private List<String> getStackComponentPids(CloudConnectionFactory factory, String pid) {

        List<String> result = new ArrayList<>();

        try {
            result = factory.getStackComponentsPids(pid);
        } catch (Exception e) {
            // nothing to do, just return an empty list
        }

        return result;
    }

    public Set<ComponentConfiguration> getStackConfigurationsByPid(final Set<String> pids) throws KuraException {

        List<String> result = new ArrayList<>();

        Set<ComponentConfiguration> resultSet = new HashSet<>();

        withAllCloudConnectionFactories(factory -> {

            Set<String> managedCloudConnectionPids = factory.getManagedCloudConnectionPids();

            for (String cloudConnectionPid : managedCloudConnectionPids) {
                List<String> stackComponentsPids = getStackComponentPids(factory, cloudConnectionPid);

                pids.stream().filter(stackComponentsPids::contains).forEach(result::add);
            }

        });

        for (String stackConfigurationPid : result) {
            ComponentConfiguration componetConfiguration = this.configurationService
                    .getComponentConfiguration(stackConfigurationPid);
            if (componetConfiguration != null) {
                resultSet.add(componetConfiguration);
            }
        }

        return resultSet;
    }

    public void createCloudEndpointFromFactory(String factoryPid, String cloudServicePid) throws KuraException {
        if (factoryPid == null || factoryPid.trim().isEmpty() || cloudServicePid == null
                || cloudServicePid.trim().isEmpty()) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        AtomicReference<Boolean> found = new AtomicReference<>(false);

        withAllCloudConnectionFactories(service -> {
            if (service.getFactoryPid().equals(factoryPid)) {
                found.set(true);
                service.createConfiguration(cloudServicePid);
            }
        });

        if (Boolean.FALSE.equals(found.get())) {
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }
    }

    public void deleteCloudEndpointFromFactory(String factoryPid, String cloudEndpointPid) throws KuraException {
        if (factoryPid == null || factoryPid.trim().isEmpty() || cloudEndpointPid == null
                || cloudEndpointPid.trim().isEmpty()) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        AtomicReference<Boolean> found = new AtomicReference<>(false);

        withAllCloudConnectionFactories(service -> {
            if (service.getFactoryPid().equals(factoryPid)) {
                found.set(true);
                service.deleteConfiguration(cloudEndpointPid);
            }
        });

        if (Boolean.FALSE.equals(found.get())) {
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }
    }

    public CloudComponentFactories getCloudComponentFactories() throws KuraException {
        final List<CloudConnectionFactoryInfo> cloudConnectionFactoryPids = new ArrayList<>();

        withAllCloudConnectionFactoryRefs((ref, ctx) -> {

            try {
                final CloudConnectionFactory service = wrap(ctx.getService(ref));
                String defaultPid = (String) ref.getProperty(KURA_UI_CSF_PID_DEFAULT);
                String pidRegex = (String) ref.getProperty(KURA_UI_CSF_PID_REGEX);

                CloudConnectionFactoryInfo cloudConnectionFactoryInfoDTO = new CloudConnectionFactoryInfo(
                        service.getFactoryPid(), defaultPid, pidRegex);

                cloudConnectionFactoryPids.add(cloudConnectionFactoryInfoDTO);
            } finally {
                ctx.ungetService(ref);
            }

        });

        final List<PubSubFactoryInfo> pubSubFactories = getPubSubFactories();

        return new CloudComponentFactories(cloudConnectionFactoryPids, pubSubFactories);
    }

    public void createPubSubInstance(final String pid, final String factoryPid, final String cloudEndpointPid)
            throws KuraException {

        if (pid == null || pid.trim().isEmpty() || //
                factoryPid == null || factoryPid.trim().isEmpty() //
                || cloudEndpointPid == null || cloudEndpointPid.trim().isEmpty()) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        requireIsPubSubFactory(factoryPid);

        ServiceUtil.applyToServiceOptionally(this.bundleContext, ConfigurationService.class, cs -> {
            cs.createFactoryConfiguration(factoryPid, pid, Collections.singletonMap(
                    CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value(), cloudEndpointPid), true);

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

    public List<ComponentConfiguration> getPubSubConfiguration(Set<String> pids) throws KuraException {

        List<ComponentConfiguration> result = new ArrayList<>();

        for (String pid : pids) {
            if (isPubSub(pid)) {
                ComponentConfiguration configuration = this.configurationService.getComponentConfiguration(pid);
                if (configuration != null) {
                    result.add(configuration);
                }
            }
        }

        return result;
    }

    public void updateStackComponentConfiguration(List<ComponentConfigurationDTO> componentConfigurations,
            boolean takeSnapshot) throws KuraException {

        for (ComponentConfigurationDTO componentConfig : componentConfigurations) {
            if (!(isPubSub(componentConfig.getPid()) || isComponentManagedByFactory(componentConfig.getPid()))) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST);
            }
        }

        updateComponentConfigurations(componentConfigurations, takeSnapshot);

    }

    private void updateComponentConfigurations(List<ComponentConfigurationDTO> componentConfigurations,
            boolean takeSnapshot) throws KuraException {

        List<ComponentConfiguration> configs = componentConfigurations.stream()
                .map(cc -> new ComponentConfigurationImpl(cc.getPid(), null,
                        DTOUtil.dtosToConfigurationProperties(cc.getProperties())))
                .collect(Collectors.toList());

        this.configurationService.updateConfigurations(configs, takeSnapshot);
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

    private void requireIsPubSub(final String pid) throws KuraException {
        if (!isPubSub(pid)) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
    }

    private boolean isPubSub(final String pid) {
        return ServiceUtil.providesService(this.bundleContext, pid, CloudPublisher.class)
                || ServiceUtil.providesService(this.bundleContext, pid, CloudSubscriber.class);
    }

    private List<PubSubFactoryInfo> getPubSubFactories() throws KuraException {

        return ServiceUtil.applyToServiceOptionally(this.bundleContext, ServiceComponentRuntime.class, scr ->

        scr.getComponentDescriptionDTOs().stream().map(this::pubSubFactoryToInfo).filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    private void requireIsPubSubFactory(final String factoryPid) throws KuraException {
        final boolean isPubSub = ServiceUtil.applyToServiceOptionally(this.bundleContext, ServiceComponentRuntime.class,
                scr -> scr.getComponentDescriptionDTOs().stream().anyMatch(c -> {
                    final Map<String, Object> properties = c.properties;

                    if (properties == null) {
                        return false;
                    }

                    return Objects.equals(factoryPid, properties.get("service.pid")) && pubSubFactoryToInfo(c) != null;
                }));

        if (!isPubSub) {
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }
    }

    private PubSubFactoryInfo pubSubFactoryToInfo(final ComponentDescriptionDTO component) {

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

        return new PubSubFactoryInfo((String) factoryPid, (String) ccsfFactoryPid, (String) defaultFactoryPid,
                (String) defaultFactoryPidRegex);
    }

    private void fillState(final CloudEndpointInstance cloudEndpointInstance) throws KuraException {

        cloudEndpointInstance.setState(CloudConnectionState.UNREGISTERED);

        final String filter = format("(%s=%s)", KURA_SERVICE_PID, cloudEndpointInstance.getCloudEndpointPid());

        ServiceUtil.withAllServices(this.bundleContext, null, filter, service -> {
            if (service instanceof CloudConnectionManager) {
                cloudEndpointInstance
                        .setState(((CloudConnectionManager) service).isConnected() ? CloudConnectionState.CONNECTED
                                : CloudConnectionState.DISCONNECTED);
                cloudEndpointInstance.setConnectionType(CloudEndpointType.CLOUD_CONNECTION_MANAGER);
            } else if (service instanceof CloudEndpoint) {
                cloudEndpointInstance.setConnectionType(CloudEndpointType.CLOUD_ENDPOINT);
            } else if (service instanceof CloudService) {
                cloudEndpointInstance.setState(((CloudService) service).isConnected() ? CloudConnectionState.CONNECTED
                        : CloudConnectionState.DISCONNECTED);
                cloudEndpointInstance.setConnectionType(CloudEndpointType.CLOUD_CONNECTION_MANAGER);
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

    private Set<PubSubInstance> getPubSubInstances(CloudPubSubType type) throws KuraException {

        final Set<PubSubInstance> result = new HashSet<>();

        try {
            this.bundleContext.getServiceReferences(CloudPublisher.class, null).stream()
                    .map(ref -> pubSubRefToDTO(ref, type)).filter(Objects::nonNull).forEach(result::add);

            return result;
        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Unexpected error");
        }
    }

    private static PubSubInstance pubSubRefToDTO(final ServiceReference<?> ref, final CloudPubSubType type) {
        final Object ccsPid = ref.getProperty(CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value());
        final Object factoryPid = ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);

        if (!(ccsPid instanceof String && factoryPid instanceof String)) {
            return null;
        }

        final String kuraServicePid = (String) ref.getProperty(ConfigurationService.KURA_SERVICE_PID);

        return new PubSubInstance((String) ccsPid, kuraServicePid, (String) factoryPid, type);

    }

}
