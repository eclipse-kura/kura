/*******************************************************************************
 * Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.UserManager;
import org.eclipse.kura.web.server.RequiredPermissions.Mode;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class KuraRemoteServiceServlet extends RemoteServiceServlet {

    /**
     *
     */
    private static final long serialVersionUID = 3473193315046407200L;
    private static final Logger logger = LoggerFactory.getLogger(KuraRemoteServiceServlet.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    /**
     *
     * If the given xsrfToken is not valid a GwtEdcException will throw.
     * Check if the given xsrfToken is valid, otherwise traces the user network info
     * and invalidates the user session.
     *
     * @param xsrfToken
     * @throws GwtEdcException
     */
    public void checkXSRFToken(GwtXSRFToken xsrfToken) throws GwtKuraException {
        HttpServletRequest req = this.perThreadRequest.get();
        performXSRFTokenValidation(req, xsrfToken);
    }

    /**
     *
     * This method perform a XSRF validation on the given request and for the
     * specific userToken.
     * This is a private method to support both, standard class validation or
     * multipart Servlet validation.
     *
     * @param req
     * @param userToken
     */
    private static void performXSRFTokenValidation(HttpServletRequest req, GwtXSRFToken userToken)
            throws GwtKuraException {
        if (!isValidXSRFToken(req, userToken.getToken())) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, null, "Invalid XSRF token");
        }
    }

    /**
     *
     * Verify if the given userToken is valid on the given session.
     * This method tests if the server xsrf token is equals on the user token.
     * If yes, the method returns true, otherwise returns false.
     * This method controls the xsrf token date validity based on the expire date
     * field.
     *
     * @param session
     * @param userToken
     * @return boolean
     */
    public static boolean isValidXSRFToken(HttpServletRequest req, String userToken) {
        logger.debug("Starting XSRF Token validation...'");

        HttpSession session = req.getSession(false);

        if (session == null) {
            auditLogger.warn("{} UI XSRF - Failure - User is not authenticated", AuditContext.currentOrInternal());
            return false;
        }

        if (userToken == null) {
            auditLogger.warn("{} UI XSRF - Failure - XSRF Token not provided",
                    AuditContext.currentOrInternal());
            session.invalidate();
            return false;
        }

        if (Objects.equals(userToken, session.getAttribute(Attributes.XSRF_TOKEN.getValue()))) {
            return true;
        } else {
            auditLogger.warn("{} UI XSRF - Failure - XSRF Token validation error",
                    AuditContext.currentOrInternal());
            session.invalidate();
            return false;
        }

    }

    public static void requirePermissions(final HttpServletRequest request, final RequiredPermissions.Mode mode,
            final String[] permissions) {
        try {
            requirePermissionsInternal(request, mode, permissions);

        } catch (final KuraPermissionException e) {
            auditLogger.warn("{} UI Auth - Failure - User does not have the required permissions",
                    AuditContext.currentOrInternal());
            throw e;
        }
    }

    private static void requirePermissionsInternal(final HttpServletRequest request,
            final RequiredPermissions.Mode mode,
            final String[] permissions) {

        final HttpSession session = request.getSession(false);

        final UserManager userManager = Console.instance().getUserManager();

        final Object rawUserName = session.getAttribute(Attributes.AUTORIZED_USER.getValue());

        if (!(rawUserName instanceof String)) {
            throw new KuraPermissionException();
        }

        final String userName = (String) rawUserName;

        Optional<GwtUserConfig> config;
        try {
            config = userManager.getUserConfig(userName);
        } catch (KuraException e) {
            throw new KuraPermissionException();
        }

        if (!config.isPresent()) {
            throw new KuraPermissionException();
        }

        if (config.get().isAdmin()) {
            return;
        }

        if (mode == Mode.ALL) {
            if (!containsAll(permissions, config.get().getPermissions())) {
                throw new KuraPermissionException();
            }
        } else {
            if (!containsAny(permissions, config.get().getPermissions())) {
                throw new KuraPermissionException();
            }
        }

    }

    private static boolean containsAll(final String[] required, final Set<String> actual) {
        for (final String req : required) {
            if (!actual.contains(req)) {
                return false;
            }
        }

        return true;
    }

    private static boolean containsAny(final String[] required, final Set<String> actual) {
        for (final String req : required) {
            if (actual.contains(req)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * Get the given value field from the Servlet request if exist.
     * Returns null if the passed fieldName isn't present in the request.
     *
     * @param req
     * @param fieldName
     * @return String
     * @throws FileUploadException
     */
    public static String getFieldFromMultiPartForm(HttpServletRequest req, String fieldName)
            throws FileUploadException {
        String fieldValue = null;

        ServletFileUpload upload = new ServletFileUpload();
        List<FileItem> items = upload.parseRequest(req);

        // Process the uploaded items
        Iterator<FileItem> iter = items.iterator();
        while (iter.hasNext()) {
            FileItem item = iter.next();
            if (item.isFormField()) {
                String name = item.getFieldName();

                if (name.equals(fieldName)) {
                    fieldValue = item.getString();
                    logger.debug("Found field name '{}' with value: {}", name, fieldValue);
                }
            }
        }

        return fieldValue;
    }

    public static void checkXSRFToken(HttpServletRequest req, GwtXSRFToken token) throws GwtKuraException {
        performXSRFTokenValidation(req, token);
    }

    /**
     *
     * Check if the given xsrfToken is valid, otherwise traces the user network info
     * and invalidates the user session.
     * This is the checkXSRFToken for the MultiPart Servlet support.
     *
     * @param req
     * @throws GwtKuraException
     */
    public static void checkXSRFTokenMultiPart(HttpServletRequest req, GwtXSRFToken token) throws GwtKuraException {
        performXSRFTokenValidation(req, token);
    }

    @Override
    protected void doUnexpectedFailure(Throwable e) {
        if (e instanceof KuraPermissionException) {
            try {
                getThreadLocalResponse().sendError(403);
                return;
            } catch (IOException e1) {
                // ignore
            }
        }
        super.doUnexpectedFailure(e);
    }

    public static class KuraPermissionException extends RuntimeException {

        private static final long serialVersionUID = 7782509676228955785L;

    }
}
