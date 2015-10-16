package org.eclipse.kura.web.server;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.client.rpc.SerializationException;

/**
 * This is the security token service, a concrete implementation to fix the XSFR security problem.
 */
public class GwtSecurityTokenServiceImpl extends OsgiRemoteServiceServlet implements GwtSecurityTokenService {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 5333012054583792499L;

	private static ThreadLocal<HttpServletRequest> perThreadRequest = new ThreadLocal<HttpServletRequest>();

    public static Logger s_logger = LoggerFactory.getLogger(GwtSecurityTokenServiceImpl.class);
    public static final String XSRF_TOKEN_KEY = "XSRF_TOKEN";

    @Override
    public String processCall(String payload) throws SerializationException {
        try {
            perThreadRequest.set(getThreadLocalRequest());
            return super.processCall(payload);
        }
        finally {
            perThreadRequest.set(null);
        }
    }

    public static HttpServletRequest getRequest() {
        return perThreadRequest.get();
    }

    public HttpSession getHttpSession() {
        HttpServletRequest request = GwtSecurityTokenServiceImpl.getRequest();
        return request.getSession();
    }

    @Override
    public GwtXSRFToken generateSecurityToken() {
        GwtXSRFToken token = null;

        // Before to generate a token we must to check if the user is correctly authenticated
        HttpSession session = getHttpSession(); 
        if (session != null) {
            token = new GwtXSRFToken(UUID.randomUUID().toString());
            session.setAttribute(XSRF_TOKEN_KEY, token);

            s_logger.debug("Generated XSRF token: {} for HTTP session: {}", token.getToken(), session.getId());
        }
        return token;
    }
}