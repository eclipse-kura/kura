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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudConnectionEntryDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudEntryDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudPubSubEntryDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.ConfigComponentDTO;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.util.service.ServiceUtil.ServiceConsumer;
import org.eclipse.kura.util.service.ServiceUtil.ServiceReferenceConsumer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

@SuppressWarnings("deprecation")
public class CloudConnectionService {

    private static final String CLOUD_CONNECTION_FACTORY_FILTER = "(|(objectClass=org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory)(objectClass=org.eclipse.kura.cloud.factory.CloudServiceFactory))";

    private BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private ConfigurationService configurationService;

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

        return this.findComponentConfigurations(getPidFilter(result.iterator()));
    }

    public List<ConfigComponentDTO> findComponentConfigurations(String osgiFilter) throws KuraException {

        try {
            final Filter filter = FrameworkUtil.createFilter(osgiFilter);
            return this.configurationService.getComponentConfigurations(filter) //
                    .stream() //
                    .map(this::createMetatypeOnlyGwtComponentConfigurationInternal) //
                    .filter(Objects::nonNull) //
                    .collect(Collectors.toList());

        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, osgiFilter, e);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    private ConfigComponentDTO createMetatypeOnlyGwtComponentConfigurationInternal(ComponentConfiguration config) {
        ConfigComponentDTO configComponent = null;

        // OCD ocd = config.getDefinition();
        // if (ocd != null) {
        //
        // gwtConfig = new GwtConfigComponent();
        // gwtConfig.setComponentId(config.getPid());
        //
        // Map<String, Object> props = config.getConfigurationProperties();
        // if (props != null && props.get(DRIVER_PID) != null) {
        // gwtConfig.set(DRIVER_PID, props.get(DRIVER_PID));
        // }
        //
        // if (props != null && props.get(SERVICE_FACTORY_PID) != null) {
        // String pid = stripPidPrefix(config.getPid());
        // gwtConfig.setComponentName(pid);
        // gwtConfig.setFactoryComponent(true);
        // gwtConfig.setFactoryPid(String.valueOf(props.get(ConfigurationAdmin.SERVICE_FACTORYPID)));
        // } else {
        // gwtConfig.setComponentName(ocd.getName());
        // gwtConfig.setFactoryComponent(false);
        // }
        //
        // gwtConfig.setComponentDescription(ocd.getDescription());
        // if (ocd.getIcon() != null && !ocd.getIcon().isEmpty()) {
        // Icon icon = ocd.getIcon().get(0);
        // gwtConfig.setComponentIcon(icon.getResource());
        // }
        //
        // List<GwtConfigParameter> gwtParams = new ArrayList<>();
        // gwtConfig.setParameters(gwtParams);
        //
        // if (config.getConfigurationProperties() != null) {
        // List<GwtConfigParameter> metatypeProps = getADProperties(config);
        // gwtParams.addAll(metatypeProps);
        // }
        // }
        return configComponent;
    }

    @SuppressWarnings("deprecation")
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

    @SuppressWarnings("deprecation")
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

    public static String getPidFilter(final Iterator<String> pids) {
        if (!pids.hasNext()) {
            throw new IllegalArgumentException("pids list must be non empty");
        }
        final StringBuilder builder = new StringBuilder();
        builder.append("(|");
        while (pids.hasNext()) {
            final String pid = pids.next();
            builder.append("(kura.service.pid=");
            builder.append(pid);
            builder.append(")");
        }
        builder.append(")");
        return builder.toString();
    }

}
