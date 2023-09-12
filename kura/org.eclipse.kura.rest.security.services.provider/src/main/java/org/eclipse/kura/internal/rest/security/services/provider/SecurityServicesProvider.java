package org.eclipse.kura.internal.rest.security.services.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.internal.rest.security.services.provider.dto.SercurityServicesDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.ssl.SslManagerService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("keystore/v1")
public class SecurityServicesProvider {

    private static final Logger logger = LoggerFactory.getLogger(SecurityServicesProvider.class);
    private static final String DEBUG_MESSSAGE = "Processing request for method '{}'";

    private static final String MQTT_APP_ID = "KST-V1";
    private static final String REST_ROLE_NAME = "keystore";
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
    @Path("/entry-list")
    @Produces(MediaType.APPLICATION_JSON)
    public SercurityServicesDTO keystoreEntriesList() {
        try {
            logger.info(DEBUG_MESSSAGE, "entryList");
            List<String> resultDTO = new ArrayList<>();

            BundleContext context = FrameworkUtil.getBundle(SecurityServicesProvider.class).getBundleContext();

            Collection<ServiceReference<KeystoreService>> keystoreServices = context
                    .getServiceReferences(KeystoreService.class, (String) null);
            Iterator<ServiceReference<KeystoreService>> keystoreIterator = keystoreServices.iterator();

            if (keystoreIterator.hasNext()) {
                resultDTO.add("Keystore Configuration");
            }
            while (keystoreIterator.hasNext()) {

                Dictionary<String, Object> objects = keystoreIterator.next().getProperties();
                resultDTO.add("kura.service.pid: " + objects.get("kura.service.pid"));
            }

            Collection<ServiceReference<SslManagerService>> sslServices = context
                    .getServiceReferences(SslManagerService.class, (String) null);
            Iterator<ServiceReference<SslManagerService>> sslIterator = sslServices.iterator();

            if (sslIterator.hasNext()) {
                resultDTO.add("SSL Configuration");
            }
            while (sslIterator.hasNext()) {

                Dictionary<String, Object> objects = sslIterator.next().getProperties();
                resultDTO.add("kura.service.pid: " + objects.get("kura.service.pid"));
            }

            return new SercurityServicesDTO(resultDTO);
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

    }

}
