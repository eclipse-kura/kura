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
import java.util.Optional;

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
import org.eclipse.kura.internal.rest.auth.dto.PasswordAuthenticationResponseDTO;
import org.eclipse.kura.internal.rest.auth.dto.UpdatePasswordDTO;
import org.eclipse.kura.internal.rest.auth.dto.UsernamePasswordDTO;
import org.eclipse.kura.internal.rest.auth.dto.XsrfTokenDTO;
import org.eclipse.kura.internal.rest.provider.RestServiceOptions;
import org.eclipse.kura.util.useradmin.UserAdminHelper;
import org.eclipse.kura.util.useradmin.UserAdminHelper.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
@Path(SessionRestServiceConstants.BASE_PATH)
public class SessionRestService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private final UserAdminHelper userAdminHelper;
    private final RestSessionHelper restSessionHelper;
    private RestServiceOptions options;

    public SessionRestService(final UserAdminHelper userAdminHelper, final RestSessionHelper restSessionHelper) {
        this.userAdminHelper = userAdminHelper;
        this.restSessionHelper = restSessionHelper;
    }

    public void setOptions(final RestServiceOptions options) {
        this.options = options;
    }

    @POST
    @Path(SessionRestServiceConstants.LOGIN_PASSWORD_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PasswordAuthenticationResponseDTO authenticateWithUsernameAndPassword(
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

            final boolean needsPasswordChange = this.userAdminHelper
                    .isPasswordChangeRequired(usernamePassword.getUsername());

            if (needsPasswordChange) {
                this.restSessionHelper.lockSession(session);
            }

            auditLogger.info("{} Rest - Success - Create session via password authentication succeeded", auditContext);

            return new PasswordAuthenticationResponseDTO(needsPasswordChange);

        } catch (final AuthenticationException e) {
            handleAuthenticationException(e);
            throw new IllegalStateException("unreachable");
        }
    }

    @POST
    @Path(SessionRestServiceConstants.LOGIN_CERTIFICATE_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public void authenticateWithCertificate(@Context final HttpServletRequest request,
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
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
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
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        if (!this.restSessionHelper.getCurrentPrincipal(requestContext).isPresent()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
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
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            this.userAdminHelper.verifyUsernamePassword(username.get(), passwordUpdate.getCurrentPassword());

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
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        this.restSessionHelper.logout(request, response);

        auditLogger.info("{} Rest - Success - Logout succeeded",
                AuditContext.currentOrInternal());
    }

    private void handleAuthenticationException(final AuthenticationException e) {
        final AuditContext auditContext = AuditContext.currentOrInternal();

        switch (e.getReason()) {
            case INCORRECT_PASSWORD:
                auditLogger.warn("{} Rest - Failure - Authentication failed as username or password not matching",
                        auditContext);
                throw new WebApplicationException(Status.UNAUTHORIZED);
            case PASSWORD_CHANGE_WITH_SAME_PASSWORD:
                auditLogger.warn("{} Rest - Failure - Password change failed as previous password equals new one",
                        auditContext);
                throw new WebApplicationException(Status.BAD_REQUEST);
            case USER_NOT_FOUND:
                auditLogger.warn("{} Rest - Failure - Identity does not exist",
                        auditContext);
                throw new WebApplicationException(Status.UNAUTHORIZED);
            case USER_NOT_IN_ROLE:
                auditLogger.warn("{} Rest - Failure - Identity does not have the required permissions", auditContext);
                throw new WebApplicationException(Status.FORBIDDEN);
            default:
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);

        }
    }

}
