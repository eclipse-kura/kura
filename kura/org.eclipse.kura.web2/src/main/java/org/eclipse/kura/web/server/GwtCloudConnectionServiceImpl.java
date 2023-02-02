/*******************************************************************************
 * Copyright (c) 2016, 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.kura.KuraConnectException;
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
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.web.server.util.GwtComponentServiceInternal;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.server.util.ServiceLocator.ServiceConsumer;
import org.eclipse.kura.web.server.util.ServiceLocator.ServiceReferenceConsumer;
import org.eclipse.kura.web.shared.FilterUtil;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtCloudComponentFactories;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry.GwtCloudConnectionType;
import org.eclipse.kura.web.shared.model.GwtCloudEntry;
import org.eclipse.kura.web.shared.model.GwtCloudPubSubEntry;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
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

    private static final String DATA_SERVICE_REFERENCE_NAME = "DataService";

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
    public List<GwtConfigComponent> getStackConfigurationsByFactory(final String factoryPid,
            final String cloudServicePid) throws GwtKuraException {

        final List<String> result = new ArrayList<>();

        withAllCloudConnectionFactories(factory -> {
            if (factoryPid.equals(factory.getFactoryPid())) {
                result.addAll(factory.getStackComponentsPids(cloudServicePid));
            }
        });

        return GwtComponentServiceInternal.findComponentConfigurations(FilterUtil.getPidFilter(result.iterator()));
    }

    @Override
    public void createCloudServiceFromFactory(GwtXSRFToken xsrfToken, String factoryPid, String cloudServicePid)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        if (factoryPid == null || factoryPid.trim().isEmpty() || cloudServicePid == null
                || cloudServicePid.trim().isEmpty()) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_NULL_ARGUMENT);
        }

        withAllCloudConnectionFactories(service -> {
            if (service.getFactoryPid().equals(factoryPid)) {
                service.createConfiguration(cloudServicePid);
            }
        });
    }

    @Override
    public void deleteCloudServiceFromFactory(GwtXSRFToken xsrfToken, String factoryPid, String cloudServicePid)
            throws GwtKuraException {
        if (factoryPid == null || factoryPid.trim().isEmpty() || cloudServicePid == null
                || cloudServicePid.trim().isEmpty()) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_NULL_ARGUMENT);
        }

        withAllCloudConnectionFactories(service -> {
            if (service.getFactoryPid().equals(factoryPid)) {
                service.deleteConfiguration(cloudServicePid);
            }
        });
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

        requireIsPubSubFactory(factoryPid);

        ServiceLocator.applyToServiceOptionally(ConfigurationService.class, cs -> {
            cs.createFactoryConfiguration(factoryPid, pid, Collections.singletonMap(
                    CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value(), cloudConnectionPid), true);

            return null;
        });
    }

    @Override
    public void deletePubSubInstance(final GwtXSRFToken token, final String pid) throws GwtKuraException {
        checkXSRFToken(token);

        requireIsPubSub(pid);

        ServiceLocator.applyToServiceOptionally(ConfigurationService.class, cs -> {
            cs.deleteFactoryConfiguration(pid, true);

            return null;
        });
    }

    private static void requireIsPubSubFactory(final String factoryPid) throws GwtKuraException {
        final boolean isPubSub = ServiceLocator.applyToServiceOptionally(ServiceComponentRuntime.class,
                scr -> scr.getComponentDescriptionDTOs().stream().anyMatch(c -> {
                    final Map<String, Object> properties = c.properties;

                    if (properties == null) {
                        return false;
                    }

                    return Objects.equals(factoryPid, properties.get("service.pid")) && pubSubToGwt(c) != null;
                }));

        if (!isPubSub) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }
    }

    private static boolean isPubSub(final String pid) {
        return GwtServerUtil.providesService(pid, CloudPublisher.class)
                || GwtServerUtil.providesService(pid, CloudSubscriber.class);
    }

    private static boolean isComponentManagedByFactory(final String pid) {
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

    private static void requireIsPubSub(final String pid) throws GwtKuraException {
        if (!isPubSub(pid)) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
        }
    }

    private static GwtCloudEntry pubSubToGwt(final ComponentDescriptionDTO component) {

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

        final GwtCloudEntry entry = new GwtCloudEntry();

        entry.setPid((String) factoryPid);
        entry.setFactoryPid((String) ccsfFactoryPid);
        entry.setDefaultFactoryPid((String) defaultFactoryPid);
        entry.setDefaultFactoryPidRegex((String) defaultFactoryPidRegex);

        return entry;
    }

    private static List<GwtCloudEntry> getPubSubFactories() throws GwtKuraException {

        return ServiceLocator.applyToServiceOptionally(ServiceComponentRuntime.class, scr ->

        scr.getComponentDescriptionDTOs().stream().map(GwtCloudConnectionServiceImpl::pubSubToGwt)
                .filter(Objects::nonNull).collect(Collectors.toList()));
    }

    private static GwtCloudPubSubEntry pubSubRefToGwt(final ServiceReference<?> ref,
            final GwtCloudPubSubEntry.Type type) {
        final Object ccsPid = ref.getProperty(CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value());
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
                    .map(ref -> pubSubRefToGwt(ref, GwtCloudPubSubEntry.Type.PUBLISHER)).filter(Objects::nonNull)
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
                    .map(ref -> pubSubRefToGwt(ref, GwtCloudPubSubEntry.Type.SUBSCRIBER)).filter(Objects::nonNull)
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

    @Override
    public GwtConfigComponent getPubSubConfiguration(GwtXSRFToken xsrfToken, String pid) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        requireIsPubSub(pid);

        return GwtComponentServiceInternal.findFilteredComponentConfiguration(pid).get(0);
    }

    @Override
    public void updateStackComponentConfiguration(GwtXSRFToken xsrfToken, GwtConfigComponent component)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        if (!(isPubSub(component.getComponentId()) || isComponentManagedByFactory(component.getComponentId()))) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

        GwtComponentServiceInternal.updateComponentConfiguration(component);
    }

    @Override
    public void connectDataService(GwtXSRFToken xsrfToken, String connectionId) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        request.getSession(false);

        Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudService.class, null);

        for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferences) {
            String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
            if (cloudServicePid.endsWith(connectionId)) {
                String dataServiceRef = (String) cloudServiceReference
                        .getProperty(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
                Collection<ServiceReference<DataService>> dataServiceReferences = ServiceLocator.getInstance()
                        .getServiceReferences(DataService.class, dataServiceRef);

                for (ServiceReference<DataService> dataServiceReference : dataServiceReferences) {
                    DataService dataService = ServiceLocator.getInstance().getService(dataServiceReference);
                    if (dataService != null) {
                        GwtKuraException gwtKuraException = null;
                        int counter = 10;
                        try {
                            dataService.connect();
                            while (!dataService.isConnected() && counter > 0) {
                                Thread.sleep(1000);
                                counter--;
                            }
                        } catch (KuraConnectException e) {
                            logger.warn("Error connecting", e);
                            gwtKuraException = new GwtKuraException(GwtKuraErrorCode.CONNECTION_FAILURE, e,
                                    "Error connecting. Please review your configuration.");
                        } catch (InterruptedException e) {
                            logger.warn("Interrupt Exception");
                            gwtKuraException = new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e,
                                    "Interrupt Exception");
                        } catch (IllegalStateException e) {
                            logger.warn("Illegal client state", e);
                            gwtKuraException = new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e,
                                    "Illegal client state");
                        }

                        if (gwtKuraException != null) {
                            throw gwtKuraException;
                        }
                    }
                    ServiceLocator.getInstance().ungetService(dataServiceReference);
                }
            }
            ServiceLocator.getInstance().ungetService(cloudServiceReference);
        }

        Collection<ServiceReference<CloudConnectionManager>> cloudConnectionManagerReferences = ServiceLocator
                .getInstance().getServiceReferences(CloudConnectionManager.class, null);

        for (ServiceReference<CloudConnectionManager> cloudConnectionManagerReference : cloudConnectionManagerReferences) {
            String cloudConnectionManagerPid = (String) cloudConnectionManagerReference.getProperty(KURA_SERVICE_PID);
            if (cloudConnectionManagerPid.endsWith(connectionId)) {
                CloudConnectionManager cloudConnectionManager = ServiceLocator.getInstance()
                        .getService(cloudConnectionManagerReference);
                try {
                    cloudConnectionManager.connect();
                } catch (KuraException e) {
                    logger.warn("Error connecting");
                    throw new GwtKuraException(GwtKuraErrorCode.CONNECTION_FAILURE, e,
                            "Error connecting. Please review your configuration.");
                }
            }
            ServiceLocator.getInstance().ungetService(cloudConnectionManagerReference);
        }
    }

    @Override
    public void disconnectDataService(GwtXSRFToken xsrfToken, String connectionId) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudService.class, null);

        for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferences) {
            String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
            if (cloudServicePid.endsWith(connectionId)) {
                String dataServiceRef = (String) cloudServiceReference
                        .getProperty(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
                Collection<ServiceReference<DataService>> dataServiceReferences = ServiceLocator.getInstance()
                        .getServiceReferences(DataService.class, dataServiceRef);

                for (ServiceReference<DataService> dataServiceReference : dataServiceReferences) {
                    DataService dataService = ServiceLocator.getInstance().getService(dataServiceReference);
                    if (dataService != null) {
                        dataService.disconnect(10);
                    }
                    ServiceLocator.getInstance().ungetService(dataServiceReference);
                }
            }
            ServiceLocator.getInstance().ungetService(cloudServiceReference);
        }

        Collection<ServiceReference<CloudConnectionManager>> cloudConnectionManagerReferences = ServiceLocator
                .getInstance().getServiceReferences(CloudConnectionManager.class, null);

        for (ServiceReference<CloudConnectionManager> cloudConnectionManagerReference : cloudConnectionManagerReferences) {
            String cloudConnectionManagerPid = (String) cloudConnectionManagerReference.getProperty(KURA_SERVICE_PID);
            if (cloudConnectionManagerPid.endsWith(connectionId)) {
                CloudConnectionManager cloudConnectionManager = ServiceLocator.getInstance()
                        .getService(cloudConnectionManagerReference);
                try {
                    cloudConnectionManager.disconnect();
                } catch (KuraException e) {
                    logger.warn("Error disconnecting");
                    throw new GwtKuraException(GwtKuraErrorCode.CONNECTION_FAILURE, e,
                            "Error disconnecting. Please review your configuration.");
                }
            }
            ServiceLocator.getInstance().ungetService(cloudConnectionManagerReference);
        }
    }

    @Override
    public boolean isConnected(GwtXSRFToken xsrfToken, String connectionId) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        boolean isConnected = false;

        Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudService.class, null);

        for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferences) {
            String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
            if (cloudServicePid.endsWith(connectionId)) {
                String dataServiceRef = (String) cloudServiceReference
                        .getProperty(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
                Collection<ServiceReference<DataService>> dataServiceReferences = ServiceLocator.getInstance()
                        .getServiceReferences(DataService.class, dataServiceRef);

                for (ServiceReference<DataService> dataServiceReference : dataServiceReferences) {
                    DataService dataService = ServiceLocator.getInstance().getService(dataServiceReference);
                    if (dataService != null) {
                        isConnected = dataService.isConnected();
                    }
                    ServiceLocator.getInstance().ungetService(dataServiceReference);
                }
            }
            ServiceLocator.getInstance().ungetService(cloudServiceReference);
        }

        Collection<ServiceReference<CloudConnectionManager>> cloudConnectionManagerReferences = ServiceLocator
                .getInstance().getServiceReferences(CloudConnectionManager.class, null);

        for (ServiceReference<CloudConnectionManager> cloudConnectionManagerReference : cloudConnectionManagerReferences) {
            String cloudConnectionManagerPid = (String) cloudConnectionManagerReference.getProperty(KURA_SERVICE_PID);
            if (cloudConnectionManagerPid.endsWith(connectionId)) {
                CloudConnectionManager cloudConnectionManager = ServiceLocator.getInstance()
                        .getService(cloudConnectionManagerReference);

                isConnected = cloudConnectionManager.isConnected();
            }
            ServiceLocator.getInstance().ungetService(cloudConnectionManagerReference);
        }

        return isConnected;
    }
}
