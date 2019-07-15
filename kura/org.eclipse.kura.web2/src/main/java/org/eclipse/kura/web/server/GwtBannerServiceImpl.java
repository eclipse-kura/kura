package org.eclipse.kura.web.server;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.ConsoleOptions;
import org.eclipse.kura.web.shared.service.GwtBannerService;


public class GwtBannerServiceImpl extends OsgiRemoteServiceServlet implements GwtBannerService {

    /**
     * 
     */
    private static final long serialVersionUID = 437355039086779658L;

    @Override
    public String getLoginBanner() {
        final ConsoleOptions options = Console.getConsoleOptions();

        if (options.isBannerEnabled()) {
            return options.getBannerContent();
        } else {
            return null;
        }
    }

}
