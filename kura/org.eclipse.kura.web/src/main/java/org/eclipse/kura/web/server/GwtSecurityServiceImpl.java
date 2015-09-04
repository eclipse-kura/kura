package org.eclipse.kura.web.server;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.security.SecurityService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.service.GwtSecurityService;

public class GwtSecurityServiceImpl extends OsgiRemoteServiceServlet implements GwtSecurityService{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7664408886756367054L;

	@Override
	public Boolean isSecurityServiceAvailable() {
		SecurityService securityService;
		
		try {
			securityService = ServiceLocator.getInstance().getService(SecurityService.class);
			if(securityService == null){
				return false;
			}
		} catch (GwtKuraException e) {
			return false;
		}
		return true;
	}
	
	@Override
	public void reloadSecurityPolicy() throws GwtKuraException {
		SecurityService securityService = ServiceLocator.getInstance().getService(SecurityService.class);
		try {
			securityService.reloadSecurityPolicyFingerprint();
		} catch (KuraException e) {
			throw new GwtKuraException(e.getMessage());
		}
	}
}
