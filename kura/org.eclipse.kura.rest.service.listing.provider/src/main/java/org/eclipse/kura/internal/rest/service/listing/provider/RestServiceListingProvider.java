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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.security.PermitAll;
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

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.internal.rest.service.listing.provider.dto.InterfacesIdsDTO;
import org.eclipse.kura.internal.rest.service.listing.provider.dto.ServiceListDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("serviceListing/v1")
public class RestServiceListingProvider {

    private static final Logger logger = LoggerFactory.getLogger(RestServiceListingProvider.class);
    private static final String REQUEST_DEBUG_MESSAGE = "Received request from: '{}'";

    private static final String APP_ID_MQTT = "SVCLIST-V1";

    private static final String KURA_SERVICE_PID_FILTER = "kura.service.pid";
    private static final String OBJECT_CLASS_FILTER = "(objectClass=";

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

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

    /**
     * GET method
     *
     * @return list of all services running on kura exposing a <kura.service.pid>
     *         property
     */
    @GET
    @PermitAll
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public ServiceListDTO getSortedServicesList(@Context final ContainerRequestContext requestContext) {
        try {
            logger.debug(REQUEST_DEBUG_MESSAGE, "serviceListing/v1/list");

            BundleContext context = FrameworkUtil.getBundle(RestServiceListingProvider.class).getBundleContext();

            if (requestContext != null && !getPrincipal(requestContext).isPresent()) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            List<String> resultDTO = getAllServices(context);

            return new ServiceListDTO(resultDTO);

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
    @Path("/list/byInterface")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ServiceListDTO getSortedServicesByInterface(final InterfacesIdsDTO interfacesList,
            @Context final ContainerRequestContext requestContext) {
        try {

            logger.debug(REQUEST_DEBUG_MESSAGE, "serviceListing/v1/list/byInterface");

            if (requestContext != null && !getPrincipal(requestContext).isPresent()) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            InterfacesIdsDTO returnInterfacesList;
            if (interfacesList == null) {
                returnInterfacesList = new InterfacesIdsDTO(null);
                returnInterfacesList.idsValidation();
            } else {
                interfacesList.idsValidation();
                returnInterfacesList = interfacesList;
            }

            BundleContext context = FrameworkUtil.getBundle(RestServiceListingProvider.class).getBundleContext();

            return generateResponseDTO(context, returnInterfacesList);

        } catch (final Exception ex) {
            throw DefaultExceptionHandler.toWebApplicationException(ex);
        }
    }

    /*
     * Utils methods
     */

    private List<String> getAllServices(BundleContext context) throws InvalidSyntaxException {

        List<ServiceReference<?>> servicesList = Arrays
                .asList(context.getServiceReferences((String) null, (String) null));

        List<String> services = new ArrayList<>();

        servicesList.stream().forEach(service -> {

            if (service.getProperty(KURA_SERVICE_PID_FILTER) != null) {
                services.add((String) service.getProperty(KURA_SERVICE_PID_FILTER));
            }
        });

        return services;
    }

    private List<String> getStrictFilteredInterfaces(BundleContext context, Set<String> interfacesIds)
            throws InvalidSyntaxException {

        List<ServiceReference<?>> servicesList = Arrays
                .asList(context.getServiceReferences((String) null, generateFilterString(interfacesIds)));

        List<String> filteredServices = new ArrayList<>();

        servicesList.stream().forEach(service -> {

            if (service.getProperty(KURA_SERVICE_PID_FILTER) != null) {
                filteredServices.add((String) service.getProperty(KURA_SERVICE_PID_FILTER));
            }
        });

        return filteredServices;

    }

    private String generateFilterString(Set<String> interfacesIds) {

        StringBuilder filterStringBuilder = new StringBuilder("(&");

        for (String serviceFilter : interfacesIds) {
            filterStringBuilder.append(OBJECT_CLASS_FILTER);
            filterStringBuilder.append(serviceFilter);
            filterStringBuilder.append(")");
        }

        filterStringBuilder.append(")");

        return filterStringBuilder.toString();
    }

    private ServiceListDTO generateResponseDTO(BundleContext context, InterfacesIdsDTO returnInterfaceIds)
            throws KuraException, InvalidSyntaxException {
        try {
            return new ServiceListDTO(
                    getStrictFilteredInterfaces(context, returnInterfaceIds.getInterfacesIds()));
        } catch (NullPointerException ex) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "No result found for the passed interfaces");
        }
    }

    private Optional<Principal> getPrincipal(final ContainerRequestContext containerRequestContext) {
        return Optional.ofNullable(containerRequestContext.getSecurityContext())
                .flatMap(entry -> Optional.ofNullable(entry.getUserPrincipal()));
    }

}
