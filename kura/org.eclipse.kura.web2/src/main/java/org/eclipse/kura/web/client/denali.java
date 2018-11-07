/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtStatusService;
import org.eclipse.kura.web.shared.service.GwtStatusServiceAsync;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class denali implements EntryPoint {

    private static final Messages MSGS = GWT.create(Messages.class);
    Logger logger = Logger.getLogger(denali.class.getSimpleName());
    private final GwtStatusServiceAsync gwtStatusService = GWT.create(GwtStatusService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);
    private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);

    private final EntryClassUi binder = GWT.create(EntryClassUi.class);

    private boolean isDevelopMode;
    private boolean connected;

    /**
     * Note, we defer all application initialization code to
     * {@link #onModuleLoad2()} so that the UncaughtExceptionHandler can catch
     * any unexpected exceptions.
     */
    @Override
    public void onModuleLoad() {
        // use deferred command to catch initialization exceptions in
        // onModuleLoad2
        Scheduler.get().scheduleDeferred(() -> onModuleLoad2());
    }

    /**
     * This is the 'real' entry point method.
     */
    public void onModuleLoad2() {

        RootPanel.get().add(this.binder);

        // load custom CSS/JS
        loadCss("denali/skin/skin.css");
        ScriptInjector.fromUrl("denali/skin/skin.js?v=1").inject(); // Make sure this request is not cached

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex, denali.class.getSimpleName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                denali.this.gwtDeviceService.findSystemProperties(token,
                        new AsyncCallback<ArrayList<GwtGroupedNVPair>>() {

                            @Override
                            public void onSuccess(ArrayList<GwtGroupedNVPair> results) {

                                final GwtSession gwtSession = new GwtSession();

                                if (results != null) {
                                    List<GwtGroupedNVPair> pairs = results;
                                    for (GwtGroupedNVPair pair : pairs) {
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
                                    }
                                }

                                denali.this.gwtSecurityService.isDebugMode(new AsyncCallback<Boolean>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        FailureHandler.handle(caught, denali.class.getSimpleName());
                                        denali.this.binder.setFooter(gwtSession);
                                        denali.this.binder.initSystemPanel(gwtSession);
                                        denali.this.binder.setSession(gwtSession);
                                        denali.this.binder.init();
                                    }

                                    @Override
                                    public void onSuccess(Boolean result) {
                                        if (result) {
                                            denali.this.isDevelopMode = true;
                                            gwtSession.setDevelopMode(true);
                                        }
                                        denali.this.binder.setFooter(gwtSession);
                                        denali.this.binder.initSystemPanel(gwtSession);
                                        denali.this.binder.setSession(gwtSession);
                                        denali.this.binder.init();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                FailureHandler.handle(caught, denali.class.getSimpleName());
                                denali.this.binder.setFooter(new GwtSession());
                                denali.this.binder.initSystemPanel(new GwtSession());
                                denali.this.binder.setSession(new GwtSession());
                            }
                        });
            }
        });
    }

    private static native void loadCss(String url) /*-{
                                                   var l = $doc.createElement("link");
                                                   l.setAttribute("id", url);
                                                   l.setAttribute("rel", "stylesheet");
                                                   l.setAttribute("type", "text/css");
                                                   l.setAttribute("href", url + "?v=1"); // Make sure this request is not cached
                                                   $doc.getElementsByTagName("head")[0].appendChild(l);
                                                   }-*/;

}
