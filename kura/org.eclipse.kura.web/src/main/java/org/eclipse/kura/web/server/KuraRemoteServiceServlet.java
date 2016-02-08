/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class KuraRemoteServiceServlet extends RemoteServiceServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 3473193315046407200L;
	private static Logger     s_logger         = LoggerFactory.getLogger(KuraRemoteServiceServlet.class);

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
    static private void performXSRFTokenValidation(HttpServletRequest req, GwtXSRFToken userToken) throws GwtKuraException {
        HttpSession session = req.getSession();

        if (!isValidXSRFToken(session, userToken)) {
            if (session != null) {
                s_logger.info("XSRF token is NOT VALID - Token={}", userToken.getToken());
                s_logger.debug("\tSender IP: {}", req.getRemoteAddr());
                s_logger.debug("\tSender Host: {}", req.getRemoteHost());
                s_logger.debug("\tSender Port: {}", req.getRemotePort());
                s_logger.debug("\tFull Request URL\n {}?{}\n\n",
                               req.getRequestURL().toString(),
                               req.getQueryString());
            }

            // forcing the console log out
            session.invalidate();
            s_logger.debug("Session invalidated.");

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
    static public boolean isValidXSRFToken(HttpSession session, GwtXSRFToken userToken) {
        s_logger.debug("Starting XSRF Token validation...'");

        if (userToken == null) {
            s_logger.debug("XSRF Token is NOT VALID -> NULL TOKEN");
            return false;
        }

        // Retrieve the server side token
        GwtXSRFToken serverXSRFToken = (GwtXSRFToken) session.getAttribute(GwtSecurityTokenServiceImpl.XSRF_TOKEN_KEY);
        if (serverXSRFToken != null) {
            String serverToken = serverXSRFToken.getToken();

            // Checking the XSRF validity on the serverToken
            if (isValidStringToken(serverToken)) {
                if (isValidStringToken(userToken.getToken())) {
                    if (serverToken.equals(userToken.getToken())) {
                        // Checking expire date
                        if (new Date().before(userToken.getExpiresOn()))
                        {
                            s_logger.debug("XSRF Token is VALID - {}", userToken.getToken());

                            // Reset used token
                            session.setAttribute(GwtSecurityTokenServiceImpl.XSRF_TOKEN_KEY, null);
                            return true;
                        }
                        else {
                            session.setAttribute(GwtSecurityTokenServiceImpl.XSRF_TOKEN_KEY, null);
                            s_logger.error("XSRF Token is EXPIRED - {}", userToken.getToken());
                        }
                    }
                }
            }
        }

        s_logger.debug("XSRF Token is NOT VALID - {}", userToken.getToken());
        return false;
    }

    /**
     * 
     * Performs some basic string validation on the given String token.
     * 
     * @param token
     * @return boolean
     */
    static private boolean isValidStringToken(String token) {
        if (token != null) {
            if (!token.isEmpty())
                return true;
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
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    static public String getFieldFromMultiPartForm(HttpServletRequest req, String fieldName) throws Exception {
        String fieldValue = null;

        ServletFileUpload upload = new ServletFileUpload();
        List<FileItem> items = upload.parseRequest(req);

        // Process the uploaded items
        Iterator<FileItem> iter = items.iterator();
        while (iter.hasNext()) {
            FileItem item = (FileItem) iter.next();
            if (item.isFormField()) {
                String name = item.getFieldName();

                if (name.equals(fieldName)) {
                    fieldValue = item.getString();
                    s_logger.debug("Found field name '{}' with value: {}", name, fieldValue);
                }
            }
        }

        return fieldValue;
    }

    static public void checkXSRFToken(HttpServletRequest req, GwtXSRFToken token) throws Exception {
        performXSRFTokenValidation(req, token);
    }

    /**
     * 
     * Check if the given xsrfToken is valid, otherwise traces the user network info and invalidates the user session.
     * This is the checkXSRFToken for the MultiPart Servlet support.
     * 
     * @param req
     * @throws Exception
     */
    static public void checkXSRFTokenMultiPart(HttpServletRequest req, GwtXSRFToken token)
        throws Exception
    {
        performXSRFTokenValidation(req, token);
    }

}