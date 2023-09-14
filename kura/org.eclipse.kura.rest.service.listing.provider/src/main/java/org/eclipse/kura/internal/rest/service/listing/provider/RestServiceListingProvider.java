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
    private static final String DEBUG_MESSSAGE = "Processing request for method '{}'";

    private static final String MQTT_APP_ID = "SERLIST-V1";
    private static final String REST_ROLE_NAME = "serviceListing";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private static final String KURA_SERVICE_PID_FILTER = "kura.service.pid";
    private static final String OBJECT_CLASS_FILTER = "(objectClass=";

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    public void bindUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_ROLE, Role.GROUP);
    }

    public void bindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.registerRequestHandler(MQTT_APP_ID, this.requestHandler);
        } catch (final Exception e) {
            logger.warn("Failed to register {} request handler", MQTT_APP_ID, e);
        }
    }

    public void unbindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.unregister(MQTT_APP_ID);
        } catch (final Exception e) {
            logger.warn("Failed to unregister {} request handler", MQTT_APP_ID, e);
        }
    }

    /**
     * GET method
     *
     * @return true if the debug is permitted. False otherwise.
     */
    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/sortedList")
    @Produces(MediaType.APPLICATION_JSON)
    public SortedServiceListDTO getSortedServicesList() {
        try {
            logger.debug(DEBUG_MESSSAGE, "serviceListing/v1/sortedList");

            BundleContext context = FrameworkUtil.getBundle(RestServiceListingProvider.class).getBundleContext();
            List<String> resultDTO = getAllServices(context);

            return new SortedServiceListDTO(resultDTO);
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/sortedList/byAllInterfaces")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public SortedServiceListDTO getSortedServicesByInterface(final InterfacesIdsDTO interfaceIds) {
        try {

            logger.debug(DEBUG_MESSSAGE, "serviceListing/v1/list/sortedList/byAllInterfaces");

            InterfacesIdsDTO returnInterfaceIds;
            if (interfaceIds == null) {
                returnInterfaceIds = new InterfacesIdsDTO(null);
                returnInterfaceIds.idsValidation();
            } else {
                interfaceIds.idsValidation();
                returnInterfaceIds = interfaceIds;
            }

            BundleContext context = FrameworkUtil.getBundle(RestServiceListingProvider.class).getBundleContext();

            return generateResponseDTO(context, returnInterfaceIds);

        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
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
