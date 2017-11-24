package org.eclipse.kura.web.client.ui.wires;

import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtWireComposerStaticInfo;
import org.eclipse.kura.web.shared.model.GwtWireGraphConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public final class WiresRPC {

    private static final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private WiresRPC() {
    }

    public static void loadStaticInfo(final Callback<GwtWireComposerStaticInfo> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtWireService.getWireComposerStaticInfo(result, new AsyncCallback<GwtWireComposerStaticInfo>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtWireComposerStaticInfo result) {
                        EntryClassUi.hideWaitModal();
                        callback.onSuccess(result);
                    }
                });
            }
        });
    }

    public static void loadWiresConfiguration(final Callback<GwtWireGraphConfiguration> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtWireService.getWiresConfiguration(result, new AsyncCallback<GwtWireGraphConfiguration>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtWireGraphConfiguration result) {
                        EntryClassUi.hideWaitModal();
                        callback.onSuccess(result);
                    }
                });
            }
        });
    }

    public static void updateWiresConfiguration(final GwtWireGraphConfiguration wireGraph,
            final Callback<Void> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtWireService.updateWireConfiguration(result, wireGraph, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(Void result) {
                        EntryClassUi.hideWaitModal();
                        callback.onSuccess(null);
                    }
                });
            }
        });
    }

    public static interface Callback<T> {

        public void onSuccess(T result);
    }

}
