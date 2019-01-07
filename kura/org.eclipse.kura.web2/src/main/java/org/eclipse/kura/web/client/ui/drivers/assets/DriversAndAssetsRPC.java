/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.drivers.assets;

import java.util.List;

import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtChannelOperationResult;
import org.eclipse.kura.web.shared.model.GwtChannelRecord;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtWireComposerStaticInfo;
import org.eclipse.kura.web.shared.model.GwtWireGraph;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtAssetService;
import org.eclipse.kura.web.shared.service.GwtAssetServiceAsync;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireGraphService;
import org.eclipse.kura.web.shared.service.GwtWireGraphServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public final class DriversAndAssetsRPC {

    private DriversAndAssetsRPC() {
    }

    private static final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private static final GwtAssetServiceAsync gwtAssetService = GWT.create(GwtAssetService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private static final GwtWireGraphServiceAsync gwtWireGraphService = GWT.create(GwtWireGraphService.class);

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
                gwtWireGraphService.getWireComposerStaticInfo(result, new AsyncCallback<GwtWireComposerStaticInfo>() {

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

    public static void loadWireGraph(final Callback<GwtWireGraph> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtWireGraphService.getWireGraph(result, new AsyncCallback<GwtWireGraph>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtWireGraph result) {
                        EntryClassUi.hideWaitModal();
                        callback.onSuccess(result);
                    }
                });
            }
        });
    }

    public static void updateConfiguration(final GwtConfigComponent config, final Callback<Void> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtComponentService.updateComponentConfiguration(result, config, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(Void result) {
                        EntryClassUi.hideWaitModal();
                        callback.onSuccess(result);
                    }
                });
            }
        });
    }

    public static void createFactoryConfiguration(final String pid, final String factoryPid,
            final GwtConfigComponent configuration, final Callback<Void> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtComponentService.createFactoryComponent(result, factoryPid, pid, configuration,
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                            }

                            @Override
                            public void onSuccess(Void result) {
                                EntryClassUi.hideWaitModal();
                                callback.onSuccess(result);
                            }
                        });
            }
        });
    }

    public static void deleteFactoryConfiguration(final String pid, final Callback<Void> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtComponentService.deleteFactoryConfiguration(result, pid, true, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(Void result) {
                        EntryClassUi.hideWaitModal();
                        callback.onSuccess(result);
                    }
                });
            }
        });
    }

    public static void readAllChannels(final String assetPid, final Callback<GwtChannelOperationResult> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtAssetService.readAllChannels(result, assetPid, new AsyncCallback<GwtChannelOperationResult>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtChannelOperationResult result) {
                        EntryClassUi.hideWaitModal();
                        callback.onSuccess(result);
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
                                gwtWireGraphService.getGwtChannelDescriptor(result, pid,
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

    public static void write(final String assetPid, final List<GwtChannelRecord> records,
            final Callback<GwtChannelOperationResult> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtAssetService.write(result, assetPid, records, new AsyncCallback<GwtChannelOperationResult>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(final GwtChannelOperationResult result) {
                        EntryClassUi.hideWaitModal();
                        callback.onSuccess(result);
                    }
                });
            }
        });
    }

    public static interface Callback<T> {

        public void onSuccess(T result);
    }
}
