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
package org.eclipse.kura.internal.rest.service.listing.provider;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.internal.rest.service.listing.provider.dto.FilterDTO;
import org.eclipse.kura.internal.rest.service.listing.provider.dto.InterfaceNamesDTO;
import org.eclipse.kura.internal.rest.service.listing.provider.dto.RefDTO;
import org.eclipse.kura.internal.rest.service.listing.provider.util.FilterBuilder;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.rest.configuration.api.PidSet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("serviceListing/v1")
public class RestServiceListingProvider {

    private static final String OBJECT_CLASS = "objectClass";

    private static final Logger logger = LoggerFactory.getLogger(RestServiceListingProvider.class);

    private static final String APP_ID_MQTT = "SVCLIST-V1";

    private static final String KURA_SERVICE_PID = "kura.service.pid";

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    private ConfigurationService configurationService;
    private ServiceComponentRuntime scr;
    private BundleContext bundleContext;

    public void bindRequestHandlerRegistry(RequestHandlerRegistry bindingRegistry) {
        try {
            bindingRegistry.registerRequestHandler(APP_ID_MQTT, this.requestHandler);
        } catch (final Exception e) {
            logger.warn("Failed to register {} request handler", APP_ID_MQTT, e);
        }
    }

    public void unbindRequestHandlerRegistry(RequestHandlerRegistry unbindingRegistry) {
        try {
            unbindingRegistry.unregister(APP_ID_MQTT);
        } catch (final Exception e) {
            logger.warn("Failed to unregister {} request handler", APP_ID_MQTT, e);
        }
    }

    public void setConfigurationService(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setServiceComponentRuntime(final ServiceComponentRuntime scr) {
        this.scr = scr;
    }

    public void activate(final ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
    }

    /**
     * GET method
     *
     * @return list of all services running on kura exposing a <kura.service.pid>
     *         property
     */
    @GET
    @Path("/servicePids")
    @Produces(MediaType.APPLICATION_JSON)
    public PidSet getServicePids(@Context final ContainerRequestContext requestContext) {
        try {

            expectAuthenticatedUser(requestContext);

            final List<ServiceReference<?>> servicesList = Arrays
                    .asList(bundleContext.getServiceReferences((String) null, (String) null));

            return new PidSet(getServicePids(servicesList));

        } catch (Exception ex) {
            throw DefaultExceptionHandler.toWebApplicationException(ex);
        }

    }

    /**
     * POST method
     *
     * @return list of all services running on kura, filtered by the list of
     *         interfaces that the services must implement. If more <interfacesList>
     *         contains more than one entry, all of them are put in an AND logic
     *         value
     */
    @POST
    @Path("/servicePids/byInterface")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public PidSet getServicePidsByInterface(final InterfaceNamesDTO interfacesList,
            @Context final ContainerRequestContext requestContext) {
        try {

            expectAuthenticatedUser(requestContext);

            interfacesList.validate();

            return new PidSet(getServicesProvidingInterfaces(interfacesList.getInterfacesIds()));

        } catch (final Exception ex) {
            throw DefaultExceptionHandler.toWebApplicationException(ex);
        }
    }

    @POST
    @Path("/servicePids/byProperty")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public PidSet getServicePidsByFilter(final FilterDTO filter,
            @Context final ContainerRequestContext requestContext) {
        try {

            expectAuthenticatedUser(requestContext);

            filter.validate();

            return new PidSet(getServicesMatchingFilter(filter.toOSGIFilter()));

        } catch (final Exception ex) {
            throw DefaultExceptionHandler.toWebApplicationException(ex);
        }
    }

    @POST
    @Path("/servicePids/satisfyingReference")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public PidSet getServicePidsSatisfyingReference(final RefDTO ref,
            @Context final ContainerRequestContext requestContext) {
        try {

            expectAuthenticatedUser(requestContext);

            ref.validate();

            final String componentName = getComponentNameFromPid(ref.getPid())
                    .orElseThrow(() -> new WebApplicationException(Status.NOT_FOUND));
            final String targetRef = ref.getReferenceName();

            final String referenceInterface = getReferenceInterface(componentName, targetRef)
                    .orElseThrow(() -> new WebApplicationException(Status.NOT_FOUND));

            final Set<String> pids = getServicesProvidingInterfaces(Collections.singleton(referenceInterface));

            return new PidSet(pids);

        } catch (final Exception ex) {
            throw DefaultExceptionHandler.toWebApplicationException(ex);
        }
    }

    @GET
    @Path("/factoryPids")
    @Produces(MediaType.APPLICATION_JSON)
    public PidSet getFactoryPids(@Context final ContainerRequestContext requestContext) {
        try {

            expectAuthenticatedUser(requestContext);

            return new PidSet(configurationService.getFactoryComponentPids());

        } catch (Exception ex) {
            throw DefaultExceptionHandler.toWebApplicationException(ex);
        }
    }

    @POST
    @Path("/factoryPids/byInterface")
    @Produces(MediaType.APPLICATION_JSON)
    public PidSet getFactoryPidsByInterface(final InterfaceNamesDTO interfacesList,
            @Context final ContainerRequestContext requestContext) {
        try {

            expectAuthenticatedUser(requestContext);

            interfacesList.validate();

            final Set<String> result = getFactoryComponentDescriptors()
                    .filter(component -> Arrays.asList(component.serviceInterfaces)
                            .containsAll(interfacesList.getInterfacesIds()))
                    .map(component -> component.name).collect(Collectors.toSet());

            return new PidSet(result);

        } catch (Exception ex) {
            throw DefaultExceptionHandler.toWebApplicationException(ex);
        }
    }

    @POST
    @Path("/factoryPids/byProperty")
    @Produces(MediaType.APPLICATION_JSON)
    public PidSet getFactoryPidsByFilter(final FilterDTO filter,
            @Context final ContainerRequestContext requestContext) {
        try {

            expectAuthenticatedUser(requestContext);

            filter.validate();

            final Filter osgiFilter = FrameworkUtil.createFilter(filter.toOSGIFilter());

            final Set<String> result = getFactoryComponentDescriptors().filter(component -> {
                final Map<String, Object> properties = new HashMap<>(component.properties);
                if (component.serviceInterfaces.length != 0) {
                    properties.put(OBJECT_CLASS, component.serviceInterfaces);
                }

                return osgiFilter.matches(properties);
            }).map(component -> component.name).collect(Collectors.toSet());

            return new PidSet(result);

        } catch (Exception ex) {
            throw DefaultExceptionHandler.toWebApplicationException(ex);
        }
    }

    /*
     * Utils methods
     */

    private void expectAuthenticatedUser(final ContainerRequestContext requestContext) {
        if (requestContext != null && !getPrincipal(requestContext).isPresent()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
    }

    private Optional<Principal> getPrincipal(final ContainerRequestContext containerRequestContext) {
        return Optional.ofNullable(containerRequestContext.getSecurityContext())
                .flatMap(entry -> Optional.ofNullable(entry.getUserPrincipal()));
    }

    private Set<String> getServicesProvidingInterfaces(final Set<String> interfacesIds) throws InvalidSyntaxException {

        final String filter = new FilterBuilder().and(f -> interfacesIds.forEach(i -> f.property(OBJECT_CLASS, i)))
                .build();

        return getServicesMatchingFilter(filter);
    }

    private Set<String> getServicesMatchingFilter(final String filter) throws InvalidSyntaxException {

        List<ServiceReference<?>> servicesList = Optional
                .ofNullable(bundleContext.getServiceReferences((String) null, filter)).map(Arrays::asList)
                .orElseGet(Collections::emptyList);

        return getServicePids(servicesList);
    }

    private Set<String> getServicePids(final List<ServiceReference<?>> references) {

        return references.stream().filter(s -> s.getProperty(KURA_SERVICE_PID) instanceof String)
                .map(s -> (String) s.getProperty(KURA_SERVICE_PID)).collect(Collectors.toSet());

    }

    private Optional<String> getComponentNameFromPid(final String pid) {
        try {
            final ServiceReference<?>[] refs = bundleContext.getServiceReferences((String) null,
                    "(kura.service.pid=" + pid + ")");

            if (refs == null || refs.length == 0) {
                return Optional.empty();
            }

            final ServiceReference<?> ref = refs[0];

            final String result = Optional.ofNullable(ref.getProperty("service.factoryPid"))
                    .flatMap(s -> s instanceof String ? Optional.of((String) s) : Optional.empty()).orElse(pid);

            return Optional.of(result);

        } catch (final InvalidSyntaxException e) {
            logger.warn("bad pid name: {}", pid);
            return Optional.empty();
        }
    }

    private Optional<String> getReferenceInterface(final String componentName, final String targetRef) {
        return scr.getComponentDescriptionDTOs().stream()
                .filter(componentDescription -> componentDescription.name.equals(componentName)).findAny()
                .flatMap(componentDescription -> Arrays.stream(componentDescription.references)
                        .filter(reference -> targetRef.equals(reference.name)).findAny()
                        .map(reference -> reference.interfaceName)

                );
    }

    private Stream<ComponentDescriptionDTO> getFactoryComponentDescriptors() {
        final Set<String> factories = configurationService.getFactoryComponentPids();

        return scr.getComponentDescriptionDTOs().stream().filter(s -> factories.contains(s.name));
    }
}
