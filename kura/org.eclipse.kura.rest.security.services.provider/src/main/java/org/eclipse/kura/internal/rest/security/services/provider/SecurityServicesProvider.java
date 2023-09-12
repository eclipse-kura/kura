package org.eclipse.kura.internal.rest.security.services.provider;

import java.util.ArrayList;
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
            List<String> resultDTO = getServices(context);

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

    private List<String> getServices(BundleContext context) throws InvalidSyntaxException {

        List<String> servicesList = new ArrayList<>();

        Collection<ServiceReference<KeystoreService>> keystoreServices = context
                .getServiceReferences(KeystoreService.class, (String) null);
        // Iterator<ServiceReference<KeystoreService>> keystoreIterator = keystoreServices.iterator();

        keystoreServices.stream().forEach(entry -> {
            servicesList.add(entry.getProperty("kura.service.pid").toString());
        });

        // if (keystoreIterator.hasNext()) {
        // servicesList.add("Keystore Configuration");
        // }
        // while (keystoreIterator.hasNext()) {
        // servicesList.add(keystoreIterator.next().getProperties().get("kura.service.pid").toString());
        // }

        Collection<ServiceReference<SslManagerService>> sslServices = context
                .getServiceReferences(SslManagerService.class, (String) null);
        // Iterator<ServiceReference<SslManagerService>> sslIterator = sslServices.iterator();

        sslServices.stream().forEach(entry -> {
            servicesList.add(entry.getProperty("kura.service.pid").toString());
        });

        // if (sslIterator.)) {
        // servicesList.add("SSL Configuration");
        // }
        // while (sslIterator.hasNext()) {
        // servicesList.add(sslIterator.next().getProperties().get("kura.service.pid").toString());
        // }
        //
        return servicesList;
    }

    private List<String> getFilteredInterfaces(BundleContext context, List<String> interfacesIds) {

        List<String> servicesList = new ArrayList<>();

        return servicesList;

    }

}
