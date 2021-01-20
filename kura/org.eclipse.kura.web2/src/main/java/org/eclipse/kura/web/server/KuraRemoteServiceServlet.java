/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

import static java.util.Objects.isNull;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.client.rpc.RpcTokenException;
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
     * Check if the given xsrfToken is valid, otherwise traces the user network info and invalidates the user session.
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
     * This method perform a XSRF validation on the given request and for the specific userToken.
     * This is a private method to support both, standard class validation or multipart Servlet validation.
     *
     * @param req
     * @param userToken
     */
    private static void performXSRFTokenValidation(HttpServletRequest req, GwtXSRFToken userToken)
            throws GwtKuraException {
        HttpSession session = req.getSession(false);

        if (!isValidXSRFToken(req, userToken.getToken())) {
            logger.info("XSRF token is NOT VALID");

            logger.info("Invalid User Token={}", userToken.getToken());
            logger.debug("\tSender IP: {}", req.getRemoteAddr());
            logger.debug("\tSender Host: {}", req.getRemoteHost());
            logger.debug("\tSender Port: {}", req.getRemotePort());
            logger.debug("\tFull Request URL\n {}?{}\n\n", req.getRequestURL(), req.getQueryString());

            // forcing the console log out
            session.invalidate();
            logger.debug("Session invalidated.");

            auditLogger.warn("UI XSRF - Failure - XSRF Token validation error for user: {}, session {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, null, "Invalid XSRF token");
        }
    }

    /**
     *
     * Verify if the given userToken is valid on the given session.
     * This method tests if the server xsrf token is equals on the user token.
     * If yes, the method returns true, otherwise returns false.
     * This method controls the xsrf token date validity based on the expire date field.
     *
     * @param session
     * @param userToken
     * @return boolean
     */
    public static boolean isValidXSRFToken(HttpServletRequest req, String userToken) {
        logger.debug("Starting XSRF Token validation...'");

        if (userToken == null) {
            logger.debug("XSRF Token is NOT VALID -> NULL TOKEN");
            return false;
        }

        Optional<Cookie> cookie = Arrays.stream(req.getCookies()).filter(c -> "JSESSIONID".equals(c.getName()))
                .findAny();

        if (!cookie.isPresent() || isNull(cookie.get().getValue()) || cookie.get().getValue().isEmpty()) {
            throw new RpcTokenException("Unable to generate XSRF cookie: the session cookie is not set or empty!");
        }

        String serverXSRFToken = null;
        final BundleContext context = FrameworkUtil.getBundle(GwtSecurityTokenServiceImpl.class).getBundleContext();
        final ServiceReference<CryptoService> ref = context.getServiceReference(CryptoService.class);
        try {
            CryptoService cryptoService = ServiceLocator.getInstance().getService(ref);
            serverXSRFToken = cryptoService.sha1Hash(cookie.get().getValue());
        } catch (GwtKuraException | NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RpcTokenException("Unable to verify the XSRF token: the crypto service is unavailable!");
        } finally {
            context.ungetService(ref);
        }

        if (!isNull(serverXSRFToken) && serverXSRFToken.equals(userToken)) {
            return true;
        }

        logger.debug("XSRF Token is NOT VALID - {}", userToken);
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
    public static String getFieldFromMultiPartForm(HttpServletRequest req, String fieldName) throws FileUploadException {
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
     * Check if the given xsrfToken is valid, otherwise traces the user network info and invalidates the user session.
     * This is the checkXSRFToken for the MultiPart Servlet support.
     *
     * @param req
     * @throws GwtKuraException 
     */
    public static void checkXSRFTokenMultiPart(HttpServletRequest req, GwtXSRFToken token) throws GwtKuraException  {
        performXSRFTokenValidation(req, token);
    }

}