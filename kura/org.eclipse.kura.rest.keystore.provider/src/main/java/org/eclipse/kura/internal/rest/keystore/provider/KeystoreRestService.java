package org.eclipse.kura.internal.rest.keystore.provider;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.internal.rest.keystore.provider.dto.KeyStoreDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("keystore/v1")
public class KeystoreRestService {
    
    private static final Logger logger = LoggerFactory.getLogger(KeystoreRestService.class);
    private static final String DEBUG_MESSSAGE = "Processing request for method '{}'";
    
    private static final String MQTT_APP_ID = "KST-V1";
    private static final String REST_ROLE_NAME = "keystore";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;
    
    private KeystoreService keystore;
    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);
    
    public void bindKeystoreService(KeystoreService keystoreService) {
        this.keystore = keystoreService;
    }
    
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
    @Path("/debug-enabled")
    @Produces(MediaType.APPLICATION_JSON)
    public KeyStoreDTO keystoreEntriesList() {
        try {
            logger.debug(DEBUG_MESSSAGE, "isDebugEnabled");
            return new KeyStoreDTO(this.keystore.getAliases());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

    }
    
}
