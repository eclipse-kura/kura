package org.eclipse.kura.web.shared.service;

import java.util.ArrayList;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtDeviceScannerModel;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("devicescanner")
public interface GwtDeviceScanner {

    public ArrayList<GwtDeviceScannerModel> findDeviceScanner(GwtXSRFToken xsrfToken, boolean hasNetAdmin)
            throws GwtKuraException;

}
