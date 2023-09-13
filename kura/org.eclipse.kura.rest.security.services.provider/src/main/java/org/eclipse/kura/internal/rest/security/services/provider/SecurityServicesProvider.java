package org.eclipse.kura.internal.rest.security.services.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.internal.rest.security.services.provider.dto.InterfacesIdsDTO;
import org.eclipse.kura.internal.rest.security.services.provider.dto.SercurityServicesDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.ssl.SslManagerService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("securityServices/v1")
public class SecurityServicesProvider {

    private static final Logger logger = LoggerFactory.getLogger(SecurityServicesProvider.class);
    private static final String DEBUG_MESSSAGE = "Processing request for method '{}'";

    private static final String MQTT_APP_ID = "SECSERV-V1";
    private static final String REST_ROLE_NAME = "security.services";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private static final String KURA_SERVICE_PID_FILTER = "kura.service.pid";
    private static final String OBJECT_CLASS_FILTER = "objectClass";

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
    @Path("/services")
    @Produces(MediaType.APPLICATION_JSON)
    public SercurityServicesDTO getServicesList() {
        try {
            logger.debug(DEBUG_MESSSAGE, "securityServices/v1/services");

            BundleContext context = FrameworkUtil.getBundle(SecurityServicesProvider.class).getBundleContext();
            List<String> resultDTO = getAllServices(context);

            return new SercurityServicesDTO(resultDTO);
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

    }

    @POST
    @RolesAllowed("byInterface")
    @Path("/services/byInterface")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public SercurityServicesDTO getServicesByInterface(final InterfacesIdsDTO interfaceIds) {
        try {
            interfaceIds.idsValidation();

            BundleContext context = FrameworkUtil.getBundle(SecurityServicesProvider.class).getBundleContext();

            return new SercurityServicesDTO(getFilteredInterfaces(context, interfaceIds.getInterfacesIds()));
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    private List<String> getAllServices(BundleContext context) throws InvalidSyntaxException {

        List<String> servicesList = new ArrayList<>();

        Collection<ServiceReference<KeystoreService>> keystoreServices = context
                .getServiceReferences(KeystoreService.class, (String) null);

        keystoreServices.stream().forEach(entry -> {
            servicesList.add(entry.getProperty(KURA_SERVICE_PID_FILTER).toString());
        });

        Collection<ServiceReference<SslManagerService>> sslServices = context
                .getServiceReferences(SslManagerService.class, (String) null);

        sslServices.stream().forEach(entry -> {
            servicesList.add(entry.getProperty(KURA_SERVICE_PID_FILTER).toString());
        });

        return servicesList;
    }

    private List<String> getFilteredInterfaces(BundleContext context, List<String> interfacesIds)
            throws InvalidSyntaxException {

        List<String> servicesList = new ArrayList<>();

        Collection<ServiceReference<KeystoreService>> keystoreServices = context
                .getServiceReferences(KeystoreService.class, (String) null);

        keystoreServices.stream().forEach(entry -> {
            List<String> objectClassesList = Arrays.asList((String[]) entry.getProperty(OBJECT_CLASS_FILTER));

            interfacesIds.forEach(filtering -> {
                if (objectClassesList.contains(filtering)) {
                    servicesList.add(entry.getProperty(KURA_SERVICE_PID_FILTER).toString());
                }
            });

        });

        Collection<ServiceReference<SslManagerService>> sslServices = context
                .getServiceReferences(SslManagerService.class, (String) null);

        sslServices.stream().forEach(entry -> {
            List<String> objectClassesList = Arrays.asList((String[]) entry.getProperty(OBJECT_CLASS_FILTER));

            interfacesIds.forEach(filtering -> {
                if (objectClassesList.contains(filtering)) {
                    servicesList.add(entry.getProperty(KURA_SERVICE_PID_FILTER).toString());
                }
            });
        });

        return servicesList;

    }

}
