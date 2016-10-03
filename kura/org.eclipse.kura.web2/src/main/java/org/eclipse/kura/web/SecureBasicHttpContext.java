/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web;

import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpContext that delegates calls to getMimeType() and getResource(), but provides
 * HTTP Basic authentication by authenticating a user against an LDAP directory. For
 * the request to be accepted, the user has to be a member of a privileged group.
 */
public class SecureBasicHttpContext implements HttpContext {

    // Logger
    private static Logger s_logger = LoggerFactory.getLogger(SecureBasicHttpContext.class);

    /**
     * The HttpContext where we delegate calls to getResource() and getMimeType()
     */
    private final HttpContext delegate;
    private final AuthenticationManager m_authMgr;
    private final String m_appRoot;

    public SecureBasicHttpContext(HttpContext delegate, AuthenticationManager authMgr) {
        this.delegate = delegate;
        this.m_authMgr = authMgr;
        this.m_appRoot = Console.getApplicationRoot();
    }

    @Override
    public String getMimeType(String name) {
        return this.delegate.getMimeType(name);
    }

    @Override
    public URL getResource(String name) {
        return this.delegate.getResource(name);
    }

    /**
     * Provides Basic authentication over HTTPS.
     */
    @Override
    public synchronized boolean handleSecurity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setHeader("X-FRAME-OPTIONS", "SAMEORIGIN");
        response.setHeader("X-XSS-protection", "1; mode=block");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-cache,no-store");
        response.setHeader("Pragma", "no-cache");

        // If a trailing "/" is used when accesssing the app, redirect
        if (request.getRequestURI().equals(this.m_appRoot + "/")) {
            response.sendRedirect(this.m_appRoot);
        }

        // If using root context, redirect
        if (request.getRequestURI().equals("/")) {
            response.sendRedirect(this.m_appRoot);
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            String logout = (String) session.getAttribute("logout");
            if (logout != null) {
                session.removeAttribute("logout");
                session.invalidate();
                return failAuthorization(response);
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            s_logger.debug("Missing 'Authorization' HTTP header");
            return failAuthorization(response);
        }

        StringTokenizer tokens = new StringTokenizer(authHeader);
        String authScheme = tokens.nextToken();
        if (!"Basic".equals(authScheme)) {
            s_logger.error("The authentication scheme is not 'Basic'");
            return failAuthorization(response);
        }

        String base64 = tokens.nextToken();
        String credentials = null;
        try {
            CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
            credentials = cryptoService.decodeBase64(base64);
        } catch (GwtKuraException e) {
            throw new IOException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e.getMessage());
        }

        int colon = credentials.indexOf(':');
        String userid = credentials.substring(0, colon);
        String password = credentials.substring(colon + 1);

        Subject subject = login(request, response, userid, password);
        if (subject == null) {
            return failAuthorization(response);
        }

        request.setAttribute(HttpContext.REMOTE_USER, null);
        request.setAttribute(HttpContext.AUTHENTICATION_TYPE, request.getAuthType());
        request.setAttribute(HttpContext.AUTHORIZATION, null);

        return true;
    }

    /**
     * Sets the correct HTTP Header to indicate that a request has failed
     *
     * @param response
     * @return always false
     */
    private boolean failAuthorization(HttpServletResponse response) {
        response.setHeader("WWW-Authenticate", "Basic realm=\"Secure Area\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    /**
     * Authenticates a user against an LDAP directory if he does not have an active session.
     *
     * @param request
     * @param response
     * @param userid
     * @param password
     * @return A Subject Object if the credentials are valid, null otherwise.
     */
    private Subject login(HttpServletRequest request, HttpServletResponse response, String userid, String password) {
        Subject subject = null;
        HttpSession session = request.getSession(true);
        // subject = (Subject) session.getAttribute("subject");
        // if (subject != null) {
        // // The user has already been authenticated
        // return subject;
        // }

        subject = authorize(userid, password);
        session.setAttribute("subject", subject);
        session.setAttribute("username", userid);
        if (session.isNew()) {
            String sessionid = session.getId();
            response.setHeader("SET-COOKIE", "JSESSIONID=" + sessionid + "; HttpOnly");  // TODO: this response header
  // is highly discouraged. Find
  // a better solution (that
  // probably will require a new
  // version of Jetty). See here:
  // https://www.owasp.org/index.php/HttpOnly#Using_Java_to_Set_HttpOnly
        }
        return subject;

    }

    /**
     * Authenticates a user against an SQL DB. The credentials have to be
     * valid and the user has to be a member of a privileged group in order for him
     * to be authorized
     *
     * @param userid
     * @param password
     * @return An empty Subject if the user is authorized, null otherwise.
     */
    private Subject authorize(String userid, String password) {
        s_logger.debug("Authenticating user [{}]", userid);
        try {
            if (this.m_authMgr.authenticate(userid, password)) {
                // TODO : We are temporarily returning an empty Subject,
                // but we should return something more significant
                return new Subject();
            }
        } catch (Exception e) {
            s_logger.error("Error during authentication", e);
        }

        return null;
    }
}
