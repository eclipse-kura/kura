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
package org.eclipse.kura.internal.rest.auth;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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

import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.internal.rest.auth.dto.AuthenticationInfoDTO;
import org.eclipse.kura.internal.rest.auth.dto.AuthenticationResponseDTO;
import org.eclipse.kura.internal.rest.auth.dto.IdentityInfoDTO;
import org.eclipse.kura.internal.rest.auth.dto.UpdatePasswordDTO;
import org.eclipse.kura.internal.rest.auth.dto.UsernamePasswordDTO;
import org.eclipse.kura.internal.rest.auth.dto.XsrfTokenDTO;
import org.eclipse.kura.internal.rest.provider.RestServiceOptions;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.util.useradmin.UserAdminHelper;
import org.eclipse.kura.util.useradmin.UserAdminHelper.AuthenticationException;
import org.eclipse.kura.util.validation.PasswordStrengthValidators;
import org.eclipse.kura.util.validation.Validator;
import org.eclipse.kura.util.validation.ValidatorOptions;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
@Path(SessionRestServiceConstants.BASE_PATH)
public class SessionRestService {

    private static final String AUDIT_FORMAT_STRING = "{} Rest - Failure - {}";
    private static final String INVALID_SESSION_MESSAGE = "Current session is not valid";
    private static final String BAD_USERNAME_OR_PASSWORD_MESSAGE = "Authentication failed as username or password not matching";
    private static final String PASSWORD_CHANGE_SAME_PASSWORD_MESSAGE = "Password change failed as previous password equals new one";
    private static final String IDENTITY_NOT_IN_ROLE_MESSAGE = "Identity does not have the required permissions";

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private final UserAdminHelper userAdminHelper;
    private final RestSessionHelper restSessionHelper;
    private final ConfigurationAdmin configAdmin;
    private RestServiceOptions options;

    public SessionRestService(final UserAdminHelper userAdminHelper, final RestSessionHelper restSessionHelper,
            final ConfigurationAdmin configurationAdmin) {
        this.userAdminHelper = userAdminHelper;
        this.restSessionHelper = restSessionHelper;
        this.configAdmin = configurationAdmin;
    }

    public void setOptions(final RestServiceOptions options) {
        this.options = options;
    }

    @POST
    @Path(SessionRestServiceConstants.LOGIN_PASSWORD_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationResponseDTO authenticateWithUsernameAndPassword(
            final UsernamePasswordDTO usernamePassword,
            @Context final HttpServletRequest request) {

        if (!options.isSessionManagementEnabled() || !options.isPasswordAuthEnabled()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        final AuditContext auditContext = AuditContext.currentOrInternal();

        usernamePassword.validate();

        auditContext.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), usernamePassword.getUsername());

        try {

            this.userAdminHelper.verifyUsernamePassword(usernamePassword.getUsername(),
                    usernamePassword.getPassword());

            final HttpSession session = this.restSessionHelper.createNewAuthenticatedSession(request,
                    usernamePassword.getUsername());

            final AuthenticationResponseDTO response = buildAuthenticationResponse(usernamePassword.getUsername());

            if (response.isPasswordChangeNeeded()) {
                this.restSessionHelper.lockSession(session);
            }

            auditLogger.info("{} Rest - Success - Create session via password authentication succeeded", auditContext);

            return response;

        } catch (final AuthenticationException e) {
            handleAuthenticationException(e);
            throw new IllegalStateException("unreachable");
        }
    }

    @POST
    @Path(SessionRestServiceConstants.LOGIN_CERTIFICATE_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationResponseDTO authenticateWithCertificate(@Context final HttpServletRequest request,
            @Context final ContainerRequestContext requestContext) {
        if (!options.isSessionManagementEnabled() || !options.isCertificateAuthEnabled()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        final CertificateAuthenticationProvider certificateAuthProvider = new CertificateAuthenticationProvider(
                userAdminHelper);

        final Optional<Principal> principal = certificateAuthProvider.authenticate(requestContext,
                "Create session via certificate authentication");

        if (principal.isPresent()) {
            this.restSessionHelper.createNewAuthenticatedSession(request,
                    principal.get().getName());
        } else {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.UNAUTHORIZED,
                    "Certificate authentication failed");
        }

        return buildAuthenticationResponse(principal.get().getName());
    }

    @GET
    @Path(SessionRestServiceConstants.XSRF_TOKEN_PATH)
    public XsrfTokenDTO getXSRFToken(@Context final HttpServletRequest request,
            @Context final ContainerRequestContext requestContext) {
        if (!options.isSessionManagementEnabled()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        final Optional<HttpSession> session = this.restSessionHelper.getExistingSession(request);

        if (!session.isPresent()) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.UNAUTHORIZED,
                    INVALID_SESSION_MESSAGE);
        }

        if (!this.restSessionHelper.getCurrentPrincipal(requestContext).isPresent()) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.UNAUTHORIZED,
                    INVALID_SESSION_MESSAGE);
        }

        return new XsrfTokenDTO(this.restSessionHelper.getOrCreateXsrfToken(session.get()));
    }

    @POST
    @Path(SessionRestServiceConstants.CHANGE_PASSWORD_PATH)
    public void updateUserPassword(@Context final ContainerRequestContext requestContext,
            @Context final HttpServletRequest request,
            final UpdatePasswordDTO passwordUpdate) {

        passwordUpdate.validate();

        try {
            final Optional<String> username = this.restSessionHelper.getCurrentPrincipal(requestContext)
                    .flatMap(c -> Optional.ofNullable(c.getName()));

            if (!username.isPresent()) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.UNAUTHORIZED,
                        INVALID_SESSION_MESSAGE);
            }

            this.userAdminHelper.verifyUsernamePassword(username.get(), passwordUpdate.getCurrentPassword());

            final String newPassword = passwordUpdate.getNewPassword();

            validatePasswordStrength(newPassword);

            this.userAdminHelper.changeUserPassword(username.get(), passwordUpdate.getNewPassword());

            final Optional<HttpSession> session = this.restSessionHelper.getExistingSession(request);

            if (session.isPresent()) {
                this.restSessionHelper.unlockSession(session.get());
            }
        } catch (final AuthenticationException e) {
            handleAuthenticationException(e);
        }
    }

    @POST
    @Path(SessionRestServiceConstants.LOGOUT_PATH)
    public void logout(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @Context final ContainerRequestContext requestContext) {
        if (!options.isSessionManagementEnabled()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        if (!this.restSessionHelper.getCurrentPrincipal(requestContext).isPresent()) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.UNAUTHORIZED,
                    INVALID_SESSION_MESSAGE);
        }

        this.restSessionHelper.logout(request, response);

        auditLogger.info("{} Rest - Success - Logout succeeded",
                AuditContext.currentOrInternal());
    }

    @GET
    @Path(SessionRestServiceConstants.CURRENT_IDENTITY)
    @Produces(MediaType.APPLICATION_JSON)
    public IdentityInfoDTO getCurrentIdentityInfo(@Context final ContainerRequestContext requestContext,
            @Context final HttpServletRequest request) {
        if (!options.isSessionManagementEnabled()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        final Optional<Principal> currentPrincipal = this.restSessionHelper.getCurrentPrincipal(requestContext);

        if (!currentPrincipal.isPresent()) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.UNAUTHORIZED,
                    INVALID_SESSION_MESSAGE);
        }

        final String identityName = currentPrincipal.get().getName();
        final Set<String> permissions = this.userAdminHelper.getIdentityPermissions(identityName);
        final boolean needsPasswordChange = this.userAdminHelper
                .isPasswordChangeRequired(identityName);

        return new IdentityInfoDTO(identityName, needsPasswordChange, permissions);
    }

    @GET
    @Path(SessionRestServiceConstants.AUTHENTICATION_INFO)
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationInfoDTO getAuthenticationMethodInfo() {

        final boolean isPasswordAuthEnabled = options.isPasswordAuthEnabled();
        final boolean isCertificateAuthenticationEnabled = options.isCertificateAuthEnabled();

        final Map<String, Object> consoleConfig = ConfigurationAdminHelper
                .loadConsoleConfigurationProperties(configAdmin);

        final String message = ConfigurationAdminHelper.getLoginMessage(consoleConfig).orElse(null);

        if (!isCertificateAuthenticationEnabled) {
            return new AuthenticationInfoDTO(isPasswordAuthEnabled, false, null, message);
        }

        final Map<String, Object> httpServiceConfig = ConfigurationAdminHelper
                .loadHttpServiceConfigurationProperties(configAdmin);

        final Set<Integer> httpsClientAuthPorts = ConfigurationAdminHelper
                .getHttpsMutualAuthPorts(httpServiceConfig);

        if (!httpsClientAuthPorts.isEmpty()) {
            return new AuthenticationInfoDTO(isPasswordAuthEnabled, true, httpsClientAuthPorts, message);
        } else {
            return new AuthenticationInfoDTO(isPasswordAuthEnabled, false, null, message);
        }

    }

    private void validatePasswordStrength(final String newPassword) {
        final ValidatorOptions validationOptions = new ValidatorOptions(
                ConfigurationAdminHelper.loadConsoleConfigurationProperties(configAdmin));

        final List<Validator<String>> validators = PasswordStrengthValidators.fromConfig(validationOptions);

        final List<String> errors = new ArrayList<>();

        for (final Validator<String> validator : validators) {
            validator.validate(newPassword, errors::add);

            if (!errors.isEmpty()) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                        "The new password does not satisfy password strenght requirements: " + errors.get(0));
            }
        }
    }

    private AuthenticationResponseDTO buildAuthenticationResponse(final String username) {
        final boolean needsPasswordChange = this.userAdminHelper
                .isPasswordChangeRequired(username);

        return new AuthenticationResponseDTO(needsPasswordChange);
    }

    private void handleAuthenticationException(final AuthenticationException e) {
        final AuditContext auditContext = AuditContext.currentOrInternal();

        switch (e.getReason()) {
            case INCORRECT_PASSWORD:
            case USER_NOT_FOUND:
                auditLogger.warn(AUDIT_FORMAT_STRING, auditContext, BAD_USERNAME_OR_PASSWORD_MESSAGE);
                throw DefaultExceptionHandler.buildWebApplicationException(Status.UNAUTHORIZED,
                        BAD_USERNAME_OR_PASSWORD_MESSAGE);
            case PASSWORD_CHANGE_WITH_SAME_PASSWORD:
                auditLogger.warn(AUDIT_FORMAT_STRING, auditContext, PASSWORD_CHANGE_SAME_PASSWORD_MESSAGE);
                throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                        PASSWORD_CHANGE_SAME_PASSWORD_MESSAGE);
            case USER_NOT_IN_ROLE:
                auditLogger.warn(AUDIT_FORMAT_STRING, auditContext, IDENTITY_NOT_IN_ROLE_MESSAGE);
                throw DefaultExceptionHandler.buildWebApplicationException(Status.FORBIDDEN,
                        IDENTITY_NOT_IN_ROLE_MESSAGE);
            default:
                throw DefaultExceptionHandler.buildWebApplicationException(Status.INTERNAL_SERVER_ERROR,
                        "An internal error occurred");
        }
    }

}
