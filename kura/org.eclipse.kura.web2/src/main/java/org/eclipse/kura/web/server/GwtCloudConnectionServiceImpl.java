/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.web.server;

import static java.lang.String.format;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.eclipse.kura.web.server.util.ServiceLocator.withAllServices;
import static org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry.GwtCloudConnectionState.CONNECTED;
import static org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry.GwtCloudConnectionState.DISCONNECTED;
import static org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry.GwtCloudConnectionState.UNREGISTERED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.factory.CloudServiceFactory;
import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.server.util.ServiceLocator.ServiceConsumer;
import org.eclipse.kura.web.server.util.ServiceLocator.ServiceReferenceConsumer;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtCloudComponentFactories;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry.GwtCloudConnectionType;
import org.eclipse.kura.web.shared.model.GwtCloudEntry;
import org.eclipse.kura.web.shared.model.GwtCloudPubSubEntry;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtCloudConnectionServiceImpl extends OsgiRemoteServiceServlet implements GwtCloudConnectionService {

    private static final long serialVersionUID = 693996483299382655L;

    private static final String CLOUD_CONNECTION_FACTORY_FILTER = "(|(objectClass=org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory)(objectClass=org.eclipse.kura.cloud.factory.CloudServiceFactory))";
    
    private static final String KURA_UI_CSF_PID_DEFAULT = "kura.ui.csf.pid.default";
    private static final String KURA_UI_CSF_PID_REGEX = "kura.ui.csf.pid.regex";

    private static final String CLOUD_PUBLISHER = CloudPublisher.class.getName();
    private static final String CLOUD_SUBSCRIBER = CloudSubscriber.class.getName();

    private static final Logger logger = LoggerFactory.getLogger(GwtCloudConnectionServiceImpl.class);

    @Override
    public List<GwtCloudEntry> findCloudEntries() throws GwtKuraException {

        final List<GwtCloudEntry> result = new ArrayList<>();

        withAllCloudConnectionFactories(service -> {

            final String factoryPid = service.getFactoryPid();
            if (factoryPid == null) {
                return;
            }

            for (final String pid : service.getManagedCloudConnectionPids()) {
                if (pid == null) {
                    continue;
                }

                final GwtCloudConnectionEntry cloudConnectionEntry = new GwtCloudConnectionEntry();
                cloudConnectionEntry.setCloudConnectionFactoryPid(factoryPid);
                cloudConnectionEntry.setPid(pid);

                fillState(cloudConnectionEntry);

                result.add(cloudConnectionEntry);
            }

        });

        result.addAll(getPublisherInstances());
        result.addAll(getSubscriberInstances());

        return result;
    }

    private static void fillState(final GwtCloudConnectionEntry cloudConnectionEntry) throws GwtKuraException {

        cloudConnectionEntry.setState(UNREGISTERED);

        final String filter = format("(%s=%s)", KURA_SERVICE_PID, cloudConnectionEntry.getPid());

        withAllServices(null, filter, service -> {
            if (service instanceof CloudConnectionManager) {
                cloudConnectionEntry
                        .setState(((CloudConnectionManager) service).isConnected() ? CONNECTED : DISCONNECTED);
                cloudConnectionEntry.setConnectionType(GwtCloudConnectionType.CONNECTION);
            } else if (service instanceof CloudEndpoint) {
                cloudConnectionEntry.setConnectionType(GwtCloudConnectionType.ENDPOINT);
            } else if (service instanceof CloudService) {
                cloudConnectionEntry.setState(((CloudService) service).isConnected() ? CONNECTED : DISCONNECTED);
                cloudConnectionEntry.setConnectionType(GwtCloudConnectionType.CONNECTION);
            }
        });
    }

    @Override
    public GwtCloudComponentFactories getCloudComponentFactories() throws GwtKuraException {

        final List<String> cloudConnectionFactoryPids = new ArrayList<>();

        withAllCloudConnectionFactories(service -> cloudConnectionFactoryPids.add(service.getFactoryPid()));

        final List<GwtCloudEntry> pubSubFactories = getPubSubFactories();

        final GwtCloudComponentFactories result = new GwtCloudComponentFactories();

        result.setCloudConnectionFactoryPids(cloudConnectionFactoryPids);
        result.setPubSubFactories(pubSubFactories);

        return result;
    }

    @Override
    public List<String> findStackPidsByFactory(final String factoryPid, final String cloudServicePid)
            throws GwtKuraException {

        final List<String> result = new ArrayList<>();

        withAllCloudConnectionFactories(factory -> {
            if (factoryPid.equals(factory.getFactoryPid())) {
                result.addAll(factory.getStackComponentsPids(cloudServicePid));
            }
        });

        return result;
    }

    @Override
    public void createCloudServiceFromFactory(GwtXSRFToken xsrfToken, String factoryPid, String cloudServicePid)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        if (factoryPid == null || factoryPid.trim().isEmpty() || cloudServicePid == null
                || cloudServicePid.trim().isEmpty()) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_NULL_ARGUMENT);
        }

        final AtomicReference<CloudConnectionFactory> ref = new AtomicReference<>();

        withAllCloudConnectionFactories(service -> {

            if (!service.getFactoryPid().equals(factoryPid) || ref.get() != null) {
                return;
            }

            ref.set(service);

        });

        try {
            ref.get().createConfiguration(cloudServicePid);
        } catch (KuraException e) {
            throw new GwtKuraException("Failed to create cloud stack");
        }

    }

    @Override
    public void deleteCloudServiceFromFactory(GwtXSRFToken xsrfToken, String factoryPid, String cloudServicePid)
            throws GwtKuraException {
        if (factoryPid == null || factoryPid.trim().isEmpty() || cloudServicePid == null
                || cloudServicePid.trim().isEmpty()) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_NULL_ARGUMENT);
        }

        final AtomicReference<CloudConnectionFactory> ref = new AtomicReference<>();

        withAllCloudConnectionFactories(service -> {

            if (!service.getFactoryPid().equals(factoryPid) || ref.get() != null) {
                return;
            }

            ref.set(service);

        });

        try {
            ref.get().deleteConfiguration(cloudServicePid);
        } catch (KuraException e) {
            throw new GwtKuraException("Failed to delete cloud stack");
        }

    }

    @Override
    public String findSuggestedCloudServicePid(String factoryPid) throws GwtKuraException {

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

    @Override
    public String findCloudServicePidRegex(String factoryPid) throws GwtKuraException {

        final AtomicReference<String> result = new AtomicReference<>();

        withAllCloudConnectionFactoryRefs((ref, ctx) -> {
            final CloudConnectionFactory cloudServiceFactory = wrap(ctx.getService(ref));
            try {
                if (!cloudServiceFactory.getFactoryPid().equals(factoryPid)) {
                    return;
                }
                Object propertyObject = ref.getProperty(KURA_UI_CSF_PID_REGEX);
                ServiceLocator.getInstance().ungetService(ref);
                if (propertyObject != null) {
                    result.set((String) propertyObject);
                }
            } finally {
                ctx.ungetService(ref);
            }
        });

        return result.get();
    }

    @Override
    public void createPubSubInstance(final GwtXSRFToken token, final String pid, final String factoryPid,
            final String cloudConnectionPid) throws GwtKuraException {
        checkXSRFToken(token);

        ServiceLocator.applyToServiceOptionally(ConfigurationService.class, cs -> {
            cs.createFactoryConfiguration(factoryPid, pid, Collections.singletonMap(
                    CloudConnectionConstants.CLOUD_CONNECTION_SERVICE_PID_PROP_NAME.value(), cloudConnectionPid), true);
            return (Void) null;
        });
    }

    @Override
    public void deletePubSubInstance(final GwtXSRFToken token, final String pid) throws GwtKuraException {
        checkXSRFToken(token);

        ServiceLocator.applyToServiceOptionally(ConfigurationService.class, cs -> {
            cs.deleteFactoryConfiguration(pid, true);
            return (Void) null;
        });
    }

    private static GwtCloudEntry toGwt(final ComponentDescriptionDTO component) {

        if (Arrays.stream(component.serviceInterfaces)
                .noneMatch(intf -> CLOUD_PUBLISHER.equals(intf) || CLOUD_SUBSCRIBER.equals(intf))) {
            return null;
        }

        final String ccsfFactoryPidPropName = CloudConnectionConstants.CLOUD_CONNECTION_FACTORY_PID_PROP_NAME.value();

        final Object ccsfFactoryPid = component.properties.get(ccsfFactoryPidPropName);
        final Object factoryPid = component.properties.get("service.pid");
        final Object defaultFactoryPid = component.properties.get(KURA_UI_CSF_PID_DEFAULT);
        final Object defaultFactoryPidRegex= component.properties.get(KURA_UI_CSF_PID_REGEX);

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

        final GwtCloudEntry entry = new GwtCloudEntry();

        entry.setPid((String) factoryPid);
        entry.setFactoryPid((String) ccsfFactoryPid);
        entry.setDefaultFactoryPid((String)defaultFactoryPid) ;
        entry.setDefaultFactoryPidRegex((String)defaultFactoryPidRegex) ;

        return entry;
    }

    private static List<GwtCloudEntry> getPubSubFactories() throws GwtKuraException {

        return ServiceLocator.applyToServiceOptionally(ServiceComponentRuntime.class, scr ->

        scr.getComponentDescriptionDTOs().stream().map(GwtCloudConnectionServiceImpl::toGwt).filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    private static GwtCloudPubSubEntry toGwt(final ServiceReference<?> ref, final GwtCloudPubSubEntry.Type type) {
        final Object ccsPid = ref.getProperty(CloudConnectionConstants.CLOUD_CONNECTION_SERVICE_PID_PROP_NAME.value());
        final Object factoryPid = ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);

        if (!(ccsPid instanceof String && factoryPid instanceof String)) {
            return null;
        }

        final String kuraServicePid = (String) ref.getProperty(ConfigurationService.KURA_SERVICE_PID);

        final GwtCloudPubSubEntry result = new GwtCloudPubSubEntry();

        result.setPid(kuraServicePid);
        result.setCloudConnectionPid((String) ccsPid);
        result.setFactoryPid((String) factoryPid);
        result.setType(type);

        return result;
    }

    private static Set<GwtCloudPubSubEntry> getPublisherInstances() throws GwtKuraException {
        final BundleContext context = FrameworkUtil.getBundle(GwtCloudConnectionServiceImpl.class).getBundleContext();

        final Set<GwtCloudPubSubEntry> result = new HashSet<>();

        try {
            context.getServiceReferences(CloudPublisher.class, null).stream()
                    .map(ref -> toGwt(ref, GwtCloudPubSubEntry.Type.PUBLISHER)).filter(Objects::nonNull)
                    .forEach(result::add);

            return result;
        } catch (InvalidSyntaxException e) {
            throw new GwtKuraException("Unexpected error");
        }
    }

    private static Set<GwtCloudPubSubEntry> getSubscriberInstances() throws GwtKuraException {
        final BundleContext context = FrameworkUtil.getBundle(GwtCloudConnectionServiceImpl.class).getBundleContext();

        final Set<GwtCloudPubSubEntry> result = new HashSet<>();

        try {
            context.getServiceReferences(CloudSubscriber.class, null).stream()
                    .map(ref -> toGwt(ref, GwtCloudPubSubEntry.Type.SUBSCRIBER)).filter(Objects::nonNull)
                    .forEach(result::add);

            return result;
        } catch (InvalidSyntaxException e) {
            throw new GwtKuraException("Unexpected error");
        }
    }

    private static void withAllCloudConnectionFactoryRefs(final ServiceReferenceConsumer<Object> consumer)
            throws GwtKuraException {
        ServiceLocator.withAllServiceReferences(CLOUD_CONNECTION_FACTORY_FILTER, consumer);
    }

    private static void withAllCloudConnectionFactories(final ServiceConsumer<CloudConnectionFactory> consumer)
            throws GwtKuraException {
        withAllServices(CLOUD_CONNECTION_FACTORY_FILTER, o -> consumer.consume(wrap(o)));
    }

    public static CloudConnectionFactory wrap(final Object o) {
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
}
