/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.SerializationException;

/**
 * This is the security token service, a concrete implementation to fix the XSFR security problem.
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
        Optional<Cookie> cookie = Arrays.stream(httpServletRequest.getCookies())
                .filter(c -> "JSESSIONID".equals(c.getName())).findAny();

        if (!cookie.isPresent() || isNull(cookie.get().getValue()) || cookie.get().getValue().isEmpty()) {
            throw new RpcTokenException("Unable to generate XSRF cookie: the session cookie is not set or empty!");
        }

        final BundleContext context = FrameworkUtil.getBundle(GwtSecurityTokenServiceImpl.class).getBundleContext();
        final ServiceReference<CryptoService> ref = context.getServiceReference(CryptoService.class);
        try {
            CryptoService cryptoService = ServiceLocator.getInstance().getService(ref);
            return new GwtXSRFToken(cryptoService.sha1Hash(cookie.get().getValue()));
        } catch (GwtKuraException | NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RpcTokenException("Unable to generate XSRF cookie: the crypto service is unavailable!");
        } finally {
            context.ungetService(ref);
        }

    }
}