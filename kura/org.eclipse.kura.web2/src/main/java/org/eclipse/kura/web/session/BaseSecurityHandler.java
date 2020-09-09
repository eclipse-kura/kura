/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.session;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseSecurityHandler implements SecurityHandler {

    private static final Logger logger = LoggerFactory.getLogger(BaseSecurityHandler.class);

    @Override
    public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        response.setHeader("X-FRAME-OPTIONS", "SAMEORIGIN");
        response.setHeader("X-XSS-protection", "1; mode=block");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-cache,no-store,max-age=0,must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Security-Policy", "default-src 'self' 'nonce-YmOhe4LWmI1czapJnE2D3UOoYbPthMkM' 'nonce-3TicouQMegMuj4ZQN6HvjNPo8BGiijJF' 'nonce-MxFCwIVsMGEul9B2RHTa7duCrD2YT3nA' 'nonce-hsaWBVY3TvVTKAUfhyQPdWXzhXOsl7rQ' 'nonce-Qgt9F8AWPOPcanFX064DolVI0VqdAVgh' 'sha256-+EAvjC+lrPs2ZhgImb+lNQ7yJG8Xerz5tP8OlHkQXBM=' 'sha256-4lbaG/yI+Yas40MYds60CXl+zg9DGuBKbYRCXZwNZVo='; script-src-elem 'self' 'unsafe-inline'");

        fixTrailingSlashes(request, response);

        return true;
    }

    private static void fixTrailingSlashes(final HttpServletRequest request, final HttpServletResponse response) {

        final String path = request.getRequestURI();

        if (!path.endsWith("/") || "/".contentEquals(path) || path.isEmpty()) {
            return;
        }

        int end = path.length() - 1;
        for (; end > 1 && path.charAt(end) == '/'; end--) {
            ;
        }

        try {
            response.sendRedirect(path.substring(0, end + 1));
        } catch (IOException e) {
            logger.warn("unexpected exception", e);
        }
    }
}
