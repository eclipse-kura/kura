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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.security.SecurityService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtSecurityServiceImpl extends OsgiRemoteServiceServlet implements GwtSecurityService {

    /**
     *
     */
    private static final long serialVersionUID = -7664408886756367054L;

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    @Override
    public Boolean isSecurityServiceAvailable() {
        SecurityService securityService;

        try {
            securityService = ServiceLocator.getInstance().getService(SecurityService.class);
            if (securityService == null) {
                return false;
            }
        } catch (GwtKuraException e) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean isDebugMode() {
        SecurityService securityService;

        try {
            securityService = ServiceLocator.getInstance().getService(SecurityService.class);
            if (securityService != null) {
                return securityService.isDebugEnabled();
            }
        } catch (GwtKuraException e) {
            // Nothing to do
        }
        return false;
    }

    @Override
    public void reloadSecurityPolicyFingerprint(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        SecurityService securityService = ServiceLocator.getInstance().getService(SecurityService.class);
        try {
            securityService.reloadSecurityPolicyFingerprint();
        } catch (KuraException e) {
            auditLogger.warn(
                    "UI Security - Failure - Failed to reload security policy fingerprint for user: {}, session: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(e.getMessage());
        }
        auditLogger.info(
                "UI Security - Success - Successfully reloaded security policy fingerprint for user: {}, session: {}",
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
    }

    @Override
    public void reloadCommandLineFingerprint(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        SecurityService securityService = ServiceLocator.getInstance().getService(SecurityService.class);
        try {
            securityService.reloadCommandLineFingerprint();
        } catch (KuraException e) {
            auditLogger.warn(
                    "UI Security - Failure - Failed to reload command line fingerprint for user: {}, session: {}",
                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
            throw new GwtKuraException(e.getMessage());
        }
        auditLogger.info(
                "UI Security - Success - Successfully reloaded command line fingerprint for user: {}, session: {}",
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId());
    }
}
