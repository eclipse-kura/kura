package org.eclipse.kura.internal.rest.service.listing.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.internal.rest.service.listing.provider.dto.InterfacesIdsDTO;
import org.eclipse.kura.internal.rest.service.listing.provider.dto.SortedServiceListDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("serviceListing/v1")
public class RestServiceListingProvider {

    private static final Logger logger = LoggerFactory.getLogger(RestServiceListingProvider.class);
    private static final String REQUEST_DEBUG_MESSAGE = "Received request from: '{}'";

    private static final String APP_ID_MQTT = "SERLIST-V1";
    private static final String REST_ROLE = "kura.permission.rest.serviceListing";

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

    public void bindUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(REST_ROLE, Role.GROUP);
    }

    /**
     * GET method
     *
     * @return list of all services running on kura exposing a <kura.service.pid> property
     */
    @GET
    @RolesAllowed("serviceListing")
    @Path("/sortedList")
    @Produces(MediaType.APPLICATION_JSON)
    public SortedServiceListDTO getSortedServicesList() {
        try {
            logger.debug(REQUEST_DEBUG_MESSAGE, "serviceListing/v1/sortedList");

            BundleContext context = FrameworkUtil.getBundle(RestServiceListingProvider.class).getBundleContext();
            List<String> resultDTO = getAllServices(context);

            return new SortedServiceListDTO(resultDTO);

        } catch (Exception ex) {
            throw DefaultExceptionHandler.toWebApplicationException(ex);
        }

    }

    /**
     * POST method
     *
     * @return list of all services running on kura, filtered by the list of interfaces that the services must
     *         implement. If more <interfacesList> contains more than one entry, all of them are put in an AND logic
     *         value
     */
    @POST
    @RolesAllowed("serviceListing")
    @Path("/sortedList/byAllInterfaces")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public SortedServiceListDTO getSortedServicesByInterface(final InterfacesIdsDTO interfacesList) {
        try {

            logger.debug(REQUEST_DEBUG_MESSAGE, "serviceListing/v1/list/sortedList/byAllInterfaces");

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

    private List<String> getStrictFilteredInterfaces(BundleContext context, List<String> interfacesIds)
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

    private String generateFilterString(List<String> interfacesIds) {

        StringBuilder filterStringBuilder = new StringBuilder("(&");

        for (String serviceFilter : interfacesIds) {
            filterStringBuilder.append(OBJECT_CLASS_FILTER);
            filterStringBuilder.append(serviceFilter);
            filterStringBuilder.append(")");
        }

        filterStringBuilder.append(")");

        return filterStringBuilder.toString();
    }

    private SortedServiceListDTO generateResponseDTO(BundleContext context, InterfacesIdsDTO returnInterfaceIds)
            throws KuraException, InvalidSyntaxException {
        try {
            return new SortedServiceListDTO(
                    getStrictFilteredInterfaces(context, returnInterfaceIds.getInterfacesIds()));
        } catch (NullPointerException ex) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "No result found for the passed interfaces");
        }
    }

}
