/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.identity.provider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.identity.AdditionalConfigurations;
import org.eclipse.kura.identity.AssignedPermissions;
import org.eclipse.kura.identity.IdentityConfiguration;
import org.eclipse.kura.identity.IdentityConfigurationComponent;
import org.eclipse.kura.identity.IdentityService;
import org.eclipse.kura.identity.PasswordConfiguration;
import org.eclipse.kura.identity.PasswordStrengthVerificationService;
import org.eclipse.kura.internal.rest.identity.provider.util.IdentityDTOUtils;
import org.eclipse.kura.internal.rest.identity.provider.util.StringUtils;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityConfigurationRequestDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PasswordStrenghtRequirementsDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PermissionDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("identity/v2")
public class IdentityRestServiceV2 {

    private static final String NAME_REQUEST_FIELD = "name";

    private static final Logger logger = LoggerFactory.getLogger(IdentityRestServiceV2.class);

    private static final String MQTT_APP_ID = "IDN-V2";

    private static final String DEBUG_MESSAGE = "Processing request for method '{}'";

    private static final String REST_ROLE_NAME = "identity";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    private IdentityService identityService;
    private PasswordStrengthVerificationService passwordStrengthVerificationService;

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

    public void bindIdentityService(IdentityService identityService) {
        this.identityService = identityService;
    }

    public void bindPasswordStrengthVerificationService(
            PasswordStrengthVerificationService passwordStrengthVerificationService) {
        this.passwordStrengthVerificationService = passwordStrengthVerificationService;
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createIdentity(final IdentityDTO identity) {
        logger.debug(DEBUG_MESSAGE, "createIdentity");

        try {

            StringUtils.validateField(NAME_REQUEST_FIELD, identity.getName());

            boolean created = this.identityService.createIdentity(identity.getName());
            if (!created) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.CONFLICT, "Identity already exists");
            }
        } catch (Exception e) {
            throw toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @PUT
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateIdentity(final IdentityConfigurationDTO identityConfigurationDTO) {
        logger.debug(DEBUG_MESSAGE, "updateIdentity");
        try {

            StringUtils.validateField(NAME_REQUEST_FIELD, identityConfigurationDTO.getIdentity().getName());

            this.identityService
                    .updateIdentityConfiguration(IdentityDTOUtils.toIdentityConfiguration(identityConfigurationDTO));
        } catch (Exception e) {
            throw toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities/byName")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public IdentityConfigurationDTO getIdentityByName(
            final IdentityConfigurationRequestDTO identityConfigurationRequestDTO) {
        logger.debug(DEBUG_MESSAGE, "getIdentityByName");
        try {

            StringUtils.validateField(NAME_REQUEST_FIELD, identityConfigurationRequestDTO.getIdentity().getName());

            String identityName = identityConfigurationRequestDTO.getIdentity().getName();

            Optional<IdentityConfiguration> identityConfiguration = this.identityService.getIdentityConfiguration(
                    identityName, //
                    IdentityDTOUtils.toIdentityConfigurationComponents(
                            identityConfigurationRequestDTO.getConfigurationComponents()));
            if (!identityConfiguration.isPresent()) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.NOT_FOUND, "Identity does not exist");
            }

            return IdentityDTOUtils.fromIdentityConfiguration(identityConfiguration.get());
        } catch (Exception e) {
            throw toWebApplicationException(e);
        }

    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities/default/byName")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public IdentityConfigurationDTO getIdentityDefaultByName(
            final IdentityConfigurationRequestDTO identityConfigurationRequestDTO) {
        logger.debug(DEBUG_MESSAGE, "getIdentityDefaultByName");

        String identityName = identityConfigurationRequestDTO.getIdentity().getName();

        try {

            StringUtils.validateField(NAME_REQUEST_FIELD, identityName);

            IdentityConfiguration identityConfiguration = this.identityService.getIdentityDefaultConfiguration(
                    identityName, //
                    IdentityDTOUtils.toIdentityConfigurationComponents(
                            identityConfigurationRequestDTO.getConfigurationComponents()));

            return IdentityDTOUtils.fromIdentityConfiguration(identityConfiguration);
        } catch (KuraException e) {
            throw toWebApplicationException(e);
        }

    }

    @DELETE
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteIdentity(final IdentityDTO identity) {
        logger.debug(DEBUG_MESSAGE, "deleteIdentity");
        try {

            StringUtils.validateField(NAME_REQUEST_FIELD, identity.getName());

            boolean deleted = this.identityService.deleteIdentity(identity.getName());
            if (!deleted) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.NOT_FOUND, "Identity not found");
            }
        } catch (Exception e) {
            throw toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @GET
    @Path("/definedPermissions")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<PermissionDTO> getDefinedPermissions() {
        logger.debug(DEBUG_MESSAGE, "getDefinedPermissions");
        try {
            return this.identityService.getPermissions().stream().map(IdentityDTOUtils::fromPermission)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Produces(MediaType.APPLICATION_JSON)
    public List<IdentityConfigurationDTO> getIdentities() {
        logger.debug(DEBUG_MESSAGE, "getIdentities");
        try {
            return this.identityService.getIdentitiesConfiguration(allIdentitiesConfiguration()).stream()
                    .map(IdentityDTOUtils::fromIdentityConfiguration).collect(Collectors.toList());

        } catch (Exception e) {
            throw toWebApplicationException(e);
        }
    }

    @GET
    @Path("/passwordStrenghtRequirements")
    @Produces(MediaType.APPLICATION_JSON)
    public PasswordStrenghtRequirementsDTO getPasswordStrenghtRequirements() {
        logger.debug(DEBUG_MESSAGE, "getPasswordStrenghtRequirements");
        try {
            return IdentityDTOUtils.fromPasswordStrengthRequirements(
                    this.passwordStrengthVerificationService.getPasswordStrengthRequirements());
        } catch (Exception e) {
            throw toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/permissions")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPermission(final PermissionDTO permissionDTO) {
        logger.debug(DEBUG_MESSAGE, "createPermission");

        try {

            StringUtils.validateField(NAME_REQUEST_FIELD, permissionDTO.getName());

            boolean created = this.identityService.createPermission(IdentityDTOUtils.toPermission(permissionDTO));
            if (!created) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.CONFLICT,
                        "Permission already exists");
            }
        } catch (KuraException e) {
            throw toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @DELETE
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/permissions")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePermission(final PermissionDTO permissionDTO) {
        logger.debug(DEBUG_MESSAGE, "deletePermission");

        StringUtils.validateField(NAME_REQUEST_FIELD, permissionDTO.getName());

        boolean deleted = false;
        try {
            deleted = this.identityService.deletePermission(IdentityDTOUtils.toPermission(permissionDTO));
            if (!deleted) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.NOT_FOUND, "Permission not found");
            }

        } catch (KuraException e) {
            throw toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response validateIdentityConfiguration(final IdentityConfigurationDTO identityConfigurationDTO) {
        try {

            StringUtils.validateField(NAME_REQUEST_FIELD, identityConfigurationDTO.getIdentity().getName());

            this.identityService
                    .validateIdentityConfiguration(IdentityDTOUtils.toIdentityConfiguration(identityConfigurationDTO));
        } catch (KuraException e) {
            throw toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    private static Set<Class<? extends IdentityConfigurationComponent>> allIdentitiesConfiguration() {
        return new HashSet<>(
                Arrays.asList(AdditionalConfigurations.class, AssignedPermissions.class, PasswordConfiguration.class));
    }

    private WebApplicationException toWebApplicationException(final Exception e) {
        if (e instanceof KuraException && ((KuraException) e).getCode() == KuraErrorCode.INVALID_PARAMETER) {
            return DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST, e.getMessage());
        } else {
            return DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

}
