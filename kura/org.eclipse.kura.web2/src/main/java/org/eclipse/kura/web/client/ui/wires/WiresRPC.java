package org.eclipse.kura.web.client.ui.wires;

import java.util.List;

import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtWireComposerStaticInfo;
import org.eclipse.kura.web.shared.model.GwtWireGraphConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public final class WiresRPC {

    private static final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
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
            final List<GwtConfigComponent> additionalConfigs, final Callback<Void> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtWireService.updateWireConfiguration(result, wireGraph, additionalConfigs, new AsyncCallback<Void>() {

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

    public static void createNewDriver(final String factoryPid, final String pid,
            final Callback<GwtConfigComponent> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtComponentService.createFactoryComponent(result, factoryPid, pid, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(Void result) {
                        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                            }

                            @Override
                            public void onSuccess(GwtXSRFToken result) {
                                gwtWireService.getGwtChannelDescriptor(result, pid,
                                        new AsyncCallback<GwtConfigComponent>() {

                                            @Override
                                            public void onFailure(Throwable ex) {
                                                EntryClassUi.hideWaitModal();
                                                FailureHandler.handle(ex);
                                            }

                                            @Override
                                            public void onSuccess(GwtConfigComponent result) {
                                                EntryClassUi.hideWaitModal();
                                                callback.onSuccess(result);
                                            }
                                        });
                            }
                        });
                    }
                });
            }
        });
    }

    public static interface Callback<T> {

        public void onSuccess(T result);
    }

}
