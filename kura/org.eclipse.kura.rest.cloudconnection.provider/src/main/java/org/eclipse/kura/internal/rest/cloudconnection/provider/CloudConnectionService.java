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
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.Cloud;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudComponentFactories;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudConnection;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudConnectionFactoryInfo;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudPubSub;
import org.eclipse.kura.internal.rest.cloudconnection.provider.util.PidUtils;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationDTO;
import org.eclipse.kura.rest.configuration.api.DTOUtil;
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

    private static final String KURA_UI_CSF_PID_DEFAULT = "kura.ui.csf.pid.default";
    private static final String KURA_UI_CSF_PID_REGEX = "kura.ui.csf.pid.regex";

    private static final String CLOUD_PUBLISHER = CloudPublisher.class.getName();
    private static final String CLOUD_SUBSCRIBER = CloudSubscriber.class.getName();

    private final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private final ConfigurationService configurationService;

    public CloudConnectionService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public List<Cloud> findCloudEntries() throws KuraException {

        final List<Cloud> result = new ArrayList<>();

        withAllCloudConnectionFactories(service -> {

            final String factoryPid = service.getFactoryPid();
            if (factoryPid == null) {
                return;
            }

            for (final String pid : service.getManagedCloudConnectionPids()) {
                if (pid == null) {
                    continue;
                }

                final CloudConnection cloudConnectionEntry = new CloudConnection(pid, factoryPid);

                fillState(cloudConnectionEntry);

                result.add(cloudConnectionEntry);
            }

        });

        result.addAll(getPublisherInstances());
        result.addAll(getSubscriberInstances());

        return result;
    }

    public List<ComponentConfiguration> getStackConfigurationsByFactory(final String factoryPid,
            final String cloudServicePid) throws KuraException {

        final List<String> result = new ArrayList<>();

        withAllCloudConnectionFactories(factory -> {
            if (factoryPid.equals(factory.getFactoryPid())) {
                result.addAll(factory.getStackComponentsPids(cloudServicePid));
            }
        });

        return findComponentConfigurations(PidUtils.getPidFilter(result.iterator()));
    }

    private List<ComponentConfiguration> findComponentConfigurations(String osgiFilter) throws KuraException {

        try {
            final Filter filter = FrameworkUtil.createFilter(osgiFilter);
            return this.configurationService.getComponentConfigurations(filter) //
                    .stream().filter(Objects::nonNull) //
                    .collect(Collectors.toList());

        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, osgiFilter, e);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
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

        final List<Cloud> pubSubFactories = getPubSubFactories();

        return new CloudComponentFactories(cloudConnectionFactoryPids, pubSubFactories);
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

    public ComponentConfiguration getPubSubConfiguration(String pid) throws KuraException {

        requireIsPubSub(pid);

        return findFilteredComponentConfiguration(pid);
    }

    public void updateStackComponentConfiguration(List<ComponentConfigurationDTO> componentConfigurations)
            throws KuraException {

        for (ComponentConfigurationDTO componentConfig : componentConfigurations) {
            if (!(isPubSub(componentConfig.getPid()) || isComponentManagedByFactory(componentConfig.getPid()))) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
            }

            updateComponentConfiguration(componentConfig);
        }

    }

    private void updateComponentConfiguration(ComponentConfigurationDTO componentConfig) throws KuraException {

        this.configurationService.updateConfiguration(componentConfig.getPid(),
                DTOUtil.dtosToConfigurationProperties(componentConfig.getProperties()), true);

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

    private ComponentConfiguration findFilteredComponentConfiguration(String componentPid) throws KuraException {

        try {
            return this.configurationService.getComponentConfiguration(componentPid);

        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
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

    private List<Cloud> getPubSubFactories() throws KuraException {

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

    private Cloud pubSubToCloudEntry(final ComponentDescriptionDTO component) {

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

        final Cloud entry = new Cloud();

        entry.setPid((String) factoryPid);
        entry.setFactoryPid((String) ccsfFactoryPid);
        entry.setDefaultFactoryPid((String) defaultFactoryPid);
        entry.setDefaultFactoryPidRegex((String) defaultFactoryPidRegex);

        return entry;
    }

    private void fillState(final CloudConnection cloudConnectionEntry) throws KuraException {

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

    private Set<CloudPubSub> getPublisherInstances() throws KuraException {

        final Set<CloudPubSub> result = new HashSet<>();

        try {
            this.bundleContext.getServiceReferences(CloudPublisher.class, null).stream()
                    .map(ref -> pubSubRefToDTO(ref, CloudPubSubType.PUBLISHER)).filter(Objects::nonNull)
                    .forEach(result::add);

            return result;
        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Unexpected error");
        }
    }

    private Set<CloudPubSub> getSubscriberInstances() throws KuraException {

        final Set<CloudPubSub> result = new HashSet<>();

        try {
            this.bundleContext.getServiceReferences(CloudSubscriber.class, null).stream()
                    .map(ref -> pubSubRefToDTO(ref, CloudPubSubType.SUBSCRIBER)).filter(Objects::nonNull)
                    .forEach(result::add);

            return result;
        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Unexpected error");
        }
    }

    private static CloudPubSub pubSubRefToDTO(final ServiceReference<?> ref, final CloudPubSubType type) {
        final Object ccsPid = ref.getProperty(CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value());
        final Object factoryPid = ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);

        if (!(ccsPid instanceof String && factoryPid instanceof String)) {
            return null;
        }

        final String kuraServicePid = (String) ref.getProperty(ConfigurationService.KURA_SERVICE_PID);

        return new CloudPubSub(kuraServicePid, (String) factoryPid, (String) ccsPid, type);

    }

}
