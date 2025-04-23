/*******************************************************************************
 * Copyright (c) 2017, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.drivers.assets;

import java.util.List;

import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtChannelOperationResult;
import org.eclipse.kura.web.shared.model.GwtChannelRecord;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtDriversAndAssetsInfo;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDriverAndAssetService;
import org.eclipse.kura.web.shared.service.GwtDriverAndAssetServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public final class DriversAndAssetsRPC {

    private DriversAndAssetsRPC() {
    }

    private static final GwtDriverAndAssetServiceAsync gwtDriverAssetService = GWT
            .create(GwtDriverAndAssetService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    public static void loadDriverAndAssetInfo(final Callback<GwtDriversAndAssetsInfo> callback) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken result) {
                gwtDriverAssetService.getDriverAndAssetInfo(result, new AsyncCallback<GwtDriversAndAssetsInfo>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtDriversAndAssetsInfo result) {
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
                gwtDriverAssetService.updateDriverOrAssetConfiguration(result, config, new AsyncCallback<Void>() {

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
                gwtDriverAssetService.createDriverOrAssetConfiguration(result, factoryPid, pid, configuration,
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
                gwtDriverAssetService.deleteDriverOrAssetConfiguration(result, pid, true, new AsyncCallback<Void>() {

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
                gwtDriverAssetService.readAllChannels(result, assetPid, new AsyncCallback<GwtChannelOperationResult>() {

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
                gwtDriverAssetService.createDriverOrAssetConfiguration(result, factoryPid, pid,
                        new AsyncCallback<Void>() {

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
                                        gwtDriverAssetService.getChannelDescriptor(result, pid,
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
                gwtDriverAssetService.write(result, assetPid, records, new AsyncCallback<GwtChannelOperationResult>() {

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
