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

import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;

import com.google.gwt.user.client.rpc.SerializationException;

/**
 * This is the security token service, a concrete implementation to fix the XSFR
 * security problem.
 */
public class GwtSecurityTokenServiceImpl extends OsgiRemoteServiceServlet implements GwtSecurityTokenService {

    /**
     *
     */
    private static final long serialVersionUID = 5333012054583792499L;

    private static ThreadLocal<HttpServletRequest> threadRequest = new ThreadLocal<>();

    static final String XSRF_TOKEN_KEY = "XSRF_TOKEN";

    @Override
    public String processCall(String payload) throws SerializationException {
        try {
            threadRequest.set(getThreadLocalRequest());
            return super.processCall(payload);
        } finally {
            threadRequest.remove();
        }
    }

    public static HttpServletRequest getRequest() {
        return threadRequest.get();
    }

    public HttpSession getHttpSession() {
        HttpServletRequest request = GwtSecurityTokenServiceImpl.getRequest();
        return request.getSession(false);
    }

    @Override
    public GwtXSRFToken generateSecurityToken() {

        HttpServletRequest httpServletRequest = getRequest();

        final HttpSession session = httpServletRequest.getSession(false);

        final Optional<String> existingToken = Optional
                .ofNullable(session.getAttribute(Attributes.XSRF_TOKEN.getValue()))
                .filter(String.class::isInstance).map(String.class::cast);

        if (existingToken.isPresent()) {
            return new GwtXSRFToken(existingToken.get());
        }

        final UUID token = UUID.randomUUID();

        final String asString = token.toString();

        session.setAttribute(Attributes.XSRF_TOKEN.getValue(), asString);

        return new GwtXSRFToken(asString);
    }
}
