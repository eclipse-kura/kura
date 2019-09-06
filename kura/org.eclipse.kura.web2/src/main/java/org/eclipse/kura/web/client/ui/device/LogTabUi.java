/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.device;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormLabel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Widget;

public class LogTabUi extends Composite {

    private static LogTabUiUiBinder uiBinder = GWT.create(LogTabUiUiBinder.class);

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final String SERVLET_URL = Console.ADMIN_ROOT + '/' + GWT.getModuleName() + "/log";

    @SuppressWarnings("unused")
    private GwtSession session;

    interface LogTabUiUiBinder extends UiBinder<Widget, LogTabUi> {
    }

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    @UiField
    Button execute;
    @UiField
    FormPanel logForm;
    @UiField
    FormLabel logLabel;

    public LogTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        
        logLabel.setText(MSGS.logDownload());

        this.execute.setText(MSGS.download());
        this.execute.addClickHandler(
                event -> LogTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        LogTabUi.this.logForm.submit();
                    }
                }));

        this.logForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        this.logForm.setMethod(FormPanel.METHOD_GET);
        this.logForm.setAction(SERVLET_URL);

    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

}
