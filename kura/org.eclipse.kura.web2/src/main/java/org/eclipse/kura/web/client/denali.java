/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtClientExtensionBundle;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtExtensionService;
import org.eclipse.kura.web.shared.service.GwtExtensionServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSessionService;
import org.eclipse.kura.web.shared.service.GwtSessionServiceAsync;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class denali implements EntryPoint {

    Logger logger = Logger.getLogger(denali.class.getSimpleName());
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);
    private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);
    private final GwtExtensionServiceAsync gwtExtensionService = GWT.create(GwtExtensionService.class);
    private final GwtSessionServiceAsync gwtSessionService = GWT.create(GwtSessionService.class);

    @Override
    public void onModuleLoad() {
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onSuccess(final GwtXSRFToken token) {
                gwtSessionService.getUserConfig(token, new AsyncCallback<GwtUserConfig>() {

                    @Override
                    public void onSuccess(final GwtUserConfig result) {
                        final EntryClassUi entryClassUi = new EntryClassUi(result);
                        RootPanel.get().add(entryClassUi);
                        init(entryClassUi);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        logger.warning("failed to get user info" + caught);
                    }
                });
            }

            @Override
            public void onFailure(Throwable caught) {
                logger.warning("failed to get token" + caught);
            }
        });

    }

    /**
     * Note, we defer all application initialization code to
     * {@link #onModuleLoad2()} so that the UncaughtExceptionHandler can catch
     * any unexpected exceptions.
     */

    public void init(final EntryClassUi entryClassUi) {
        RootPanel.get().add(entryClassUi);

        this.gwtExtensionService.getConsoleExtensions(new AsyncCallback<List<GwtClientExtensionBundle>>() {

            @Override
            public void onFailure(Throwable caught) {
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(List<GwtClientExtensionBundle> result) {

                for (final GwtClientExtensionBundle extension : result) {
                    ScriptInjector.fromUrl(extension.getEntryPointUrl()).inject();
                }
            }
        });

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex, denali.class.getSimpleName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                denali.this.gwtDeviceService.findSystemProperties(token, new AsyncCallback<List<GwtGroupedNVPair>>() {

                    @Override
                    public void onSuccess(List<GwtGroupedNVPair> results) {

                        final GwtSession gwtSession = new GwtSession();

                        if (results != null) {
                            List<GwtGroupedNVPair> pairs = results;
                            pairs.forEach(pair -> {
                                String name = pair.getName();
                                if ("kura.have.net.admin".equals(name)) {
                                    Boolean value = Boolean.valueOf(pair.getValue());
                                    gwtSession.setNetAdminAvailable(value);
                                }
                                if ("kura.version".equals(name)) {
                                    gwtSession.setKuraVersion(pair.getValue());
                                }
                                if ("kura.os.version".equals(name)) {
                                    gwtSession.setOsVersion(pair.getValue());
                                }
                            });
                        }

                        denali.this.gwtSecurityService.isDebugMode(new AsyncCallback<Boolean>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                FailureHandler.handle(caught, denali.class.getSimpleName());
                                entryClassUi.setFooter(gwtSession);
                                entryClassUi.initSystemPanel(gwtSession);
                                entryClassUi.setSession(gwtSession);
                                entryClassUi.init();
                            }

                            @Override
                            public void onSuccess(Boolean result) {
                                if (result) {
                                    gwtSession.setDevelopMode(true);
                                }
                                entryClassUi.setFooter(gwtSession);
                                entryClassUi.initSystemPanel(gwtSession);
                                entryClassUi.setSession(gwtSession);
                                entryClassUi.init();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        FailureHandler.handle(caught, denali.class.getSimpleName());
                        entryClassUi.setFooter(new GwtSession());
                        entryClassUi.initSystemPanel(new GwtSession());
                        entryClassUi.setSession(new GwtSession());
                    }
                });
            }
        });
    }

}
