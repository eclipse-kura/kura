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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.identity.AdditionalConfigurations;
import org.eclipse.kura.identity.AssignedPermissions;
import org.eclipse.kura.identity.IdentityConfiguration;
import org.eclipse.kura.identity.IdentityConfigurationComponent;
import org.eclipse.kura.identity.IdentityService;
import org.eclipse.kura.identity.PasswordConfiguration;
import org.eclipse.kura.identity.PasswordHash;
import org.eclipse.kura.internal.rest.identity.provider.dto.ValidatorOptionsDTO;
import org.eclipse.kura.internal.rest.identity.provider.util.IdentityDTOUtils;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityConfigurationRequestDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PermissionDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.util.validation.ValidatorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
@Path("identity/v2")
public class IdentityRestServiceV2 extends AbstractIdentityRestService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityRestServiceV2.class);

    private static final String MQTT_APP_ID = "IDN-V2";

    private IdentityService identityService;

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected String getMqttApplicationId() {
        return MQTT_APP_ID;
    }

    public void bindIdentityService(IdentityService identityService) {
        this.identityService = identityService;
    }

    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createIdentity(final IdentityDTO identity) {

        boolean created = false;

        try {
            logger.debug(DEBUG_MESSAGE, "createIdentity");
            created = this.identityService.createIdentity(identity.getName());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return created ? Response.ok().build() : Response.status(Status.CONFLICT).build();
    }

    @PUT
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateIdentity(final IdentityConfigurationDTO identityConfigurationDTO) {
        try {

            logger.debug(DEBUG_MESSAGE, "updateIdentity");
            Function<char[], PasswordHash> passwordHashFunction = t -> {
                try {
                    return this.identityService.computePasswordHash(t);
                } catch (KuraException e) {
                    throw DefaultExceptionHandler.toWebApplicationException(e);
                }
            };
            List<IdentityConfiguration> configurations = Collections.singletonList(
                    IdentityDTOUtils.toIdentityConfiguration(identityConfigurationDTO, passwordHashFunction));

            this.identityService.validateIdentityConfigurations(configurations);
            this.identityService.updateIdentityConfigurations(configurations);
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
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
        try {
            logger.debug(DEBUG_MESSAGE, "getIdentityByName");
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
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

    }

    @DELETE
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteIdentity(final IdentityDTO identity) {
        boolean deleted = false;

        try {
            logger.debug(DEBUG_MESSAGE, "deleteIdentity");
            deleted = this.identityService.deleteIdentity(identity.getName());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return deleted ? Response.ok().build() : Response.status(Status.NOT_FOUND).build();
    }

    @GET
    @Path("/definedPermissions")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<PermissionDTO> getDefinedPermissions() {
        try {
            logger.debug(DEBUG_MESSAGE, "getDefinedPermissions");
            return this.identityService.getPermissions().stream().map(IdentityDTOUtils::fromPermission)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/identities")
    @Produces(MediaType.APPLICATION_JSON)
    public List<IdentityConfigurationDTO> getIdentities() {
        try {
            logger.debug(DEBUG_MESSAGE, "getIdentities");

            return this.identityService.getIdentitiesConfiguration(allIdentitiesConfiguration()).stream()
                    .map(IdentityDTOUtils::fromIdentityConfiguration).collect(Collectors.toList());

        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @Path("/passwordRequirements")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidatorOptionsDTO getPasswordRequirements() {
        try {
            logger.debug(DEBUG_MESSAGE, "getPasswordRequirements");
            ValidatorOptions validatorOptions = this.legacyIdentityService.getValidatorOptions();
            return new ValidatorOptionsDTO(//
                    validatorOptions.isPasswordMinimumLength(), //
                    validatorOptions.isPasswordRequireDigits(), //
                    validatorOptions.isPasswordRequireBothCases(), //
                    validatorOptions.isPasswordRequireSpecialChars());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    private static Set<Class<? extends IdentityConfigurationComponent>> allIdentitiesConfiguration() {
        return new HashSet<>(
                Arrays.asList(AdditionalConfigurations.class, AssignedPermissions.class, PasswordConfiguration.class));
    }

}
