package org.eclipse.kura.web.session;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SessionLockedSecurityHandler implements SecurityHandler {

    @Override
    public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        final HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        return session.getAttribute(Attributes.LOCKED.getValue()) == null;
    }

}
